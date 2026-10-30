package com.lavinuela.shop;

import static org.assertj.core.api.Assertions.assertThat;

import com.lavinuela.shop.notifications.domain.model.EmailMessage;
import com.lavinuela.shop.testsupport.MutableClock;
import com.lavinuela.shop.testsupport.RecordingEmailSender;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * El registro entero, de punta a punta: HTTP, casos de uso, MySQL real y correo (que se recoge en memoria, no se envía).
 * Es la prueba que comprueba lo que los repositorios en memoria no pueden: que los intentos fallidos se guardan aunque
 * la petición termine en error, que en la base de datos nunca hay contraseñas ni códigos en claro, y que las
 * respuestas HTTP tienen la forma esperada.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(RegistrationFlowTests.TestBeans.class)
@RequiresDocker
class RegistrationFlowTests {

    private static final String PASSWORD = "Una-contraseña-larga-2026";
    private static final HttpClient CLIENT = HttpClient.newHttpClient();
    private static final Pattern SIX_DIGITS = Pattern.compile("\\b(\\d{6})\\b");

    /**
     * Los métodos no se llaman {@code clock} ni {@code emailSender}: la aplicación ya tiene un bean con el nombre
     * {@code clock} y Spring no deja que dos beans compartan nombre.
     */
    @TestConfiguration
    static class TestBeans {

        @Bean
        @Primary
        MutableClock testClock() {
            return new MutableClock(Instant.now());
        }

        @Bean
        @Primary
        RecordingEmailSender testEmailSender() {
            return new RecordingEmailSender();
        }
    }

    private record Response(int status, String body) {
    }

    private final JdbcTemplate jdbc;
    private final MutableClock clock;
    private final RecordingEmailSender emails;

    @Value("${local.server.port}")
    private int port;

    @Autowired
    RegistrationFlowTests(JdbcTemplate jdbc, MutableClock clock, RecordingEmailSender emails) {
        this.jdbc = jdbc;
        this.clock = clock;
        this.emails = emails;
    }

    @Test
    void registersTheAccountSendsTheCodeAndActivatesItWhenVerified() throws Exception {
        String username = uniqueUsername();
        String email = username + "@example.com";

        Response registered = register(username, email, PASSWORD, true);

        assertThat(registered.status()).isEqualTo(202);
        assertThat(registered.body()).contains("\"codeValiditySeconds\":900").contains("\"resendAfterSeconds\":60");
        assertThat(registered.body()).doesNotContain(PASSWORD).doesNotContain(email);

        String code = codeSentTo(email);
        Map<String, Object> user = jdbc.queryForMap("SELECT status, password_hash, username FROM users WHERE email = ?", email);
        assertThat(user.get("status")).isEqualTo("PENDING_VERIFICATION");
        assertThat(user.get("username")).isEqualTo(username);
        assertThat((String) user.get("password_hash")).startsWith("$argon2id$v=19$").doesNotContain(PASSWORD);

        String storedHash = jdbc.queryForObject(
                "SELECT t.token_hash FROM user_tokens t JOIN users u ON u.id = t.user_id WHERE u.email = ?", String.class, email);
        assertThat(storedHash).hasSize(64).isNotEqualTo(code);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM user_consents c JOIN users u ON u.id = c.user_id WHERE u.email = ?", Integer.class, email))
                .isEqualTo(2);

        Response verified = verify(email, code);

        assertThat(verified.status()).isEqualTo(204);
        Map<String, Object> active = jdbc.queryForMap("SELECT status, email_verified_at FROM users WHERE email = ?", email);
        assertThat(active.get("status")).isEqualTo("ACTIVE");
        assertThat(active.get("email_verified_at")).isNotNull();
    }

    @Test
    void anAccountCanBeCreatedWithoutNameSurnameOrPhone() throws Exception {
        String username = uniqueUsername();
        register(username, username + "@example.com", PASSWORD, true);

        Map<String, Object> user = jdbc.queryForMap("SELECT name, surname, phone FROM users WHERE username = ?", username);

        assertThat(user.get("name")).isNull();
        assertThat(user.get("surname")).isNull();
        assertThat(user.get("phone")).isNull();
    }

    @Test
    void failedAttemptsAreSavedEvenThoughTheRequestFailsAndFiveBlockTheCode() throws Exception {
        String username = uniqueUsername();
        String email = username + "@example.com";
        register(username, email, PASSWORD, true);
        String code = codeSentTo(email);
        String wrong = code.equals("000000") ? "000001" : "000000";

        for (int attempt = 1; attempt <= 4; attempt++) {
            Response response = verify(email, wrong);
            assertThat(response.status()).isEqualTo(422);
            assertThat(response.body()).contains("\"code\":\"VERIFICATION_CODE_INVALID\"");
        }
        Response fifth = verify(email, wrong);
        assertThat(fifth.status()).isEqualTo(429);
        assertThat(fifth.body()).contains("\"code\":\"VERIFICATION_TOO_MANY_ATTEMPTS\"");

        assertThat(jdbc.queryForObject(
                "SELECT t.failed_attempts FROM user_tokens t JOIN users u ON u.id = t.user_id WHERE u.email = ?",
                Integer.class, email)).isEqualTo(5);
        assertThat(verify(email, code).status()).as("ni siquiera el código bueno").isEqualTo(429);
        assertThat(jdbc.queryForObject("SELECT status FROM users WHERE email = ?", String.class, email))
                .isEqualTo("PENDING_VERIFICATION");
    }

    @Test
    void anExpiredCodeIsRejected() throws Exception {
        String username = uniqueUsername();
        String email = username + "@example.com";
        register(username, email, PASSWORD, true);
        String code = codeSentTo(email);

        clock.advance(Duration.ofMinutes(16));
        Response response = verify(email, code);

        assertThat(response.status()).isEqualTo(422);
        assertThat(response.body()).contains("\"code\":\"VERIFICATION_CODE_EXPIRED\"");
    }

    @Test
    void aUsernameThatIsAlreadyTakenIsReported() throws Exception {
        String username = uniqueUsername();
        register(username, username + "@example.com", PASSWORD, true);

        Response response = register(username.toUpperCase(), "otro-" + username + "@example.com", PASSWORD, true);

        assertThat(response.status()).isEqualTo(409);
        assertThat(response.body()).contains("\"code\":\"USERNAME_TAKEN\"");
    }

    @Test
    void anEmailThatAlreadyHasAnActiveAccountGetsTheSameAnswerAndANoticeInsteadOfANewAccount() throws Exception {
        String username = uniqueUsername();
        String email = username + "@example.com";
        Response first = register(username, email, PASSWORD, true);
        verify(email, codeSentTo(email));

        Response second = register("otro" + username, email, PASSWORD, true);

        assertThat(second.status()).isEqualTo(202);
        assertThat(second.body()).isEqualTo(first.body());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE email = ?", Integer.class, email)).isEqualTo(1);
        assertThat(emails.lastTo(email).subject()).startsWith("Ya tienes una cuenta");
    }

    @Test
    void invalidDataIsRejectedWithTheFieldThatFailed() throws Exception {
        String username = uniqueUsername();

        Response weak = register(username, username + "@example.com", "corta", true);
        assertThat(weak.status()).isEqualTo(400);
        assertThat(weak.body()).contains("\"code\":\"VALIDATION_FAILED\"").contains("\"field\":\"password\"");

        Response noTerms = register(username, username + "@example.com", PASSWORD, false);
        assertThat(noTerms.status()).isEqualTo(400);
        assertThat(noTerms.body()).contains("\"field\":\"acceptTerms\"");

        Response badCode = verify(username + "@example.com", "12ab");
        assertThat(badCode.status()).isEqualTo(400);
        assertThat(badCode.body()).contains("\"field\":\"code\"");

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE username = ?", Integer.class, username)).isZero();
    }

    @Test
    void resendingRespectsTheCooldownAndNeverRevealsWhetherTheAccountExists() throws Exception {
        String username = uniqueUsername();
        String email = username + "@example.com";
        register(username, email, PASSWORD, true);

        Response immediate = resend(email);
        assertThat(immediate.status()).isEqualTo(202);
        assertThat(emails.sentTo(email)).as("dentro de la espera no sale otro correo").hasSize(1);

        clock.advance(Duration.ofSeconds(61));
        resend(email);
        assertThat(emails.sentTo(email)).hasSize(2);

        Response unknown = resend("nadie-" + username + "@example.com");
        assertThat(unknown.status()).isEqualTo(202);
        assertThat(unknown.body()).isEqualTo(immediate.body());
    }

    @Test
    void anOldCodeStopsWorkingWhenANewOneIsSent() throws Exception {
        String username = uniqueUsername();
        String email = username + "@example.com";
        register(username, email, PASSWORD, true);
        String oldCode = codeSentTo(email);
        clock.advance(Duration.ofSeconds(61));
        resend(email);
        String newCode = codeSentTo(email);

        if (!newCode.equals(oldCode)) {
            assertThat(verify(email, oldCode).status()).isEqualTo(422);
        }
        assertThat(verify(email, newCode).status()).isEqualTo(204);
    }

    // ------------------------------------------------------------------------------------------ ayudas

    private String codeSentTo(String email) {
        EmailMessage message = emails.lastTo(email);
        String text = message.text();
        assertThat(text).isNotNull();
        Matcher matcher = SIX_DIGITS.matcher(text == null ? "" : text);
        assertThat(matcher.find()).as("el correo lleva un código de 6 cifras").isTrue();
        return matcher.group(1);
    }

    private static String uniqueUsername() {
        return "u" + Long.toString(System.nanoTime(), 36);
    }

    private Response register(String username, String email, String password, boolean acceptTerms) throws Exception {
        return post("/api/v1/auth/register", "{\"username\":\"" + username + "\",\"email\":\"" + email
                + "\",\"password\":\"" + password + "\",\"acceptTerms\":" + acceptTerms + "}");
    }

    private Response verify(String email, String code) throws Exception {
        return post("/api/v1/auth/verify-email", "{\"email\":\"" + email + "\",\"code\":\"" + code + "\"}");
    }

    private Response resend(String email) throws Exception {
        return post("/api/v1/auth/resend-verification", "{\"email\":\"" + email + "\"}");
    }

    private Response post(String path, String json) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return new Response(response.statusCode(), response.body());
    }
}

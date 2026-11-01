package com.lavinuela.shop;

import static org.assertj.core.api.Assertions.assertThat;

import com.lavinuela.shop.testsupport.MutableClock;
import com.lavinuela.shop.testsupport.RecordingEmailSender;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Inicio de sesión de punta a punta: HTTP, Spring Security, MySQL real y los tokens. Comprueba lo que los repositorios en
 * memoria no pueden: la forma exacta de la cookie, la firma de los tokens, la caducidad con el reloj de la aplicación y que
 * en la base de datos nunca queda un token en claro.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(RegistrationFlowTests.TestBeans.class)
@RequiresDocker
class LoginFlowTests {

    private static final String PASSWORD = "Una-contraseña-larga-2026";
    private static final HttpClient CLIENT = HttpClient.newHttpClient();
    private static final Pattern SIX_DIGITS = Pattern.compile("\\b(\\d{6})\\b");
    private static final Pattern ACCESS_TOKEN = Pattern.compile("\"accessToken\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern COOKIE_VALUE = Pattern.compile("lv_refresh=([^;]*)");
    private static final Map<String, String> WEB_CLIENT = Map.of("X-LV-Client", "web");

    private record Response(int status, String body, List<String> setCookies) {
        String accessToken() {
            Matcher matcher = ACCESS_TOKEN.matcher(body);
            assertThat(matcher.find()).as("la respuesta lleva un token de acceso: " + body).isTrue();
            return matcher.group(1);
        }

        String refreshCookieValue() {
            for (String cookie : setCookies) {
                Matcher matcher = COOKIE_VALUE.matcher(cookie);
                if (matcher.find()) {
                    return matcher.group(1);
                }
            }
            throw new AssertionError("La respuesta no deja la cookie de renovación: " + setCookies);
        }
    }

    private final JdbcTemplate jdbc;
    private final MutableClock clock;
    private final RecordingEmailSender emails;

    @Value("${local.server.port}")
    private int port;

    @Autowired
    LoginFlowTests(JdbcTemplate jdbc, MutableClock clock, RecordingEmailSender emails) {
        this.jdbc = jdbc;
        this.clock = clock;
        this.emails = emails;
    }

    @Test
    void loggingInWithTheEmailGivesATokenAndASafeCookieAndStoresNoTokenInTheClear() throws Exception {
        String email = activeAccount();

        Response login = login(email, PASSWORD);

        assertThat(login.status()).isEqualTo(200);
        assertThat(login.body()).contains("\"expiresInSeconds\":900").contains("\"roles\":[\"CUSTOMER\"]").contains("\"username\"");
        assertThat(login.body()).as("el token de renovación nunca viaja en el cuerpo").doesNotContain(login.refreshCookieValue());

        String cookie = login.setCookies().get(0);
        assertThat(cookie).startsWith("lv_refresh=").contains("HttpOnly").contains("SameSite=Strict").contains("Path=/api/v1/auth");

        String storedHash = jdbc.queryForObject(
                "SELECT t.token_hash FROM refresh_tokens t JOIN users u ON u.id = t.user_id WHERE u.email = ?", String.class, email);
        assertThat(storedHash).hasSize(64).isNotEqualTo(login.refreshCookieValue());
        assertThat(jdbc.queryForObject("SELECT last_login_at FROM users WHERE email = ?", java.sql.Timestamp.class, email)).isNotNull();
    }

    @Test
    void theEmailMatchesIgnoringCase() throws Exception {
        String email = activeAccount();

        assertThat(login(email.toUpperCase(), PASSWORD).status()).isEqualTo(200);
    }

    @Test
    void everyNewAccountHasTheCustomerRole() throws Exception {
        String email = activeAccount();

        assertThat(jdbc.queryForList(
                "SELECT r.code FROM roles r JOIN user_roles ur ON ur.role_id = r.id JOIN users u ON u.id = ur.user_id WHERE u.email = ?",
                String.class, email)).containsExactly("CUSTOMER");
    }

    @Test
    void theTokenOpensTheProtectedProfileAndWithoutItTheAnswerIsAStandardError() throws Exception {
        String email = activeAccount();
        String token = login(email, PASSWORD).accessToken();

        Response me = get("/api/v1/me", Map.of("Authorization", "Bearer " + token));
        assertThat(me.status()).isEqualTo(200);
        assertThat(me.body()).contains(email).contains("\"roles\":[\"CUSTOMER\"]");

        Response anonymous = get("/api/v1/me", Map.of());
        assertThat(anonymous.status()).isEqualTo(401);
        assertThat(anonymous.body()).contains("\"code\":\"AUTH_REQUIRED\"");

        Response forged = get("/api/v1/me", Map.of("Authorization", "Bearer " + token.substring(0, token.length() - 3) + "abc"));
        assertThat(forged.status()).isEqualTo(401);
        assertThat(forged.body()).contains("\"code\":\"AUTH_TOKEN_EXPIRED\"");
    }

    @Test
    void theAccessTokenStopsWorkingAfterFifteenMinutes() throws Exception {
        String email = activeAccount();
        String token = login(email, PASSWORD).accessToken();

        clock.advance(Duration.ofMinutes(14));
        assertThat(get("/api/v1/me", Map.of("Authorization", "Bearer " + token)).status()).isEqualTo(200);

        clock.advance(Duration.ofMinutes(2));
        assertThat(get("/api/v1/me", Map.of("Authorization", "Bearer " + token)).status()).isEqualTo(401);
    }

    @Test
    void aWrongPasswordAndAnUnknownEmailGetTheSameAnswer() throws Exception {
        String email = activeAccount();

        Response wrongPassword = login(email, "Otra-contraseña-2026");
        Response unknownEmail = login("nadie-" + email, PASSWORD);

        assertThat(wrongPassword.status()).isEqualTo(401);
        assertThat(unknownEmail.status()).isEqualTo(401);
        assertThat(detailOf(wrongPassword)).isEqualTo(detailOf(unknownEmail));
        assertThat(wrongPassword.body()).contains("\"code\":\"AUTH_INVALID_CREDENTIALS\"");
        assertThat(wrongPassword.setCookies()).isEmpty();
    }

    @Test
    void anUnverifiedAccountIsToldToVerifyOnlyWhenThePasswordIsRight() throws Exception {
        String username = uniqueUsername();
        String email = username + "@example.com";
        register(username, email);

        Response right = login(email, PASSWORD);
        assertThat(right.status()).isEqualTo(403);
        assertThat(right.body()).contains("\"code\":\"AUTH_EMAIL_NOT_VERIFIED\"");
        assertThat(right.setCookies()).isEmpty();

        assertThat(login(email, "Otra-contraseña-2026").status()).isEqualTo(401);
    }

    @Test
    void fiveFailedAttemptsLockTheLoginForFifteenMinutes() throws Exception {
        String email = activeAccount();
        for (int attempt = 1; attempt <= 5; attempt++) {
            assertThat(login(email, "Otra-contraseña-2026").status()).isEqualTo(401);
        }

        Response locked = login(email, PASSWORD);
        assertThat(locked.status()).isEqualTo(423);
        assertThat(locked.body()).contains("\"code\":\"AUTH_ACCOUNT_LOCKED\"");

        clock.advance(Duration.ofMinutes(16));
        assertThat(login(email, PASSWORD).status()).isEqualTo(200);
    }

    @Test
    void renewingGivesANewTokenAndANewCookieAndTheOldCookieIsRetired() throws Exception {
        String email = activeAccount();
        Response login = login(email, PASSWORD);
        String firstCookie = login.refreshCookieValue();

        Response renewed = refresh(firstCookie);

        assertThat(renewed.status()).isEqualTo(200);
        assertThat(renewed.accessToken()).isNotEqualTo(login.accessToken());
        assertThat(renewed.refreshCookieValue()).isNotEqualTo(firstCookie);
        assertThat(get("/api/v1/me", Map.of("Authorization", "Bearer " + renewed.accessToken())).status()).isEqualTo(200);
    }

    @Test
    void reusingAnOldRefreshCookieClosesTheWholeSession() throws Exception {
        String email = activeAccount();
        String first = login(email, PASSWORD).refreshCookieValue();
        String second = refresh(first).refreshCookieValue();

        Response reused = refresh(first);
        assertThat(reused.status()).isEqualTo(401);
        assertThat(reused.body()).contains("\"code\":\"AUTH_TOKEN_REUSED\"");

        Response newCookieNow = refresh(second);
        assertThat(newCookieNow.status()).isEqualTo(401);
        assertThat(newCookieNow.body()).contains("\"code\":\"AUTH_TOKEN_EXPIRED\"");
    }

    @Test
    void renewingNeedsTheClientHeaderAndACookie() throws Exception {
        String email = activeAccount();
        String cookie = login(email, PASSWORD).refreshCookieValue();

        Response withoutHeader = post("/api/v1/auth/refresh", "", Map.of("Cookie", "lv_refresh=" + cookie));
        assertThat(withoutHeader.status()).isEqualTo(403);
        assertThat(withoutHeader.body()).contains("\"code\":\"ACCESS_DENIED\"");

        Response withoutCookie = post("/api/v1/auth/refresh", "", WEB_CLIENT);
        assertThat(withoutCookie.status()).isEqualTo(401);
        assertThat(withoutCookie.body()).contains("\"code\":\"AUTH_TOKEN_EXPIRED\"");
    }

    @Test
    void loggingOutRetiresTheSessionAndClearsTheCookie() throws Exception {
        String email = activeAccount();
        String cookie = login(email, PASSWORD).refreshCookieValue();

        Response logout = post("/api/v1/auth/logout", "", Map.of("X-LV-Client", "web", "Cookie", "lv_refresh=" + cookie));

        assertThat(logout.status()).isEqualTo(204);
        assertThat(logout.setCookies().get(0)).startsWith("lv_refresh=").contains("Max-Age=0");
        assertThat(refresh(cookie).status()).isEqualTo(401);
    }

    // ------------------------------------------------------------------------------------------ ayudas

    private String uniqueUsername() {
        return "u" + Long.toString(System.nanoTime(), 36);
    }

    private void register(String username, String email) throws Exception {
        Response response = post("/api/v1/auth/register", "{\"username\":\"" + username + "\",\"email\":\"" + email
                + "\",\"password\":\"" + PASSWORD + "\",\"acceptTerms\":true}", Map.of());
        assertThat(response.status()).isEqualTo(202);
    }

    /** Crea una cuenta, confirma su correo con el código recibido y devuelve su correo. */
    private String activeAccount() throws Exception {
        String username = uniqueUsername();
        String email = username + "@example.com";
        register(username, email);
        Matcher matcher = SIX_DIGITS.matcher(String.valueOf(emails.lastTo(email).text()));
        assertThat(matcher.find()).isTrue();
        Response verified = post("/api/v1/auth/verify-email", "{\"email\":\"" + email + "\",\"code\":\"" + matcher.group(1) + "\"}", Map.of());
        assertThat(verified.status()).isEqualTo(204);
        return email;
    }

    private Response login(String email, String password) throws Exception {
        return post("/api/v1/auth/login", "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}", Map.of());
    }

    private Response refresh(String cookieValue) throws Exception {
        return post("/api/v1/auth/refresh", "", Map.of("X-LV-Client", "web", "Cookie", "lv_refresh=" + cookieValue));
    }

    private static String detailOf(Response response) {
        Matcher matcher = Pattern.compile("\"detail\"\\s*:\\s*\"([^\"]*)\"").matcher(response.body());
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    private Response get(String path, Map<String, String> headers) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET();
        headers.forEach(builder::header);
        return send(builder.build());
    }

    private Response post(String path, String json, Map<String, String> headers) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8));
        if (!json.isEmpty()) {
            builder.header("Content-Type", "application/json");
        }
        headers.forEach(builder::header);
        return send(builder.build());
    }

    private Response send(HttpRequest request) throws Exception {
        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return new Response(response.statusCode(), response.body(), response.headers().allValues("set-cookie"));
    }
}

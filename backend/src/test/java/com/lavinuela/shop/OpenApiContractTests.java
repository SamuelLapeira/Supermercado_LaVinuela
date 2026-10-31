package com.lavinuela.shop;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Vigila el contrato de la API: el documento OpenAPI que genera el servidor tiene que ser idéntico al que está guardado
 * en {@code contracts/openapi.json} (del que se genera el cliente de Angular).
 *
 * <p>Si se cambia un endpoint, un campo o un error y no se actualiza el documento guardado, esta prueba falla, y con
 * ella la integración continua: así el frontend nunca se queda con un cliente desfasado. Para actualizarlo, desde la
 * carpeta {@code backend}:
 *
 * <pre>mvn test "-Dtest=OpenApiContractTests" "-Dopenapi.update=true"</pre>
 *
 * (o, para hacerlo todo de una vez, {@code tools/update-api-contract.ps1} desde la raíz del proyecto).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@RequiresDocker
class OpenApiContractTests {

    private static final Path CONTRACT = Path.of("..", "contracts", "openapi.json");
    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    @Value("${local.server.port}")
    private int port;

    @Test
    void theContractMatchesTheCommittedOne() throws Exception {
        String current = normalize(fetchContract());
        boolean update = Boolean.getBoolean("openapi.update")
                || (!Files.exists(CONTRACT) && System.getenv("CI") == null);

        if (update) {
            Files.createDirectories(CONTRACT.getParent());
            Files.writeString(CONTRACT, current, StandardCharsets.UTF_8);
            return;
        }

        assertThat(Files.exists(CONTRACT))
                .as("Falta contracts/openapi.json. Genéralo con: mvn test \"-Dtest=OpenApiContractTests\" \"-Dopenapi.update=true\"")
                .isTrue();
        assertThat(current)
                .as("El contrato de la API ha cambiado y contracts/openapi.json no está actualizado. "
                        + "Actualízalo con: mvn test \"-Dtest=OpenApiContractTests\" \"-Dopenapi.update=true\" "
                        + "(o .\\tools\\update-api-contract.ps1), y sube el cambio junto con el cliente generado.")
                .isEqualTo(normalize(Files.readString(CONTRACT, StandardCharsets.UTF_8)));
    }

    @Test
    void everyOperationHasAStableIdentifierAndTheErrorsAreDescribed() throws Exception {
        String doc = fetchContract();

        for (String operation : new String[] {"registerAccount", "verifyEmail", "resendVerification"}) {
            assertThat(doc).containsPattern("\"operationId\"\\s*:\\s*\"" + operation + "\"");
        }
        assertThat(doc).containsPattern("\"Problem\"\\s*:");
        assertThat(doc).containsPattern("\"FieldError\"\\s*:");
        assertThat(doc).contains("application/problem+json");
    }

    @Test
    void theDocumentIsDeterministicAndRevealsNothingAboutTheRunningServer() throws Exception {
        String doc = fetchContract();

        assertThat(doc).doesNotContain("localhost").doesNotContain("127.0.0.1").doesNotContain(Integer.toString(port));
        assertThat(normalize(fetchContract())).isEqualTo(normalize(doc));
    }

    @Test
    void doesNotDocumentTheInternalManagementEndpoints() throws Exception {
        assertThat(fetchContract()).doesNotContain("/actuator");
    }

    private String fetchContract() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/v3/api-docs")).GET().build();
        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertThat(response.statusCode()).isEqualTo(200);
        return response.body();
    }

    /** Mismo texto con saltos de línea de Unix y una sola línea final, para que Windows y Linux comparen igual. */
    private static String normalize(String text) {
        return text.replace("\r\n", "\n").stripTrailing() + "\n";
    }
}

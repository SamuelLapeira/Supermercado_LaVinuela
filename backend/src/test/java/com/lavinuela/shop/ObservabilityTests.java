package com.lavinuela.shop;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Arranca la aplicación entera (con MySQL real) y comprueba lo que verá quien la opere: sondas de salud
 * sin filtraciones, métricas, identificador de seguimiento y errores en formato estándar.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@RequiresDocker
class ObservabilityTests {

    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    @Value("${local.server.port}")
    private int port;

    @Test
    void healthIsUpAndRevealsNoInternalDetails() throws Exception {
        HttpResponse<String> response = get("/actuator/health", null);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"status\":\"UP\"");
        assertThat(response.body()).doesNotContain("components").doesNotContain("jdbc").doesNotContain("mysql");
    }

    @Test
    void livenessAndReadinessProbesRespond() throws Exception {
        assertThat(get("/actuator/health/liveness", null).statusCode()).isEqualTo(200);
        assertThat(get("/actuator/health/readiness", null).statusCode()).isEqualTo(200);
    }

    @Test
    void exposesMetricsInPrometheusFormatTaggedWithTheApplication() throws Exception {
        HttpResponse<String> response = get("/actuator/prometheus", null);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("jvm_memory");
        assertThat(response.body()).contains("application=\"lavinuela-shop\"");
    }

    @Test
    void answersWithAnIdentifierForEveryRequest() throws Exception {
        assertThat(get("/actuator/health", null).headers().firstValue("X-Request-Id")).isPresent();
        assertThat(get("/actuator/health", "peticion-de-prueba-1").headers().firstValue("X-Request-Id"))
                .contains("peticion-de-prueba-1");
    }

    @Test
    void unknownRoutesAnswerWithAStandardErrorThatCarriesTheTraceIdentifier() throws Exception {
        HttpResponse<String> response = get("/api/v1/no-existe", "peticion-de-prueba-2");

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.headers().firstValue("Content-Type").orElse("")).contains("application/problem+json");
        assertThat(response.body()).contains("\"code\":\"RESOURCE_NOT_FOUND\"");
        assertThat(response.body()).contains("\"traceId\":\"peticion-de-prueba-2\"");
        assertThat(response.body()).doesNotContain("Exception").doesNotContain("at com.");
    }

    private HttpResponse<String> get(String path, String requestId) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET();
        if (requestId != null) {
            builder.header("X-Request-Id", requestId);
        }
        return CLIENT.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }
}

package com.lavinuela.shop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

/**
 * Comprueba los ficheros de configuración de cada perfil <b>sin arrancar la aplicación</b>.
 *
 * <p>Las pruebas de Spring solo cargan el perfil {@code test}; los perfiles {@code local} y {@code prod} no los carga
 * nadie hasta que alguien arranca la aplicación de verdad. Esta prueba lee todos con la misma regla estricta que usa
 * Spring Boot (una clave repetida es un error) para que un fichero mal escrito se detecte aquí y no al arrancar.
 */
class ConfigurationFilesTests {

    private static final List<String> FILES = List.of("application.yml", "application-local.yml", "application-prod.yml");

    private static Object load(String file) throws IOException {
        LoaderOptions options = new LoaderOptions();
        options.setAllowDuplicateKeys(false);
        try (InputStream in = new ClassPathResource(file).getInputStream()) {
            return new Yaml(options).load(in);
        }
    }

    private static Object lookup(Object root, String... path) {
        Object current = root;
        for (String key : path) {
            if (current instanceof Map<?, ?> map) {
                current = map.get(key);
            } else {
                return null;
            }
        }
        return current;
    }

    @Test
    void noConfigurationFileHasRepeatedKeys() {
        for (String file : FILES) {
            assertThatCode(() -> load(file)).as(file).doesNotThrowAnyException();
        }
    }

    @Test
    void theLocalProfileConfiguresTheDatabaseTheMailServerAndTheSecretKey() throws IOException {
        Object local = load("application-local.yml");

        assertThat(lookup(local, "spring", "datasource", "url")).isNotNull();
        assertThat(lookup(local, "spring", "flyway", "user")).isNotNull();
        assertThat(lookup(local, "spring", "mail", "host")).isNotNull();
        assertThat(lookup(local, "app", "identity", "code-pepper")).isNotNull();
        assertThat(lookup(local, "management", "server", "port")).isNotNull();
    }

    @Test
    void theProductionProfileConfiguresTheMailServerAndTheSecretKeyFromTheEnvironment() throws IOException {
        Object prod = load("application-prod.yml");

        assertThat(lookup(prod, "spring", "mail", "host")).isEqualTo("${MAIL_HOST}");
        assertThat(lookup(prod, "app", "identity", "code-pepper")).isEqualTo("${APP_CODE_PEPPER}");
        assertThat(lookup(prod, "app", "mail", "from")).isEqualTo("${APP_MAIL_FROM}");
    }

    @Test
    void secretsAreNeverWrittenInTheConfigurationFiles() throws IOException {
        for (String file : FILES) {
            try (InputStream in = new ClassPathResource(file).getInputStream()) {
                String text = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                assertThat(text).as(file).doesNotContainPattern("(?i)password:\\s*[^$\\s#]");
            }
        }
    }
}

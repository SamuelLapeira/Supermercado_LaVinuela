package com.lavinuela.shop;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * El contexto de la aplicación arranca contra un MySQL real: Flyway aplica todas las migraciones y
 * Hibernate valida que las entidades (cuando las haya) coinciden con las tablas.
 */
@SpringBootTest
@ActiveProfiles("test")
@EnabledIf("com.lavinuela.shop.DockerSupport#isDockerAvailable")
class ShopApplicationTests {

    @Test
    void contextLoads() {
        // Si el contexto no arranca, la prueba falla antes de llegar aquí.
    }
}

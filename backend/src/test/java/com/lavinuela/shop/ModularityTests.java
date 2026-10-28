package com.lavinuela.shop;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * Verifica las fronteras entre módulos con Spring Modulith y genera la documentación (diagramas) en
 * {@code target/spring-modulith-docs}.
 */
class ModularityTests {

    private static final ApplicationModules MODULES = ApplicationModules.of(ShopApplication.class);

    @Test
    void verifiesModuleStructure() {
        // Falla si un módulo usa a otro sin permiso, si hay ciclos o si se accede a un paquete interno.
        MODULES.verify();
    }

    @Test
    void writesModuleDocumentation() {
        new Documenter(MODULES).writeDocumentation();
    }
}

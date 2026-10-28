package com.lavinuela.shop;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.GeneralCodingRules;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Reglas de arquitectura de las fases 2 y 3 convertidas en pruebas. Si alguien las incumple, la
 * compilación falla con un mensaje que señala la clase y la regla.
 *
 * <p>Las reglas de capas se aplican a todos los módulos por patrón de paquete, de modo que valen
 * también para los que todavía no tienen clases.
 */
class ArchitectureTests {

    private static final String BASE = "com.lavinuela.shop";
    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages(BASE);
    }

    @Test
    void domainDoesNotKnowFrameworksNorInfrastructure() {
        check(noClasses().that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..",
                        "jakarta.persistence..",
                        "jakarta.validation..",
                        "com.fasterxml.jackson..",
                        "tools.jackson..",
                        "..infrastructure.."));
    }

    @Test
    void applicationDoesNotDependOnInfrastructure() {
        check(noClasses().that().resideInAPackage("..application..")
                .should().dependOnClassesThat().resideInAPackage("..infrastructure.."));
    }

    @Test
    void controllersDoNotTouchPersistenceNorExternalClients() {
        check(noClasses().that().resideInAPackage("..infrastructure.web..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..infrastructure.persistence..", "..infrastructure.external.."));
    }

    @Test
    void persistenceDoesNotDependOnWeb() {
        check(noClasses().that().resideInAPackage("..infrastructure.persistence..")
                .should().dependOnClassesThat().resideInAPackage("..infrastructure.web.."));
    }

    @Test
    void sharedKernelDoesNotDependOnAnyModule() {
        check(noClasses().that().resideInAPackage(BASE + ".shared..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        BASE + ".identity..",
                        BASE + ".catalog..",
                        BASE + ".inventory..",
                        BASE + ".cart..",
                        BASE + ".delivery..",
                        BASE + ".ordering..",
                        BASE + ".payments..",
                        BASE + ".promotions..",
                        BASE + ".billing..",
                        BASE + ".content..",
                        BASE + ".notifications..",
                        BASE + ".reporting..",
                        BASE + ".audit.."));
    }

    @Test
    void auditModuleDoesNotDependOnOtherModules() {
        check(noClasses().that().resideInAPackage(BASE + ".audit..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        BASE + ".identity..",
                        BASE + ".catalog..",
                        BASE + ".inventory..",
                        BASE + ".cart..",
                        BASE + ".delivery..",
                        BASE + ".ordering..",
                        BASE + ".payments..",
                        BASE + ".promotions..",
                        BASE + ".billing..",
                        BASE + ".content..",
                        BASE + ".notifications..",
                        BASE + ".reporting.."));
    }

    @Test
    void thereAreNoCyclesBetweenModules() {
        check(slices().matching(BASE + ".(*)..").should().beFreeOfCycles());
    }

    @Test
    void codeNeverReadsTheSystemClockDirectly() {
        // El tiempo se inyecta con un Clock para poder probar fechas (ver Fase 3, 1.4).
        check(noClasses().that().resideOutsideOfPackage("..shared.time..")
                .should().callMethod(Instant.class, "now")
                .orShould().callMethod(LocalDate.class, "now")
                .orShould().callMethod(LocalDateTime.class, "now")
                .orShould().callMethod(OffsetDateTime.class, "now")
                .orShould().callMethod(ZonedDateTime.class, "now"));
    }

    @Test
    void generalCodingRules() {
        GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS.check(classes);
        GeneralCodingRules.NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING.check(classes);
        GeneralCodingRules.NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS.check(classes);
        GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION.check(classes);
    }

    private static void check(ArchRule rule) {
        rule.allowEmptyShould(true).check(classes);
    }
}

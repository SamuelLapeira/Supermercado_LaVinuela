package com.lavinuela.shop;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.extension.ExtendWith;

/** La prueba necesita Docker (MySQL real con Testcontainers). Si no hay Docker, se salta. */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(DockerAvailableCondition.class)
public @interface RequiresDocker {
}

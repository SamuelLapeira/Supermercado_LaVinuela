package com.lavinuela.shop.shared.time;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Reloj de la aplicación.
 *
 * <p>Todo el código obtiene la hora de este {@link Clock} inyectado, nunca de {@code Instant.now()}
 * ni similares (lo comprueban las pruebas de arquitectura). Así las pruebas pueden fijar la fecha.
 * Siempre en UTC: la zona {@code Europe/Madrid} solo se aplica al mostrar fechas.
 */
@Configuration(proxyBeanMethods = false)
public class ClockConfiguration {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}

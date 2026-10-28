package com.lavinuela.shop;

import java.lang.reflect.Method;

/**
 * Indica si hay Docker disponible. Las pruebas que necesitan una base de datos MySQL real se saltan
 * (no fallan) en una máquina sin Docker; en integración continua siempre deben ejecutarse.
 *
 * <p>Se consulta por reflexión: así esta clase no depende de Testcontainers para cargarse y, pase lo que pase
 * con Docker, la comprobación nunca rompe el arranque de las pruebas.
 */
public final class DockerSupport {

    private static Boolean available;

    private DockerSupport() {
    }

    public static synchronized boolean isDockerAvailable() {
        if (available == null) {
            available = detect();
        }
        return available;
    }

    private static boolean detect() {
        try {
            Class<?> factory = Class.forName("org.testcontainers.DockerClientFactory");
            Object instance = factory.getMethod("instance").invoke(null);
            Method check = factory.getMethod("isDockerAvailable");
            return Boolean.TRUE.equals(check.invoke(instance));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            return false;
        }
    }
}

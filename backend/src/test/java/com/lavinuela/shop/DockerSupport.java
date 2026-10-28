package com.lavinuela.shop;

import org.testcontainers.DockerClientFactory;

/**
 * Indica si hay Docker disponible. Las pruebas que necesitan una base de datos MySQL real se saltan
 * (no fallan) en una máquina sin Docker; en integración continua siempre deben ejecutarse.
 */
public final class DockerSupport {

    private DockerSupport() {
    }

    public static boolean isDockerAvailable() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
    }
}

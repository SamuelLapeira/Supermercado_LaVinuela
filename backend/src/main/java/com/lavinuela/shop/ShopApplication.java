package com.lavinuela.shop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.modulith.Modulithic;

/**
 * Punto de entrada del monolito modular de Supermercado La Viñuela.
 *
 * <p>Los módulos son los paquetes de primer nivel de {@code com.lavinuela.shop}. Spring Modulith verifica
 * sus fronteras en cada compilación (ver {@code ModularityTests}). Los módulos compartidos son
 * accesibles desde cualquier otro sin declararlos: {@code shared} (núcleo mínimo sin lógica de
 * negocio) y {@code audit} (puerto de auditoría).
 */
@SpringBootApplication
@Modulithic(systemName = "Supermercado La Viñuela", sharedModules = {"shared", "audit"})
public class ShopApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShopApplication.class, args);
    }
}

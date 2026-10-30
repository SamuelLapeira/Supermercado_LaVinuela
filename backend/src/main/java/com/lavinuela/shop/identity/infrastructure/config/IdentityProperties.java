package com.lavinuela.shop.identity.infrastructure.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Ajustes del módulo de identidad ({@code app.identity.*}).
 *
 * @param codePepper clave secreta con la que se calcula la huella de los códigos de verificación. Es un secreto del
 *     servidor: llega por variable de entorno ({@code APP_CODE_PEPPER}), nunca se escribe en un fichero del
 *     repositorio, y tiene al menos 32 caracteres. Si falta, la aplicación no arranca.
 */
@Validated
@ConfigurationProperties(prefix = "app.identity")
public record IdentityProperties(@NotBlank @Size(min = 32) String codePepper) {

    /** La clave secreta no se muestra nunca en los registros. */
    @Override
    public String toString() {
        return "IdentityProperties[codePepper=***]";
    }
}

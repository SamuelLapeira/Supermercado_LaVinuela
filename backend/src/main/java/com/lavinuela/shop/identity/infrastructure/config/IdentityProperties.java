package com.lavinuela.shop.identity.infrastructure.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Ajustes del módulo de identidad ({@code app.identity.*}).
 *
 * @param codePepper clave secreta con la que se calcula la huella de los códigos de verificación
 *     ({@code APP_CODE_PEPPER})
 * @param jwtSecret clave secreta con la que se firman los tokens de acceso ({@code APP_JWT_SECRET}). Quien la conozca
 *     podría fabricar tokens: nunca se escribe en un fichero del repositorio
 * @param refreshCookieSecure si la cookie de renovación solo viaja por HTTPS. Siempre verdadero salvo en el entorno
 *     local, que va por HTTP
 *
 * <p>Las dos claves llegan por variable de entorno, tienen al menos 32 caracteres y, si faltan, la aplicación no arranca.
 */
@Validated
@ConfigurationProperties(prefix = "app.identity")
public record IdentityProperties(
        @NotBlank @Size(min = 32) String codePepper,
        @NotBlank @Size(min = 32) String jwtSecret,
        @DefaultValue("true") boolean refreshCookieSecure) {

    /** Las claves secretas no se muestran nunca en los registros. */
    @Override
    public String toString() {
        return "IdentityProperties[codePepper=***, jwtSecret=***, refreshCookieSecure=" + refreshCookieSecure + "]";
    }
}

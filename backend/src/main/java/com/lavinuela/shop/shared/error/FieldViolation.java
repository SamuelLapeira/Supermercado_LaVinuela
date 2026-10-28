package com.lavinuela.shop.shared.error;

import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Error de un campo concreto de una petición, para mostrarlo junto al campo del formulario.
 *
 * @param field nombre del campo (vacío si el error afecta a toda la petición)
 * @param message explicación en español para la persona usuaria
 * @param code código técnico de la regla incumplida (por ejemplo {@code NotBlank}); puede faltar
 */
public record FieldViolation(String field, String message, @Nullable String code) {

    public FieldViolation {
        Objects.requireNonNull(field, "El campo es obligatorio");
        Objects.requireNonNull(message, "El mensaje es obligatorio");
    }

    /** Error sin código técnico. */
    public static FieldViolation of(String field, String message) {
        return new FieldViolation(field, message, null);
    }
}

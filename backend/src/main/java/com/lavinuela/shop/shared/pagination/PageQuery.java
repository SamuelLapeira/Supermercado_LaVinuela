package com.lavinuela.shop.shared.pagination;

import com.lavinuela.shop.shared.error.FieldViolation;
import com.lavinuela.shop.shared.error.ValidationFailedException;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Qué página se pide: número (desde 0) y tamaño.
 *
 * <p>El tamaño máximo es {@value #MAX_SIZE}: ninguna lista de la API devuelve más, para proteger al
 * servidor de peticiones enormes.
 *
 * @param page número de página, desde 0
 * @param size elementos por página, entre 1 y {@value #MAX_SIZE}
 */
public record PageQuery(int page, int size) {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 50;

    public PageQuery {
        List<FieldViolation> violations = new ArrayList<>();
        if (page < 0) {
            violations.add(FieldViolation.of("page", "La página debe ser 0 o mayor."));
        }
        if (size < 1 || size > MAX_SIZE) {
            violations.add(FieldViolation.of("size", "El tamaño debe estar entre 1 y " + MAX_SIZE + "."));
        }
        if (!violations.isEmpty()) {
            throw new ValidationFailedException(violations);
        }
    }

    /** Construye la consulta con los valores por defecto cuando faltan (página 0, tamaño 20). */
    public static PageQuery of(@Nullable Integer page, @Nullable Integer size) {
        return new PageQuery(page == null ? 0 : page, size == null ? DEFAULT_SIZE : size);
    }

    /** Posición del primer elemento de la página dentro del total. */
    public long offset() {
        return (long) page * size;
    }
}

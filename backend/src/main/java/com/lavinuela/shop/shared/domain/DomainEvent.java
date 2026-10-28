package com.lavinuela.shop.shared.domain;

import java.time.Instant;

/**
 * Hecho relevante que ya ha ocurrido en el negocio (por ejemplo, un pedido pagado).
 *
 * <p>Los eventos se nombran en pasado, llevan lo mínimo (identificadores, no objetos completos) y
 * los consumidores deben ser idempotentes: recibir el mismo evento dos veces no cambia el resultado.
 */
public interface DomainEvent {

    /** Instante en que ocurrió el hecho. */
    Instant occurredAt();
}

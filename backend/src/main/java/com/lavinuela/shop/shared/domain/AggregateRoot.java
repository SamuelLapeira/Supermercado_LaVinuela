package com.lavinuela.shop.shared.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Base de las raíces de agregado del dominio.
 *
 * <p>Acumula los eventos de dominio que se producen al aplicar una regla de negocio. La capa de
 * aplicación los recoge con {@link #pullEvents()} y los publica cuando la transacción se confirma.
 */
public abstract class AggregateRoot {

    private final List<DomainEvent> events = new ArrayList<>();

    /** Registra un evento que se publicará después de confirmar la transacción. */
    protected void registerEvent(DomainEvent event) {
        events.add(Objects.requireNonNull(event, "El evento es obligatorio"));
    }

    /** Devuelve los eventos pendientes y los borra del agregado. */
    public List<DomainEvent> pullEvents() {
        List<DomainEvent> pending = List.copyOf(events);
        events.clear();
        return pending;
    }
}

package com.lavinuela.shop.testsupport;

import java.util.ArrayList;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;

/** Guarda los eventos publicados para poder comprobarlos. */
public final class RecordingEventPublisher implements ApplicationEventPublisher {

    private final List<Object> events = new ArrayList<>();

    @Override
    public void publishEvent(Object event) {
        events.add(event);
    }

    public <T> List<T> of(Class<T> type) {
        return events.stream().filter(type::isInstance).map(type::cast).toList();
    }

    public <T> T last(Class<T> type) {
        List<T> matching = of(type);
        if (matching.isEmpty()) {
            throw new AssertionError("No se ha publicado ningún evento " + type.getSimpleName());
        }
        return matching.get(matching.size() - 1);
    }
}

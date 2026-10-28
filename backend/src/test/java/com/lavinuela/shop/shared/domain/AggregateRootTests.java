package com.lavinuela.shop.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class AggregateRootTests {

    private record SomethingHappened(Instant occurredAt) implements DomainEvent {
    }

    private static final class Sample extends AggregateRoot {
        void doSomething() {
            registerEvent(new SomethingHappened(Instant.parse("2026-10-28T10:00:00Z")));
        }
    }

    @Test
    void collectsEventsAndClearsThemWhenPulled() {
        Sample sample = new Sample();
        sample.doSomething();
        sample.doSomething();

        assertThat(sample.pullEvents()).hasSize(2);
        assertThat(sample.pullEvents()).isEmpty();
    }
}

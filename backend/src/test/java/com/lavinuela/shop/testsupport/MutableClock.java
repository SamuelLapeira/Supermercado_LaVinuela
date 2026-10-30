package com.lavinuela.shop.testsupport;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Reloj que las pruebas pueden adelantar, para comprobar caducidades y esperas sin esperar de verdad. */
public final class MutableClock extends Clock {

    private Instant now;

    public MutableClock(Instant start) {
        this.now = start;
    }

    public MutableClock() {
        this(Instant.parse("2026-10-28T10:00:00Z"));
    }

    public synchronized void advance(Duration duration) {
        now = now.plus(duration);
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }

    @Override
    public synchronized Instant instant() {
        return now;
    }
}

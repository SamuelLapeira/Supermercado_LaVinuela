package com.lavinuela.shop.identity;

import static org.assertj.core.api.Assertions.assertThat;

import com.lavinuela.shop.identity.application.LoginAttemptLimiter;
import com.lavinuela.shop.testsupport.MutableClock;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class LoginAttemptLimiterTests {

    private final MutableClock clock = new MutableClock();
    private final LoginAttemptLimiter limiter = new LoginAttemptLimiter(clock);

    @Test
    void doesNotBlockBeforeTheFifthFailure() {
        for (int i = 0; i < 4; i++) {
            limiter.recordFailure("a@example.com");
        }

        assertThat(limiter.isBlocked("a@example.com")).isFalse();
    }

    @Test
    void blocksAtTheFifthFailureForFifteenMinutes() {
        for (int i = 0; i < 5; i++) {
            limiter.recordFailure("a@example.com");
        }
        assertThat(limiter.isBlocked("a@example.com")).isTrue();

        clock.advance(Duration.ofMinutes(14).plusSeconds(59));
        assertThat(limiter.isBlocked("a@example.com")).isTrue();

        clock.advance(Duration.ofSeconds(2));
        assertThat(limiter.isBlocked("a@example.com")).isFalse();
    }

    @Test
    void eachEmailHasItsOwnCount() {
        for (int i = 0; i < 5; i++) {
            limiter.recordFailure("a@example.com");
        }

        assertThat(limiter.isBlocked("b@example.com")).isFalse();
    }

    @Test
    void failuresOutsideTheWindowDoNotAddUp() {
        for (int i = 0; i < 4; i++) {
            limiter.recordFailure("a@example.com");
        }
        clock.advance(Duration.ofMinutes(16));

        limiter.recordFailure("a@example.com");

        assertThat(limiter.isBlocked("a@example.com")).isFalse();
    }

    @Test
    void aSuccessfulLoginClearsTheCount() {
        for (int i = 0; i < 4; i++) {
            limiter.recordFailure("a@example.com");
        }
        limiter.reset("a@example.com");
        for (int i = 0; i < 4; i++) {
            limiter.recordFailure("a@example.com");
        }

        assertThat(limiter.isBlocked("a@example.com")).isFalse();
    }
}

package com.lavinuela.shop.identity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class ResendVerificationServiceTests {

    private static final String EMAIL = "samuel@example.com";

    private final IdentityTestFixture fx = new IdentityTestFixture();

    @Test
    void sendsANewCodeToAPendingAccountAfterTheCooldown() {
        fx.register.register(fx.command("samuel", EMAIL));
        fx.clock.advance(Duration.ofSeconds(61));

        fx.resend.resend(EMAIL);

        assertThat(fx.codesIssued()).isEqualTo(2);
    }

    @Test
    void doesNothingDuringTheCooldown() {
        fx.register.register(fx.command("samuel", EMAIL));
        fx.clock.advance(Duration.ofSeconds(30));

        fx.resend.resend(EMAIL);

        assertThat(fx.codesIssued()).isEqualTo(1);
    }

    @Test
    void doesNothingForAnUnknownEmail() {
        fx.resend.resend("nadie@example.com");

        assertThat(fx.codesIssued()).isZero();
    }

    @Test
    void doesNothingForAnAlreadyActiveAccount() {
        fx.register.register(fx.command("samuel", EMAIL));
        fx.verify.verify(EMAIL, fx.lastCode());
        int before = fx.codesIssued();
        fx.clock.advance(Duration.ofMinutes(5));

        fx.resend.resend(EMAIL);

        assertThat(fx.codesIssued()).isEqualTo(before);
    }

    @Test
    void allowsAtMostFiveCodesPerHour() {
        fx.register.register(fx.command("samuel", EMAIL));
        for (int i = 0; i < 10; i++) {
            fx.clock.advance(Duration.ofSeconds(61));
            fx.resend.resend(EMAIL);
        }

        // Con 61 s entre intentos, en la primera hora caben 5 códigos: el del registro y 4 reenvíos.
        assertThat(fx.codesIssued()).isEqualTo(5);
        assertThat(fx.store.issuedCodes()).isEqualTo(5);
    }

    @Test
    void theLimitLiftsOnceTheHourHasPassed() {
        fx.register.register(fx.command("samuel", EMAIL));
        for (int i = 0; i < 6; i++) {
            fx.clock.advance(Duration.ofSeconds(61));
            fx.resend.resend(EMAIL);
        }
        int atLimit = fx.codesIssued();
        fx.clock.advance(Duration.ofSeconds(61));
        fx.resend.resend(EMAIL);
        assertThat(fx.codesIssued()).as("sigue bloqueado dentro de la hora").isEqualTo(atLimit);

        fx.clock.advance(Duration.ofHours(1));
        fx.resend.resend(EMAIL);

        assertThat(fx.codesIssued()).isEqualTo(atLimit + 1);
    }
}

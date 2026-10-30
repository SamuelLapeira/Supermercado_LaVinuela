package com.lavinuela.shop.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lavinuela.shop.identity.domain.model.UserStatus;
import com.lavinuela.shop.shared.error.CodedException;
import com.lavinuela.shop.shared.error.ErrorCode;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class VerifyEmailServiceTests {

    private static final String EMAIL = "samuel@example.com";

    private final IdentityTestFixture fx = new IdentityTestFixture();

    private void registered() {
        fx.register.register(fx.command("samuel", EMAIL));
    }

    private static String wrongCode(String realCode) {
        return realCode.equals("000000") ? "000001" : "000000";
    }

    private void assertFails(ErrorCode expected, Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(CodedException.class, e -> assertThat(e.code()).isEqualTo(expected));
    }

    @Test
    void theRightCodeActivatesTheAccount() {
        registered();

        fx.verify.verify(EMAIL, fx.lastCode());

        assertThat(fx.store.users().get(0).status()).isEqualTo(UserStatus.ACTIVE);
        assertThat(fx.store.users().get(0).emailVerifiedAt()).isEqualTo(fx.clock.instant());
    }

    @Test
    void theEmailIsMatchedIgnoringCaseAndSpaces() {
        registered();

        fx.verify.verify("  SAMUEL@Example.com ", fx.lastCode());

        assertThat(fx.store.users().get(0).status()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void aCodeWorksOnlyOnce() {
        registered();
        String code = fx.lastCode();
        fx.verify.verify(EMAIL, code);

        assertFails(ErrorCode.VERIFICATION_CODE_INVALID, () -> fx.verify.verify(EMAIL, code));
    }

    @Test
    void aWrongCodeIsRejectedAndLeavesTheAccountPending() {
        registered();

        assertFails(ErrorCode.VERIFICATION_CODE_INVALID, () -> fx.verify.verify(EMAIL, wrongCode(fx.lastCode())));

        assertThat(fx.store.users().get(0).status()).isEqualTo(UserStatus.PENDING_VERIFICATION);
    }

    @Test
    void afterFiveWrongAttemptsEvenTheRightCodeIsBlocked() {
        registered();
        String code = fx.lastCode();
        String wrong = wrongCode(code);

        for (int attempt = 1; attempt <= 4; attempt++) {
            assertFails(ErrorCode.VERIFICATION_CODE_INVALID, () -> fx.verify.verify(EMAIL, wrong));
        }
        assertFails(ErrorCode.VERIFICATION_TOO_MANY_ATTEMPTS, () -> fx.verify.verify(EMAIL, wrong));

        assertFails(ErrorCode.VERIFICATION_TOO_MANY_ATTEMPTS, () -> fx.verify.verify(EMAIL, code));
        assertThat(fx.store.users().get(0).status()).isEqualTo(UserStatus.PENDING_VERIFICATION);
    }

    @Test
    void aNewCodeStartsFreshAfterTooManyAttempts() {
        registered();
        String firstCode = fx.lastCode();
        for (int attempt = 1; attempt <= 5; attempt++) {
            try {
                fx.verify.verify(EMAIL, wrongCode(firstCode));
            } catch (CodedException expected) {
                // Se esperan los errores de código incorrecto y, al quinto, el de demasiados intentos.
            }
        }

        fx.clock.advance(Duration.ofSeconds(61));
        fx.resend.resend(EMAIL);
        fx.verify.verify(EMAIL, fx.lastCode());

        assertThat(fx.store.users().get(0).status()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void anExpiredCodeIsRejected() {
        registered();
        String code = fx.lastCode();

        fx.clock.advance(Duration.ofMinutes(15).plusSeconds(1));

        assertFails(ErrorCode.VERIFICATION_CODE_EXPIRED, () -> fx.verify.verify(EMAIL, code));
    }

    @Test
    void aCodeIsStillValidJustBeforeItExpires() {
        registered();
        String code = fx.lastCode();

        fx.clock.advance(Duration.ofMinutes(14).plusSeconds(59));

        fx.verify.verify(EMAIL, code);
        assertThat(fx.store.users().get(0).status()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void anUnknownEmailGetsTheSameAnswerAsAWrongCode() {
        assertFails(ErrorCode.VERIFICATION_CODE_INVALID, () -> fx.verify.verify("nadie@example.com", "123456"));
    }

    @Test
    void anAlreadyActiveAccountGetsTheSameAnswerAsAWrongCode() {
        registered();
        fx.verify.verify(EMAIL, fx.lastCode());

        assertFails(ErrorCode.VERIFICATION_CODE_INVALID, () -> fx.verify.verify(EMAIL, "123456"));
    }

    @Test
    void aNewCodeCancelsThePreviousOne() {
        registered();
        String oldCode = fx.lastCode();
        fx.clock.advance(Duration.ofSeconds(61));
        fx.resend.resend(EMAIL);
        String newCode = fx.lastCode();
        assertThat(newCode).isNotEqualTo(oldCode);

        assertFails(ErrorCode.VERIFICATION_CODE_INVALID, () -> fx.verify.verify(EMAIL, oldCode));
        fx.verify.verify(EMAIL, newCode);

        assertThat(fx.store.users().get(0).status()).isEqualTo(UserStatus.ACTIVE);
    }
}

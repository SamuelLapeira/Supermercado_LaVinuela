package com.lavinuela.shop.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lavinuela.shop.identity.application.AuthSession;
import com.lavinuela.shop.identity.domain.model.UserStatus;
import com.lavinuela.shop.shared.error.CodedException;
import com.lavinuela.shop.shared.error.ErrorCode;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class LoginServiceTests {

    private static final String EMAIL = "ana@example.com";
    private static final String PASSWORD = IdentityTestFixture.PASSWORD;

    private final IdentityTestFixture fx = new IdentityTestFixture();

    private void assertFails(ErrorCode expected, Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(CodedException.class, e -> assertThat(e.code()).isEqualTo(expected));
    }

    @Test
    void anActiveAccountLogsInWithItsEmailIgnoringCaseAndSpaces() {
        fx.activeAccount("ana", EMAIL);

        AuthSession session = fx.login.login("  ANA@Example.COM ", PASSWORD, "Mozilla/5.0");

        assertThat(session.user().username()).isEqualTo("ana");
        assertThat(session.user().email()).isEqualTo(EMAIL);
        assertThat(session.user().roles()).containsExactly("CUSTOMER");
        assertThat(session.accessToken()).startsWith("acceso-");
        assertThat(session.refreshToken()).isNotBlank();
        assertThat(fx.store.users().get(0).lastLoginAt()).isEqualTo(fx.clock.instant());
    }

    @Test
    void theRefreshTokenIsStoredOnlyAsAFingerprint() {
        fx.activeAccount("ana", EMAIL);

        AuthSession session = fx.login.login(EMAIL, PASSWORD, null);

        assertThat(fx.store.refreshRows()).hasSize(1);
        String stored = fx.store.refreshRows().get(0).hash;
        assertThat(stored).isNotEqualTo(session.refreshToken()).hasSize(64);
        assertThat(stored).isEqualTo(fx.tokenCodec.hash(session.refreshToken()));
    }

    @Test
    void eachLoginOpensItsOwnFamilyOfTokens() {
        fx.activeAccount("ana", EMAIL);

        fx.login.login(EMAIL, PASSWORD, null);
        fx.login.login(EMAIL, PASSWORD, null);

        assertThat(fx.store.refreshRows().stream().map(r -> r.familyId).distinct().count()).isEqualTo(2);
    }

    @Test
    void aWrongPasswordAndAnUnknownEmailGetTheSameAnswer() {
        fx.activeAccount("ana", EMAIL);

        assertFails(ErrorCode.AUTH_INVALID_CREDENTIALS, () -> fx.login.login(EMAIL, "Otra-contraseña-2026", null));
        assertFails(ErrorCode.AUTH_INVALID_CREDENTIALS, () -> fx.login.login("nadie@example.com", PASSWORD, null));
    }

    @Test
    void anUnknownEmailStillCostsAPasswordCheckSoTheTimeDoesNotGiveItAway() {
        int before = fx.passwordChecks.get();

        assertFails(ErrorCode.AUTH_INVALID_CREDENTIALS, () -> fx.login.login("nadie@example.com", PASSWORD, null));

        assertThat(fx.passwordChecks.get() - before).isEqualTo(1);
    }

    @Test
    void anUnverifiedAccountIsToldToVerifyOnlyAfterTheRightPassword() {
        fx.register.register(fx.command("ana", EMAIL));

        assertFails(ErrorCode.AUTH_EMAIL_NOT_VERIFIED, () -> fx.login.login(EMAIL, PASSWORD, null));
        assertFails(ErrorCode.AUTH_INVALID_CREDENTIALS, () -> fx.login.login(EMAIL, "Otra-contraseña-2026", null));
        assertThat(fx.store.refreshRows()).isEmpty();
    }

    @Test
    void lockedAndDeletedAccountsGetTheGenericAnswer() {
        fx.activeAccount("ana", EMAIL);

        fx.store.setStatus(EMAIL, UserStatus.LOCKED);
        assertFails(ErrorCode.AUTH_INVALID_CREDENTIALS, () -> fx.login.login(EMAIL, PASSWORD, null));

        fx.store.setStatus(EMAIL, UserStatus.DELETED);
        assertFails(ErrorCode.AUTH_INVALID_CREDENTIALS, () -> fx.login.login(EMAIL, PASSWORD, null));
    }

    @Test
    void fiveFailuresLockTheLoginEvenForTheRightPasswordUntilTheWaitIsOver() {
        fx.activeAccount("ana", EMAIL);
        for (int attempt = 1; attempt <= 5; attempt++) {
            assertFails(ErrorCode.AUTH_INVALID_CREDENTIALS, () -> fx.login.login(EMAIL, "Otra-contraseña-2026", null));
        }

        assertFails(ErrorCode.AUTH_ACCOUNT_LOCKED, () -> fx.login.login(EMAIL, PASSWORD, null));

        fx.clock.advance(Duration.ofMinutes(14));
        assertFails(ErrorCode.AUTH_ACCOUNT_LOCKED, () -> fx.login.login(EMAIL, PASSWORD, null));

        fx.clock.advance(Duration.ofMinutes(2));
        assertThat(fx.login.login(EMAIL, PASSWORD, null).user().username()).isEqualTo("ana");
    }

    @Test
    void anUnknownEmailLocksTooSoLockingDoesNotRevealWhichAccountsExist() {
        for (int attempt = 1; attempt <= 5; attempt++) {
            assertFails(ErrorCode.AUTH_INVALID_CREDENTIALS, () -> fx.login.login("nadie@example.com", PASSWORD, null));
        }

        assertFails(ErrorCode.AUTH_ACCOUNT_LOCKED, () -> fx.login.login("nadie@example.com", PASSWORD, null));
    }

    @Test
    void aSuccessfulLoginClearsTheFailureCount() {
        fx.activeAccount("ana", EMAIL);
        for (int attempt = 1; attempt <= 4; attempt++) {
            assertFails(ErrorCode.AUTH_INVALID_CREDENTIALS, () -> fx.login.login(EMAIL, "Otra-contraseña-2026", null));
        }
        fx.login.login(EMAIL, PASSWORD, null);

        for (int attempt = 1; attempt <= 4; attempt++) {
            assertFails(ErrorCode.AUTH_INVALID_CREDENTIALS, () -> fx.login.login(EMAIL, "Otra-contraseña-2026", null));
        }
        assertThat(fx.login.login(EMAIL, PASSWORD, null).user().username()).isEqualTo("ana");
    }

    @Test
    void theSessionNeverShowsItsTokensInLogs() {
        fx.activeAccount("ana", EMAIL);

        AuthSession session = fx.login.login(EMAIL, PASSWORD, null);

        assertThat(session.toString()).doesNotContain(session.accessToken()).doesNotContain(session.refreshToken());
    }
}

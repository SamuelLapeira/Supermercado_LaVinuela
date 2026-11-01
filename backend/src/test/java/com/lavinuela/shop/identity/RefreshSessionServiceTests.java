package com.lavinuela.shop.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lavinuela.shop.identity.application.AuthSession;
import com.lavinuela.shop.identity.domain.model.UserStatus;
import com.lavinuela.shop.shared.error.CodedException;
import com.lavinuela.shop.shared.error.ErrorCode;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class RefreshSessionServiceTests {

    private static final String EMAIL = "ana@example.com";

    private final IdentityTestFixture fx = new IdentityTestFixture();

    private AuthSession login() {
        fx.activeAccount("ana", EMAIL);
        return fx.login.login(EMAIL, IdentityTestFixture.PASSWORD, "Mozilla/5.0");
    }

    private void assertFails(ErrorCode expected, Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(CodedException.class, e -> assertThat(e.code()).isEqualTo(expected));
    }

    @Test
    void renewingGivesNewTokensAndRetiresTheOldRefreshToken() {
        AuthSession first = login();

        AuthSession second = fx.refresh.refresh(first.refreshToken(), "Mozilla/5.0");

        assertThat(second.refreshToken()).isNotEqualTo(first.refreshToken());
        assertThat(second.accessToken()).isNotEqualTo(first.accessToken());
        assertThat(second.user().username()).isEqualTo("ana");
        assertThat(fx.store.refreshRows()).hasSize(2);
        assertThat(fx.store.refreshRows().get(0).revokedAt).isNotNull();
        assertThat(fx.store.refreshRows().get(0).replacedById).isEqualTo(fx.store.refreshRows().get(1).id);
        assertThat(fx.store.refreshRows().get(1).revokedAt).isNull();
    }

    @Test
    void theNewTokenStaysInTheSameFamilyAndCanBeRenewedAgain() {
        AuthSession first = login();

        AuthSession second = fx.refresh.refresh(first.refreshToken(), null);
        AuthSession third = fx.refresh.refresh(second.refreshToken(), null);

        assertThat(third.user().username()).isEqualTo("ana");
        assertThat(fx.store.refreshRows().stream().map(r -> r.familyId).distinct().count()).isEqualTo(1);
    }

    @Test
    void reusingAnAlreadyUsedTokenRevokesTheWholeFamily() {
        AuthSession first = login();
        AuthSession second = fx.refresh.refresh(first.refreshToken(), null);

        assertFails(ErrorCode.AUTH_TOKEN_REUSED, () -> fx.refresh.refresh(first.refreshToken(), null));

        // Quien tenía el token nuevo también queda fuera: no se sabe cuál de los dos es el ladrón.
        assertFails(ErrorCode.AUTH_TOKEN_EXPIRED, () -> fx.refresh.refresh(second.refreshToken(), null));
        assertThat(fx.store.refreshRows()).allSatisfy(row -> assertThat(row.revokedAt).isNotNull());
    }

    @Test
    void anExpiredRefreshTokenIsRejected() {
        AuthSession first = login();

        fx.clock.advance(Duration.ofDays(30).plusSeconds(1));

        assertFails(ErrorCode.AUTH_TOKEN_EXPIRED, () -> fx.refresh.refresh(first.refreshToken(), null));
    }

    @Test
    void aRefreshTokenIsStillValidJustBeforeItExpires() {
        AuthSession first = login();

        fx.clock.advance(Duration.ofDays(29).plusHours(23));

        assertThat(fx.refresh.refresh(first.refreshToken(), null).user().username()).isEqualTo("ana");
    }

    @Test
    void unknownBlankOrMissingTokensAreRejected() {
        login();

        assertFails(ErrorCode.AUTH_TOKEN_EXPIRED, () -> fx.refresh.refresh("token-que-no-existe", null));
        assertFails(ErrorCode.AUTH_TOKEN_EXPIRED, () -> fx.refresh.refresh("", null));
        assertFails(ErrorCode.AUTH_TOKEN_EXPIRED, () -> fx.refresh.refresh("   ", null));
        assertFails(ErrorCode.AUTH_TOKEN_EXPIRED, () -> fx.refresh.refresh(null, null));
    }

    @Test
    void anAccountThatIsNoLongerActiveCannotRenew() {
        AuthSession first = login();
        fx.store.setStatus(EMAIL, UserStatus.LOCKED);

        assertFails(ErrorCode.AUTH_TOKEN_EXPIRED, () -> fx.refresh.refresh(first.refreshToken(), null));
        assertThat(fx.store.refreshRows()).allSatisfy(row -> assertThat(row.revokedAt).isNotNull());
    }

    @Test
    void loggingOutRevokesTheWholeFamilySoRenewingFails() {
        AuthSession first = login();
        AuthSession second = fx.refresh.refresh(first.refreshToken(), null);

        fx.logout.logout(second.refreshToken());

        assertFails(ErrorCode.AUTH_TOKEN_EXPIRED, () -> fx.refresh.refresh(second.refreshToken(), null));
        assertThat(fx.store.refreshRows()).allSatisfy(row -> assertThat(row.revokedAt).isNotNull());
    }

    @Test
    void loggingOutIsHarmlessWithoutASession() {
        fx.logout.logout(null);
        fx.logout.logout("");
        fx.logout.logout("token-que-no-existe");

        assertThat(fx.store.refreshRows()).isEmpty();
    }

    @Test
    void loggingOutOneSessionDoesNotCloseTheOthers() {
        fx.activeAccount("ana", EMAIL);
        AuthSession phone = fx.login.login(EMAIL, IdentityTestFixture.PASSWORD, null);
        AuthSession computer = fx.login.login(EMAIL, IdentityTestFixture.PASSWORD, null);

        fx.logout.logout(phone.refreshToken());

        assertFails(ErrorCode.AUTH_TOKEN_EXPIRED, () -> fx.refresh.refresh(phone.refreshToken(), null));
        assertThat(fx.refresh.refresh(computer.refreshToken(), null).user().username()).isEqualTo("ana");
    }

    @Test
    void theCurrentUserIsFoundByTheIdentifierInTheToken() {
        AuthSession session = login();

        assertThat(fx.currentUser.currentUser(session.user().id()).username()).isEqualTo("ana");
    }

    @Test
    void anUnknownOrInactiveAccountIsNotTheCurrentUser() {
        AuthSession session = login();

        assertFails(ErrorCode.AUTH_REQUIRED, () -> fx.currentUser.currentUser("01ARZ3NDEKTSV4RRFFQ69G5FAV"));

        fx.store.setStatus(EMAIL, UserStatus.LOCKED);
        assertFails(ErrorCode.AUTH_REQUIRED, () -> fx.currentUser.currentUser(session.user().id()));
    }
}

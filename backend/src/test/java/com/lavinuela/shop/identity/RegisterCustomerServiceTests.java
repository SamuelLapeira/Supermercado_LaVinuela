package com.lavinuela.shop.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lavinuela.shop.identity.application.RegisterCustomerCommand;
import com.lavinuela.shop.identity.application.RegistrationResult;
import com.lavinuela.shop.identity.domain.model.LegalDocuments;
import com.lavinuela.shop.identity.domain.model.User;
import com.lavinuela.shop.identity.domain.model.UserStatus;
import com.lavinuela.shop.shared.error.CodedException;
import com.lavinuela.shop.shared.error.ErrorCode;
import com.lavinuela.shop.shared.error.FieldViolation;
import com.lavinuela.shop.shared.error.ValidationFailedException;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class RegisterCustomerServiceTests {

    private final IdentityTestFixture fx = new IdentityTestFixture();

    @Test
    void createsAPendingAccountAndSendsACodeByEvent() {
        RegistrationResult result = fx.register.register(fx.command("samuel", "samuel@example.com"));

        User user = fx.store.users().get(0);
        assertThat(user.status()).isEqualTo(UserStatus.PENDING_VERIFICATION);
        assertThat(user.username()).isEqualTo("samuel");
        assertThat(user.passwordHash()).isNotEqualTo(IdentityTestFixture.PASSWORD);
        assertThat(fx.codesIssued()).isEqualTo(1);
        assertThat(fx.lastCode()).matches("[0-9]{6}");
        assertThat(fx.events.last(IdentityEvents.VerificationCodeIssued.class).email()).isEqualTo("samuel@example.com");
        assertThat(result.maskedEmail()).isEqualTo("s***@example.com");
        assertThat(result.codeValiditySeconds()).isEqualTo(900);
        assertThat(result.resendAfterSeconds()).isEqualTo(60);
    }

    @Test
    void neverStoresTheCodeInTheClear() {
        fx.register.register(fx.command("samuel", "samuel@example.com"));

        long userId = fx.store.users().get(0).requireId();
        assertThat(fx.store.lastHash(userId)).isNotEqualTo(fx.lastCode()).hasSize(64);
    }

    @Test
    void recordsTheAcceptedLegalDocuments() {
        fx.register.register(fx.command("samuel", "samuel@example.com"));

        assertThat(fx.store.consents()).extracting(InMemoryIdentityStore.Consent::type)
                .containsExactlyInAnyOrder("TERMS", "PRIVACY");
        assertThat(fx.store.consents()).extracting(InMemoryIdentityStore.Consent::version)
                .containsOnly(LegalDocuments.CURRENT_VERSION);
    }

    @Test
    void storesUsernameAndEmailInLowercase() {
        fx.register.register(fx.command("  Samuel.L ", "  Samuel@Example.COM "));

        assertThat(fx.store.users().get(0).username()).isEqualTo("samuel.l");
        assertThat(fx.store.users().get(0).email()).isEqualTo("samuel@example.com");
    }

    @Test
    void rejectsAWeakPasswordWithAFieldError() {
        RegisterCustomerCommand weak = new RegisterCustomerCommand("samuel", "samuel@example.com", "corta", true);

        assertThatThrownBy(() -> fx.register.register(weak))
                .isInstanceOfSatisfying(ValidationFailedException.class, e -> assertThat(e.violations())
                        .extracting(FieldViolation::field).containsExactly("password"));
        assertThat(fx.store.users()).isEmpty();
        assertThat(fx.codesIssued()).isZero();
    }

    @Test
    void requiresAcceptingTheTerms() {
        RegisterCustomerCommand noTerms = new RegisterCustomerCommand(
                "samuel", "samuel@example.com", IdentityTestFixture.PASSWORD, false);

        assertThatThrownBy(() -> fx.register.register(noTerms))
                .isInstanceOfSatisfying(ValidationFailedException.class, e -> assertThat(e.violations())
                        .extracting(FieldViolation::field).containsExactly("acceptTerms"));
        assertThat(fx.store.users()).isEmpty();
    }

    @Test
    void reportsAUsernameThatIsAlreadyTaken() {
        fx.register.register(fx.command("samuel", "uno@example.com"));

        assertThatThrownBy(() -> fx.register.register(fx.command("Samuel", "otro@example.com")))
                .isInstanceOfSatisfying(CodedException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.USERNAME_TAKEN));
        assertThat(fx.store.users()).hasSize(1);
    }

    @Test
    void anExistingPendingEmailDoesNotCreateAnotherAccountAndRespondsTheSame() {
        RegistrationResult first = fx.register.register(fx.command("samuel", "samuel@example.com"));

        RegistrationResult second = fx.register.register(fx.command("otro.nombre", "samuel@example.com"));

        assertThat(fx.store.users()).hasSize(1);
        assertThat(second).isEqualTo(first);
    }

    @Test
    void anExistingPendingEmailGetsANewCodeOnlyAfterTheCooldown() {
        fx.register.register(fx.command("samuel", "samuel@example.com"));

        fx.register.register(fx.command("samuel", "samuel@example.com"));
        assertThat(fx.codesIssued()).as("dentro de la espera de 60 s").isEqualTo(1);

        fx.clock.advance(Duration.ofSeconds(61));
        fx.register.register(fx.command("samuel", "samuel@example.com"));
        assertThat(fx.codesIssued()).as("pasada la espera").isEqualTo(2);
    }

    @Test
    void anExistingActiveEmailTellsItsOwnerInsteadOfCreatingAnything() {
        fx.register.register(fx.command("samuel", "samuel@example.com"));
        fx.verify.verify("samuel@example.com", fx.lastCode());
        int codesBefore = fx.codesIssued();

        RegistrationResult result = fx.register.register(fx.command("intruso", "samuel@example.com"));

        assertThat(fx.store.users()).hasSize(1);
        assertThat(fx.codesIssued()).isEqualTo(codesBefore);
        assertThat(fx.events.last(IdentityEvents.AccountAlreadyExists.class).email()).isEqualTo("samuel@example.com");
        assertThat(result.maskedEmail()).isEqualTo("s***@example.com");
    }

    @Test
    void aValidationErrorListsEveryProblemAtOnce() {
        RegisterCustomerCommand bad = new RegisterCustomerCommand("x", "no-es-un-correo", "corta", false);

        assertThatThrownBy(() -> fx.register.register(bad))
                .isInstanceOfSatisfying(ValidationFailedException.class, e -> assertThat(e.violations())
                        .extracting(FieldViolation::field)
                        .containsExactlyInAnyOrder("username", "email", "password", "acceptTerms"));
    }

    @Test
    void theCommandNeverShowsThePasswordInLogs() {
        String text = fx.command("samuel", "samuel@example.com").toString();

        assertThat(text).doesNotContain(IdentityTestFixture.PASSWORD).doesNotContain("samuel@example.com");
    }

    @Test
    void theCodeEventNeverShowsTheCodeInLogs() {
        fx.register.register(fx.command("samuel", "samuel@example.com"));

        assertThat(fx.events.last(IdentityEvents.VerificationCodeIssued.class).toString()).doesNotContain(fx.lastCode());
        assertThat(List.of(fx.lastCode())).hasSize(1);
    }
}

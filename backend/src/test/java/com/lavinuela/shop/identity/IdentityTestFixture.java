package com.lavinuela.shop.identity;

import com.lavinuela.shop.identity.application.RegisterCustomerCommand;
import com.lavinuela.shop.identity.application.RegisterCustomerService;
import com.lavinuela.shop.identity.application.ResendVerificationService;
import com.lavinuela.shop.identity.application.VerificationCodeService;
import com.lavinuela.shop.identity.application.VerifyEmailService;
import com.lavinuela.shop.identity.domain.port.PasswordHasher;
import com.lavinuela.shop.identity.domain.service.AccountPolicy;
import com.lavinuela.shop.identity.infrastructure.config.IdentityProperties;
import com.lavinuela.shop.identity.infrastructure.security.HmacCodeHasher;
import com.lavinuela.shop.identity.infrastructure.security.SecureRandomCodeGenerator;
import com.lavinuela.shop.testsupport.MutableClock;
import com.lavinuela.shop.testsupport.RecordingEventPublisher;
import java.util.Set;

/** Monta los casos de uso del registro con piezas en memoria (sin Spring ni base de datos). */
final class IdentityTestFixture {

    static final String PASSWORD = "Una-contraseña-larga-y-rara-2026";

    final MutableClock clock = new MutableClock();
    final InMemoryIdentityStore store = new InMemoryIdentityStore();
    final RecordingEventPublisher events = new RecordingEventPublisher();
    final AccountPolicy policy = new AccountPolicy(Set.of("password123", "1234567890"));
    final HmacCodeHasher codeHasher =
            new HmacCodeHasher(new IdentityProperties("clave-de-pruebas-0123456789-0123456789"));

    final RegisterCustomerService register;
    final VerifyEmailService verify;
    final ResendVerificationService resend;

    IdentityTestFixture() {
        PasswordHasher fastHasher = new PasswordHasher() {
            @Override
            public String hash(CharSequence rawPassword) {
                return "hash:" + rawPassword;
            }

            @Override
            public boolean matches(CharSequence rawPassword, String storedHash) {
                return storedHash.equals("hash:" + rawPassword);
            }
        };
        VerificationCodeService codes =
                new VerificationCodeService(store, new SecureRandomCodeGenerator(), codeHasher, events, clock);
        register = new RegisterCustomerService(store, codes, policy, fastHasher, events, clock);
        verify = new VerifyEmailService(store, store, codeHasher, policy, clock);
        resend = new ResendVerificationService(store, codes, policy);
    }

    RegisterCustomerCommand command(String username, String email) {
        return new RegisterCustomerCommand(username, email, PASSWORD, true);
    }

    /** Código en claro del último correo de verificación pedido (el que viaja en el evento). */
    String lastCode() {
        return events.last(IdentityEvents.VerificationCodeIssued.class).code();
    }

    int codesIssued() {
        return events.of(IdentityEvents.VerificationCodeIssued.class).size();
    }
}

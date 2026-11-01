package com.lavinuela.shop.identity;

import com.lavinuela.shop.identity.application.CurrentUserService;
import com.lavinuela.shop.identity.application.LoginAttemptLimiter;
import com.lavinuela.shop.identity.application.LoginService;
import com.lavinuela.shop.identity.application.LogoutService;
import com.lavinuela.shop.identity.application.RefreshSessionService;
import com.lavinuela.shop.identity.application.RegisterCustomerCommand;
import com.lavinuela.shop.identity.application.RegisterCustomerService;
import com.lavinuela.shop.identity.application.ResendVerificationService;
import com.lavinuela.shop.identity.application.SessionIssuer;
import com.lavinuela.shop.identity.application.VerificationCodeService;
import com.lavinuela.shop.identity.application.VerifyEmailService;
import com.lavinuela.shop.identity.domain.port.PasswordHasher;
import com.lavinuela.shop.identity.domain.service.AccountPolicy;
import com.lavinuela.shop.identity.infrastructure.config.IdentityProperties;
import com.lavinuela.shop.identity.infrastructure.security.HmacCodeHasher;
import com.lavinuela.shop.identity.infrastructure.security.Sha256SessionTokenCodec;
import com.lavinuela.shop.identity.infrastructure.security.SecureRandomCodeGenerator;
import com.lavinuela.shop.testsupport.MutableClock;
import com.lavinuela.shop.testsupport.RecordingEventPublisher;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/** Monta los casos de uso del registro con piezas en memoria (sin Spring ni base de datos). */
final class IdentityTestFixture {

    static final String PASSWORD = "Una-contraseña-larga-y-rara-2026";

    final MutableClock clock = new MutableClock();
    final InMemoryIdentityStore store = new InMemoryIdentityStore();
    final RecordingEventPublisher events = new RecordingEventPublisher();
    final AccountPolicy policy = new AccountPolicy(Set.of("password123", "1234567890"));
    final HmacCodeHasher codeHasher =
            new HmacCodeHasher(new IdentityProperties("clave-de-pruebas-0123456789-0123456789", "firma-de-pruebas-0123456789-0123456789-abc", false));

    /** Cuántas veces se ha comprobado una contraseña (para comprobar que con un correo desconocido también se comprueba). */
    final AtomicInteger passwordChecks = new AtomicInteger();

    final FakeAccessTokenIssuer accessTokens = new FakeAccessTokenIssuer();
    final Sha256SessionTokenCodec tokenCodec = new Sha256SessionTokenCodec();
    final LoginAttemptLimiter limiter = new LoginAttemptLimiter(clock);

    final RegisterCustomerService register;
    final VerifyEmailService verify;
    final ResendVerificationService resend;
    final LoginService login;
    final RefreshSessionService refresh;
    final LogoutService logout;
    final CurrentUserService currentUser;

    IdentityTestFixture() {
        PasswordHasher fastHasher = new PasswordHasher() {
            @Override
            public String hash(CharSequence rawPassword) {
                return "hash:" + rawPassword;
            }

            @Override
            public boolean matches(CharSequence rawPassword, String storedHash) {
                passwordChecks.incrementAndGet();
                return storedHash.equals("hash:" + rawPassword);
            }
        };
        VerificationCodeService codes =
                new VerificationCodeService(store, new SecureRandomCodeGenerator(), codeHasher, events, clock);
        register = new RegisterCustomerService(store, codes, policy, fastHasher, events, clock);
        verify = new VerifyEmailService(store, store, codeHasher, policy, clock);
        resend = new ResendVerificationService(store, codes, policy);
        SessionIssuer sessions = new SessionIssuer(accessTokens, tokenCodec, store, clock);
        login = new LoginService(store, fastHasher, sessions, limiter, policy, clock);
        refresh = new RefreshSessionService(store, tokenCodec, store, sessions, clock);
        logout = new LogoutService(store, tokenCodec, clock);
        currentUser = new CurrentUserService(store);
    }

    RegisterCustomerCommand command(String username, String email) {
        return new RegisterCustomerCommand(username, email, PASSWORD, true);
    }

    /** Registra una cuenta y confirma su correo: queda activa y lista para iniciar sesión. */
    void activeAccount(String username, String email) {
        register.register(command(username, email));
        verify.verify(email, lastCode());
    }

    /** Código en claro del último correo de verificación pedido (el que viaja en el evento). */
    String lastCode() {
        return events.last(IdentityEvents.VerificationCodeIssued.class).code();
    }

    int codesIssued() {
        return events.of(IdentityEvents.VerificationCodeIssued.class).size();
    }
}

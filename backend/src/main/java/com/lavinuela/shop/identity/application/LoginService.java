package com.lavinuela.shop.identity.application;

import com.lavinuela.shop.identity.domain.model.User;
import com.lavinuela.shop.identity.domain.model.UserStatus;
import com.lavinuela.shop.identity.domain.port.PasswordHasher;
import com.lavinuela.shop.identity.domain.port.UserRepository;
import com.lavinuela.shop.identity.domain.service.AccountPolicy;
import com.lavinuela.shop.shared.domain.PublicId;
import com.lavinuela.shop.shared.error.BusinessRuleViolation;
import com.lavinuela.shop.shared.error.ErrorCode;
import com.lavinuela.shop.shared.logging.Masking;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Inicio de sesión con correo y contraseña.
 *
 * <p>Defensas:
 * <ul>
 *   <li><b>Misma respuesta y mismo tiempo</b> si el correo no existe o la contraseña es incorrecta: cuando no hay
 *       cuenta se comprueba la contraseña contra una huella ficticia, para que no se note por lo que tarda.</li>
 *   <li><b>Bloqueo tras {@code MAX_FAILED_ATTEMPTS} fallos</b> por correo, exista o no (ver {@link LoginAttemptLimiter}).</li>
 *   <li>La falta de verificación del correo solo se revela <b>después</b> de acertar la contraseña.</li>
 *   <li>Las cuentas bloqueadas o borradas reciben la misma respuesta genérica.</li>
 * </ul>
 */
@Service
public class LoginService {

    private static final Logger LOG = LoggerFactory.getLogger(LoginService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository users;
    private final PasswordHasher passwordHasher;
    private final SessionIssuer sessions;
    private final LoginAttemptLimiter limiter;
    private final AccountPolicy policy;
    private final Clock clock;
    private final String dummyHash;

    public LoginService(
            UserRepository users,
            PasswordHasher passwordHasher,
            SessionIssuer sessions,
            LoginAttemptLimiter limiter,
            AccountPolicy policy,
            Clock clock) {
        this.users = users;
        this.passwordHasher = passwordHasher;
        this.sessions = sessions;
        this.limiter = limiter;
        this.policy = policy;
        this.clock = clock;
        this.dummyHash = passwordHasher.hash("contraseña-ficticia-que-nadie-tiene-0123456789");
    }

    @Transactional
    public AuthSession login(String rawEmail, String password, @Nullable String userAgent) {
        String email = policy.normalizeEmail(rawEmail);
        if (limiter.isBlocked(email)) {
            throw new BusinessRuleViolation(ErrorCode.AUTH_ACCOUNT_LOCKED);
        }

        Optional<User> found = users.findByEmail(email);
        boolean passwordMatches = passwordHasher.matches(password, found.map(User::passwordHash).orElse(dummyHash));
        if (found.isEmpty() || !passwordMatches) {
            limiter.recordFailure(email);
            throw new BusinessRuleViolation(ErrorCode.AUTH_INVALID_CREDENTIALS);
        }

        User user = found.get();
        if (user.status() == UserStatus.LOCKED || user.status() == UserStatus.DELETED) {
            limiter.recordFailure(email);
            throw new BusinessRuleViolation(ErrorCode.AUTH_INVALID_CREDENTIALS);
        }
        if (user.isPendingVerification()) {
            throw new BusinessRuleViolation(ErrorCode.AUTH_EMAIL_NOT_VERIFIED);
        }

        limiter.reset(email);
        user.registerLogin(clock.instant());
        users.save(user);
        AuthSession session = sessions.issue(user, PublicId.generate(clock, RANDOM).value(), userAgent).session();
        LOG.info("Inicio de sesión [id={}, correo={}]", user.publicId().value(), Masking.email(email));
        return session;
    }
}

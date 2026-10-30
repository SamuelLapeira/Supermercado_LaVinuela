package com.lavinuela.shop.identity.application;

import com.lavinuela.shop.identity.domain.model.User;
import com.lavinuela.shop.identity.domain.port.CodeHasher;
import com.lavinuela.shop.identity.domain.port.UserRepository;
import com.lavinuela.shop.identity.domain.port.VerificationCodeStore;
import com.lavinuela.shop.identity.domain.port.VerificationCodeStore.IssuedCode;
import com.lavinuela.shop.identity.domain.service.AccountPolicy;
import com.lavinuela.shop.identity.domain.service.VerificationPolicy;
import com.lavinuela.shop.shared.error.BusinessRuleViolation;
import com.lavinuela.shop.shared.error.DomainException;
import com.lavinuela.shop.shared.error.ErrorCode;
import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Confirma el correo con el código de 6 cifras y activa la cuenta.
 *
 * <p>Cada código admite {@code MAX_FAILED_ATTEMPTS} intentos fallidos; después hay que pedir otro. Los intentos
 * fallidos se guardan aunque la operación termine en error (por eso {@code noRollbackFor}): si no, se podría probar
 * códigos sin límite. La respuesta es la misma si el correo no existe o no está pendiente, para no revelar cuentas.
 */
@Service
public class VerifyEmailService {

    private static final Logger LOG = LoggerFactory.getLogger(VerifyEmailService.class);

    private final UserRepository users;
    private final VerificationCodeStore store;
    private final CodeHasher hasher;
    private final AccountPolicy policy;
    private final Clock clock;

    public VerifyEmailService(
            UserRepository users,
            VerificationCodeStore store,
            CodeHasher hasher,
            AccountPolicy policy,
            Clock clock) {
        this.users = users;
        this.store = store;
        this.hasher = hasher;
        this.policy = policy;
        this.clock = clock;
    }

    @Transactional(noRollbackFor = DomainException.class)
    public void verify(String rawEmail, String code) {
        String email = policy.normalizeEmail(rawEmail);
        User user = users.findByEmail(email)
                .filter(User::isPendingVerification)
                .orElseThrow(VerifyEmailService::invalidCode);
        long userId = user.requireId();

        IssuedCode issued = store.findLatestActive(userId).orElseThrow(VerifyEmailService::invalidCode);
        Instant now = clock.instant();

        if (issued.failedAttempts() >= VerificationPolicy.MAX_FAILED_ATTEMPTS) {
            throw tooManyAttempts();
        }
        if (!issued.expiresAt().isAfter(now)) {
            throw new BusinessRuleViolation(ErrorCode.VERIFICATION_CODE_EXPIRED);
        }
        if (!hasher.matches(userId, code, issued.codeHash())) {
            store.recordFailedAttempt(issued.id());
            LOG.info("Código de verificación incorrecto [id={}, intento={}]",
                    user.publicId().value(), issued.failedAttempts() + 1);
            throw issued.failedAttempts() + 1 >= VerificationPolicy.MAX_FAILED_ATTEMPTS
                    ? tooManyAttempts()
                    : invalidCode();
        }

        store.markUsed(issued.id(), now);
        user.verifyEmail(now);
        users.save(user);
        LOG.info("Correo verificado, cuenta activada [id={}]", user.publicId().value());
    }

    private static BusinessRuleViolation invalidCode() {
        return new BusinessRuleViolation(ErrorCode.VERIFICATION_CODE_INVALID);
    }

    private static BusinessRuleViolation tooManyAttempts() {
        return new BusinessRuleViolation(ErrorCode.VERIFICATION_TOO_MANY_ATTEMPTS);
    }
}

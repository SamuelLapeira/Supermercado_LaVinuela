package com.lavinuela.shop.identity.application;

import com.lavinuela.shop.identity.IdentityEvents;
import com.lavinuela.shop.identity.domain.model.LegalDocuments;
import com.lavinuela.shop.identity.domain.model.User;
import com.lavinuela.shop.identity.domain.port.PasswordHasher;
import com.lavinuela.shop.identity.domain.port.UserRepository;
import com.lavinuela.shop.identity.domain.service.AccountPolicy;
import com.lavinuela.shop.identity.domain.service.VerificationPolicy;
import com.lavinuela.shop.shared.domain.PublicId;
import com.lavinuela.shop.shared.error.ConflictException;
import com.lavinuela.shop.shared.error.ErrorCode;
import com.lavinuela.shop.shared.error.FieldViolation;
import com.lavinuela.shop.shared.error.ValidationFailedException;
import com.lavinuela.shop.shared.logging.Masking;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registro de una cuenta nueva: usuario, correo y contraseña. La cuenta queda pendiente hasta que se confirme el
 * correo con el código que se envía.
 *
 * <p><b>No revela qué correos tienen cuenta.</b> Si el correo ya existe, la respuesta es la misma que para uno nuevo:
 * si la cuenta estaba pendiente se le envía un código nuevo y, si ya estaba activa, se avisa por correo a su titular.
 * Lo que sí se puede saber es si un nombre de usuario está cogido (como en cualquier sitio con nombres de usuario).
 */
@Service
public class RegisterCustomerService {

    private static final Logger LOG = LoggerFactory.getLogger(RegisterCustomerService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository users;
    private final VerificationCodeService codes;
    private final AccountPolicy policy;
    private final PasswordHasher passwordHasher;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public RegisterCustomerService(
            UserRepository users,
            VerificationCodeService codes,
            AccountPolicy policy,
            PasswordHasher passwordHasher,
            ApplicationEventPublisher events,
            Clock clock) {
        this.users = users;
        this.codes = codes;
        this.policy = policy;
        this.passwordHasher = passwordHasher;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public RegistrationResult register(RegisterCustomerCommand command) {
        String username = policy.normalizeUsername(command.username());
        String email = policy.normalizeEmail(command.email());

        List<FieldViolation> violations = policy.validate(username, email, command.password());
        if (!command.termsAccepted()) {
            violations.add(FieldViolation.of("acceptTerms",
                    "Tienes que aceptar las condiciones y la política de privacidad."));
        }
        if (!violations.isEmpty()) {
            throw new ValidationFailedException(violations);
        }

        Optional<User> existing = users.findByEmail(email);
        if (existing.isPresent()) {
            handleExistingEmail(existing.get(), username);
        } else {
            createAccount(username, email, command.password());
        }
        return new RegistrationResult(
                Masking.email(email),
                (int) VerificationPolicy.VALIDITY.toSeconds(),
                (int) VerificationPolicy.RESEND_COOLDOWN.toSeconds());
    }

    private void createAccount(String username, String email, String password) {
        if (users.existsByUsername(username)) {
            throw new ConflictException(ErrorCode.USERNAME_TAKEN);
        }
        User user = User.register(PublicId.generate(clock, RANDOM), username, email, passwordHasher.hash(password));
        User saved = users.save(user);
        users.recordConsent(saved.requireId(), "TERMS", LegalDocuments.CURRENT_VERSION);
        users.recordConsent(saved.requireId(), "PRIVACY", LegalDocuments.CURRENT_VERSION);
        codes.issue(saved);
        LOG.info("Cuenta creada pendiente de verificar [usuario={}, id={}]", username, saved.publicId().value());
    }

    private void handleExistingEmail(User existing, String requestedUsername) {
        if (existing.isPendingVerification()) {
            // La persona probablemente no recibió o perdió el código: se le manda uno nuevo (con sus límites).
            codes.issue(existing);
        } else {
            events.publishEvent(new IdentityEvents.AccountAlreadyExists(existing.email(), existing.username()));
        }
        LOG.info("Registro con un correo que ya existe [correo={}, usuarioPedido={}]",
                Masking.email(existing.email()), requestedUsername);
    }
}

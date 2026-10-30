package com.lavinuela.shop.identity.application;

import com.lavinuela.shop.identity.IdentityEvents;
import com.lavinuela.shop.identity.domain.model.User;
import com.lavinuela.shop.identity.domain.port.CodeHasher;
import com.lavinuela.shop.identity.domain.port.VerificationCodeGenerator;
import com.lavinuela.shop.identity.domain.port.VerificationCodeStore;
import com.lavinuela.shop.identity.domain.service.VerificationPolicy;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/**
 * Emite códigos de verificación respetando los límites: una espera mínima entre códigos y un máximo por hora.
 *
 * <p>El código se guarda solo como huella. El código en claro viaja únicamente en el evento (en memoria) que
 * dispara el envío del correo, después de confirmar la transacción.
 */
@Service
public class VerificationCodeService {

    private final VerificationCodeStore store;
    private final VerificationCodeGenerator generator;
    private final CodeHasher hasher;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public VerificationCodeService(
            VerificationCodeStore store,
            VerificationCodeGenerator generator,
            CodeHasher hasher,
            ApplicationEventPublisher events,
            Clock clock) {
        this.store = store;
        this.generator = generator;
        this.hasher = hasher;
        this.events = events;
        this.clock = clock;
    }

    /**
     * Emite un código nuevo y publica el evento para enviarlo.
     *
     * @return {@code false} si no se ha emitido porque se ha superado un límite (en ese caso no se hace nada)
     */
    public boolean issue(User user) {
        long userId = user.requireId();
        Instant now = clock.instant();

        Optional<Instant> last = store.lastIssuedAt(userId);
        if (last.isPresent() && last.get().plus(VerificationPolicy.RESEND_COOLDOWN).isAfter(now)) {
            return false;
        }
        if (store.countIssuedSince(userId, now.minus(Duration.ofHours(1))) >= VerificationPolicy.MAX_CODES_PER_HOUR) {
            return false;
        }

        String code = generator.generate();
        store.invalidateActive(userId, now);
        store.save(userId, hasher.hash(userId, code), now, now.plus(VerificationPolicy.VALIDITY));
        events.publishEvent(new IdentityEvents.VerificationCodeIssued(
                user.email(), user.username(), code, (int) VerificationPolicy.VALIDITY.toMinutes()));
        return true;
    }
}

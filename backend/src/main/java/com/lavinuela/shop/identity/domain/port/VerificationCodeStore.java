package com.lavinuela.shop.identity.domain.port;

import java.time.Instant;
import java.util.Optional;

/** Dónde se guardan los códigos de verificación de correo. Nunca se guarda el código, solo su huella. */
public interface VerificationCodeStore {

    /** Código emitido que todavía no se ha usado. */
    record IssuedCode(long id, String codeHash, Instant expiresAt, int failedAttempts) {
    }

    /** Anula los códigos pendientes de la cuenta (cuando se emite uno nuevo, los anteriores dejan de valer). */
    void invalidateActive(long userId, Instant now);

    void save(long userId, String codeHash, Instant createdAt, Instant expiresAt);

    /** El código pendiente más reciente de la cuenta. */
    Optional<IssuedCode> findLatestActive(long userId);

    void recordFailedAttempt(long codeId);

    void markUsed(long codeId, Instant now);

    /** Cuántos códigos se han emitido para la cuenta desde ese instante. */
    int countIssuedSince(long userId, Instant since);

    Optional<Instant> lastIssuedAt(long userId);
}

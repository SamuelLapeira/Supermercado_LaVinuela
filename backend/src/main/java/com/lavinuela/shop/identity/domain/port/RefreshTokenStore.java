package com.lavinuela.shop.identity.domain.port;

import java.time.Instant;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Dónde se guardan los tokens de renovación. Nunca se guarda el token, solo su huella.
 *
 * <p>Cada inicio de sesión abre una «familia» de tokens: cada vez que se renueva, el token usado se sustituye por uno
 * nuevo de la misma familia. Si alguien presenta un token que ya se había sustituido, es que lo han copiado: se anula
 * toda la familia.
 */
public interface RefreshTokenStore {

    record StoredRefreshToken(
            long id,
            long userId,
            String familyId,
            Instant expiresAt,
            @Nullable Instant revokedAt,
            @Nullable Long replacedById) {
    }

    /** @return el identificador del token guardado */
    long save(long userId, String familyId, String tokenHash, Instant createdAt, Instant expiresAt, @Nullable String userAgent);

    Optional<StoredRefreshToken> findByHash(String tokenHash);

    /** Marca el token como usado y sustituido por otro. */
    void markRotated(long oldId, long newId, Instant now);

    /** Anula todos los tokens de la familia (cerrar sesión o robo detectado). */
    void revokeFamily(String familyId, Instant now);
}

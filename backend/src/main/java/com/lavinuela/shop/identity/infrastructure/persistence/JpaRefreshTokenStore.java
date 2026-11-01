package com.lavinuela.shop.identity.infrastructure.persistence;

import com.lavinuela.shop.identity.domain.port.RefreshTokenStore;
import java.time.Instant;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Repository;

/** Adaptador: guarda las huellas de los tokens de renovación en {@code refresh_tokens}. */
@Repository
class JpaRefreshTokenStore implements RefreshTokenStore {

    private final RefreshTokenJpaRepository tokens;

    JpaRefreshTokenStore(RefreshTokenJpaRepository tokens) {
        this.tokens = tokens;
    }

    @Override
    public long save(long userId, String familyId, String tokenHash, Instant createdAt, Instant expiresAt, @Nullable String userAgent) {
        return tokens.saveAndFlush(RefreshTokenJpaEntity.create(userId, familyId, tokenHash, createdAt, expiresAt, userAgent)).requireId();
    }

    @Override
    public Optional<StoredRefreshToken> findByHash(String tokenHash) {
        return tokens.findByTokenHash(tokenHash).map(entity -> new StoredRefreshToken(
                entity.requireId(),
                entity.getUserId(),
                entity.getFamilyId(),
                entity.getExpiresAt(),
                entity.getRevokedAt(),
                entity.getReplacedById()));
    }

    @Override
    public void markRotated(long oldId, long newId, Instant now) {
        RefreshTokenJpaEntity entity = tokens.findById(oldId).orElseThrow(() -> new IllegalStateException("El token ya no existe"));
        entity.rotateTo(newId, now);
        tokens.saveAndFlush(entity);
    }

    @Override
    public void revokeFamily(String familyId, Instant now) {
        tokens.revokeFamily(familyId, now);
    }
}

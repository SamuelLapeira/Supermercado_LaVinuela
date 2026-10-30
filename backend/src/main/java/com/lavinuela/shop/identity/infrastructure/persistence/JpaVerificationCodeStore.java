package com.lavinuela.shop.identity.infrastructure.persistence;

import com.lavinuela.shop.identity.domain.port.VerificationCodeStore;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** Adaptador: guarda las huellas de los códigos de verificación en {@code user_tokens}. */
@Repository
class JpaVerificationCodeStore implements VerificationCodeStore {

    private static final String TYPE = UserTokenJpaEntity.EMAIL_VERIFICATION;

    private final UserTokenJpaRepository tokens;

    JpaVerificationCodeStore(UserTokenJpaRepository tokens) {
        this.tokens = tokens;
    }

    @Override
    public void invalidateActive(long userId, Instant now) {
        tokens.invalidateActive(userId, TYPE, now);
    }

    @Override
    public void save(long userId, String codeHash, Instant createdAt, Instant expiresAt) {
        tokens.saveAndFlush(UserTokenJpaEntity.verificationCode(userId, codeHash, createdAt, expiresAt));
    }

    @Override
    public Optional<IssuedCode> findLatestActive(long userId) {
        return tokens.findFirstByUserIdAndTokenTypeAndUsedAtIsNullOrderByIdDesc(userId, TYPE)
                .map(entity -> new IssuedCode(
                        entity.requireId(), entity.getTokenHash(), entity.getExpiresAt(), entity.getFailedAttempts()));
    }

    @Override
    public void recordFailedAttempt(long codeId) {
        UserTokenJpaEntity entity = tokens.findById(codeId)
                .orElseThrow(() -> new IllegalStateException("El código ya no existe"));
        entity.registerFailedAttempt();
        tokens.saveAndFlush(entity);
    }

    @Override
    public void markUsed(long codeId, Instant now) {
        UserTokenJpaEntity entity = tokens.findById(codeId)
                .orElseThrow(() -> new IllegalStateException("El código ya no existe"));
        entity.markUsed(now);
        tokens.saveAndFlush(entity);
    }

    @Override
    public int countIssuedSince(long userId, Instant since) {
        return tokens.countByUserIdAndTokenTypeAndCreatedAtAfter(userId, TYPE, since);
    }

    @Override
    public Optional<Instant> lastIssuedAt(long userId) {
        return tokens.findFirstByUserIdAndTokenTypeOrderByIdDesc(userId, TYPE).map(UserTokenJpaEntity::getCreatedAt);
    }
}

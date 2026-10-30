package com.lavinuela.shop.identity.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.jspecify.annotations.Nullable;

/** Tabla {@code user_tokens}: códigos de verificación (solo su huella), y más adelante enlaces de recuperación. */
@Entity
@Table(name = "user_tokens")
public class UserTokenJpaEntity {

    /** Valor de {@code token_type} para los códigos de verificación de correo. */
    public static final String EMAIL_VERIFICATION = "EMAIL_VERIFICATION";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private @Nullable Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private long userId;

    @Column(name = "token_type", nullable = false, updatable = false, length = 20)
    private String tokenType = EMAIL_VERIFICATION;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "token_hash", nullable = false, updatable = false, length = 64)
    private String tokenHash = "";

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt = Instant.EPOCH;

    @Column(name = "used_at")
    private @Nullable Instant usedAt;

    @Column(name = "failed_attempts", nullable = false)
    private short failedAttempts;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.EPOCH;

    protected UserTokenJpaEntity() {
        // Lo exige JPA.
    }

    public static UserTokenJpaEntity verificationCode(long userId, String tokenHash, Instant createdAt, Instant expiresAt) {
        UserTokenJpaEntity entity = new UserTokenJpaEntity();
        entity.userId = userId;
        entity.tokenHash = tokenHash;
        entity.createdAt = createdAt;
        entity.expiresAt = expiresAt;
        return entity;
    }

    public long requireId() {
        if (id == null) {
            throw new IllegalStateException("El código todavía no se ha guardado");
        }
        return id;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public int getFailedAttempts() {
        return failedAttempts;
    }

    public void registerFailedAttempt() {
        failedAttempts++;
    }

    public void markUsed(Instant now) {
        this.usedAt = now;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

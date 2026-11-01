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

/** Tabla {@code refresh_tokens}: los tokens de renovación (solo su huella), agrupados por familia. */
@Entity
@Table(name = "refresh_tokens")
public class RefreshTokenJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private @Nullable Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private long userId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "token_hash", nullable = false, updatable = false, length = 64)
    private String tokenHash = "";

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "family_id", nullable = false, updatable = false, length = 26)
    private String familyId = "";

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt = Instant.EPOCH;

    @Column(name = "revoked_at")
    private @Nullable Instant revokedAt;

    @Column(name = "replaced_by_id")
    private @Nullable Long replacedById;

    @Column(name = "user_agent", length = 255)
    private @Nullable String userAgent;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.EPOCH;

    protected RefreshTokenJpaEntity() {
        // Lo exige JPA.
    }

    public static RefreshTokenJpaEntity create(
            long userId, String familyId, String tokenHash, Instant createdAt, Instant expiresAt, @Nullable String userAgent) {
        RefreshTokenJpaEntity entity = new RefreshTokenJpaEntity();
        entity.userId = userId;
        entity.familyId = familyId;
        entity.tokenHash = tokenHash;
        entity.createdAt = createdAt;
        entity.expiresAt = expiresAt;
        entity.userAgent = userAgent;
        return entity;
    }

    public long requireId() {
        if (id == null) {
            throw new IllegalStateException("El token todavía no se ha guardado");
        }
        return id;
    }

    public long getUserId() {
        return userId;
    }

    public String getFamilyId() {
        return familyId;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public @Nullable Instant getRevokedAt() {
        return revokedAt;
    }

    public @Nullable Long getReplacedById() {
        return replacedById;
    }

    /** El token se ha usado para renovar: queda anulado y apunta al que lo sustituye. */
    public void rotateTo(long newId, Instant now) {
        this.revokedAt = now;
        this.replacedById = newId;
    }
}

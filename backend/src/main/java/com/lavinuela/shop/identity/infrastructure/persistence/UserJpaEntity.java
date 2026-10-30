package com.lavinuela.shop.identity.infrastructure.persistence;

import com.lavinuela.shop.identity.domain.model.UserStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.jspecify.annotations.Nullable;

/**
 * Tabla {@code users}. Solo se mapean las columnas que usa el registro; el resto (nombre, apellidos, teléfono,
 * bloqueo...) se añadirán con las historias que las necesitan y mientras tanto toman su valor por defecto.
 */
@Entity
@Table(name = "users")
public class UserJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private @Nullable Long id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "public_id", nullable = false, updatable = false, length = 26)
    private String publicId = "";

    @Column(nullable = false, length = 254)
    private String email = "";

    @Column(length = 30)
    private @Nullable String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash = "";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private UserStatus status = UserStatus.PENDING_VERIFICATION;

    @Column(name = "email_verified_at")
    private @Nullable Instant emailVerifiedAt;

    @Column(name = "marketing_consent", nullable = false)
    private boolean marketingConsent;

    @Version
    private int version;

    protected UserJpaEntity() {
        // Lo exige JPA.
    }

    public @Nullable Long getId() {
        return id;
    }

    public String getPublicId() {
        return publicId;
    }

    public void setPublicId(String publicId) {
        this.publicId = publicId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public @Nullable String getUsername() {
        return username;
    }

    public void setUsername(@Nullable String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public @Nullable Instant getEmailVerifiedAt() {
        return emailVerifiedAt;
    }

    public void setEmailVerifiedAt(@Nullable Instant emailVerifiedAt) {
        this.emailVerifiedAt = emailVerifiedAt;
    }

    public boolean isMarketingConsent() {
        return marketingConsent;
    }

    public void setMarketingConsent(boolean marketingConsent) {
        this.marketingConsent = marketingConsent;
    }

    public int getVersion() {
        return version;
    }

    public static UserJpaEntity create() {
        return new UserJpaEntity();
    }
}

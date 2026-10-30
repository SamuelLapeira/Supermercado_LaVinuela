package com.lavinuela.shop.identity.domain.model;

import com.lavinuela.shop.shared.domain.PublicId;
import java.time.Instant;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Cuenta de usuario. Para registrarse solo hacen falta nombre de usuario, correo y contraseña; el nombre real, el
 * teléfono y las direcciones se piden en el primer pedido.
 *
 * <p>Esta clase no sabe nada de bases de datos ni de Spring: las reglas son suyas.
 */
public final class User {

    private final @Nullable Long id;
    private final PublicId publicId;
    private final String username;
    private final String email;
    private final String passwordHash;
    private UserStatus status;
    private @Nullable Instant emailVerifiedAt;
    private final boolean marketingConsent;
    private final int version;

    public User(
            @Nullable Long id,
            PublicId publicId,
            String username,
            String email,
            String passwordHash,
            UserStatus status,
            @Nullable Instant emailVerifiedAt,
            boolean marketingConsent,
            int version) {
        this.id = id;
        this.publicId = Objects.requireNonNull(publicId);
        this.username = Objects.requireNonNull(username);
        this.email = Objects.requireNonNull(email);
        this.passwordHash = Objects.requireNonNull(passwordHash);
        this.status = Objects.requireNonNull(status);
        this.emailVerifiedAt = emailVerifiedAt;
        this.marketingConsent = marketingConsent;
        this.version = version;
    }

    /** Cuenta nueva: queda pendiente de verificar el correo. */
    public static User register(PublicId publicId, String username, String email, String passwordHash) {
        return new User(null, publicId, username, email, passwordHash, UserStatus.PENDING_VERIFICATION, null, false, 0);
    }

    /** Confirma el correo y activa la cuenta. */
    public void verifyEmail(Instant now) {
        if (status != UserStatus.PENDING_VERIFICATION) {
            throw new IllegalStateException("Solo se puede verificar una cuenta pendiente de verificación");
        }
        this.status = UserStatus.ACTIVE;
        this.emailVerifiedAt = Objects.requireNonNull(now);
    }

    public boolean isPendingVerification() {
        return status == UserStatus.PENDING_VERIFICATION;
    }

    public Long requireId() {
        if (id == null) {
            throw new IllegalStateException("El usuario todavía no se ha guardado");
        }
        return id;
    }

    public @Nullable Long id() {
        return id;
    }

    public PublicId publicId() {
        return publicId;
    }

    public String username() {
        return username;
    }

    public String email() {
        return email;
    }

    public String passwordHash() {
        return passwordHash;
    }

    public UserStatus status() {
        return status;
    }

    public @Nullable Instant emailVerifiedAt() {
        return emailVerifiedAt;
    }

    public boolean marketingConsent() {
        return marketingConsent;
    }

    public int version() {
        return version;
    }
}

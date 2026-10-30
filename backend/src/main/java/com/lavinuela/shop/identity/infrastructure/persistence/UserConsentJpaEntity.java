package com.lavinuela.shop.identity.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.jspecify.annotations.Nullable;

/** Tabla {@code user_consents}: qué documento legal (y qué versión) aceptó cada persona. */
@Entity
@Table(name = "user_consents")
public class UserConsentJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private @Nullable Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private long userId;

    @Column(name = "consent_type", nullable = false, updatable = false, length = 16)
    private String consentType = "";

    @Column(name = "document_version", nullable = false, updatable = false, length = 20)
    private String documentVersion = "";

    @Column(nullable = false, updatable = false)
    private boolean granted = true;

    protected UserConsentJpaEntity() {
        // Lo exige JPA.
    }

    public static UserConsentJpaEntity granted(long userId, String consentType, String documentVersion) {
        UserConsentJpaEntity entity = new UserConsentJpaEntity();
        entity.userId = userId;
        entity.consentType = consentType;
        entity.documentVersion = documentVersion;
        return entity;
    }
}

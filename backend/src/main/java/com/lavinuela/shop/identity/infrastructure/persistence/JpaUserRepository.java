package com.lavinuela.shop.identity.infrastructure.persistence;

import com.lavinuela.shop.identity.domain.model.User;
import com.lavinuela.shop.identity.domain.port.UserRepository;
import com.lavinuela.shop.shared.domain.PublicId;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** Adaptador: guarda y lee las cuentas con JPA. Convierte entre la clase de dominio {@link User} y la entidad. */
@Repository
class JpaUserRepository implements UserRepository {

    private final UserJpaRepository users;
    private final UserConsentJpaRepository consents;

    JpaUserRepository(UserJpaRepository users, UserConsentJpaRepository consents) {
        this.users = users;
        this.consents = consents;
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return users.findByEmail(email).map(JpaUserRepository::toDomain);
    }

    @Override
    public boolean existsByUsername(String username) {
        return users.existsByUsername(username);
    }

    @Override
    public User save(User user) {
        UserJpaEntity entity = user.id() == null
                ? UserJpaEntity.create()
                : users.findById(user.requireId()).orElseThrow(() -> new IllegalStateException("La cuenta ya no existe"));
        entity.setPublicId(user.publicId().value());
        entity.setEmail(user.email());
        entity.setUsername(user.username());
        entity.setPasswordHash(user.passwordHash());
        entity.setStatus(user.status());
        entity.setEmailVerifiedAt(user.emailVerifiedAt());
        entity.setMarketingConsent(user.marketingConsent());
        return toDomain(users.saveAndFlush(entity));
    }

    @Override
    public void recordConsent(long userId, String consentType, String documentVersion) {
        consents.save(UserConsentJpaEntity.granted(userId, consentType, documentVersion));
    }

    private static User toDomain(UserJpaEntity entity) {
        String username = entity.getUsername();
        return new User(
                entity.getId(),
                PublicId.parse(entity.getPublicId()),
                username == null ? "" : username,
                entity.getEmail(),
                entity.getPasswordHash(),
                entity.getStatus(),
                entity.getEmailVerifiedAt(),
                entity.isMarketingConsent(),
                entity.getVersion());
    }
}

package com.lavinuela.shop.identity.infrastructure.persistence;

import com.lavinuela.shop.identity.domain.model.User;
import com.lavinuela.shop.identity.domain.port.UserRepository;
import com.lavinuela.shop.shared.domain.PublicId;
import java.util.Optional;
import java.util.Set;
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
        return users.findByEmail(email).map(this::toDomain);
    }

    @Override
    public Optional<User> findByPublicId(String publicId) {
        return users.findByPublicId(publicId).map(this::toDomain);
    }

    @Override
    public Optional<User> findById(long id) {
        return users.findById(id).map(this::toDomain);
    }

    @Override
    public void assignRole(long userId, String roleCode) {
        users.assignRole(userId, roleCode);
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
        entity.setLastLoginAt(user.lastLoginAt());
        return toDomain(users.saveAndFlush(entity));
    }

    @Override
    public void recordConsent(long userId, String consentType, String documentVersion) {
        consents.save(UserConsentJpaEntity.granted(userId, consentType, documentVersion));
    }

    private User toDomain(UserJpaEntity entity) {
        String username = entity.getUsername();
        Long id = entity.getId();
        Set<String> roles = id == null ? Set.of() : Set.copyOf(users.findRoleCodes(id));
        return new User(
                id,
                PublicId.parse(entity.getPublicId()),
                username == null ? "" : username,
                entity.getEmail(),
                entity.getPasswordHash(),
                entity.getStatus(),
                entity.getEmailVerifiedAt(),
                entity.isMarketingConsent(),
                entity.getVersion(),
                entity.getLastLoginAt(),
                roles);
    }
}

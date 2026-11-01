package com.lavinuela.shop.identity;

import com.lavinuela.shop.identity.domain.model.User;
import com.lavinuela.shop.identity.domain.model.UserStatus;
import com.lavinuela.shop.identity.domain.port.RefreshTokenStore;
import com.lavinuela.shop.identity.domain.port.UserRepository;
import com.lavinuela.shop.identity.domain.port.VerificationCodeStore;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Repositorio de cuentas y almacén de códigos en memoria, para probar los casos de uso sin base de datos ni Spring.
 * Se comportan como los reales: cada código tiene su identificador, su caducidad y su contador de intentos.
 */
final class InMemoryIdentityStore implements UserRepository, VerificationCodeStore, RefreshTokenStore {

    record Consent(long userId, String type, String version) {
    }

    private static final class Token {
        final long id;
        final long userId;
        final String hash;
        final Instant createdAt;
        final Instant expiresAt;
        int failedAttempts;
        boolean used;

        Token(long id, long userId, String hash, Instant createdAt, Instant expiresAt) {
            this.id = id;
            this.userId = userId;
            this.hash = hash;
            this.createdAt = createdAt;
            this.expiresAt = expiresAt;
        }
    }

    private final AtomicLong nextId = new AtomicLong(1);
    private final List<User> users = new ArrayList<>();
    private final List<Token> tokens = new ArrayList<>();
    private final List<Consent> consents = new ArrayList<>();
    private final List<RefreshRow> refreshRows = new ArrayList<>();

    /** Un token de renovación guardado (solo su huella). */
    static final class RefreshRow {
        final long id;
        final long userId;
        final String familyId;
        final String hash;
        final Instant expiresAt;
        Instant revokedAt;
        Long replacedById;

        RefreshRow(long id, long userId, String familyId, String hash, Instant expiresAt) {
            this.id = id;
            this.userId = userId;
            this.familyId = familyId;
            this.hash = hash;
            this.expiresAt = expiresAt;
        }
    }

    // ---- UserRepository
    @Override
    public Optional<User> findByEmail(String email) {
        return users.stream().filter(user -> user.email().equals(email)).findFirst();
    }

    @Override
    public boolean existsByUsername(String username) {
        return users.stream().anyMatch(user -> user.username().equals(username));
    }

    @Override
    public Optional<User> findByPublicId(String publicId) {
        return users.stream().filter(user -> user.publicId().value().equals(publicId)).findFirst();
    }

    @Override
    public Optional<User> findById(long id) {
        return users.stream().filter(user -> user.id() != null && user.id() == id).findFirst();
    }

    @Override
    public void assignRole(long userId, String roleCode) {
        for (int i = 0; i < users.size(); i++) {
            User current = users.get(i);
            if (current.id() != null && current.id() == userId) {
                Set<String> roles = new LinkedHashSet<>(current.roles());
                roles.add(roleCode);
                users.set(i, current.withRoles(roles));
            }
        }
    }

    @Override
    public User save(User user) {
        if (user.id() != null) {
            for (int i = 0; i < users.size(); i++) {
                User current = users.get(i);
                if (current.id() != null && current.id().equals(user.id())) {
                    users.set(i, user.withRoles(current.roles()));
                }
            }
            return user;
        }
        User saved = new User(nextId.getAndIncrement(), user.publicId(), user.username(), user.email(),
                user.passwordHash(), user.status(), user.emailVerifiedAt(), user.marketingConsent(), 0);
        users.add(saved);
        return saved;
    }

    @Override
    public void recordConsent(long userId, String consentType, String documentVersion) {
        consents.add(new Consent(userId, consentType, documentVersion));
    }

    // ---- VerificationCodeStore
    @Override
    public void invalidateActive(long userId, Instant now) {
        tokens.stream().filter(t -> t.userId == userId && !t.used).forEach(t -> t.used = true);
    }

    @Override
    public void save(long userId, String codeHash, Instant createdAt, Instant expiresAt) {
        tokens.add(new Token(nextId.getAndIncrement(), userId, codeHash, createdAt, expiresAt));
    }

    @Override
    public Optional<IssuedCode> findLatestActive(long userId) {
        return tokens.stream()
                .filter(t -> t.userId == userId && !t.used)
                .reduce((first, second) -> second)
                .map(t -> new IssuedCode(t.id, t.hash, t.expiresAt, t.failedAttempts));
    }

    @Override
    public void recordFailedAttempt(long codeId) {
        tokens.stream().filter(t -> t.id == codeId).forEach(t -> t.failedAttempts++);
    }

    @Override
    public void markUsed(long codeId, Instant now) {
        tokens.stream().filter(t -> t.id == codeId).forEach(t -> t.used = true);
    }

    @Override
    public int countIssuedSince(long userId, Instant since) {
        return (int) tokens.stream().filter(t -> t.userId == userId && t.createdAt.isAfter(since)).count();
    }

    @Override
    public Optional<Instant> lastIssuedAt(long userId) {
        return tokens.stream().filter(t -> t.userId == userId).map(t -> t.createdAt).reduce((a, b) -> b);
    }

    // ---- para las comprobaciones
    List<User> users() {
        return users;
    }

    List<Consent> consents() {
        return consents;
    }

    int issuedCodes() {
        return tokens.size();
    }

    /** Huella del último código emitido para esa cuenta (para comprobar que no es el código en claro). */
    String lastHash(long userId) {
        return tokens.stream().filter(t -> t.userId == userId).map(t -> t.hash).reduce((a, b) -> b).orElseThrow();
    }

    // ---- RefreshTokenStore
    @Override
    public long save(long userId, String familyId, String tokenHash, Instant createdAt, Instant expiresAt, String userAgent) {
        long id = nextId.getAndIncrement();
        refreshRows.add(new RefreshRow(id, userId, familyId, tokenHash, expiresAt));
        return id;
    }

    @Override
    public Optional<StoredRefreshToken> findByHash(String tokenHash) {
        return refreshRows.stream().filter(r -> r.hash.equals(tokenHash)).findFirst()
                .map(r -> new StoredRefreshToken(r.id, r.userId, r.familyId, r.expiresAt, r.revokedAt, r.replacedById));
    }

    @Override
    public void markRotated(long oldId, long newId, Instant now) {
        refreshRows.stream().filter(r -> r.id == oldId).forEach(r -> {
            r.revokedAt = now;
            r.replacedById = newId;
        });
    }

    @Override
    public void revokeFamily(String familyId, Instant now) {
        refreshRows.stream().filter(r -> r.familyId.equals(familyId) && r.revokedAt == null).forEach(r -> r.revokedAt = now);
    }

    // ---- para las comprobaciones
    List<RefreshRow> refreshRows() {
        return refreshRows;
    }

    /** Cambia el estado de una cuenta (por ejemplo, para bloquearla). */
    void setStatus(String email, UserStatus status) {
        for (int i = 0; i < users.size(); i++) {
            User current = users.get(i);
            if (current.email().equals(email)) {
                users.set(i, new User(current.id(), current.publicId(), current.username(), current.email(), current.passwordHash(),
                        status, current.emailVerifiedAt(), current.marketingConsent(), current.version(), current.lastLoginAt(), current.roles()));
            }
        }
    }
}

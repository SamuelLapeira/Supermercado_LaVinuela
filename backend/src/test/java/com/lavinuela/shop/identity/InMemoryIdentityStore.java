package com.lavinuela.shop.identity;

import com.lavinuela.shop.identity.domain.model.User;
import com.lavinuela.shop.identity.domain.port.UserRepository;
import com.lavinuela.shop.identity.domain.port.VerificationCodeStore;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Repositorio de cuentas y almacén de códigos en memoria, para probar los casos de uso sin base de datos ni Spring.
 * Se comportan como los reales: cada código tiene su identificador, su caducidad y su contador de intentos.
 */
final class InMemoryIdentityStore implements UserRepository, VerificationCodeStore {

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
    public User save(User user) {
        if (user.id() != null) {
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
}

package com.lavinuela.shop.identity.application;

import com.lavinuela.shop.identity.domain.model.User;
import com.lavinuela.shop.identity.domain.port.RefreshTokenStore;
import com.lavinuela.shop.identity.domain.port.RefreshTokenStore.StoredRefreshToken;
import com.lavinuela.shop.identity.domain.port.SessionTokenCodec;
import com.lavinuela.shop.identity.domain.port.UserRepository;
import com.lavinuela.shop.shared.error.BusinessRuleViolation;
import com.lavinuela.shop.shared.error.DomainException;
import com.lavinuela.shop.shared.error.ErrorCode;
import java.time.Clock;
import java.time.Instant;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Renueva la sesión: cambia un token de renovación por otro nuevo (y por un token de acceso nuevo).
 *
 * <p><b>Detección de robo.</b> Cada token de renovación vale una sola vez. Si llega uno que ya se había sustituido,
 * alguien lo ha copiado (o se está reutilizando): se anula toda la familia y las dos partes tienen que volver a
 * iniciar sesión. Por eso la operación guarda sus cambios aunque termine en error ({@code noRollbackFor}).
 */
@Service
public class RefreshSessionService {

    private static final Logger LOG = LoggerFactory.getLogger(RefreshSessionService.class);

    private final RefreshTokenStore store;
    private final SessionTokenCodec codec;
    private final UserRepository users;
    private final SessionIssuer sessions;
    private final Clock clock;

    public RefreshSessionService(
            RefreshTokenStore store, SessionTokenCodec codec, UserRepository users, SessionIssuer sessions, Clock clock) {
        this.store = store;
        this.codec = codec;
        this.users = users;
        this.sessions = sessions;
        this.clock = clock;
    }

    @Transactional(noRollbackFor = DomainException.class)
    public AuthSession refresh(@Nullable String refreshToken, @Nullable String userAgent) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw expired();
        }
        Instant now = clock.instant();
        StoredRefreshToken stored = store.findByHash(codec.hash(refreshToken)).orElseThrow(RefreshSessionService::expired);

        if (stored.revokedAt() != null) {
            if (stored.replacedById() != null) {
                store.revokeFamily(stored.familyId(), now);
                LOG.warn("Token de renovación reutilizado: se anula la sesión entera [familia={}]", stored.familyId());
                throw new BusinessRuleViolation(ErrorCode.AUTH_TOKEN_REUSED);
            }
            throw expired();
        }
        if (!stored.expiresAt().isAfter(now)) {
            throw expired();
        }

        User user = users.findById(stored.userId()).filter(User::isActive).orElse(null);
        if (user == null) {
            store.revokeFamily(stored.familyId(), now);
            throw expired();
        }

        SessionIssuer.IssuedSession next = sessions.issue(user, stored.familyId(), userAgent);
        store.markRotated(stored.id(), next.refreshTokenId(), now);
        return next.session();
    }

    private static BusinessRuleViolation expired() {
        return new BusinessRuleViolation(ErrorCode.AUTH_TOKEN_EXPIRED);
    }
}

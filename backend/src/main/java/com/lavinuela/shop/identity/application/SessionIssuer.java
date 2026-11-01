package com.lavinuela.shop.identity.application;

import com.lavinuela.shop.identity.domain.model.User;
import com.lavinuela.shop.identity.domain.port.AccessTokenIssuer;
import com.lavinuela.shop.identity.domain.port.AccessTokenIssuer.IssuedAccessToken;
import com.lavinuela.shop.identity.domain.port.RefreshTokenStore;
import com.lavinuela.shop.identity.domain.port.SessionTokenCodec;
import com.lavinuela.shop.identity.domain.service.LoginPolicy;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

/** Crea los dos tokens de una sesión: el de acceso (corto) y el de renovación (que se guarda solo como huella). */
@Service
public class SessionIssuer {

    /** La sesión y el identificador del token de renovación recién guardado (para encadenar la rotación). */
    public record IssuedSession(AuthSession session, long refreshTokenId) {
    }

    private final AccessTokenIssuer accessTokens;
    private final SessionTokenCodec codec;
    private final RefreshTokenStore refreshTokens;
    private final Clock clock;

    public SessionIssuer(AccessTokenIssuer accessTokens, SessionTokenCodec codec, RefreshTokenStore refreshTokens, Clock clock) {
        this.accessTokens = accessTokens;
        this.codec = codec;
        this.refreshTokens = refreshTokens;
        this.clock = clock;
    }

    public IssuedSession issue(User user, String familyId, @Nullable String userAgent) {
        Instant now = clock.instant();
        IssuedAccessToken access = accessTokens.issue(user, now);
        String refreshToken = codec.newToken();
        Instant refreshExpiresAt = now.plus(LoginPolicy.REFRESH_TOKEN_VALIDITY);
        long refreshId = refreshTokens.save(user.requireId(), familyId, codec.hash(refreshToken), now, refreshExpiresAt, truncate(userAgent));
        return new IssuedSession(
                new AuthSession(access.token(), access.expiresAt(), refreshToken, refreshExpiresAt, summaryOf(user)),
                refreshId);
    }

    static UserSummary summaryOf(User user) {
        return new UserSummary(user.publicId().value(), user.username(), user.email(), List.copyOf(user.roles()));
    }

    /** La columna admite 255 caracteres. */
    private static @Nullable String truncate(@Nullable String userAgent) {
        if (userAgent == null) {
            return null;
        }
        return userAgent.length() <= 255 ? userAgent : userAgent.substring(0, 255);
    }
}

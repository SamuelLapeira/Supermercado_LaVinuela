package com.lavinuela.shop.identity.domain.port;

import com.lavinuela.shop.identity.domain.model.User;
import java.time.Instant;

/** Emite los tokens de acceso (los que acompañan a cada petición). */
public interface AccessTokenIssuer {

    record IssuedAccessToken(String token, Instant expiresAt) {
    }

    IssuedAccessToken issue(User user, Instant now);
}

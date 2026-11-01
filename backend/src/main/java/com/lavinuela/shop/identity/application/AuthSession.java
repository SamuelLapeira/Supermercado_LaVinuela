package com.lavinuela.shop.identity.application;

import java.time.Instant;

/** Una sesión recién abierta o renovada: los dos tokens y quién es la persona. */
public record AuthSession(
        String accessToken,
        Instant accessTokenExpiresAt,
        String refreshToken,
        Instant refreshTokenExpiresAt,
        UserSummary user) {

    /** Los tokens nunca se muestran en los registros. */
    @Override
    public String toString() {
        return "AuthSession[user=" + user.username() + ", tokens=***]";
    }
}

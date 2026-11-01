package com.lavinuela.shop.identity;

import com.lavinuela.shop.identity.domain.model.User;
import com.lavinuela.shop.identity.domain.port.AccessTokenIssuer;
import com.lavinuela.shop.identity.domain.service.LoginPolicy;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

/** Emite «tokens» de mentira (no JWT) para probar los casos de uso sin Spring Security. */
final class FakeAccessTokenIssuer implements AccessTokenIssuer {

    private final AtomicInteger counter = new AtomicInteger();

    @Override
    public IssuedAccessToken issue(User user, Instant now) {
        return new IssuedAccessToken("acceso-" + user.publicId().value() + "-" + counter.incrementAndGet(),
                now.plus(LoginPolicy.ACCESS_TOKEN_VALIDITY));
    }
}

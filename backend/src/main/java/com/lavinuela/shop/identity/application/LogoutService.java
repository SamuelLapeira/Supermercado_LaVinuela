package com.lavinuela.shop.identity.application;

import com.lavinuela.shop.identity.domain.port.RefreshTokenStore;
import com.lavinuela.shop.identity.domain.port.SessionTokenCodec;
import java.time.Clock;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cierra la sesión: anula toda la familia de tokens de renovación. Es idempotente: si no hay sesión, no pasa nada. */
@Service
public class LogoutService {

    private final RefreshTokenStore store;
    private final SessionTokenCodec codec;
    private final Clock clock;

    public LogoutService(RefreshTokenStore store, SessionTokenCodec codec, Clock clock) {
        this.store = store;
        this.codec = codec;
        this.clock = clock;
    }

    @Transactional
    public void logout(@Nullable String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        store.findByHash(codec.hash(refreshToken)).ifPresent(stored -> store.revokeFamily(stored.familyId(), clock.instant()));
    }
}

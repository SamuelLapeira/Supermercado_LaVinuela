package com.lavinuela.shop.identity.application;

import com.lavinuela.shop.identity.domain.service.LoginPolicy;
import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * Cuenta los intentos fallidos de inicio de sesión por correo y bloquea tras {@code MAX_FAILED_ATTEMPTS}.
 *
 * <p>Se aplica <b>igual a cualquier correo, exista o no</b>: si solo se bloqueasen las cuentas reales, el bloqueo
 * delataría cuáles existen. Vive en memoria (se reinicia al reiniciar la aplicación y no se comparte entre varias
 * instancias); cuando haya varias instancias pasará a Redis (historia E14-01).
 */
@Component
public class LoginAttemptLimiter {

    private static final int SWEEP_THRESHOLD = 50_000;

    private record Entry(int failures, Instant windowStart, @Nullable Instant blockedUntil) {
    }

    private final Clock clock;
    private final ConcurrentHashMap<String, Entry> entries = new ConcurrentHashMap<>();

    public LoginAttemptLimiter(Clock clock) {
        this.clock = clock;
    }

    public boolean isBlocked(String key) {
        Instant now = clock.instant();
        Entry entry = entries.get(key);
        if (entry == null) {
            return false;
        }
        Instant blockedUntil = entry.blockedUntil();
        if (blockedUntil != null && blockedUntil.isAfter(now)) {
            return true;
        }
        if (isStale(entry, now)) {
            entries.remove(key, entry);
        }
        return false;
    }

    public void recordFailure(String key) {
        Instant now = clock.instant();
        entries.compute(key, (k, current) -> {
            int failures = current == null || isStale(current, now) ? 1 : current.failures() + 1;
            Instant windowStart = current == null || isStale(current, now) ? now : current.windowStart();
            Instant blockedUntil = failures >= LoginPolicy.MAX_FAILED_ATTEMPTS ? now.plus(LoginPolicy.ATTEMPT_WINDOW) : null;
            return new Entry(failures, windowStart, blockedUntil);
        });
        if (entries.size() > SWEEP_THRESHOLD) {
            entries.entrySet().removeIf(e -> isStale(e.getValue(), now));
        }
    }

    public void reset(String key) {
        entries.remove(key);
    }

    private static boolean isStale(Entry entry, Instant now) {
        Instant blockedUntil = entry.blockedUntil();
        boolean blocked = blockedUntil != null && blockedUntil.isAfter(now);
        return !blocked && !entry.windowStart().plus(LoginPolicy.ATTEMPT_WINDOW).isAfter(now);
    }
}

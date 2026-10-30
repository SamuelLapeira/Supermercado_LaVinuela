package com.lavinuela.shop.identity.domain.port;

import com.lavinuela.shop.identity.domain.model.User;
import java.util.Optional;

/** Dónde se guardan las cuentas. */
public interface UserRepository {

    /** @param email ya normalizado (minúsculas) */
    Optional<User> findByEmail(String email);

    /** @param username ya normalizado (minúsculas) */
    boolean existsByUsername(String username);

    /** Guarda una cuenta nueva o los cambios de una existente y devuelve la versión guardada (con su identificador). */
    User save(User user);

    /** Deja constancia de que la persona aceptó un documento legal (condiciones, privacidad...). */
    void recordConsent(long userId, String consentType, String documentVersion);
}

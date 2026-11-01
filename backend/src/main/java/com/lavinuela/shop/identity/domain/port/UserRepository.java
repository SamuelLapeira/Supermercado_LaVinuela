package com.lavinuela.shop.identity.domain.port;

import com.lavinuela.shop.identity.domain.model.User;
import java.util.Optional;

/** Dónde se guardan las cuentas. */
public interface UserRepository {

    /** @param email ya normalizado (minúsculas) */
    Optional<User> findByEmail(String email);

    /** Busca por el identificador público (el que lleva el token de acceso). */
    Optional<User> findByPublicId(String publicId);

    Optional<User> findById(long id);

    /** Da un rol a la cuenta (por ejemplo {@code CUSTOMER}). */
    void assignRole(long userId, String roleCode);

    /** @param username ya normalizado (minúsculas) */
    boolean existsByUsername(String username);

    /** Guarda una cuenta nueva o los cambios de una existente y devuelve la versión guardada (con su identificador). */
    User save(User user);

    /** Deja constancia de que la persona aceptó un documento legal (condiciones, privacidad...). */
    void recordConsent(long userId, String consentType, String documentVersion);
}

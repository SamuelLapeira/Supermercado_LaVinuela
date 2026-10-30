package com.lavinuela.shop.identity.domain.port;

/** Calcula y comprueba la huella de una contraseña. Nunca se guarda la contraseña, solo esta huella. */
public interface PasswordHasher {

    String hash(CharSequence rawPassword);

    boolean matches(CharSequence rawPassword, String storedHash);
}

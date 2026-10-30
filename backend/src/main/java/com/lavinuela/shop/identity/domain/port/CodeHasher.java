package com.lavinuela.shop.identity.domain.port;

/** Calcula y comprueba la huella de un código de verificación, ligada a la cuenta a la que pertenece. */
public interface CodeHasher {

    String hash(long userId, String code);

    boolean matches(long userId, String code, String expectedHash);
}

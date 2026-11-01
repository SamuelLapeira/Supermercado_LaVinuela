package com.lavinuela.shop.identity.domain.port;

/** Genera los tokens de renovación y calcula su huella. */
public interface SessionTokenCodec {

    /** Un token nuevo, impredecible (256 bits aleatorios). */
    String newToken();

    /** La huella con la que se guarda; con ella se vuelve a encontrar el token sin guardarlo. */
    String hash(String token);
}

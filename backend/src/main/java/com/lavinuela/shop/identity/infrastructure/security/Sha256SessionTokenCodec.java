package com.lavinuela.shop.identity.infrastructure.security;

import com.lavinuela.shop.identity.domain.port.SessionTokenCodec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

/**
 * Tokens de renovación: 256 bits aleatorios en Base64 de URL (43 caracteres) y huella SHA-256.
 *
 * <p>Aquí basta un hash normal (sin clave secreta ni coste alto): el token ya es tan largo y aleatorio que no se puede
 * adivinar, así que la huella solo sirve para no guardar el token en la base de datos.
 */
@Component
public class Sha256SessionTokenCodec implements SessionTokenCodec {

    private static final int TOKEN_BYTES = 32;

    private final SecureRandom random = new SecureRandom();

    @Override
    public String newToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    @Override
    public String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no está disponible", e);
        }
    }
}

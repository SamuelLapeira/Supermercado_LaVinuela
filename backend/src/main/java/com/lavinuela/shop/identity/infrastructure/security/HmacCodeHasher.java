package com.lavinuela.shop.identity.infrastructure.security;

import com.lavinuela.shop.identity.domain.port.CodeHasher;
import com.lavinuela.shop.identity.infrastructure.config.IdentityProperties;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/**
 * Huella de los códigos de verificación: HMAC-SHA256 con una clave secreta del servidor (la «pimienta») y el
 * identificador de la cuenta.
 *
 * <p>Un código de 6 cifras tiene solo un millón de posibilidades: si se guardara con un hash normal, quien robara la
 * base de datos recuperaría todos los códigos en un instante. Con la clave secreta, que no está en la base de datos,
 * eso no es posible. Además el código solo vale 15 minutos y admite 5 intentos.
 */
@Component
public class HmacCodeHasher implements CodeHasher {

    private static final String ALGORITHM = "HmacSHA256";

    private final SecretKeySpec key;

    public HmacCodeHasher(IdentityProperties properties) {
        this.key = new SecretKeySpec(properties.codePepper().getBytes(StandardCharsets.UTF_8), ALGORITHM);
    }

    @Override
    public String hash(long userId, String code) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(key);
            byte[] digest = mac.doFinal((userId + ":" + code).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se puede calcular la huella del código", e);
        }
    }

    @Override
    public boolean matches(long userId, String code, String expectedHash) {
        return MessageDigest.isEqual(
                hash(userId, code).getBytes(StandardCharsets.UTF_8), expectedHash.getBytes(StandardCharsets.UTF_8));
    }
}

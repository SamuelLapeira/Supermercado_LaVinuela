package com.lavinuela.shop.identity.infrastructure.security;

import com.lavinuela.shop.identity.domain.port.PasswordHasher;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.text.Normalizer;
import java.util.Base64;
import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;
import org.springframework.stereotype.Component;

/**
 * Huella de contraseñas con Argon2id, la recomendada por OWASP. El resultado usa el formato estándar
 * {@code $argon2id$v=19$m=19456,t=2,p=1$<sal>$<huella>}, que entiende cualquier librería de Argon2 (incluida la de
 * Spring Security, cuando se incorpore con el inicio de sesión).
 *
 * <p>Parámetros: 19 MiB de memoria, 2 iteraciones y 1 hilo (el mínimo que recomienda OWASP). Se recalibrarán con el
 * servidor real para que cada comprobación tarde unos 200-500 ms. Cada contraseña lleva su propia sal aleatoria.
 *
 * <p>Antes de calcular la huella, la contraseña se normaliza (NFKC) para que «é» escrita de dos formas distintas dé
 * el mismo resultado.
 */
@Component
public class Argon2idPasswordHasher implements PasswordHasher {

    static final int MEMORY_KIB = 19_456;
    static final int ITERATIONS = 2;
    static final int PARALLELISM = 1;
    private static final int SALT_BYTES = 16;
    private static final int HASH_BYTES = 32;
    private static final String PREFIX = "$argon2id$v=19$";

    private final SecureRandom random = new SecureRandom();

    @Override
    public String hash(CharSequence rawPassword) {
        byte[] salt = new byte[SALT_BYTES];
        random.nextBytes(salt);
        byte[] hash = derive(rawPassword, salt, MEMORY_KIB, ITERATIONS, PARALLELISM, HASH_BYTES);
        Base64.Encoder encoder = Base64.getEncoder().withoutPadding();
        return PREFIX + "m=" + MEMORY_KIB + ",t=" + ITERATIONS + ",p=" + PARALLELISM
                + "$" + encoder.encodeToString(salt) + "$" + encoder.encodeToString(hash);
    }

    @Override
    public boolean matches(CharSequence rawPassword, String storedHash) {
        if (!storedHash.startsWith(PREFIX)) {
            return false;
        }
        String[] parts = storedHash.substring(PREFIX.length()).split("\\$");
        if (parts.length != 3) {
            return false;
        }
        try {
            int memory = 0;
            int iterations = 0;
            int parallelism = 0;
            for (String parameter : parts[0].split(",")) {
                String[] pair = parameter.split("=");
                if (pair.length != 2) {
                    return false;
                }
                int value = Integer.parseInt(pair[1]);
                switch (pair[0]) {
                    case "m" -> memory = value;
                    case "t" -> iterations = value;
                    case "p" -> parallelism = value;
                    default -> {
                        return false;
                    }
                }
            }
            byte[] salt = Base64.getDecoder().decode(parts[1]);
            byte[] expected = Base64.getDecoder().decode(parts[2]);
            if (memory <= 0 || iterations <= 0 || parallelism <= 0 || expected.length == 0) {
                return false;
            }
            byte[] actual = derive(rawPassword, salt, memory, iterations, parallelism, expected.length);
            return MessageDigest.isEqual(expected, actual);
        } catch (IllegalArgumentException malformed) {
            return false;
        }
    }

    private static byte[] derive(
            CharSequence rawPassword, byte[] salt, int memoryKib, int iterations, int parallelism, int length) {
        Argon2Parameters parameters = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                .withVersion(Argon2Parameters.ARGON2_VERSION_13)
                .withMemoryAsKB(memoryKib)
                .withIterations(iterations)
                .withParallelism(parallelism)
                .withSalt(salt)
                .build();
        Argon2BytesGenerator generator = new Argon2BytesGenerator();
        generator.init(parameters);
        byte[] output = new byte[length];
        String normalized = Normalizer.normalize(rawPassword, Normalizer.Form.NFKC);
        generator.generateBytes(normalized.getBytes(StandardCharsets.UTF_8), output);
        return output;
    }
}

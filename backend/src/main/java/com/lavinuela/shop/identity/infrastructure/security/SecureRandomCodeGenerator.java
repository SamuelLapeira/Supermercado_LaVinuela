package com.lavinuela.shop.identity.infrastructure.security;

import com.lavinuela.shop.identity.domain.port.VerificationCodeGenerator;
import com.lavinuela.shop.identity.domain.service.VerificationPolicy;
import java.security.SecureRandom;
import java.util.Locale;
import org.springframework.stereotype.Component;

/** Códigos numéricos de 6 cifras (con ceros a la izquierda) sacados de un generador criptográficamente seguro. */
@Component
public class SecureRandomCodeGenerator implements VerificationCodeGenerator {

    private static final int BOUND = (int) Math.pow(10, VerificationPolicy.CODE_LENGTH);

    private final SecureRandom random = new SecureRandom();

    @Override
    public String generate() {
        return String.format(Locale.ROOT, "%0" + VerificationPolicy.CODE_LENGTH + "d", random.nextInt(BOUND));
    }
}

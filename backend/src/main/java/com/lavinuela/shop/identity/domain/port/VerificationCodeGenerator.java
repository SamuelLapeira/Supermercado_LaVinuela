package com.lavinuela.shop.identity.domain.port;

/** Genera códigos de verificación impredecibles. */
public interface VerificationCodeGenerator {

    /** Un código de {@code VerificationPolicy.CODE_LENGTH} cifras. */
    String generate();
}

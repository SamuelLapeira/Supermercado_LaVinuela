package com.lavinuela.shop.identity.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.lavinuela.shop.identity.infrastructure.security.Argon2idPasswordHasher;
import org.junit.jupiter.api.Test;

class Argon2idPasswordHasherTests {

    private final Argon2idPasswordHasher hasher = new Argon2idPasswordHasher();

    @Test
    void producesTheStandardArgon2idFormat() {
        String hash = hasher.hash("una-contraseña-larga");

        assertThat(hash).startsWith("$argon2id$v=19$m=19456,t=2,p=1$");
        assertThat(hash.split("\\$")).hasSize(6);
        assertThat(hash).doesNotContain("una-contraseña");
    }

    @Test
    void verifiesTheRightPasswordAndRejectsTheWrongOne() {
        String hash = hasher.hash("una-contraseña-larga");

        assertThat(hasher.matches("una-contraseña-larga", hash)).isTrue();
        assertThat(hasher.matches("otra-contraseña-larga", hash)).isFalse();
        assertThat(hasher.matches("", hash)).isFalse();
    }

    @Test
    void everyHashUsesItsOwnRandomSalt() {
        assertThat(hasher.hash("misma-contraseña-1")).isNotEqualTo(hasher.hash("misma-contraseña-1"));
    }

    @Test
    void treatsEquivalentUnicodeSpellingsAsTheSamePassword() {
        String composed = "contrase\u00f1a-larga-\u00e9";       // ñ y é como un solo carácter
        String decomposed = "contrasen\u0303a-larga-e\u0301";   // n + tilde y e + tilde
        String hash = hasher.hash(composed);

        assertThat(hasher.matches(decomposed, hash)).isTrue();
    }

    @Test
    void rejectsMalformedStoredHashesWithoutFailing() {
        for (String bad : new String[] {"", "texto", "$argon2id$v=19$", "$argon2id$v=19$m=1,t=1,p=1$solo-sal",
                "$argon2id$v=19$m=x,t=2,p=1$c2Fs$aGFzaA", "$argon2id$v=19$m=19456,t=2,p=1$***$***", "$bcrypt$algo"}) {
            assertThat(hasher.matches("una-contraseña-larga", bad)).as(bad).isFalse();
        }
    }
}

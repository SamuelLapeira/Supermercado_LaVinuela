package com.lavinuela.shop.identity.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.lavinuela.shop.identity.infrastructure.security.Sha256SessionTokenCodec;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class Sha256SessionTokenCodecTests {

    private final Sha256SessionTokenCodec codec = new Sha256SessionTokenCodec();

    @Test
    void tokensAreLongRandomAndSafeInAUrlOrACookie() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 200; i++) {
            String token = codec.newToken();
            assertThat(token).matches("[A-Za-z0-9_-]{43}");
            seen.add(token);
        }
        assertThat(seen).hasSize(200);
    }

    @Test
    void theFingerprintIsA64CharacterHexadecimalAndDeterministic() {
        String hash = codec.hash("un-token");

        assertThat(hash).matches("[0-9a-f]{64}").isEqualTo(codec.hash("un-token")).isNotEqualTo(codec.hash("otro-token"));
        assertThat(hash).doesNotContain("un-token");
    }
}

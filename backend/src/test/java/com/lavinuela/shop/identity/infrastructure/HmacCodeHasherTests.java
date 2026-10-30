package com.lavinuela.shop.identity.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.lavinuela.shop.identity.infrastructure.config.IdentityProperties;
import com.lavinuela.shop.identity.infrastructure.security.HmacCodeHasher;
import com.lavinuela.shop.identity.infrastructure.security.SecureRandomCodeGenerator;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class HmacCodeHasherTests {

    private final HmacCodeHasher hasher = new HmacCodeHasher(new IdentityProperties("a".repeat(40)));

    @Test
    void producesA64CharacterHexadecimalFingerprint() {
        assertThat(hasher.hash(7, "123456")).matches("[0-9a-f]{64}").doesNotContain("123456");
    }

    @Test
    void isDeterministicForTheSameAccountAndCode() {
        assertThat(hasher.hash(7, "123456")).isEqualTo(hasher.hash(7, "123456"));
    }

    @Test
    void theSameCodeGivesDifferentFingerprintsForDifferentAccounts() {
        assertThat(hasher.hash(7, "123456")).isNotEqualTo(hasher.hash(8, "123456"));
    }

    @Test
    void dependsOnTheSecretKey() {
        HmacCodeHasher other = new HmacCodeHasher(new IdentityProperties("b".repeat(40)));
        assertThat(other.hash(7, "123456")).isNotEqualTo(hasher.hash(7, "123456"));
    }

    @Test
    void matchesOnlyTheRightCode() {
        String hash = hasher.hash(7, "123456");

        assertThat(hasher.matches(7, "123456", hash)).isTrue();
        assertThat(hasher.matches(7, "123457", hash)).isFalse();
        assertThat(hasher.matches(8, "123456", hash)).isFalse();
    }

    @Test
    void generatedCodesHaveSixDigitsAndAreNotAlwaysTheSame() {
        SecureRandomCodeGenerator generator = new SecureRandomCodeGenerator();
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 200; i++) {
            String code = generator.generate();
            assertThat(code).matches("[0-9]{6}");
            seen.add(code);
        }
        assertThat(seen.size()).isGreaterThan(150);
    }

    @Test
    void theSecretKeyNeverAppearsInLogs() {
        assertThat(new IdentityProperties("a".repeat(40)).toString()).doesNotContain("aaaa");
    }
}

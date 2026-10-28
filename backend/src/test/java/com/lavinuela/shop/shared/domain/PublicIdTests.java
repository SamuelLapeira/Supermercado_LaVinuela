package com.lavinuela.shop.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PublicIdTests {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Clock FIXED = Clock.fixed(Instant.parse("2026-10-28T10:00:00Z"), ZoneOffset.UTC);

    @Test
    void generatesValidIdentifiers() {
        PublicId id = PublicId.generate(FIXED, RANDOM);
        assertThat(id.value()).hasSize(PublicId.LENGTH);
        assertThat(PublicId.isValid(id.value())).isTrue();
    }

    @Test
    void doesNotCollideEvenWithinTheSameMillisecond() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 20_000; i++) {
            seen.add(PublicId.generate(FIXED, RANDOM).value());
        }
        assertThat(seen).hasSize(20_000);
    }

    @Test
    void sortsByCreationTime() {
        Clock earlier = Clock.fixed(Instant.parse("2026-10-28T10:00:00.000Z"), ZoneOffset.UTC);
        Clock later = Clock.fixed(Instant.parse("2026-10-28T10:00:00.001Z"), ZoneOffset.UTC);
        assertThat(PublicId.generate(earlier, RANDOM)).isLessThan(PublicId.generate(later, RANDOM));
    }

    @Test
    void parsesCaseInsensitivelyAndTrims() {
        PublicId id = PublicId.generate(FIXED, RANDOM);
        assertThat(PublicId.parse("  " + id.value().toLowerCase(java.util.Locale.ROOT) + " ")).isEqualTo(id);
    }

    @Test
    void acceptsTheReferenceUlid() {
        assertThat(PublicId.isValid("01ARZ3NDEKTSV4RRFFQ69G5FAV")).isTrue();
    }

    @Test
    void rejectsInvalidFormats() {
        for (String bad : new String[] {
            "", "ABC", "01ARZ3NDEKTSV4RRFFQ69G5FAI", "81ARZ3NDEKTSV4RRFFQ69G5FAV", "01ARZ3NDEKTSV4RRFFQ69G5FA!"
        }) {
            assertThat(PublicId.isValid(bad)).as(bad).isFalse();
            assertThatThrownBy(() -> new PublicId(bad)).isInstanceOf(IllegalArgumentException.class);
        }
    }
}

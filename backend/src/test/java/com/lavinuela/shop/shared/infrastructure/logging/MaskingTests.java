package com.lavinuela.shop.shared.infrastructure.logging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MaskingTests {

    @Test
    void masksEmails() {
        assertThat(Masking.email("ana.perez@example.com")).isEqualTo("a***@example.com");
        assertThat(Masking.email("x@example.com")).isEqualTo("x***@example.com");
    }

    @Test
    void emailWithoutAtSignIsFullyHidden() {
        assertThat(Masking.email("sin-arroba")).isEqualTo("***");
        assertThat(Masking.email("@example.com")).isEqualTo("***");
    }

    @Test
    void handlesMissingValues() {
        assertThat(Masking.email(null)).isEqualTo("-");
        assertThat(Masking.email("  ")).isEqualTo("-");
        assertThat(Masking.phone(null)).isEqualTo("-");
        assertThat(Masking.secret(null)).isEqualTo("-");
    }

    @Test
    void keepsOnlyTheLastTwoDigitsOfAPhone() {
        assertThat(Masking.phone("600 123 456")).isEqualTo("*******56");
        assertThat(Masking.phone("+34 600 123 456")).isEqualTo("*********56");
        assertThat(Masking.phone("12")).isEqualTo("***");
    }

    @Test
    void neverRevealsASecret() {
        assertThat(Masking.secret("super-secreto")).isEqualTo("***").doesNotContain("secreto");
    }
}

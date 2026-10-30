package com.lavinuela.shop.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.lavinuela.shop.identity.domain.service.AccountPolicy;
import com.lavinuela.shop.shared.error.FieldViolation;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AccountPolicyTests {

    private final AccountPolicy policy = new AccountPolicy(Set.of("password123", "contraseña123"));

    private String message(String password) {
        return policy.validate("usuario1", "ok@example.com", password).get(0).message();
    }

    private List<String> fieldsWithErrors(String username, String email, String password) {
        return policy.validate(username, email, password).stream().map(FieldViolation::field).toList();
    }

    @Test
    void acceptsAValidRegistration() {
        assertThat(policy.validate("samuel.lapeira", "samuel@example.com", "Una-contraseña-larga-2026")).isEmpty();
    }

    @Test
    void normalizesUsernameAndEmailToLowercase() {
        assertThat(policy.normalizeUsername("  Samuel.L  ")).isEqualTo("samuel.l");
        assertThat(policy.normalizeEmail(" Samuel@Example.COM ")).isEqualTo("samuel@example.com");
    }

    @Test
    void rejectsMalformedUsernames() {
        for (String bad : new String[] {"ab", "a".repeat(31), "con espacios", ".empieza", "termina.", "ñandú", "a/b/c", "<script>"}) {
            assertThat(fieldsWithErrors(bad, "ok@example.com", "Una-contraseña-larga-2026")).as(bad).containsExactly("username");
        }
    }

    @Test
    void rejectsReservedUsernames() {
        assertThat(fieldsWithErrors("admin", "ok@example.com", "Una-contraseña-larga-2026")).containsExactly("username");
        assertThat(fieldsWithErrors("lavinuela", "ok@example.com", "Una-contraseña-larga-2026")).containsExactly("username");
    }

    @Test
    void rejectsMalformedEmails() {
        for (String bad : new String[] {"sin-arroba", "dos@@example.com", "sin@punto", "con espacio@example.com", "@example.com", "a@b.c"}) {
            assertThat(fieldsWithErrors("usuario1", bad, "Una-contraseña-larga-2026")).as(bad).containsExactly("email");
        }
    }

    @Test
    void requiresAtLeastTenCharactersInThePassword() {
        assertThat(fieldsWithErrors("usuario1", "ok@example.com", "corta-123")).containsExactly("password");
        assertThat(fieldsWithErrors("usuario1", "ok@example.com", "Larga-1234")).isEmpty();
    }

    @Test
    void requiresUppercaseLowercaseAndADigit() {
        for (String weak : new String[] {"todominusculas1", "TODOMAYUSCULAS1", "SinNumerosAqui", "solominusculas", "1234567890123"}) {
            assertThat(fieldsWithErrors("usuario1", "ok@example.com", weak)).as(weak).containsExactly("password");
        }
        assertThat(fieldsWithErrors("usuario1", "ok@example.com", "ConTodo-123x")).isEmpty();
    }

    @Test
    void explainsExactlyWhatIsMissing() {
        assertThat(message("todominusculas1")).isEqualTo("La contraseña debe incluir al menos una mayúscula.");
        assertThat(message("SinNumerosAqui")).isEqualTo("La contraseña debe incluir al menos un número.");
        assertThat(message("solominusculas")).isEqualTo("La contraseña debe incluir al menos una mayúscula y un número.");
        assertThat(message("1234567890123")).isEqualTo("La contraseña debe incluir al menos una mayúscula y una minúscula.");
    }

    @Test
    void doesNotForceSymbolsAndAdmitsSpaces() {
        assertThat(fieldsWithErrors("usuario1", "ok@example.com", "Tres Palabras Sueltas 7")).isEmpty();
    }

    @Test
    void rejectsCommonPasswordsIgnoringCase() {
        assertThat(fieldsWithErrors("usuario1", "ok@example.com", "Contraseña123")).containsExactly("password");
    }

    @Test
    void rejectsRepetitions() {
        assertThat(fieldsWithErrors("usuario1", "ok@example.com", "aaaaaaaaaaaa")).containsExactly("password");
        assertThat(fieldsWithErrors("usuario1", "ok@example.com", "abababababab")).containsExactly("password");
    }

    @Test
    void rejectsPasswordsContainingTheUsernameOrEmail() {
        assertThat(fieldsWithErrors("samuel.lapeira", "ok@example.com", "Xx-samuel.lapeira-99")).containsExactly("password");
        assertThat(fieldsWithErrors("usuario1", "maria.garcia@example.com", "Maria.Garcia-1234")).containsExactly("password");
    }

    @Test
    void acceptsAnyCharactersAndCountsThemAsPeopleDo() {
        assertThat(fieldsWithErrors("usuario1", "ok@example.com", "Contraseña-ñandú-😀-2026")).isEmpty();
        // Cada emoji cuenta como un carácter para una persona, aunque ocupe más bytes: 3 + 7 = 10.
        assertThat(fieldsWithErrors("usuario1", "ok@example.com", "Aa1😀😁😂🤣😃😄😅")).isEmpty();
        assertThat(fieldsWithErrors("usuario1", "ok@example.com", "Aa1😀😁😂🤣😃😄")).containsExactly("password");
    }

    @Test
    void rejectsExcessivelyLongPasswords() {
        assertThat(fieldsWithErrors("usuario1", "ok@example.com", "Ab1".repeat(43))).containsExactly("password");
    }
}

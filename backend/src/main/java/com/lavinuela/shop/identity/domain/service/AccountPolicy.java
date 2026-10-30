package com.lavinuela.shop.identity.domain.service;

import com.lavinuela.shop.shared.error.FieldViolation;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Reglas de lo que se puede elegir al registrarse: nombre de usuario, correo y contraseña.
 *
 * <p>Contraseña: al menos {@value #PASSWORD_MIN_LENGTH} caracteres, con una mayúscula, una minúscula y un número como
 * mínimo (los símbolos y espacios se admiten pero no se obligan). Además se rechazan las contraseñas más comunes, las
 * repeticiones y las que contienen el propio usuario o correo. Se admiten todos los caracteres, incluidos acentos y
 * emojis.
 */
public final class AccountPolicy {

    public static final int PASSWORD_MIN_LENGTH = 10;
    public static final int PASSWORD_MAX_LENGTH = 128;

    private static final Pattern USERNAME = Pattern.compile("^[a-z0-9][a-z0-9._-]{1,28}[a-z0-9]$");
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]{2,}$");
    private static final Set<String> RESERVED_USERNAMES = Set.of(
            "admin", "administrador", "administrator", "root", "soporte", "support", "ayuda", "help", "api",
            "lavinuela", "supermercado", "tienda", "pedidos", "facturacion", "info", "contacto", "correo", "sistema");

    private final Set<String> commonPasswords;

    /** @param commonPasswords contraseñas comunes, ya en minúsculas */
    public AccountPolicy(Set<String> commonPasswords) {
        this.commonPasswords = Set.copyOf(Objects.requireNonNull(commonPasswords));
    }

    public String normalizeUsername(String raw) {
        return raw.trim().toLowerCase(Locale.ROOT);
    }

    public String normalizeEmail(String raw) {
        return raw.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Comprueba los tres datos y devuelve los errores por campo (lista vacía si todo es válido). La lista devuelta se
     * puede modificar.
     *
     * @param username ya normalizado
     * @param email ya normalizado
     * @param password tal como lo escribió la persona
     */
    public List<FieldViolation> validate(String username, String email, String password) {
        List<FieldViolation> violations = new ArrayList<>();
        validateUsername(username, violations);
        validateEmail(email, violations);
        validatePassword(username, email, password, violations);
        return violations;
    }

    private void validateUsername(String username, List<FieldViolation> violations) {
        if (!USERNAME.matcher(username).matches()) {
            violations.add(FieldViolation.of("username",
                    "El nombre de usuario debe tener entre 3 y 30 caracteres: letras, números, punto, guion o guion bajo."));
        } else if (RESERVED_USERNAMES.contains(username)) {
            violations.add(FieldViolation.of("username", "Ese nombre de usuario no está disponible."));
        }
    }

    private void validateEmail(String email, List<FieldViolation> violations) {
        if (email.length() > 254 || !EMAIL.matcher(email).matches()) {
            violations.add(FieldViolation.of("email", "Escribe un correo electrónico válido."));
        }
    }

    private void validatePassword(String username, String email, String password, List<FieldViolation> violations) {
        String candidate = Normalizer.normalize(password, Normalizer.Form.NFKC);
        int length = candidate.codePointCount(0, candidate.length());
        if (length < PASSWORD_MIN_LENGTH) {
            violations.add(FieldViolation.of("password",
                    "La contraseña debe tener al menos " + PASSWORD_MIN_LENGTH + " caracteres."));
            return;
        }
        if (length > PASSWORD_MAX_LENGTH) {
            violations.add(FieldViolation.of("password",
                    "La contraseña no puede tener más de " + PASSWORD_MAX_LENGTH + " caracteres."));
            return;
        }
        List<String> missing = missingCharacterTypes(candidate);
        if (!missing.isEmpty()) {
            violations.add(FieldViolation.of("password",
                    "La contraseña debe incluir al menos " + joinWithAnd(missing) + "."));
            return;
        }
        String lower = candidate.toLowerCase(Locale.ROOT);
        if (commonPasswords.contains(lower) || isRepetition(lower)) {
            violations.add(FieldViolation.of("password",
                    "Esa contraseña es demasiado común. Elige otra más difícil de adivinar."));
            return;
        }
        String emailLocalPart = email.contains("@") ? email.substring(0, email.indexOf('@')) : email;
        if (containsIdentity(lower, username) || containsIdentity(lower, emailLocalPart)) {
            violations.add(FieldViolation.of("password",
                    "La contraseña no puede contener tu nombre de usuario ni tu correo."));
        }
    }

    /** Qué le falta a la contraseña de entre una mayúscula, una minúscula y un número. */
    private static List<String> missingCharacterTypes(String password) {
        boolean upper = false;
        boolean lower = false;
        boolean digit = false;
        for (int i = 0; i < password.length(); ) {
            int codePoint = password.codePointAt(i);
            i += Character.charCount(codePoint);
            if (Character.isUpperCase(codePoint)) {
                upper = true;
            } else if (Character.isLowerCase(codePoint)) {
                lower = true;
            } else if (Character.isDigit(codePoint)) {
                digit = true;
            }
        }
        List<String> missing = new ArrayList<>();
        if (!upper) {
            missing.add("una mayúscula");
        }
        if (!lower) {
            missing.add("una minúscula");
        }
        if (!digit) {
            missing.add("un número");
        }
        return missing;
    }

    /** «a», «a y b» o «a, b y c». */
    private static String joinWithAnd(List<String> items) {
        if (items.size() == 1) {
            return items.get(0);
        }
        String last = items.get(items.size() - 1);
        return String.join(", ", items.subList(0, items.size() - 1)) + " y " + last;
    }

    /** Solo se compara con textos de 4 o más caracteres: con menos, casi cualquier contraseña los contendría. */
    private static boolean containsIdentity(String lowerPassword, String identity) {
        return identity.length() >= 4 && lowerPassword.contains(identity.toLowerCase(Locale.ROOT));
    }

    /** Una sola letra repetida («aaaaaaaaaaaa») o un bloque corto repetido («abababababab»). */
    private static boolean isRepetition(String text) {
        for (int block = 1; block <= 3; block++) {
            if (text.length() % block == 0 && text.equals(text.substring(0, block).repeat(text.length() / block))) {
                return true;
            }
        }
        return false;
    }
}

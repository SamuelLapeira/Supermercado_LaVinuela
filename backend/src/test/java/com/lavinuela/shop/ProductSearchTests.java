package com.lavinuela.shop;

import static org.assertj.core.api.Assertions.assertThat;

import com.lavinuela.shop.shared.domain.PublicId;
import java.security.SecureRandom;
import java.time.Clock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Comprueba la búsqueda de texto de productos (índice FULLTEXT con analizador ngram).
 *
 * <p>No usa transacción: InnoDB solo indexa para búsquedas de texto lo que ya se ha confirmado, así que
 * los datos se insertan, se buscan y se limpian explícitamente.
 *
 * <p>Si una de estas pruebas falla, es información valiosa sobre la relevancia de la búsqueda (decisión
 * pendiente de la Fase 5, riesgo 3): dímelo y valoramos el plan B (otro motor detrás del puerto ProductSearch).
 */
@SpringBootTest
@ActiveProfiles("test")
@RequiresDocker
class ProductSearchTests {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String SEARCH =
            "SELECT COUNT(*) FROM products WHERE sku LIKE 'TST-FT-%' AND MATCH(name, short_description) AGAINST (? IN BOOLEAN MODE)";

    private final JdbcTemplate jdbc;

    @Autowired
    ProductSearchTests(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @BeforeEach
    void insertProducts() {
        jdbc.update("INSERT INTO categories (public_id, name, slug, path, depth) VALUES (?, 'tst-ft', 'tst-ft', '/1/', 0)", publicId());
        long category = jdbc.queryForObject("SELECT id FROM categories WHERE slug = 'tst-ft'", Long.class);
        long tax = jdbc.queryForObject("SELECT id FROM tax_rates WHERE code = 'VAT_REDUCED'", Long.class);
        insert("TST-FT-1", "Aceite de oliva virgen extra", category, tax);
        insert("TST-FT-2", "Jamón serrano", category, tax);
        insert("TST-FT-3", "Pan de pueblo", category, tax);
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM products WHERE sku LIKE 'TST-FT-%'");
        jdbc.update("DELETE FROM categories WHERE slug = 'tst-ft'");
    }

    @Test
    void findsProductsByAWordIgnoringCase() {
        assertThat(count("oliva")).isEqualTo(1);
        assertThat(count("OLIVA")).isEqualTo(1);
        assertThat(count("pueblo")).isEqualTo(1);
    }

    @Test
    void toleratesFragmentsOfAWord() {
        assertThat(count("aceit")).isEqualTo(1);
    }

    @Test
    void ignoresAccents() {
        assertThat(count("jamon")).isEqualTo(1);
    }

    @Test
    void doesNotMatchUnrelatedText() {
        assertThat(count("zzzqqq")).isZero();
    }

    private int count(String term) {
        Integer count = jdbc.queryForObject(SEARCH, Integer.class, term);
        return count == null ? 0 : count;
    }

    private void insert(String sku, String name, long category, long tax) {
        jdbc.update("INSERT INTO products (public_id, sku, slug, category_id, tax_rate_id, name, format_label, price, content_quantity,"
                + " content_unit, status) VALUES (?, ?, ?, ?, ?, ?, 'Formato', 2.00, 1.000, 'ud', 'ACTIVE')",
                publicId(), sku, sku.toLowerCase(java.util.Locale.ROOT), category, tax, name);
    }

    private static String publicId() {
        return PublicId.generate(Clock.systemUTC(), RANDOM).value();
    }
}

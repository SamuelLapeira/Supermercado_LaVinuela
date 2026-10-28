package com.lavinuela.shop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lavinuela.shop.shared.domain.PublicId;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Comprueba contra un MySQL 8.4 real que las migraciones crean el esquema de la Fase 5 y que las
 * restricciones protegen los datos aunque la aplicación falle: stock que no puede ser negativo, un solo
 * pago cobrado por pedido, importes coherentes, registros que no se pueden modificar...
 *
 * <p>Cada prueba corre dentro de una transacción que se deshace al terminar.
 */
@SpringBootTest
@ActiveProfiles("test")
@RequiresDocker
@Transactional
class DatabaseMigrationTests {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final JdbcTemplate jdbc;

    @Autowired
    DatabaseMigrationTests(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ------------------------------------------------------------------ esquema y datos iniciales

    @Test
    void appliesEveryMigration() {
        Integer applied = jdbc.queryForObject("SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1", Integer.class);

        assertThat(applied).isEqualTo(11);
    }

    @Test
    void createsTheExpectedTables() {
        List<String> tables = jdbc.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = DATABASE() AND table_type = 'BASE TABLE'",
                String.class);

        assertThat(tables).contains("users", "addresses", "products", "stock_items", "stock_reservations",
                "carts", "orders", "order_lines", "payments", "payment_events", "refunds", "invoices",
                "audit_logs", "app_settings", "media_assets", "email_templates");
        assertThat(tables).hasSizeGreaterThanOrEqualTo(54);
    }

    @Test
    void loadsTheReferenceData() {
        assertThat(count("roles")).isEqualTo(5);
        assertThat(count("permissions")).isEqualTo(15);
        assertThat(count("tax_rates")).isEqualTo(3);
        assertThat(count("allergens")).isEqualTo(14);
        assertThat(count("delivery_fee_bands")).isEqualTo(4);
        assertThat(count("email_templates")).isEqualTo(6);
        assertThat(jdbc.queryForObject("SELECT setting_value FROM app_settings WHERE setting_key = 'delivery.max_km'", String.class))
                .isEqualTo("40");
    }

    @Test
    void assignsPermissionsToRoles() {
        assertThat(permissionsOf("ADMIN")).isEqualTo(15);
        assertThat(permissionsOf("PICKER")).isEqualTo(2);
        assertThat(permissionsOf("DRIVER")).isEqualTo(2);
        assertThat(permissionsOf("CATALOG_MANAGER")).isEqualTo(5);
        assertThat(permissionsOf("CUSTOMER")).isZero();
    }

    @Test
    void legalPagesStartAsDrafts() {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM content_pages WHERE status <> 'DRAFT'", Integer.class)).isZero();
    }

    @Test
    void storesTheDefaultBannerAsJson() {
        String title = jdbc.queryForObject(
                "SELECT JSON_UNQUOTE(JSON_EXTRACT(data, '$.title')) FROM content_blocks WHERE block_key = 'home.banner'",
                String.class);

        assertThat(title).isNotBlank();
    }

    // ------------------------------------------------------------------ catálogo

    @Test
    void calculatesThePricePerUnit() {
        long category = insertCategory("tst-unit");
        long oil = insertProduct("TST-OIL", category, "8.90", "1.000");
        long avocado = insertProduct("TST-AVO", category, "3.90", "0.500");

        assertThat(jdbc.queryForObject("SELECT price_per_unit FROM products WHERE id = ?", String.class, oil)).isEqualTo("8.90");
        assertThat(jdbc.queryForObject("SELECT price_per_unit FROM products WHERE id = ?", String.class, avocado)).isEqualTo("7.80");
    }

    @Test
    void rejectsInvalidProducts() {
        long category = insertCategory("tst-bad");

        assertRejected(() -> insertProduct("TST-ZERO", category, "0.00", "1.000"));
        assertRejected(() -> jdbc.update(
                "INSERT INTO products (public_id, sku, slug, category_id, tax_rate_id, name, format_label, price, previous_price,"
                        + " content_quantity, content_unit) VALUES (?, 'TST-OFFER', 'tst-offer', ?, ?, 'x', 'x', 5.00, 5.00, 1, 'ud')",
                publicId(), category, taxRateId()));
        assertRejected(() -> insertProduct("TST-DUP", category, "1.00", "1.000"), () -> insertProduct("TST-DUP", category, "1.00", "1.000"));
    }

    // ------------------------------------------------------------------ inventario

    @Test
    void stockCanNeverBeNegativeNorReservedBeyondWhatIsOnHand() {
        long product = insertProduct("TST-STOCK", insertCategory("tst-stock"), "2.00", "1.000");

        assertRejected(() -> jdbc.update("INSERT INTO stock_items (product_id, on_hand, reserved) VALUES (?, -1, 0)", product));
        assertRejected(() -> jdbc.update("INSERT INTO stock_items (product_id, on_hand, reserved) VALUES (?, 5, 6)", product));

        jdbc.update("INSERT INTO stock_items (product_id, on_hand, reserved) VALUES (?, 5, 5)", product);
        assertRejected(() -> jdbc.update("UPDATE stock_items SET reserved = reserved + 1 WHERE product_id = ?", product));
    }

    @Test
    void reservesOnlyWhatIsAvailable() {
        long product = insertProduct("TST-RESERVE", insertCategory("tst-reserve"), "2.00", "1.000");
        jdbc.update("INSERT INTO stock_items (product_id, on_hand, reserved, safety_stock) VALUES (?, 10, 0, 2)", product);
        String reserve = "UPDATE stock_items SET reserved = reserved + ? WHERE product_id = ? AND on_hand - reserved - safety_stock >= ?";

        // Disponible = 10 - 0 - 2 = 8. La condición del UPDATE es la del diseño (Fase 3, apartado 7.2).
        assertThat(jdbc.update(reserve, 8, product, 8)).isEqualTo(1);
        assertThat(jdbc.update(reserve, 1, product, 1)).isZero();
    }

    // ------------------------------------------------------------------ cuentas y carrito

    @Test
    void allowsOnlyOneDefaultAddressPerUser() {
        long user = insertUser("default@example.com");
        insertAddress(user, true, false);

        assertRejected(() -> insertAddress(user, true, false));
        insertAddress(user, false, false);
        // Una dirección por defecto ya dada de baja no cuenta.
        insertAddress(user, true, true);
    }

    @Test
    void allowsOnlyOneActiveCartPerUser() {
        long user = insertUser("cart@example.com");
        jdbc.update("INSERT INTO carts (public_id, user_id, status, expires_at) VALUES (?, ?, 'ACTIVE', NOW(3) + INTERVAL 1 DAY)", publicId(), user);

        assertRejected(() -> jdbc.update(
                "INSERT INTO carts (public_id, user_id, status, expires_at) VALUES (?, ?, 'ACTIVE', NOW(3) + INTERVAL 1 DAY)", publicId(), user));
        jdbc.update("INSERT INTO carts (public_id, user_id, status, expires_at) VALUES (?, ?, 'CONVERTED', NOW(3) + INTERVAL 1 DAY)", publicId(), user);
    }

    @Test
    void emailIsUniqueIgnoringCase() {
        insertUser("Maria@Example.com");

        assertRejected(() -> insertUser("maria@example.com"));
    }

    // ------------------------------------------------------------------ pedidos y pagos

    @Test
    void orderTotalMustMatchItsComponents() {
        long user = insertUser("order@example.com");

        assertRejected(() -> insertOrder(user, "LV-2026-T00001", "10.00", "0.00", "3.50", "99.99"));
        insertOrder(user, "LV-2026-T00002", "10.00", "1.00", "3.50", "12.50");
    }

    @Test
    void orderLineTotalMustEqualPriceTimesQuantity() {
        long user = insertUser("line@example.com");
        long order = insertOrder(user, "LV-2026-T00003", "10.00", "0.00", "0.00", "10.00");
        long product = insertProduct("TST-LINE", insertCategory("tst-line"), "2.50", "1.000");
        String sql = "INSERT INTO order_lines (order_id, product_id, sku, product_name, format_label, quantity, unit_price, tax_rate,"
                + " line_total, line_net, line_tax) VALUES (?, ?, 'TST-LINE', 'x', 'x', 4, 2.50, 10.00, ?, 9.09, 0.91)";

        assertRejected(() -> jdbc.update(sql, order, product, "9.99"));
        jdbc.update(sql, order, product, "10.00");
    }

    @Test
    void anOrderCanHaveOnlyOnePaidPayment() {
        long user = insertUser("pay@example.com");
        long order = insertOrder(user, "LV-2026-T00004", "10.00", "0.00", "0.00", "10.00");

        insertPayment(order, "1234ABCD5678", "FAILED");
        insertPayment(order, "2345ABCD5678", "PENDING");
        insertPayment(order, "3456ABCD5678", "PAID");

        assertRejected(() -> insertPayment(order, "4567ABCD5678", "PAID"));
        assertRejected(() -> insertPayment(order, "3456ABCD5678", "FAILED"));
    }

    @Test
    void redsysReferenceMustHaveFourLeadingDigits() {
        long user = insertUser("ref@example.com");
        long order = insertOrder(user, "LV-2026-T00005", "10.00", "0.00", "0.00", "10.00");

        assertRejected(() -> insertPayment(order, "ABCD12345678", "PENDING"));
        assertRejected(() -> insertPayment(order, "12345", "PENDING"));
    }

    // ------------------------------------------------------------------ registros inmutables

    @Test
    void auditLogsAreInsertOnly() {
        jdbc.update("INSERT INTO audit_logs (action, entity_type, entity_id) VALUES ('TEST', 'TEST', '1')");

        assertRejected(() -> jdbc.update("UPDATE audit_logs SET action = 'OTRA' WHERE action = 'TEST'"));
        assertRejected(() -> jdbc.update("DELETE FROM audit_logs WHERE action = 'TEST'"));
    }

    @Test
    void stockMovementsAreInsertOnly() {
        long product = insertProduct("TST-MOVE", insertCategory("tst-move"), "2.00", "1.000");
        jdbc.update("INSERT INTO stock_movements (product_id, movement_type, quantity, balance_after) VALUES (?, 'SUPPLIER_IN', 10, 10)", product);

        assertRejected(() -> jdbc.update("UPDATE stock_movements SET quantity = 99 WHERE product_id = ?", product));
        assertRejected(() -> jdbc.update("DELETE FROM stock_movements WHERE product_id = ?", product));
        assertRejected(() -> jdbc.update(
                "INSERT INTO stock_movements (product_id, movement_type, quantity, balance_after) VALUES (?, 'SALE', 0, 10)", product));
    }

    @Test
    void issuedInvoicesCannotBeModifiedOrDeleted() {
        long user = insertUser("invoice@example.com");
        long order = insertOrder(user, "LV-2026-T00006", "10.00", "0.00", "0.00", "10.00");
        jdbc.update("INSERT INTO invoices (public_id, order_id, invoice_type, series, fiscal_year, number, full_number, issue_date,"
                + " net_total, tax_total, total) VALUES (?, ?, 'SIMPLIFIED', 'T', 2026, 1, 'T-2026-000001', CURDATE(), 9.09, 0.91, 10.00)",
                publicId(), order);

        assertRejected(() -> jdbc.update("UPDATE invoices SET total = 1.00 WHERE full_number = 'T-2026-000001'"));
        assertRejected(() -> jdbc.update("DELETE FROM invoices WHERE full_number = 'T-2026-000001'"));
        assertRejected(() -> jdbc.update(
                "INSERT INTO invoices (public_id, order_id, invoice_type, series, fiscal_year, number, full_number, issue_date,"
                        + " net_total, tax_total, total) VALUES (?, ?, 'SIMPLIFIED', 'T', 2026, 1, 'T-2026-000002', CURDATE(), 1, 0, 1)",
                publicId(), order));
    }

    // ------------------------------------------------------------------ ayudas

    private int count(String table) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
        return count == null ? 0 : count;
    }

    private int permissionsOf(String role) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM role_permissions rp JOIN roles r ON r.id = rp.role_id WHERE r.code = ?", Integer.class, role);
        return count == null ? 0 : count;
    }

    private static String publicId() {
        return PublicId.generate(Clock.systemUTC(), RANDOM).value();
    }

    private long taxRateId() {
        Long id = jdbc.queryForObject("SELECT id FROM tax_rates WHERE code = 'VAT_REDUCED'", Long.class);
        return id == null ? 0L : id;
    }

    private long insertUser(String email) {
        jdbc.update("INSERT INTO users (public_id, email, password_hash, name, surname, status) VALUES (?, ?, 'hash', 'Ana', 'Pérez', 'ACTIVE')",
                publicId(), email);
        Long id = jdbc.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, email);
        return id == null ? 0L : id;
    }

    private void insertAddress(long user, boolean isDefault, boolean deleted) {
        jdbc.update("INSERT INTO addresses (public_id, user_id, recipient_name, street, postal_code, city, province, is_default, deleted_at)"
                + " VALUES (?, ?, 'Ana Pérez', 'Calle Ejemplo', '29712', 'La Viñuela', 'Málaga', ?, " + (deleted ? "NOW(3)" : "NULL") + ")",
                publicId(), user, isDefault);
    }

    private long insertCategory(String slug) {
        jdbc.update("INSERT INTO categories (public_id, name, slug, path, depth) VALUES (?, ?, ?, '/1/', 0)", publicId(), slug, slug);
        Long id = jdbc.queryForObject("SELECT id FROM categories WHERE slug = ?", Long.class, slug);
        return id == null ? 0L : id;
    }

    private long insertProduct(String sku, long category, String price, String contentQuantity) {
        jdbc.update("INSERT INTO products (public_id, sku, slug, category_id, tax_rate_id, name, format_label, price, content_quantity,"
                + " content_unit, status) VALUES (?, ?, ?, ?, ?, ?, 'Formato', ?, ?, 'ud', 'ACTIVE')",
                publicId(), sku, sku.toLowerCase(java.util.Locale.ROOT), category, taxRateId(), "Producto " + sku, price, contentQuantity);
        Long id = jdbc.queryForObject("SELECT id FROM products WHERE sku = ?", Long.class, sku);
        return id == null ? 0L : id;
    }

    private long insertOrder(long user, String number, String items, String discount, String shipping, String total) {
        jdbc.update("INSERT INTO orders (public_id, order_number, user_id, status, placed_at, ship_recipient_name, ship_street,"
                + " ship_postal_code, ship_city, ship_province, delivery_distance_km, distance_provider, distance_calculated_at,"
                + " estimated_delivery_from, estimated_delivery_to, items_total, discount_total, shipping_fee, total_amount, net_total, tax_total)"
                + " VALUES (?, ?, ?, 'PENDING_PAYMENT', NOW(3), 'Ana Pérez', 'Calle Ejemplo', '29712', 'La Viñuela', 'Málaga', 19.0, 'TEST', NOW(3),"
                + " CURDATE() + INTERVAL 1 DAY, CURDATE() + INTERVAL 3 DAY, ?, ?, ?, ?, 8.00, 1.00)",
                publicId(), number, user, items, discount, shipping, total);
        Long id = jdbc.queryForObject("SELECT id FROM orders WHERE order_number = ?", Long.class, number);
        return id == null ? 0L : id;
    }

    private void insertPayment(long order, String reference, String status) {
        jdbc.update("INSERT INTO payments (public_id, order_id, redsys_order_ref, amount, status) VALUES (?, ?, ?, 10.00, ?)",
                publicId(), order, reference, status);
    }

    /** Cada acción de la lista debe ser rechazada por la base de datos; las anteriores se ejecutan antes. */
    private static void assertRejected(Runnable... actions) {
        for (int i = 0; i < actions.length - 1; i++) {
            actions[i].run();
        }
        assertThatThrownBy(actions[actions.length - 1]::run).isInstanceOf(DataAccessException.class);
    }
}

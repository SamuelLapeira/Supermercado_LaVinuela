package com.lavinuela.shop.content.infrastructure.persistence;

import com.lavinuela.shop.content.ContentApi;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** Lee las plantillas de correo y los ajustes de la tienda de sus tablas. */
@Component
class JdbcContentApi implements ContentApi {

    private static final String DEFAULT_SITE_NAME = "Supermercado La Viñuela";

    private final JdbcClient jdbc;

    JdbcContentApi(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<EmailTemplateView> findEmailTemplate(String code) {
        return jdbc.sql("SELECT subject, body_html, body_text FROM email_templates"
                        + " WHERE code = :code AND locale = 'es-ES' AND active = TRUE")
                .param("code", code)
                .query((rs, rowNum) -> new EmailTemplateView(
                        rs.getString("subject"), rs.getString("body_html"), rs.getString("body_text")))
                .optional();
    }

    @Override
    public String siteName() {
        return jdbc.sql("SELECT setting_value FROM app_settings WHERE setting_key = 'site.name'")
                .query(String.class)
                .optional()
                .orElse(DEFAULT_SITE_NAME);
    }
}

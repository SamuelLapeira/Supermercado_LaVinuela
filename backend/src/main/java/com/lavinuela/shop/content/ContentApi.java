package com.lavinuela.shop.content;

import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * API pública del módulo de contenido: lo que otros módulos pueden pedirle. Las plantillas de los correos y los textos
 * de la tienda los edita la empresa desde el panel, por eso no están escritos en el código.
 */
public interface ContentApi {

    /** Plantilla de correo activa con ese código (por ejemplo {@code EMAIL_VERIFICATION}), en español. */
    Optional<EmailTemplateView> findEmailTemplate(String code);

    /** Nombre comercial de la tienda, tal como aparece en los correos. */
    String siteName();

    /**
     * Plantilla de correo. Los huecos se escriben {@code {{nombre}}}.
     *
     * @param bodyText versión en texto plano, para clientes de correo que no muestran HTML
     */
    record EmailTemplateView(String subject, String bodyHtml, @Nullable String bodyText) {
    }
}

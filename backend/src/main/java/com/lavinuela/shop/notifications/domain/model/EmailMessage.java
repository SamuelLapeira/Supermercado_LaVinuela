package com.lavinuela.shop.notifications.domain.model;

import org.jspecify.annotations.Nullable;

/** Un correo listo para enviar. */
public record EmailMessage(String to, String subject, String html, @Nullable String text) {

    /** El contenido puede llevar códigos y datos personales: no se muestra en los registros. */
    @Override
    public String toString() {
        return "EmailMessage[to=***]";
    }
}

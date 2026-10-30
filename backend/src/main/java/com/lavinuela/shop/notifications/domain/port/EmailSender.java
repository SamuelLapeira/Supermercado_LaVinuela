package com.lavinuela.shop.notifications.domain.port;

import com.lavinuela.shop.notifications.domain.model.EmailMessage;

/** Envía correos. La implementación real usa SMTP; en las pruebas se sustituye por una que los guarda en memoria. */
public interface EmailSender {

    void send(EmailMessage message);
}

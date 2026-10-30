package com.lavinuela.shop.notifications.infrastructure.external;

import com.lavinuela.shop.notifications.domain.model.EmailMessage;
import com.lavinuela.shop.notifications.domain.port.EmailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** No envía nada: solo deja constancia. Se usa cuando no hay un servidor de correo configurado. */
class LoggingEmailSender implements EmailSender {

    private static final Logger LOG = LoggerFactory.getLogger(LoggingEmailSender.class);

    @Override
    public void send(EmailMessage message) {
        LOG.warn("Correo NO enviado: no hay un servidor de correo configurado (spring.mail.host) [{}]", message);
    }
}

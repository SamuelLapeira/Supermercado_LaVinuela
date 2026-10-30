package com.lavinuela.shop.testsupport;

import com.lavinuela.shop.notifications.domain.model.EmailMessage;
import com.lavinuela.shop.notifications.domain.port.EmailSender;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Guarda los correos en memoria en vez de enviarlos, para que las pruebas lean el código que llevan. */
public final class RecordingEmailSender implements EmailSender {

    private final List<EmailMessage> sent = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void send(EmailMessage message) {
        sent.add(message);
    }

    public List<EmailMessage> sentTo(String address) {
        synchronized (sent) {
            return sent.stream().filter(message -> message.to().equalsIgnoreCase(address)).toList();
        }
    }

    public EmailMessage lastTo(String address) {
        List<EmailMessage> messages = sentTo(address);
        if (messages.isEmpty()) {
            throw new AssertionError("No se ha enviado ningún correo a " + address);
        }
        return messages.get(messages.size() - 1);
    }
}

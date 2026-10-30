package com.lavinuela.shop.notifications.infrastructure.external;

import com.lavinuela.shop.notifications.domain.model.EmailMessage;
import com.lavinuela.shop.notifications.domain.port.EmailSender;
import com.lavinuela.shop.shared.error.ErrorCode;
import com.lavinuela.shop.shared.error.ExternalServiceUnavailable;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

/** Envía los correos por SMTP: Mailpit en local, el proveedor de correo real en producción. */
class SmtpEmailSender implements EmailSender {

    private static final Logger LOG = LoggerFactory.getLogger(SmtpEmailSender.class);

    private final JavaMailSender mailSender;
    private final String from;

    SmtpEmailSender(JavaMailSender mailSender, String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @Override
    public void send(EmailMessage message) {
        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, StandardCharsets.UTF_8.name());
            helper.setFrom(from);
            helper.setTo(message.to());
            helper.setSubject(message.subject());
            String text = message.text();
            if (text == null) {
                helper.setText(message.html(), true);
            } else {
                helper.setText(text, message.html());
            }
            mailSender.send(mime);
        } catch (MessagingException | MailException e) {
            LOG.error("Fallo al enviar un correo por SMTP", e);
            throw new ExternalServiceUnavailable(ErrorCode.EMAIL_SERVICE_UNAVAILABLE);
        }
    }
}

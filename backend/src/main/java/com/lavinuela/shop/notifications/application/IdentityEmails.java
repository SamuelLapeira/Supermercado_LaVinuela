package com.lavinuela.shop.notifications.application;

import com.lavinuela.shop.content.ContentApi;
import com.lavinuela.shop.content.ContentApi.EmailTemplateView;
import com.lavinuela.shop.notifications.domain.model.EmailMessage;
import com.lavinuela.shop.notifications.domain.port.EmailSender;
import com.lavinuela.shop.notifications.domain.service.TemplateRenderer;
import com.lavinuela.shop.shared.logging.Masking;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Correos relacionados con las cuentas: el código de verificación y el aviso de «ya tienes una cuenta». */
@Service
public class IdentityEmails {

    static final String VERIFICATION_TEMPLATE = "EMAIL_VERIFICATION";
    static final String ACCOUNT_EXISTS_TEMPLATE = "ACCOUNT_EXISTS";

    private static final Logger LOG = LoggerFactory.getLogger(IdentityEmails.class);

    private final ContentApi content;
    private final EmailSender sender;

    public IdentityEmails(ContentApi content, EmailSender sender) {
        this.content = content;
        this.sender = sender;
    }

    public void sendVerificationCode(String email, String username, String code, int validForMinutes) {
        send(VERIFICATION_TEMPLATE, email, Map.of(
                "customerName", username,
                "code", code,
                "minutes", Integer.toString(validForMinutes),
                "siteName", content.siteName()));
    }

    public void sendAccountExists(String email, String username) {
        send(ACCOUNT_EXISTS_TEMPLATE, email, Map.of(
                "customerName", username,
                "siteName", content.siteName()));
    }

    private void send(String templateCode, String to, Map<String, String> values) {
        Optional<EmailTemplateView> template = content.findEmailTemplate(templateCode);
        if (template.isEmpty()) {
            LOG.error("No existe la plantilla de correo {}: no se envía el mensaje a {}", templateCode, Masking.email(to));
            return;
        }
        EmailTemplateView view = template.get();
        String text = view.bodyText() == null ? null : TemplateRenderer.renderText(view.bodyText(), values);
        sender.send(new EmailMessage(
                to,
                TemplateRenderer.renderText(view.subject(), values),
                TemplateRenderer.renderHtml(view.bodyHtml(), values),
                text));
        LOG.info("Correo enviado [plantilla={}, destino={}]", templateCode, Masking.email(to));
    }
}

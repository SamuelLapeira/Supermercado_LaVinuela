package com.lavinuela.shop.notifications;

import static org.assertj.core.api.Assertions.assertThat;

import com.lavinuela.shop.content.ContentApi;
import com.lavinuela.shop.notifications.application.IdentityEmails;
import com.lavinuela.shop.notifications.domain.model.EmailMessage;
import com.lavinuela.shop.testsupport.RecordingEmailSender;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class IdentityEmailsTests {

    private final RecordingEmailSender sender = new RecordingEmailSender();

    private ContentApi content(Map<String, ContentApi.EmailTemplateView> templates) {
        return new ContentApi() {
            @Override
            public Optional<EmailTemplateView> findEmailTemplate(String code) {
                return Optional.ofNullable(templates.get(code));
            }

            @Override
            public String siteName() {
                return "La Viñuela";
            }
        };
    }

    private final Map<String, ContentApi.EmailTemplateView> templates = Map.of(
            "EMAIL_VERIFICATION", new ContentApi.EmailTemplateView(
                    "Tu código de {{siteName}}",
                    "<p>Hola, {{customerName}}: <b>{{code}}</b> ({{minutes}} min)</p>",
                    "Hola, {{customerName}}: {{code}} ({{minutes}} min)"),
            "ACCOUNT_EXISTS", new ContentApi.EmailTemplateView(
                    "Ya tienes cuenta en {{siteName}}", "<p>{{customerName}}</p>", null));

    @Test
    void sendsTheVerificationCodeUsingTheTemplate() {
        new IdentityEmails(content(templates), sender).sendVerificationCode("ana@example.com", "ana", "123456", 15);

        EmailMessage message = sender.lastTo("ana@example.com");
        assertThat(message.subject()).isEqualTo("Tu código de La Viñuela");
        assertThat(message.html()).isEqualTo("<p>Hola, ana: <b>123456</b> (15 min)</p>");
        assertThat(message.text()).isEqualTo("Hola, ana: 123456 (15 min)");
    }

    @Test
    void escapesTheUsernameInTheHtmlVersionOnly() {
        new IdentityEmails(content(templates), sender).sendVerificationCode("x@example.com", "<b>x</b>", "123456", 15);

        EmailMessage message = sender.lastTo("x@example.com");
        assertThat(message.html()).contains("&lt;b&gt;x&lt;/b&gt;").doesNotContain("<b>x</b>");
        assertThat(message.text()).contains("<b>x</b>");
    }

    @Test
    void sendsTheAccountExistsNoticeWithoutPlainTextWhenTheTemplateHasNone() {
        new IdentityEmails(content(templates), sender).sendAccountExists("ana@example.com", "ana");

        EmailMessage message = sender.lastTo("ana@example.com");
        assertThat(message.subject()).isEqualTo("Ya tienes cuenta en La Viñuela");
        assertThat(message.text()).isNull();
    }

    @Test
    void sendsNothingWhenTheTemplateDoesNotExist() {
        new IdentityEmails(content(Map.of()), sender).sendVerificationCode("ana@example.com", "ana", "123456", 15);

        assertThat(sender.sentTo("ana@example.com")).isEmpty();
    }

    @Test
    void theMessageNeverShowsItsContentInLogs() {
        EmailMessage message = new EmailMessage("ana@example.com", "asunto", "<p>123456</p>", "123456");

        assertThat(message.toString()).doesNotContain("123456").doesNotContain("ana");
    }
}

package com.lavinuela.shop.notifications.infrastructure.external;

import com.lavinuela.shop.notifications.domain.port.EmailSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * Elige cómo se envían los correos: por SMTP si hay un servidor configurado ({@code spring.mail.host}); si no, no se
 * envía nada y se deja constancia en el registro (por ejemplo, al arrancar sin el entorno local).
 */
@Configuration
class EmailConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "spring.mail", name = "host")
    EmailSender smtpEmailSender(JavaMailSender mailSender, @Value("${app.mail.from}") String from) {
        return new SmtpEmailSender(mailSender, from);
    }

    @Bean
    @ConditionalOnMissingBean(EmailSender.class)
    EmailSender loggingEmailSender() {
        return new LoggingEmailSender();
    }
}

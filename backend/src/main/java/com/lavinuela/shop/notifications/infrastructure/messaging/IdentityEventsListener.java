package com.lavinuela.shop.notifications.infrastructure.messaging;

import com.lavinuela.shop.identity.IdentityEvents.AccountAlreadyExists;
import com.lavinuela.shop.identity.IdentityEvents.VerificationCodeIssued;
import com.lavinuela.shop.notifications.application.IdentityEmails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Envía los correos de cuentas cuando el módulo de identidad lo pide.
 *
 * <p>Se ejecuta <b>después de confirmar</b> la transacción: si el registro fallara, no sale ningún correo. Y si el
 * envío falla (el servidor de correo no responde), el error se registra pero no se propaga: la cuenta ya está creada y
 * la persona puede pedir un código nuevo.
 */
@Component
class IdentityEventsListener {

    private static final Logger LOG = LoggerFactory.getLogger(IdentityEventsListener.class);

    private final IdentityEmails emails;

    IdentityEventsListener(IdentityEmails emails) {
        this.emails = emails;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    void onVerificationCodeIssued(VerificationCodeIssued event) {
        try {
            emails.sendVerificationCode(event.email(), event.username(), event.code(), event.validForMinutes());
        } catch (RuntimeException e) {
            LOG.error("No se ha podido enviar el código de verificación", e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    void onAccountAlreadyExists(AccountAlreadyExists event) {
        try {
            emails.sendAccountExists(event.email(), event.username());
        } catch (RuntimeException e) {
            LOG.error("No se ha podido enviar el aviso de cuenta existente", e);
        }
    }
}

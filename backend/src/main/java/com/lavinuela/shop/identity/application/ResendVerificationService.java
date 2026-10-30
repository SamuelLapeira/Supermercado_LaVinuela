package com.lavinuela.shop.identity.application;

import com.lavinuela.shop.identity.domain.model.User;
import com.lavinuela.shop.identity.domain.port.UserRepository;
import com.lavinuela.shop.identity.domain.service.AccountPolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reenvía el código de verificación. No dice nunca si el correo existe: si la cuenta está pendiente y no se ha
 * superado ningún límite se emite un código nuevo, y en cualquier otro caso no se hace nada.
 */
@Service
public class ResendVerificationService {

    private final UserRepository users;
    private final VerificationCodeService codes;
    private final AccountPolicy policy;

    public ResendVerificationService(UserRepository users, VerificationCodeService codes, AccountPolicy policy) {
        this.users = users;
        this.codes = codes;
        this.policy = policy;
    }

    @Transactional
    public void resend(String rawEmail) {
        users.findByEmail(policy.normalizeEmail(rawEmail))
                .filter(User::isPendingVerification)
                .ifPresent(codes::issue);
    }
}

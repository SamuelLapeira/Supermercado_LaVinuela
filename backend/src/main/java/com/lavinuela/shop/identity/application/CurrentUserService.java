package com.lavinuela.shop.identity.application;

import com.lavinuela.shop.identity.domain.model.User;
import com.lavinuela.shop.identity.domain.port.UserRepository;
import com.lavinuela.shop.shared.error.BusinessRuleViolation;
import com.lavinuela.shop.shared.error.ErrorCode;
import org.springframework.stereotype.Service;

/** Quién es la persona de la sesión actual (a partir del identificador público que lleva el token de acceso). */
@Service
public class CurrentUserService {

    private final UserRepository users;

    public CurrentUserService(UserRepository users) {
        this.users = users;
    }

    public UserSummary currentUser(String publicId) {
        return users.findByPublicId(publicId)
                .filter(User::isActive)
                .map(SessionIssuer::summaryOf)
                .orElseThrow(() -> new BusinessRuleViolation(ErrorCode.AUTH_REQUIRED));
    }
}

package com.lavinuela.shop.identity.infrastructure.security;

import com.lavinuela.shop.identity.domain.model.User;
import com.lavinuela.shop.identity.domain.port.AccessTokenIssuer;
import com.lavinuela.shop.identity.domain.service.LoginPolicy;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

/**
 * Emite los tokens de acceso: JWT firmados con HS256 que duran {@code ACCESS_TOKEN_VALIDITY} (15 minutos).
 *
 * <p>Llevan el identificador público de la cuenta (nunca el interno), sus roles y el nombre de usuario. No llevan el
 * correo ni nada sensible: un JWT se puede leer (no está cifrado, solo firmado).
 */
@Component
class JwtAccessTokenIssuer implements AccessTokenIssuer {

    static final String ISSUER = "lavinuela-shop";

    private final JwtEncoder encoder;

    JwtAccessTokenIssuer(JwtEncoder encoder) {
        this.encoder = encoder;
    }

    @Override
    public IssuedAccessToken issue(User user, Instant now) {
        Instant expiresAt = now.plus(LoginPolicy.ACCESS_TOKEN_VALIDITY);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(user.publicId().value())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .id(UUID.randomUUID().toString())
                .claim("username", user.username())
                .claim("roles", List.copyOf(user.roles()))
                .build();
        String token = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new IssuedAccessToken(token, expiresAt);
    }
}

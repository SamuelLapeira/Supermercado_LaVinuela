package com.lavinuela.shop.identity.infrastructure.security;

import com.lavinuela.shop.identity.infrastructure.config.IdentityProperties;
import com.lavinuela.shop.shared.error.ErrorCode;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

/**
 * Seguridad de la API.
 *
 * <ul>
 *   <li><b>Sin estado:</b> no hay sesión en el servidor; cada petición lleva su token de acceso (JWT) en la cabecera
 *       {@code Authorization: Bearer ...}.</li>
 *   <li><b>Públicas:</b> {@code /api/v1/auth/**} (registro, verificación, inicio de sesión, renovación y cierre), el
 *       contrato OpenAPI y los puntos de control del sistema. <b>Todo lo demás exige sesión.</b></li>
 *   <li><b>CSRF desactivado</b> a propósito: el token de acceso va en una cabecera (no en una cookie), así que otra web
 *       no puede usarlo. Las dos operaciones que sí usan una cookie (renovar y cerrar sesión) exigen una cabecera propia
 *       y la cookie es {@code SameSite=Strict}.</li>
 *   <li><b>Errores en formato estándar:</b> sin sesión (401) y sin permiso (403) responden con {@code application/problem+json}.</li>
 * </ul>
 */
@Configuration
class SecurityConfiguration {

    @Bean
    SecurityFilterChain apiSecurity(HttpSecurity http, JwtDecoder jwtDecoder) throws Exception {
        AuthenticationEntryPoint unauthenticated = SecurityConfiguration::writeUnauthenticated;
        AccessDeniedHandler denied = (request, response, exception) -> ProblemJsonWriter.write(response, ErrorCode.ACCESS_DENIED);

        http.csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers("/api/v1/auth/**", "/v3/api-docs", "/v3/api-docs/**", "/actuator/**", "/error")
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                .exceptionHandling(handling -> handling.authenticationEntryPoint(unauthenticated).accessDeniedHandler(denied))
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt.decoder(jwtDecoder).jwtAuthenticationConverter(authenticationConverter()))
                        .authenticationEntryPoint(unauthenticated)
                        .accessDeniedHandler(denied));
        return http.build();
    }

    /** Sin token: «tienes que iniciar sesión». Con un token que no vale (caducado o manipulado): «tu sesión ha caducado». */
    private static void writeUnauthenticated(
            HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException {
        boolean hadToken = request.getHeader(HttpHeaders.AUTHORIZATION) != null;
        ProblemJsonWriter.write(response, hadToken ? ErrorCode.AUTH_TOKEN_EXPIRED : ErrorCode.AUTH_REQUIRED);
    }

    @Bean
    JwtEncoder jwtEncoder(IdentityProperties properties) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey(properties)));
    }

    /** Valida la firma, el emisor y la caducidad (con el reloj de la aplicación, sin margen de tolerancia). */
    @Bean
    JwtDecoder jwtDecoder(IdentityProperties properties, Clock clock) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey(properties)).macAlgorithm(MacAlgorithm.HS256).build();
        JwtTimestampValidator timestamps = new JwtTimestampValidator(Duration.ZERO);
        timestamps.setClock(clock);
        List<OAuth2TokenValidator<Jwt>> validators = List.of(timestamps, new JwtIssuerValidator(JwtAccessTokenIssuer.ISSUER));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(validators));
        return decoder;
    }

    private static SecretKey secretKey(IdentityProperties properties) {
        return new SecretKeySpec(properties.jwtSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    /** Los roles del token pasan a ser permisos de Spring Security con el prefijo {@code ROLE_} (ROLE_CUSTOMER, ROLE_ADMIN...). */
    private static JwtAuthenticationConverter authenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("roles");
        authorities.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }
}

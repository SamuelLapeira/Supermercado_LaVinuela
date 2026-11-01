package com.lavinuela.shop.identity.infrastructure.web;

import java.time.Duration;
import org.springframework.http.ResponseCookie;

/**
 * La cookie con el token de renovación.
 *
 * <ul>
 *   <li>{@code HttpOnly}: JavaScript no puede leerla, así que un fallo de seguridad en la página (XSS) no la roba.</li>
 *   <li>{@code Secure}: solo viaja por HTTPS (salvo en el entorno local).</li>
 *   <li>{@code SameSite=Strict}: el navegador no la envía en peticiones que vienen de otra web.</li>
 *   <li>{@code Path=/api/v1/auth}: solo se envía a las operaciones de sesión, no a cada petición de la API.</li>
 * </ul>
 */
final class RefreshCookie {

    static final String NAME = "lv_refresh";
    static final String PATH = "/api/v1/auth";

    private RefreshCookie() {
    }

    static ResponseCookie issue(String token, Duration maxAge, boolean secure) {
        return base(token, secure).maxAge(maxAge).build();
    }

    /** Orden de borrar la cookie. */
    static ResponseCookie clear(boolean secure) {
        return base("", secure).maxAge(Duration.ZERO).build();
    }

    private static ResponseCookie.ResponseCookieBuilder base(String value, boolean secure) {
        return ResponseCookie.from(NAME, value).httpOnly(true).secure(secure).sameSite("Strict").path(PATH);
    }
}

package com.lavinuela.shop.identity.infrastructure.security;

import com.lavinuela.shop.shared.error.ErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.slf4j.MDC;

/**
 * Escribe un error estándar ({@code application/problem+json}) directamente en la respuesta. Hace falta porque los
 * errores de seguridad (sin sesión, sin permiso) los produce el filtro de Spring Security antes de llegar a los
 * controladores, así que no pasan por el manejador global de errores.
 */
final class ProblemJsonWriter {

    private ProblemJsonWriter() {
    }

    static void write(HttpServletResponse response, ErrorCode code) throws IOException {
        response.setStatus(code.httpStatus());
        response.setContentType("application/problem+json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        if (code.httpStatus() == HttpServletResponse.SC_UNAUTHORIZED) {
            response.setHeader("WWW-Authenticate", "Bearer");
        }
        String traceId = MDC.get("traceId");
        StringBuilder json = new StringBuilder(256)
                .append("{\"type\":\"").append(code.problemType())
                .append("\",\"title\":\"").append(code.name())
                .append("\",\"status\":").append(code.httpStatus())
                .append(",\"detail\":\"").append(escape(code.defaultMessage()))
                .append("\",\"code\":\"").append(code.name()).append('"');
        if (traceId != null) {
            json.append(",\"traceId\":\"").append(escape(traceId)).append('"');
        }
        json.append('}');
        response.getWriter().write(json.toString());
    }

    private static String escape(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

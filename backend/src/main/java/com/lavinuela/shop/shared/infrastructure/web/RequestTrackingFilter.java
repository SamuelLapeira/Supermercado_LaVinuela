package com.lavinuela.shop.shared.infrastructure.web;

import com.lavinuela.shop.shared.domain.PublicId;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Da a cada petición un identificador de seguimiento y escribe una línea de registro al terminar.
 *
 * <p>Se llama {@code RequestTrackingFilter} y no {@code RequestContextFilter} porque Spring MVC ya define un
 * componente con ese nombre y los dos chocarían.
 *
 * <p><b>Identificador.</b> Si la petición trae una cabecera {@code X-Request-Id} válida, se respeta; si no, se
 * genera uno. Se devuelve siempre en la respuesta y se añade a todos los registros de esa petición (campo
 * {@code requestId}). Las respuestas de error lo incluyen como {@code traceId}: la persona que reporta un
 * problema puede darnos ese código y se encuentra todo lo ocurrido en esa petición. Cuando se incorpore
 * OpenTelemetry, el identificador de traza ocupará su lugar sin cambiar nada más.
 *
 * <p><b>Cabecera de entrada.</b> Solo se acepta si tiene entre 8 y 64 caracteres alfanuméricos, punto, guion
 * o guion bajo. Cualquier otro valor se descarta: así no se puede inyectar texto falso en los registros.
 *
 * <p><b>Línea de registro.</b> Método, ruta, estado y duración. <b>Nunca</b> la cadena de consulta (puede llevar
 * datos personales), ni cabeceras, ni el cuerpo.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RequestTrackingFilter extends OncePerRequestFilter {

    /** Cabecera que lleva el identificador de la petición. */
    public static final String HEADER = "X-Request-Id";

    /** Clave en el contexto de registro (MDC). */
    public static final String MDC_REQUEST_ID = "requestId";

    /** Clave que usan las respuestas de error; hoy coincide con el identificador de la petición. */
    public static final String MDC_TRACE_ID = "traceId";

    private static final Logger LOG = LoggerFactory.getLogger(RequestTrackingFilter.class);
    private static final Pattern VALID_ID = Pattern.compile("^[A-Za-z0-9._-]{8,64}$");
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final long NANOS_PER_MILLI = 1_000_000L;

    private final Clock clock;

    public RequestTrackingFilter(Clock clock) {
        this.clock = clock;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String requestId = resolveId(request.getHeader(HEADER));
        MDC.put(MDC_REQUEST_ID, requestId);
        MDC.put(MDC_TRACE_ID, requestId);
        response.setHeader(HEADER, requestId);
        long start = System.nanoTime();
        try {
            chain.doFilter(request, response);
        } finally {
            logRequest(request, response.getStatus(), (System.nanoTime() - start) / NANOS_PER_MILLI);
            MDC.remove(MDC_REQUEST_ID);
            MDC.remove(MDC_TRACE_ID);
        }
    }

    private String resolveId(@Nullable String incoming) {
        if (incoming != null && VALID_ID.matcher(incoming).matches()) {
            return incoming;
        }
        return PublicId.generate(clock, RANDOM).value();
    }

    private static void logRequest(HttpServletRequest request, int status, long millis) {
        String path = request.getRequestURI();
        if (path.startsWith("/actuator")) {
            // Las sondas de salud y las métricas se consultan a menudo: no llenan el registro.
            LOG.debug("{} {} -> {} ({} ms)", request.getMethod(), path, status, millis);
        } else if (status >= 500) {
            LOG.warn("{} {} -> {} ({} ms)", request.getMethod(), path, status, millis);
        } else {
            LOG.info("{} {} -> {} ({} ms)", request.getMethod(), path, status, millis);
        }
    }
}

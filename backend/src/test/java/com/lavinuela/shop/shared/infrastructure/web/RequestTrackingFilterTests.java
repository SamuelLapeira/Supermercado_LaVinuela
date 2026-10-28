package com.lavinuela.shop.shared.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.lavinuela.shop.shared.domain.PublicId;
import jakarta.servlet.FilterChain;
import java.time.Clock;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestTrackingFilterTests {

    private final RequestTrackingFilter filter = new RequestTrackingFilter(Clock.systemUTC());

    @Test
    void generatesAnIdentifierWhenTheRequestHasNone() throws Exception {
        MockHttpServletResponse response = run(new MockHttpServletRequest("GET", "/api/v1/x"));

        String id = response.getHeader(RequestTrackingFilter.HEADER);
        assertThat(id).isNotNull();
        assertThat(PublicId.isValid(id)).isTrue();
    }

    @Test
    void respectsAValidIncomingIdentifier() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/x");
        request.addHeader(RequestTrackingFilter.HEADER, "mi-peticion-0001");

        assertThat(run(request).getHeader(RequestTrackingFilter.HEADER)).isEqualTo("mi-peticion-0001");
    }

    @Test
    void discardsInvalidIncomingIdentifiers() throws Exception {
        for (String bad : new String[] {"corto", "tiene espacios en medio", "x".repeat(65), "linea1\nlinea2xx", "<script>alert(1)</script>"}) {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/x");
            request.addHeader(RequestTrackingFilter.HEADER, bad);

            String id = run(request).getHeader(RequestTrackingFilter.HEADER);

            assertThat(id).as(bad).isNotEqualTo(bad);
            assertThat(PublicId.isValid(id)).as(bad).isTrue();
        }
    }

    @Test
    void exposesTheIdentifierInTheLoggingContextDuringTheRequestAndClearsItAfterwards() throws Exception {
        AtomicReference<String> requestId = new AtomicReference<>();
        AtomicReference<String> traceId = new AtomicReference<>();
        FilterChain chain = (req, res) -> {
            requestId.set(MDC.get(RequestTrackingFilter.MDC_REQUEST_ID));
            traceId.set(MDC.get(RequestTrackingFilter.MDC_TRACE_ID));
        };

        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest("GET", "/api/v1/x"), response, chain);

        assertThat(requestId.get()).isEqualTo(response.getHeader(RequestTrackingFilter.HEADER));
        assertThat(traceId.get()).isEqualTo(requestId.get());
        assertThat(MDC.get(RequestTrackingFilter.MDC_REQUEST_ID)).isNull();
        assertThat(MDC.get(RequestTrackingFilter.MDC_TRACE_ID)).isNull();
    }

    @Test
    void clearsTheLoggingContextEvenWhenTheRequestFails() {
        FilterChain failing = (req, res) -> {
            throw new IllegalStateException("fallo simulado");
        };

        try {
            filter.doFilter(new MockHttpServletRequest("GET", "/api/v1/x"), new MockHttpServletResponse(), failing);
        } catch (Exception expected) {
            assertThat(expected).isInstanceOf(IllegalStateException.class);
        }

        assertThat(MDC.get(RequestTrackingFilter.MDC_REQUEST_ID)).isNull();
    }

    private MockHttpServletResponse run(MockHttpServletRequest request) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, (req, res) -> { });
        return response;
    }
}

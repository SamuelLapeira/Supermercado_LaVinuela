package com.lavinuela.shop.shared.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.lavinuela.shop.shared.error.ConflictException;
import com.lavinuela.shop.shared.error.ErrorCode;
import com.lavinuela.shop.shared.error.ResourceNotFound;
import com.lavinuela.shop.shared.error.ValidationFailedException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTests {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void translatesADomainErrorIntoAProblem() {
        ResponseEntity<ProblemDetail> response = handler.handleCoded(new ResourceNotFound(ErrorCode.RESOURCE_NOT_FOUND));

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        ProblemDetail body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getDetail()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND.defaultMessage());
        assertThat(body.getType()).hasToString("urn:lavinuela:problem:resource-not-found");
        assertThat(body.getProperties()).containsEntry("code", "RESOURCE_NOT_FOUND");
    }

    @Test
    void includesDetailsOfAConflict() {
        ResponseEntity<ProblemDetail> response = handler.handleCoded(new ConflictException(
                ErrorCode.STOCK_INSUFFICIENT, "Sin stock", Map.of("productos", List.of("Aguacate"))));

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        ProblemDetail body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getProperties()).containsKey("details");
    }

    @Test
    void listsFieldErrorsOfAValidationFailure() {
        ResponseEntity<ProblemDetail> response =
                handler.handleCoded(ValidationFailedException.of("email", "No es un correo válido."));

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        ProblemDetail body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getProperties()).containsKey("errors");
    }

    @Test
    void neverLeaksTheDetailOfUnexpectedErrors() {
        ResponseEntity<ProblemDetail> response =
                handler.handleUnexpected(new IllegalStateException("password=secreto jdbc:mysql://interno"));

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        ProblemDetail body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getDetail()).isEqualTo(ErrorCode.INTERNAL_ERROR.defaultMessage());
        assertThat(String.valueOf(body.getDetail())).doesNotContain("secreto", "jdbc");
    }
}

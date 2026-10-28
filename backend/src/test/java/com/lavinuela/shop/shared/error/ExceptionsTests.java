package com.lavinuela.shop.shared.error;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ExceptionsTests {

    @Test
    void usesTheDefaultMessageOfTheCode() {
        ResourceNotFound ex = new ResourceNotFound(ErrorCode.RESOURCE_NOT_FOUND);

        assertThat(ex.code()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
        assertThat(ex).hasMessage(ErrorCode.RESOURCE_NOT_FOUND.defaultMessage());
    }

    @Test
    void carriesDetails() {
        ConflictException ex = new ConflictException(
                ErrorCode.STOCK_INSUFFICIENT, "Sin stock", Map.of("productos", List.of("A", "B")));

        assertThat(ex.details()).containsKey("productos");
    }

    @Test
    void validationFailureListsFieldErrors() {
        ValidationFailedException ex = ValidationFailedException.of("email", "No es un correo válido.");

        assertThat(ex.code()).isEqualTo(ErrorCode.VALIDATION_FAILED);
        assertThat(ex.violations()).containsExactly(FieldViolation.of("email", "No es un correo válido."));
    }

    @Test
    void infrastructureFailuresAreCodedToo() {
        ExternalServiceUnavailable ex = new ExternalServiceUnavailable(ErrorCode.DELIVERY_SERVICE_UNAVAILABLE);

        assertThat(ex).isInstanceOf(InfrastructureException.class);
        assertThat(ex.code().httpStatus()).isEqualTo(503);
    }
}

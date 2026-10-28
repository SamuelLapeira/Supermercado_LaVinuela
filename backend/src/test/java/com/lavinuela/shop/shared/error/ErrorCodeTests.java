package com.lavinuela.shop.shared.error;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ErrorCodeTests {

    @Test
    void everyCodeHasAnErrorStatusAndAUserMessage() {
        for (ErrorCode code : ErrorCode.values()) {
            assertThat(code.httpStatus()).as(code.name()).isBetween(400, 599);
            assertThat(code.defaultMessage()).as(code.name()).isNotBlank();
        }
    }

    @Test
    void problemTypesAreUnique() {
        Set<String> types = new HashSet<>();
        for (ErrorCode code : ErrorCode.values()) {
            assertThat(types.add(code.problemType())).as(code.name()).isTrue();
        }
    }

    @Test
    void problemTypeUsesKebabCase() {
        assertThat(ErrorCode.STOCK_INSUFFICIENT.problemType()).isEqualTo("urn:lavinuela:problem:stock-insufficient");
    }

    @Test
    void userMessagesNeverMentionTechnicalTerms() {
        for (ErrorCode code : ErrorCode.values()) {
            assertThat(code.defaultMessage().toLowerCase(java.util.Locale.ROOT))
                    .as(code.name())
                    .doesNotContain("exception", "sql", "null", "stack");
        }
    }
}

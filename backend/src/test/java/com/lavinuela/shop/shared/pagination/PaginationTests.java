package com.lavinuela.shop.shared.pagination;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lavinuela.shop.shared.error.ValidationFailedException;
import java.util.List;
import org.junit.jupiter.api.Test;

class PaginationTests {

    @Test
    void usesDefaultsWhenMissing() {
        assertThat(PageQuery.of(null, null)).isEqualTo(new PageQuery(0, PageQuery.DEFAULT_SIZE));
    }

    @Test
    void computesTheOffset() {
        assertThat(PageQuery.of(2, 10).offset()).isEqualTo(20L);
    }

    @Test
    void rejectsOutOfRangeValues() {
        assertThatThrownBy(() -> new PageQuery(-1, 10)).isInstanceOf(ValidationFailedException.class);
        assertThatThrownBy(() -> new PageQuery(0, 0)).isInstanceOf(ValidationFailedException.class);
        assertThatThrownBy(() -> new PageQuery(0, PageQuery.MAX_SIZE + 1)).isInstanceOf(ValidationFailedException.class);
    }

    @Test
    void reportsEveryInvalidField() {
        assertThatThrownBy(() -> new PageQuery(-1, 99))
                .isInstanceOfSatisfying(ValidationFailedException.class,
                        ex -> assertThat(ex.violations()).hasSize(2));
    }

    @Test
    void computesTotalPages() {
        PageResult<Integer> page = PageResult.of(List.of(1, 2, 3), new PageQuery(0, 3), 7);

        assertThat(page.totalPages()).isEqualTo(3);
        assertThat(page.totalItems()).isEqualTo(7L);
    }

    @Test
    void emptyResultHasNoPages() {
        assertThat(PageResult.of(List.<Integer>of(), new PageQuery(0, 20), 0).totalPages()).isZero();
    }

    @Test
    void mapKeepsPaginationData() {
        PageResult<Integer> page = PageResult.of(List.of(1, 2, 3), new PageQuery(1, 3), 9);

        PageResult<String> mapped = page.map(n -> "n" + n);

        assertThat(mapped.items()).containsExactly("n1", "n2", "n3");
        assertThat(mapped.page()).isEqualTo(1);
        assertThat(mapped.totalPages()).isEqualTo(3);
    }
}

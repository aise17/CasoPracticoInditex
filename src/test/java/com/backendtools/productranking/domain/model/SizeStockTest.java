package com.backendtools.productranking.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SizeStockTest {

    @Test
    void shouldDetectAvailableStockWhenUnitsAreGreaterThanZero() {
        SizeStock sizeStock = new SizeStock(Size.S, 4);

        assertThat(sizeStock.hasAvailableUnits()).isTrue();
    }

    @Test
    void shouldDetectUnavailableStockWhenUnitsAreZero() {
        SizeStock sizeStock = new SizeStock(Size.L, 0);

        assertThat(sizeStock.hasAvailableUnits()).isFalse();
    }

    @Test
    void shouldRejectNegativeStockUnits() {
        assertThatThrownBy(() -> new SizeStock(Size.M, -1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Stock units cannot be negative");
    }

    @Test
    void shouldBeEqualWhenValuesAreEqual() {
        assertThat(new SizeStock(Size.S, 4)).isEqualTo(new SizeStock(Size.S, 4));
    }
}

package com.backendtools.productranking.domain.model;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StockTest {

    @Test
    void shouldCalculateFullAvailabilityRatio() {
        Stock stock = stock(35, 9, 9);

        assertThat(stock.availabilityRatio()).isEqualTo(1.0);
    }

    @Test
    void shouldCalculatePartialAvailabilityRatio() {
        Stock stock = stock(4, 9, 0);

        assertThat(stock.availableSizesCount()).isEqualTo(2);
        assertThat(stock.totalSizesCount()).isEqualTo(3);
        assertThat(stock.availabilityRatio()).isEqualTo(2.0 / 3.0);
    }

    @Test
    void shouldCalculateZeroAvailabilityRatio() {
        Stock stock = stock(0, 0, 0);

        assertThat(stock.availabilityRatio()).isZero();
    }

    @Test
    void shouldRejectEmptyStock() {
        assertThatThrownBy(() -> new Stock(Collections.emptyList()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Stock must contain at least one size");
    }

    @Test
    void shouldRejectDuplicatedSizes() {
        assertThatThrownBy(() -> new Stock(Arrays.asList(
            new SizeStock(Size.S, 4),
            new SizeStock(Size.S, 9)
        )))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Stock cannot contain duplicated sizes");
    }

    @Test
    void shouldBeEqualWhenValuesAreEqual() {
        assertThat(stock(4, 9, 0)).isEqualTo(stock(4, 9, 0));
    }

    private Stock stock(int smallUnits, int mediumUnits, int largeUnits) {
        return new Stock(Arrays.asList(
            new SizeStock(Size.S, smallUnits),
            new SizeStock(Size.M, mediumUnits),
            new SizeStock(Size.L, largeUnits)
        ));
    }
}

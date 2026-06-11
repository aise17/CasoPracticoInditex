package com.backendtools.productranking.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SalesUnitsTest {

    @Test
    void shouldCreateSalesUnitsWhenValueIsZero() {
        SalesUnits salesUnits = new SalesUnits(0);

        assertThat(salesUnits.value()).isZero();
    }

    @Test
    void shouldCreateSalesUnitsWhenValueIsPositive() {
        SalesUnits salesUnits = new SalesUnits(100);

        assertThat(salesUnits.value()).isEqualTo(100);
    }

    @Test
    void shouldRejectNegativeSalesUnits() {
        assertThatThrownBy(() -> new SalesUnits(-1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Sales units cannot be negative");
    }

    @Test
    void shouldBeEqualWhenValuesAreEqual() {
        assertThat(new SalesUnits(100)).isEqualTo(new SalesUnits(100));
    }
}

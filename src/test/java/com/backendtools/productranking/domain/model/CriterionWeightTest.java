package com.backendtools.productranking.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CriterionWeightTest {

    @Test
    void shouldCreateCriterionWeightWhenValueIsZero() {
        CriterionWeight weight = new CriterionWeight(0);

        assertThat(weight.value()).isZero();
    }

    @Test
    void shouldCreateCriterionWeightWhenValueIsPositive() {
        CriterionWeight weight = new CriterionWeight(0.7);

        assertThat(weight.value()).isEqualTo(0.7);
    }

    @Test
    void shouldRejectNegativeCriterionWeight() {
        assertThatThrownBy(() -> new CriterionWeight(-1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Criterion weight cannot be negative");
    }

    @Test
    void shouldBeEqualWhenValuesAreEqual() {
        assertThat(new CriterionWeight(0.7)).isEqualTo(new CriterionWeight(0.7));
    }
}

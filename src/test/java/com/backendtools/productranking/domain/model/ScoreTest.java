package com.backendtools.productranking.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScoreTest {

    @Test
    void shouldAddTwoScores() {
        Score total = new Score(70).add(new Score(20));

        assertThat(total.value()).isEqualTo(90);
    }

    @Test
    void shouldMultiplyScoreByCriterionWeight() {
        Score weightedScore = new Score(100).multiplyBy(new CriterionWeight(0.7));

        assertThat(weightedScore.value()).isEqualTo(70);
    }

    @Test
    void shouldRejectNanScore() {
        assertThatThrownBy(() -> new Score(Double.NaN))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Score must be a valid number");
    }

    @Test
    void shouldRejectInfiniteScore() {
        assertThatThrownBy(() -> new Score(Double.POSITIVE_INFINITY))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Score must be a valid number");
    }

    @Test
    void shouldBeEqualWhenValuesAreEqual() {
        assertThat(new Score(90)).isEqualTo(new Score(90));
    }
}

package com.backendtools.productranking.domain.model;

import com.backendtools.productranking.domain.ranking.CriterionName;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RankingWeightsTest {

    @Test
    void shouldReturnConfiguredCriterionWeight() {
        RankingWeights rankingWeights = RankingWeightsTestMother.weights(0.7, 0.3);

        assertThat(rankingWeights.weightFor(CriterionName.SALES_UNITS))
            .isEqualTo(new CriterionWeight(0.7));
    }

    @Test
    void shouldReturnZeroWhenCriterionWeightIsMissing() {
        RankingWeights rankingWeights = RankingWeightsTestMother.withoutStockRatioWeight(1);

        assertThat(rankingWeights.weightFor(CriterionName.STOCK_RATIO).value()).isZero();
    }

    @Test
    void shouldRejectEmptyWeights() {
        Map<CriterionName, CriterionWeight> emptyWeights = new EnumMap<>(CriterionName.class);

        assertThatThrownBy(() -> new RankingWeights(emptyWeights))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Ranking weights cannot be empty");
    }

    @Test
    void shouldBeEqualWhenValuesAreEqual() {
        assertThat(RankingWeightsTestMother.weights(0.7, 0.3))
            .isEqualTo(RankingWeightsTestMother.weights(0.7, 0.3));
    }
}

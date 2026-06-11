package com.backendtools.productranking.domain.ranking;

import com.backendtools.productranking.domain.model.Product;
import com.backendtools.productranking.domain.model.ProductRanking;
import com.backendtools.productranking.domain.model.ProductTestMother;
import com.backendtools.productranking.domain.model.RankingWeightsTestMother;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class ProductRankingServiceTest {

    private final ProductRankingService rankingService =
        ProductRankingServiceTestMother.defaultService();

    @Test
    void shouldReturnProductsOrderedByHighestWeightedScore() {
        ProductRanking ranking = rankingService.rank(
            ProductTestMother.initialDataset(),
            RankingWeightsTestMother.weights(0.7, 0.3)
        );

        assertThat(rankedProductIds(ranking))
            .containsExactly("5", "1", "3", "2", "6", "4");
    }

    @Test
    void shouldCalculateFinalScoreUsingAllConfiguredCriteria() {
        ProductRanking ranking = rankingService.rank(
            Collections.singletonList(
                ProductTestMother.product("1", "V-NECH BASIC SHIRT", 100, 4, 9, 0)
            ),
            RankingWeightsTestMother.weights(0.7, 0.3)
        );

        assertThat(ranking.products().get(0).score().value())
            .isCloseTo(90.0, within(0.01));
    }

    @Test
    void shouldRankProductsOnlyBySalesUnitsWhenStockRatioWeightIsZero() {
        ProductRanking ranking = rankingService.rank(
            ProductTestMother.initialDataset(),
            RankingWeightsTestMother.salesOnly()
        );

        assertThat(rankedProductIds(ranking))
            .containsExactly("5", "1", "3", "2", "6", "4");
    }

    @Test
    void shouldRankProductsOnlyByStockRatioWhenSalesUnitsWeightIsZero() {
        ProductRanking ranking = rankingService.rank(
            ProductTestMother.initialDataset(),
            RankingWeightsTestMother.stockOnly()
        );

        assertThat(rankedProductIds(ranking))
            .containsExactly("2", "3", "4", "6", "1", "5");
    }

    @Test
    void shouldUseZeroWeightWhenCriterionWeightIsMissing() {
        ProductRanking ranking = rankingService.rank(
            ProductTestMother.initialDataset(),
            RankingWeightsTestMother.withoutStockRatioWeight(1)
        );

        assertThat(rankedProductIds(ranking))
            .containsExactly("5", "1", "3", "2", "6", "4");
    }

    @Test
    void shouldApplyDeterministicTieBreakerUsingProductId() {
        List<Product> tiedProductsInReverseIdOrder = Arrays.asList(
            ProductTestMother.product("4", "PLEATED T-SHIRT", 50, 1, 1, 1),
            ProductTestMother.product("2", "CONTRASTING FABRIC T-SHIRT", 50, 1, 1, 1)
        );

        ProductRanking ranking = rankingService.rank(
            tiedProductsInReverseIdOrder,
            RankingWeightsTestMother.weights(1, 1)
        );

        assertThat(rankedProductIds(ranking)).containsExactly("2", "4");
    }

    @Test
    void shouldOrderByTieBreakerWhenAllWeightsAreZero() {
        ProductRanking ranking = rankingService.rank(
            ProductTestMother.initialDataset(),
            RankingWeightsTestMother.weights(0, 0)
        );

        assertThat(rankedProductIds(ranking))
            .containsExactly("1", "2", "3", "4", "5", "6");
    }

    @Test
    void shouldRejectEmptyCriteria() {
        assertThatThrownBy(() -> new ProductRankingService(Collections.emptyList()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("At least one ranking criterion is required");
    }

    private List<String> rankedProductIds(ProductRanking ranking) {
        return ranking.products()
            .stream()
            .map(rankedProduct -> rankedProduct.product().id().value())
            .collect(Collectors.toList());
    }
}

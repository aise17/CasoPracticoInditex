package com.backendtools.productranking.domain.ranking;

import com.backendtools.productranking.domain.model.Product;
import com.backendtools.productranking.domain.model.ProductTestMother;
import com.backendtools.productranking.domain.model.Score;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class StockRatioCriterionTest {

    private final StockRatioCriterion criterion = new StockRatioCriterion();

    @Test
    void shouldCalculateFullStockRatioScore() {
        Product product = ProductTestMother.productWithStock(1, 2, 3);

        Score score = criterion.calculateScore(product);

        assertThat(score.value()).isEqualTo(100);
    }

    @Test
    void shouldCalculatePartialStockRatioScore() {
        Product product = ProductTestMother.productWithStock(1, 0, 3);

        Score score = criterion.calculateScore(product);

        assertThat(score.value()).isCloseTo(66.67, within(0.01));
    }

    @Test
    void shouldCalculateZeroStockRatioScore() {
        Product product = ProductTestMother.productWithStock(0, 0, 0);

        Score score = criterion.calculateScore(product);

        assertThat(score.value()).isZero();
    }

    @Test
    void shouldReturnStockRatioCriterionName() {
        assertThat(criterion.name()).isEqualTo(CriterionName.STOCK_RATIO);
    }
}

package com.backendtools.productranking.domain.ranking;

import com.backendtools.productranking.domain.model.Product;
import com.backendtools.productranking.domain.model.ProductTestMother;
import com.backendtools.productranking.domain.model.Score;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SalesUnitsCriterionTest {

    private final SalesUnitsCriterion criterion = new SalesUnitsCriterion();

    @Test
    void shouldCalculateScoreUsingProductSalesUnits() {
        Product product = ProductTestMother.productWithSalesUnits(100);

        Score score = criterion.calculateScore(product);

        assertThat(score.value()).isEqualTo(100);
    }

    @Test
    void shouldReturnSalesUnitsCriterionName() {
        assertThat(criterion.name()).isEqualTo(CriterionName.SALES_UNITS);
    }
}

package com.backendtools.productranking.domain.ranking;

import com.backendtools.productranking.domain.model.Product;
import com.backendtools.productranking.domain.model.Score;

public final class SalesUnitsCriterion implements RankingCriterion {

    @Override
    public CriterionName name() {
        return CriterionName.SALES_UNITS;
    }

    @Override
    public Score calculateScore(Product product) {
        return new Score(product.salesUnits().value());
    }
}

package com.backendtools.productranking.domain.ranking;

import com.backendtools.productranking.domain.model.Product;
import com.backendtools.productranking.domain.model.Score;

public final class StockRatioCriterion implements RankingCriterion {

    private static final int MAX_SCORE = 100;

    @Override
    public CriterionName name() {
        return CriterionName.STOCK_RATIO;
    }

    @Override
    public Score calculateScore(Product product) {
        return new Score(product.stock().availabilityRatio() * MAX_SCORE);
    }
}

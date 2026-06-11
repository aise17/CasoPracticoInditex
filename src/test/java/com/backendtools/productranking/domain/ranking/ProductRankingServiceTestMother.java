package com.backendtools.productranking.domain.ranking;

import java.util.Arrays;

public final class ProductRankingServiceTestMother {

    private ProductRankingServiceTestMother() {
    }

    public static ProductRankingService defaultService() {
        return new ProductRankingService(Arrays.asList(
            new SalesUnitsCriterion(),
            new StockRatioCriterion()
        ));
    }
}

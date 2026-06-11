package com.backendtools.productranking.domain.model;

import com.backendtools.productranking.domain.ranking.CriterionName;

import java.util.EnumMap;
import java.util.Map;

public final class RankingWeightsTestMother {

    private RankingWeightsTestMother() {
    }

    public static RankingWeights weights(double salesUnitsWeight, double stockRatioWeight) {
        Map<CriterionName, CriterionWeight> weights = new EnumMap<>(CriterionName.class);
        weights.put(CriterionName.SALES_UNITS, new CriterionWeight(salesUnitsWeight));
        weights.put(CriterionName.STOCK_RATIO, new CriterionWeight(stockRatioWeight));
        return new RankingWeights(weights);
    }

    public static RankingWeights salesOnly() {
        return weights(1, 0);
    }

    public static RankingWeights stockOnly() {
        return weights(0, 1);
    }

    public static RankingWeights withoutStockRatioWeight(double salesUnitsWeight) {
        Map<CriterionName, CriterionWeight> weights = new EnumMap<>(CriterionName.class);
        weights.put(CriterionName.SALES_UNITS, new CriterionWeight(salesUnitsWeight));
        return new RankingWeights(weights);
    }
}

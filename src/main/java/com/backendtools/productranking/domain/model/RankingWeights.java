package com.backendtools.productranking.domain.model;

import com.backendtools.productranking.domain.ranking.CriterionName;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public final class RankingWeights {

    private static final CriterionWeight ZERO_WEIGHT = new CriterionWeight(0);

    private final Map<CriterionName, CriterionWeight> weights;

    public RankingWeights(Map<CriterionName, CriterionWeight> weights) {
        if (weights == null || weights.isEmpty()) {
            throw new IllegalArgumentException("Ranking weights cannot be empty");
        }
        this.weights = Collections.unmodifiableMap(new EnumMap<>(weights));
    }

    public CriterionWeight weightFor(CriterionName criterionName) {
        return weights.getOrDefault(criterionName, ZERO_WEIGHT);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof RankingWeights)) {
            return false;
        }
        return weights.equals(((RankingWeights) other).weights);
    }

    @Override
    public int hashCode() {
        return weights.hashCode();
    }

    @Override
    public String toString() {
        return weights.toString();
    }
}

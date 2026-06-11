package com.backendtools.productranking.application.usecase;

import com.backendtools.productranking.domain.model.RankingWeights;

import java.util.Objects;

public final class RankProductsCommand {

    private final RankingWeights rankingWeights;

    public RankProductsCommand(RankingWeights rankingWeights) {
        this.rankingWeights = Objects.requireNonNull(rankingWeights, "Ranking weights cannot be null");
    }

    public RankingWeights rankingWeights() {
        return rankingWeights;
    }
}

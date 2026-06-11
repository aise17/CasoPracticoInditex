package com.backendtools.productranking.infrastructure.rest;

public class ProductRankingRequest {

    private RankingWeightsRequest weights;

    public RankingWeightsRequest getWeights() {
        return weights;
    }

    public void setWeights(RankingWeightsRequest weights) {
        this.weights = weights;
    }
}

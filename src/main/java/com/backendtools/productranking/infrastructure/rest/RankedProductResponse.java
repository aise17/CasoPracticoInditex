package com.backendtools.productranking.infrastructure.rest;

import java.util.Map;

public class RankedProductResponse {

    private final String id;
    private final String name;
    private final int salesUnits;
    private final Map<String, Integer> stock;
    private final double score;

    public RankedProductResponse(
        String id,
        String name,
        int salesUnits,
        Map<String, Integer> stock,
        double score
    ) {
        this.id = id;
        this.name = name;
        this.salesUnits = salesUnits;
        this.stock = stock;
        this.score = score;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getSalesUnits() {
        return salesUnits;
    }

    public Map<String, Integer> getStock() {
        return stock;
    }

    public double getScore() {
        return score;
    }
}

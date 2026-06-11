package com.backendtools.productranking.infrastructure.rest;

import java.util.List;

public class ProductRankingResponse {

    private final List<RankedProductResponse> products;

    public ProductRankingResponse(List<RankedProductResponse> products) {
        this.products = products;
    }

    public List<RankedProductResponse> getProducts() {
        return products;
    }
}

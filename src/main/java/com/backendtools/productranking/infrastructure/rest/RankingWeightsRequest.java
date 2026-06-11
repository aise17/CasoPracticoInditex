package com.backendtools.productranking.infrastructure.rest;

import java.math.BigDecimal;

public class RankingWeightsRequest {

    private BigDecimal salesUnits;
    private BigDecimal stockRatio;

    public BigDecimal getSalesUnits() {
        return salesUnits;
    }

    public void setSalesUnits(BigDecimal salesUnits) {
        this.salesUnits = salesUnits;
    }

    public BigDecimal getStockRatio() {
        return stockRatio;
    }

    public void setStockRatio(BigDecimal stockRatio) {
        this.stockRatio = stockRatio;
    }
}

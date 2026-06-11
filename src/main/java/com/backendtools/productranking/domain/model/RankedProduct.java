package com.backendtools.productranking.domain.model;

import java.util.Comparator;
import java.util.Objects;

public final class RankedProduct {

    private final Product product;
    private final Score score;

    public RankedProduct(Product product, Score score) {
        this.product = Objects.requireNonNull(product, "Product cannot be null");
        this.score = Objects.requireNonNull(score, "Score cannot be null");
    }

    public Product product() {
        return product;
    }

    public Score score() {
        return score;
    }

    public static Comparator<RankedProduct> highestScoreFirst() {
        return Comparator.comparing(RankedProduct::score).reversed()
            .thenComparing(rankedProduct -> rankedProduct.product().id().value());
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof RankedProduct)) {
            return false;
        }
        RankedProduct that = (RankedProduct) other;
        return product.equals(that.product) && score.equals(that.score);
    }

    @Override
    public int hashCode() {
        return Objects.hash(product, score);
    }

    @Override
    public String toString() {
        return "RankedProduct{product=" + product + ", score=" + score + "}";
    }
}

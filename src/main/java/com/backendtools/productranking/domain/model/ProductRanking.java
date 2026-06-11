package com.backendtools.productranking.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class ProductRanking {

    private final List<RankedProduct> products;

    public ProductRanking(List<RankedProduct> products) {
        this.products = Collections.unmodifiableList(
            new ArrayList<>(Objects.requireNonNull(products, "Ranked products cannot be null"))
        );
    }

    public List<RankedProduct> products() {
        return products;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ProductRanking)) {
            return false;
        }
        return products.equals(((ProductRanking) other).products);
    }

    @Override
    public int hashCode() {
        return products.hashCode();
    }

    @Override
    public String toString() {
        return "ProductRanking{products=" + products + "}";
    }
}

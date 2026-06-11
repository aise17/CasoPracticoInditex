package com.backendtools.productranking.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Stock {

    private final List<SizeStock> sizes;

    public Stock(List<SizeStock> sizes) {
        if (sizes == null || sizes.isEmpty()) {
            throw new IllegalArgumentException("Stock must contain at least one size");
        }
        if (containsDuplicatedSizes(sizes)) {
            throw new IllegalArgumentException("Stock cannot contain duplicated sizes");
        }
        this.sizes = Collections.unmodifiableList(new ArrayList<>(sizes));
    }

    private static boolean containsDuplicatedSizes(List<SizeStock> sizes) {
        return sizes.stream()
            .map(SizeStock::size)
            .distinct()
            .count() != sizes.size();
    }

    public List<SizeStock> sizes() {
        return sizes;
    }

    public long availableSizesCount() {
        return sizes.stream()
            .filter(SizeStock::hasAvailableUnits)
            .count();
    }

    public int totalSizesCount() {
        return sizes.size();
    }

    public double availabilityRatio() {
        return (double) availableSizesCount() / totalSizesCount();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Stock)) {
            return false;
        }
        return sizes.equals(((Stock) other).sizes);
    }

    @Override
    public int hashCode() {
        return sizes.hashCode();
    }

    @Override
    public String toString() {
        return sizes.toString();
    }
}

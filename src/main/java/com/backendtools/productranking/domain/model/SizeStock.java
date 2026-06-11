package com.backendtools.productranking.domain.model;

import java.util.Objects;

public final class SizeStock {

    private final Size size;
    private final int units;

    public SizeStock(Size size, int units) {
        if (units < 0) {
            throw new IllegalArgumentException("Stock units cannot be negative");
        }
        this.size = Objects.requireNonNull(size, "Size cannot be null");
        this.units = units;
    }

    public Size size() {
        return size;
    }

    public int units() {
        return units;
    }

    public boolean hasAvailableUnits() {
        return units > 0;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SizeStock)) {
            return false;
        }
        SizeStock that = (SizeStock) other;
        return size == that.size && units == that.units;
    }

    @Override
    public int hashCode() {
        return Objects.hash(size, units);
    }

    @Override
    public String toString() {
        return size + ":" + units;
    }
}

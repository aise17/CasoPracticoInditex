package com.backendtools.productranking.domain.model;

public final class CriterionWeight {

    private final double value;

    public CriterionWeight(double value) {
        if (value < 0) {
            throw new IllegalArgumentException("Criterion weight cannot be negative");
        }
        this.value = value;
    }

    public double value() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CriterionWeight)) {
            return false;
        }
        return Double.compare(value, ((CriterionWeight) other).value) == 0;
    }

    @Override
    public int hashCode() {
        return Double.hashCode(value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}

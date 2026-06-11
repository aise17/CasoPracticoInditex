package com.backendtools.productranking.domain.model;

public final class SalesUnits {

    private final int value;

    public SalesUnits(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("Sales units cannot be negative");
        }
        this.value = value;
    }

    public int value() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SalesUnits)) {
            return false;
        }
        return value == ((SalesUnits) other).value;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}

package com.backendtools.productranking.domain.model;

public final class Score implements Comparable<Score> {

    private final double value;

    public Score(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException("Score must be a valid number");
        }
        this.value = value;
    }

    public double value() {
        return value;
    }

    public Score multiplyBy(CriterionWeight weight) {
        return new Score(value * weight.value());
    }

    public Score add(Score other) {
        return new Score(value + other.value);
    }

    @Override
    public int compareTo(Score other) {
        return Double.compare(value, other.value);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Score)) {
            return false;
        }
        return Double.compare(value, ((Score) other).value) == 0;
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

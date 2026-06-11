package com.backendtools.productranking.domain.model;

import java.util.Objects;

public final class Product {

    private final ProductId id;
    private final ProductName name;
    private final SalesUnits salesUnits;
    private final Stock stock;

    public Product(ProductId id, ProductName name, SalesUnits salesUnits, Stock stock) {
        this.id = Objects.requireNonNull(id, "Product id cannot be null");
        this.name = Objects.requireNonNull(name, "Product name cannot be null");
        this.salesUnits = Objects.requireNonNull(salesUnits, "Sales units cannot be null");
        this.stock = Objects.requireNonNull(stock, "Stock cannot be null");
    }

    public ProductId id() {
        return id;
    }

    public ProductName name() {
        return name;
    }

    public SalesUnits salesUnits() {
        return salesUnits;
    }

    public Stock stock() {
        return stock;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Product)) {
            return false;
        }
        return id.equals(((Product) other).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Product{id=" + id + ", name=" + name + "}";
    }
}

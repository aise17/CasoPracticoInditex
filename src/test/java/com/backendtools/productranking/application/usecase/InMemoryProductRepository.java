package com.backendtools.productranking.application.usecase;

import com.backendtools.productranking.application.port.out.ProductRepository;
import com.backendtools.productranking.domain.model.Product;

import java.util.List;

final class InMemoryProductRepository implements ProductRepository {

    private final List<Product> products;

    InMemoryProductRepository(List<Product> products) {
        this.products = products;
    }

    @Override
    public List<Product> findAll() {
        return products;
    }
}

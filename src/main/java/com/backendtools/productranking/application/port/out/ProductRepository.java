package com.backendtools.productranking.application.port.out;

import com.backendtools.productranking.domain.model.Product;

import java.util.List;

public interface ProductRepository {

    List<Product> findAll();
}

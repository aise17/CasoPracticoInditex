package com.backendtools.productranking.infrastructure.persistence.mongo;

import com.backendtools.productranking.application.port.out.ProductRepository;
import com.backendtools.productranking.domain.model.Product;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Repository
public class MongoProductRepository implements ProductRepository {

    private final SpringDataMongoProductRepository repository;
    private final ProductMongoMapper mapper;

    public MongoProductRepository(
        SpringDataMongoProductRepository repository,
        ProductMongoMapper mapper
    ) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<Product> findAll() {
        return repository.findAll()
            .stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }
}

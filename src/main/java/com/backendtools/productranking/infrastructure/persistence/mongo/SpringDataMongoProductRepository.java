package com.backendtools.productranking.infrastructure.persistence.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface SpringDataMongoProductRepository extends MongoRepository<ProductDocument, String> {
}

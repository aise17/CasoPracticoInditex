package com.backendtools.productranking.infrastructure.persistence.mongo;

import com.backendtools.productranking.domain.model.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@DataMongoTest
class MongoProductRepositoryIntegrationTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7");

    @DynamicPropertySource
    static void configureMongoProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    private SpringDataMongoProductRepository springDataRepository;

    private MongoProductRepository productRepository;

    @BeforeEach
    void setUp() {
        springDataRepository.deleteAll();
        productRepository = new MongoProductRepository(
            springDataRepository,
            new ProductMongoMapper()
        );
    }

    @Test
    void shouldFindAllProductsFromMongoAndMapThemToDomain() {
        springDataRepository.save(
            ProductDocumentTestMother.product("1", "V-NECH BASIC SHIRT", 100, 4, 9, 0)
        );

        List<Product> products = productRepository.findAll();

        assertThat(products)
            .hasSize(1)
            .first()
            .satisfies(product -> {
                assertThat(product.id().value()).isEqualTo("1");
                assertThat(product.name().value()).isEqualTo("V-NECH BASIC SHIRT");
                assertThat(product.salesUnits().value()).isEqualTo(100);
                assertThat(product.stock().availabilityRatio()).isEqualTo(2.0 / 3.0);
            });
    }

    @Test
    void shouldReturnEmptyListWhenNoProductsExist() {
        List<Product> products = productRepository.findAll();

        assertThat(products).isEmpty();
    }

    @Test
    void shouldFailWhenPersistedProductContainsInvalidDomainData() {
        springDataRepository.save(
            ProductDocumentTestMother.product("1", "V-NECH BASIC SHIRT", -10, 4, 9, 0)
        );

        assertThatThrownBy(() -> productRepository.findAll())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Sales units cannot be negative");
    }
}

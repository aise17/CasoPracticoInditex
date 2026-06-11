package com.backendtools.productranking.e2e;

import com.backendtools.productranking.infrastructure.persistence.mongo.ProductDocumentTestMother;
import com.backendtools.productranking.infrastructure.persistence.mongo.SpringDataMongoProductRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductRankingE2ETest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7");

    @DynamicPropertySource
    static void configureMongoProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @LocalServerPort
    private int port;

    @Autowired
    private SpringDataMongoProductRepository repository;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        repository.deleteAll();
        repository.saveAll(ProductDocumentTestMother.initialDataset());
    }

    @Test
    void shouldReturnRankedProductsUsingSalesUnitsAndStockRatioWeights() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {"weights":{"salesUnits":0.7,"stockRatio":0.3}}
                """)
        .when()
            .post("/api/products/ranking")
        .then()
            .statusCode(200)
            .body("products.id", contains("5", "1", "3", "2", "6", "4"))
            .body("products[0].score", equalTo(465.0f));
    }

    @Test
    void shouldReturnRankedProductsUsingOnlyStockRatioWeight() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {"weights":{"salesUnits":0,"stockRatio":1}}
                """)
        .when()
            .post("/api/products/ranking")
        .then()
            .statusCode(200)
            .body("products.id", contains("2", "3", "4", "6", "1", "5"));
    }

    @Test
    void shouldReturnBadRequestWhenWeightIsNegative() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {"weights":{"salesUnits":-1,"stockRatio":0.3}}
                """)
        .when()
            .post("/api/products/ranking")
        .then()
            .statusCode(400)
            .body("message", equalTo("Criterion weight cannot be negative"));
    }

    @Test
    void shouldReturnBadRequestWhenWeightsAreEmpty() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {"weights":{}}
                """)
        .when()
            .post("/api/products/ranking")
        .then()
            .statusCode(400)
            .body("message", equalTo("Ranking weights cannot be empty"));
    }

    @Test
    void shouldReturnBadRequestWhenJsonIsMalformed() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"weights\":{\"salesUnits\":}")
        .when()
            .post("/api/products/ranking")
        .then()
            .statusCode(400)
            .body("message", equalTo("Malformed JSON request"));
    }

    @Test
    void shouldReturnEmptyProductListWhenNoProductsExist() {
        repository.deleteAll();

        given()
            .contentType(ContentType.JSON)
            .body("""
                {"weights":{"salesUnits":0.7,"stockRatio":0.3}}
                """)
        .when()
            .post("/api/products/ranking")
        .then()
            .statusCode(200)
            .body("products", empty());
    }

    @Test
    void shouldIgnoreUnknownCriterionWeights() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {"weights":{"salesUnits":1,"margin":5}}
                """)
        .when()
            .post("/api/products/ranking")
        .then()
            .statusCode(200)
            .body("products.id", contains("5", "1", "3", "2", "6", "4"));
    }

    @Test
    void shouldReturnBadRequestWhenOnlyUnknownCriteriaAreProvided() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {"weights":{"margin":0.3}}
                """)
        .when()
            .post("/api/products/ranking")
        .then()
            .statusCode(400)
            .body("message", equalTo("Ranking weights cannot be empty"));
    }
}

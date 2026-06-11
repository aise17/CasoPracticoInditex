007 - Implementation Plan

Product Ranking Backend Tools

1. Purpose

This document defines the step-by-step implementation plan for the product ranking service.

The objective is to implement the exercise in a professional and controlled way, following:

* Hexagonal Architecture
* Tactical DDD
* Rich domain model
* Strategy pattern for ranking criteria
* MongoDB persistence
* REST API
* Unit, integration and E2E tests

This plan should be followed in order to avoid mixing layers or pushing business logic into controllers, repositories or DTOs.

⸻

2. Implementation Principles

The implementation must follow these principles:

Domain first.
Application second.
Infrastructure last.

Business rules should be implemented before REST and MongoDB.

This avoids designing the system around frameworks instead of around the domain.

⸻

3. Recommended Technology Stack

Use:

Java 17
Spring Boot 3.x
Maven
MongoDB
Rest Assured
Testcontainers
AssertJ
JUnit 5
ArchUnit

The exercise allows Java 8 or superior. The decision for this implementation is Java 17 with Spring Boot 3.x:

* Java 17 is a modern LTS version, fully compatible with the "Java 8 or superior" requirement.
* Spring Boot 3.x requires Java 17 and is the currently supported line.
* Records can be used for REST DTOs, reducing boilerplate without anemia concerns (DTOs are transport models, not domain models).
* Text blocks make JSON test bodies readable.

All specs assume this stack. Do not mix Java 8 restrictions into the implementation.

⸻

4. Project Creation

Create a Spring Boot project with the following dependencies:

Spring Web
Spring Data MongoDB
Spring Boot Starter Test
Rest Assured
Testcontainers MongoDB
Testcontainers JUnit Jupiter
ArchUnit

Note: the Bean Validation starter is intentionally not included. Request validation is handled by the REST mapper and the domain (see 004 section 9.1), so the dependency would be unused.

Recommended base package:

com.backendtools.productranking

⸻

5. Target Package Structure

Create the following structure:

src/main/java/com/backendtools/productranking
  ProductRankingApplication.java
  domain
    model
      Product.java
      ProductId.java
      ProductName.java
      SalesUnits.java
      Size.java
      SizeStock.java
      Stock.java
      Score.java
      CriterionWeight.java
      RankingWeights.java
      RankedProduct.java
      ProductRanking.java
    ranking
      CriterionName.java
      RankingCriterion.java
      SalesUnitsCriterion.java
      StockRatioCriterion.java
      ProductRankingService.java
  application
    port
      in
        RankProductsUseCase.java
      out
        ProductRepository.java
    usecase
      RankProductsCommand.java
      RankProductsService.java
  infrastructure
    rest
      ProductRankingController.java
      ProductRankingRequest.java
      RankingWeightsRequest.java
      ProductRankingResponse.java
      RankedProductResponse.java
      ProductRankingRestMapper.java
      RestExceptionHandler.java
      ErrorResponse.java
    persistence
      mongo
        ProductDocument.java
        SpringDataMongoProductRepository.java
        MongoProductRepository.java
        ProductMongoMapper.java
    configuration
      ProductRankingConfiguration.java
      MongoSeedConfiguration.java

⸻

Phase 1 - Domain Model

6. Implement Domain Value Objects

Start with the smallest domain pieces.

Implement:

ProductId
ProductName
SalesUnits
Size
SizeStock
Stock
Score
CriterionWeight
RankingWeights

Cross-cutting rule for all value objects:

* Immutable.
* equals and hashCode by value.
* toString implemented.

See 001 section 5.0. This is part of the definition of done for every value object below.

⸻

6.1 ProductId

Responsibilities:

* Wrap product id.
* Reject null or blank values.

Done when:

ProductId cannot be created with null or blank value.
ProductId exposes its value.

⸻

6.2 ProductName

Responsibilities:

* Wrap product name.
* Reject null or blank values.

Done when:

ProductName cannot be created with null or blank value.
ProductName exposes its value.

⸻

6.3 SalesUnits

Responsibilities:

* Wrap sold units.
* Reject negative values.
* Accept zero.

Done when:

SalesUnits accepts zero and positive values.
SalesUnits rejects negative values.

⸻

6.4 Size

Implement enum:

public enum Size {
    S,
    M,
    L
}

Done when:

The system supports S, M and L sizes.

⸻

6.5 SizeStock

Responsibilities:

* Represent stock for a specific size.
* Reject negative units.
* Know whether the size has available units.

Done when:

SizeStock.hasAvailableUnits() returns true when units > 0.
SizeStock.hasAvailableUnits() returns false when units == 0.

⸻

6.6 Stock

Responsibilities:

* Represent all size stock entries.
* Reject empty stock.
* Reject duplicated sizes.
* Calculate available size count.
* Calculate total size count.
* Calculate availability ratio.

Done when:

Stock can calculate 2 / 3 when two out of three sizes have stock.
Stock can calculate 1 when all sizes have stock.
Stock can calculate 0 when no size has stock.
Stock rejects a list containing the same size twice.

⸻

6.7 Score

Responsibilities:

* Wrap score value.
* Reject NaN and infinite values.
* Add another score.
* Multiply by weight.
* Compare scores.

Done when:

Score can be added.
Score can be multiplied by CriterionWeight.
Score is comparable.
Invalid score values are rejected.

⸻

6.8 CriterionWeight

Responsibilities:

* Wrap criterion weight.
* Reject negative values.
* Accept zero.
* Accept decimal values.

Done when:

CriterionWeight accepts 0, 0.3, 1, 2.
CriterionWeight rejects -1.

⸻

6.9 RankingWeights

Responsibilities:

* Store weights by criterion.
* Reject empty weight map.
* Return zero weight when criterion is missing.

Done when:

RankingWeights.weightFor(SALES_UNITS) returns configured value.
RankingWeights.weightFor(STOCK_RATIO) returns 0 when missing.

⸻

7. Implement Product Aggregate

Implement:

Product

Responsibilities:

* Hold product identity.
* Hold product name.
* Hold sales units.
* Hold stock.

Done when:

Product is immutable.
Product uses value objects.
Product does not contain Spring or MongoDB annotations.

⸻

8. Implement Ranking Criteria

Implement:

CriterionName
RankingCriterion
SalesUnitsCriterion
StockRatioCriterion

⸻

8.1 CriterionName

Initial values:

SALES_UNITS
STOCK_RATIO

⸻

8.2 RankingCriterion

Interface:

public interface RankingCriterion {
    CriterionName name();
    Score calculateScore(Product product);
}

⸻

8.3 SalesUnitsCriterion

Formula:

salesUnitsScore = product.salesUnits

Done when:

A product with 100 sales units receives score 100.

⸻

8.4 StockRatioCriterion

Formula:

stockRatioScore = product.stock.availabilityRatio * 100

Done when:

Stock S:4, M:9, L:0 receives score 66.67.
Stock S:1, M:1, L:1 receives score 100.
Stock S:0, M:0, L:0 receives score 0.

⸻

9. Implement Ranking Result Models

Implement:

RankedProduct
ProductRanking

⸻

9.1 RankedProduct

Responsibilities:

* Hold product.
* Hold final score.
* Provide comparator by highest score first.
* Apply tie-breaker by product id if implemented.

Done when:

Ranked products can be sorted by score descending.

Recommended comparator:

public static Comparator<RankedProduct> highestScoreFirst() {
    return Comparator
        .comparing(RankedProduct::score)
        .reversed()
        .thenComparing(rankedProduct -> rankedProduct.product().id().value());
}

⸻

9.2 ProductRanking

Responsibilities:

* Hold ordered ranked products.
* Expose immutable list.

Done when:

ProductRanking cannot be modified externally.

⸻

10. Implement ProductRankingService

Implement:

ProductRankingService

Responsibilities:

* Receive products and ranking weights.
* Apply all configured ranking criteria.
* Calculate final weighted score.
* Sort products by score descending.
* Return ProductRanking.

Done when:

Given the initial dataset and weights 0.7 and 0.3, the order is 5, 1, 3, 2, 6, 4.

⸻

Phase 2 - Domain Unit Tests

11. Implement Domain Tests

Implement tests before moving to infrastructure.

Required tests:

ProductIdTest
ProductNameTest
SalesUnitsTest
SizeStockTest
StockTest
ScoreTest
CriterionWeightTest
RankingWeightsTest
SalesUnitsCriterionTest
StockRatioCriterionTest
ProductRankingServiceTest

⸻

12. Implement Test Mothers

Create:

ProductTestMother
RankingWeightsTestMother
ProductRankingServiceTestMother

Purpose:

* Avoid duplicate test setup.
* Keep test data readable.
* Avoid mystery guests.

⸻

13. Domain Acceptance Check

Before continuing, run:

./mvnw test

At this point:

Domain tests should pass.
No Spring context should be needed.
No MongoDB should be needed.

⸻

Phase 3 - Application Layer

14. Implement Input Port

Create:

application.port.in.RankProductsUseCase

Interface:

public interface RankProductsUseCase {
    ProductRanking rankProducts(RankProductsCommand command);
}

Done when:

The application exposes a use case for ranking products.

⸻

15. Implement Output Port

Create:

application.port.out.ProductRepository

Interface:

public interface ProductRepository {
    List<Product> findAll();
}

Done when:

The application depends on ProductRepository, not on MongoDB.

⸻

16. Implement Command

Create:

application.usecase.RankProductsCommand

Responsibilities:

* Hold RankingWeights.
* Reject null ranking weights.

Done when:

The use case input is represented without REST objects.

⸻

17. Implement Use Case Service

Create:

application.usecase.RankProductsService

Responsibilities:

* Load products from ProductRepository.
* Delegate ranking to ProductRankingService.
* Return ProductRanking.

Done when:

RankProductsService orchestrates the use case without knowing MongoDB or REST.

⸻

18. Implement Application Tests

Create:

RankProductsServiceTest

Use:

InMemoryProductRepository

Test:

shouldLoadProductsAndReturnRanking

Done when:

Application layer is tested without Spring, HTTP or MongoDB.

⸻

Phase 4 - Spring Configuration

19. Implement ProductRankingConfiguration

Create:

infrastructure.configuration.ProductRankingConfiguration

Responsibilities:

* Register ProductRankingService.
* Register RankProductsUseCase.

Example:

@Configuration
public class ProductRankingConfiguration {
    @Bean
    public ProductRankingService productRankingService() {
        return new ProductRankingService(Arrays.asList(
            new SalesUnitsCriterion(),
            new StockRatioCriterion()
        ));
    }
    @Bean
    public RankProductsUseCase rankProductsUseCase(
        ProductRepository productRepository,
        ProductRankingService productRankingService
    ) {
        return new RankProductsService(
            productRepository,
            productRankingService
        );
    }
}

Done when:

Domain and application services are assembled by Spring without being annotated themselves.

⸻

Phase 5 - MongoDB Infrastructure

20. Implement ProductDocument

Create:

infrastructure.persistence.mongo.ProductDocument

Fields:

id
name
salesUnits
stock

Mongo collection:

products

Done when:

ProductDocument represents MongoDB storage format.

⸻

21. Implement SpringDataMongoProductRepository

Create:

SpringDataMongoProductRepository
public interface SpringDataMongoProductRepository
    extends MongoRepository<ProductDocument, String> {
}

Done when:

Spring Data can load and store ProductDocument.

⸻

22. Implement ProductMongoMapper

Create:

ProductMongoMapper

Responsibilities:

* Convert ProductDocument to Product.
* Convert Product to ProductDocument.

Done when:

Mongo documents are converted into rich domain objects.

⸻

23. Implement MongoProductRepository

Create:

MongoProductRepository

Responsibilities:

* Implement ProductRepository.
* Use SpringDataMongoProductRepository.
* Use ProductMongoMapper.

Done when:

Application can load domain products through ProductRepository.
MongoDB remains hidden inside infrastructure.

⸻

24. Implement Mongo Seed

Create:

MongoSeedConfiguration

Responsibilities:

* Insert initial dataset when collection is empty.

Done when:

Running the application with empty MongoDB inserts the six exercise products.

⸻

25. Add Docker Compose

Create:

docker-compose.yml

Content (no version attribute, it is obsolete in Docker Compose v2):

services:
  mongodb:
    image: mongo:7
    container_name: product-ranking-mongodb
    ports:
      - "27017:27017"
    environment:
      MONGO_INITDB_DATABASE: product_ranking

Done when:

docker-compose up -d

starts MongoDB locally.

⸻

26. Add application.yml

Create:

src/main/resources/application.yml

Content:

spring:
  data:
    mongodb:
      uri: mongodb://localhost:27017/product_ranking
server:
  port: 8080

Done when:

Spring Boot can connect to local MongoDB.

⸻

27. Implement Mongo Integration Tests

Create:

MongoProductRepositoryIntegrationTest

Use:

Testcontainers
@DataMongoTest
MongoDBContainer

Test cases:

shouldFindAllProductsFromMongoAndMapThemToDomain
shouldReturnEmptyListWhenNoProductsExist
shouldFailWhenPersistedProductContainsInvalidDomainData

Done when:

Mongo adapter works against a real MongoDB container.

⸻

Phase 6 - REST Infrastructure

28. Implement REST Request Models

Create:

ProductRankingRequest
RankingWeightsRequest

Responsibilities:

* Represent incoming JSON.
* Keep REST-specific structure in infrastructure.

Done when:

Request body can receive salesUnits and stockRatio weights.

⸻

29. Implement REST Response Models

Create:

ProductRankingResponse
RankedProductResponse
ErrorResponse

Responsibilities:

* Represent outgoing JSON.
* Avoid exposing domain internals directly.

Done when:

The API response contains products ordered by score.

⸻

30. Implement ProductRankingRestMapper

Responsibilities:

* Convert ProductRankingRequest to RankProductsCommand.
* Convert ProductRanking to ProductRankingResponse.

Done when:

REST DTOs are not passed into the application layer.
Domain models are not directly exposed through REST.

⸻

31. Implement ProductRankingController

Create:

ProductRankingController

Endpoint:

POST /api/products/ranking

Responsibilities:

* Receive request.
* Map request to command.
* Call use case.
* Map result to response.

Done when:

Controller contains no ranking logic.
Controller contains no MongoDB access.

⸻

32. Implement RestExceptionHandler

Create:

RestExceptionHandler

Handle:

IllegalArgumentException
HttpMessageNotReadableException

Note: there is no MethodArgumentNotValidException handler because Bean Validation is not used (see 004 section 9.1).

Done when:

Invalid requests return 400 Bad Request with readable error messages.
Every handler is exercised by at least one test.

⸻

Phase 7 - REST and E2E Tests

33. Optional Controller Tests

Create:

ProductRankingControllerTest

Use:

@WebMvcTest
MockMvc

Test:

shouldReturnOkWhenRequestIsValid
shouldReturnBadRequestWhenWeightsAreMissing
shouldReturnBadRequestWhenJsonIsMalformed

Done when:

REST mapping is tested in isolation.

⸻

34. Implement E2E Tests

Create:

ProductRankingE2ETest

Use:

@SpringBootTest(webEnvironment = RANDOM_PORT)
RestAssured
Testcontainers MongoDB

Test cases:

shouldReturnRankedProductsUsingSalesUnitsAndStockRatioWeights
shouldReturnRankedProductsUsingOnlyStockRatioWeight
shouldReturnBadRequestWhenWeightIsNegative
shouldReturnBadRequestWhenWeightsAreEmpty
shouldReturnBadRequestWhenJsonIsMalformed
shouldReturnEmptyProductListWhenNoProductsExist
shouldIgnoreUnknownCriterionWeights

Done when:

The full HTTP flow works from request to MongoDB and back.

⸻

35. Main E2E Acceptance Test

Request:

{
  "weights": {
    "salesUnits": 0.7,
    "stockRatio": 0.3
  }
}

Expected order:

5, 1, 3, 2, 6, 4

This test is mandatory.

⸻

Phase 8 - Architecture Tests

36. Add ArchUnit

Add dependency:

<dependency>
    <groupId>com.tngtech.archunit</groupId>
    <artifactId>archunit-junit5</artifactId>
    <scope>test</scope>
</dependency>

⸻

37. Implement HexagonalArchitectureTest

Create:

HexagonalArchitectureTest

Rules:

domainShouldNotDependOnSpring
domainShouldNotDependOnMongo
applicationShouldNotDependOnInfrastructure
controllersShouldBeLocatedInInfrastructureRest
mongoDocumentsShouldBeLocatedInInfrastructurePersistenceMongo

Done when:

Architecture boundaries are automatically verified.

⸻

Phase 9 - Documentation

38. Create README.md

The README must include:

Project description
Architecture explanation
How to run
How to test
Endpoint documentation
Example requests
Design decisions
Testing strategy

⸻

39. Add Design Decisions Section

Include:

Why Hexagonal Architecture
Why rich domain model
Why Strategy pattern, and why not inheritance or a decorator (see 003 section 21.5)
Why ProductDocument is separated from Product
Why stock ratio is scaled to 0..100
Why missing weights are treated as zero
Why double instead of BigDecimal for Score (see 003 section 21.4)
Why Bean Validation is not used (see 004 section 9.1)
Why the tie-breaker uses product id ascending
Why Testcontainers are used

⸻

40. Add How To Run

Example:

docker-compose up -d
./mvnw spring-boot:run

⸻

41. Add How To Test

Example:

./mvnw test

⸻

42. Add API Example

Example:

curl -X POST http://localhost:8080/api/products/ranking \
  -H "Content-Type: application/json" \
  -d '{
    "weights": {
      "salesUnits": 0.7,
      "stockRatio": 0.3
    }
  }'

⸻

Phase 10 - Code Quality Review

43. Review Naming

Ensure all code uses English names.

Avoid:

Producto
Ordenacion
Puntuacion
Ventas
StockPorTalla

Use:

Product
Ranking
Score
SalesUnits
SizeStock

⸻

44. Remove Unused Code

Before submitting:

Remove unused imports.
Remove unused methods.
Remove unused classes.
Remove commented-out code.
Remove debug logs.

⸻

45. Format Code

Run IDE formatter or Maven formatter if configured.

Check:

Indentation
Line breaks
Consistent package names
Readable method names
No excessive comments

⸻

46. Avoid Comments As Smell

Avoid comments that explain bad code.

Bad:

// Calculate stock ratio
double ratio = ...

Good:

double availabilityRatio = stock.availabilityRatio();

Use expressive names instead of comments.

⸻

47. Check Domain Independence

Verify:

No Spring annotation in domain.
No Mongo annotation in domain.
No REST class imported in application.
No Mongo document returned by application port.

⸻

48. Run Full Test Suite

Run:

./mvnw test

Done when:

All tests pass.
No architecture rule fails.

⸻

Phase 11 - Interview Preparation

49. Prepare Architecture Explanation

Be ready to explain:

I started from the domain model and kept it independent from frameworks.
The ranking criteria are implemented using the Strategy pattern.
The application layer exposes a use case and depends on a ProductRepository port.
MongoDB is implemented as an output adapter.
REST is implemented as an input adapter.
The controller does not calculate scores or access MongoDB.

⸻

50. Prepare Algorithm Explanation

Be ready to explain:

The final score is the sum of each criterion score multiplied by its configured weight.
Sales units score uses the number of sold units directly.
Stock ratio score measures the proportion of sizes with available stock and scales it to 0..100.
The expected order for weights 0.7 and 0.3 is 5, 1, 3, 2, 6, 4.

⸻

51. Prepare Testing Explanation

Be ready to explain:

Domain tests verify business rules without Spring.
Application tests verify orchestration with a fake repository.
MongoDB integration tests verify the persistence adapter using Testcontainers.
E2E tests verify the full HTTP flow using Rest Assured.
Architecture tests verify layer boundaries.

⸻

52. Implementation Checklist

Domain

[ ] ProductId
[ ] ProductName
[ ] SalesUnits
[ ] Size
[ ] SizeStock
[ ] Stock
[ ] Score
[ ] CriterionWeight
[ ] RankingWeights
[ ] Product
[ ] CriterionName
[ ] RankingCriterion
[ ] SalesUnitsCriterion
[ ] StockRatioCriterion
[ ] RankedProduct
[ ] ProductRanking
[ ] ProductRankingService

⸻

Application

[ ] RankProductsUseCase
[ ] ProductRepository
[ ] RankProductsCommand
[ ] RankProductsService

⸻

Infrastructure - MongoDB

[ ] ProductDocument
[ ] SpringDataMongoProductRepository
[ ] ProductMongoMapper
[ ] MongoProductRepository
[ ] MongoSeedConfiguration

⸻

Infrastructure - REST

[ ] ProductRankingRequest
[ ] RankingWeightsRequest
[ ] ProductRankingResponse
[ ] RankedProductResponse
[ ] ErrorResponse
[ ] ProductRankingRestMapper
[ ] ProductRankingController
[ ] RestExceptionHandler

⸻

Configuration

[ ] ProductRankingConfiguration
[ ] application.yml
[ ] docker-compose.yml

⸻

Tests

[ ] ProductIdTest
[ ] ProductNameTest
[ ] SalesUnitsTest
[ ] SizeStockTest
[ ] StockTest
[ ] ScoreTest
[ ] CriterionWeightTest
[ ] RankingWeightsTest
[ ] SalesUnitsCriterionTest
[ ] StockRatioCriterionTest
[ ] ProductRankingServiceTest
[ ] RankProductsServiceTest
[ ] MongoProductRepositoryIntegrationTest
[ ] ProductRankingE2ETest
[ ] HexagonalArchitectureTest

⸻

Documentation

[ ] README.md
[ ] docs/specs/001-domain-model.md
[ ] docs/specs/002-hexagonal-architecture.md
[ ] docs/specs/003-ranking-algorithm.md
[ ] docs/specs/004-rest-api.md
[ ] docs/specs/005-persistence-mongodb.md
[ ] docs/specs/006-testing-strategy.md
[ ] docs/specs/007-implementation-plan.md

⸻

53. Suggested Implementation Order

The recommended order is:

1. Domain value objects
2. Product aggregate
3. Ranking criteria
4. ProductRankingService
5. Domain unit tests
6. Application ports and use case
7. Application tests
8. Spring configuration
9. MongoDB adapter
10. MongoDB integration tests
11. REST adapter
12. E2E tests
13. Architecture tests
14. README
15. Final cleanup

⸻

54. Final Definition of Done

The exercise is complete when:

The application runs with Spring Boot.
MongoDB persistence is working.
The initial dataset is loaded.
POST /api/products/ranking returns ranked products.
The ranking order matches the expected result.
The domain is independent from Spring and MongoDB.
The application depends on ports, not adapters.
The controller contains no business logic.
The repository contains no ranking logic.
Unit tests pass.
Integration tests pass.
E2E tests pass.
Architecture tests pass.
README explains the design decisions.

Main expected result:

For weights salesUnits=0.7 and stockRatio=0.3:
5, 1, 3, 2, 6, 4
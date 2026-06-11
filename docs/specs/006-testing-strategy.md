006 - Testing Strategy Specification

Product Ranking Backend Tools

1. Purpose

This document defines the testing strategy for the product ranking service.

The goal is to provide confidence in the correctness of the solution while keeping tests readable, maintainable and aligned with Hexagonal Architecture.

The project should include three main types of tests:

Unit tests
Integration tests
E2E tests

Each test type has a different responsibility and should not be confused with the others.

⸻

2. Testing Goals

The test suite must verify:

* Domain rules.
* Ranking algorithm correctness.
* Use case orchestration.
* MongoDB adapter behavior.
* REST API behavior.
* Full application flow.
* Error handling.
* Expected ranking order for the exercise dataset.

The tests must also demonstrate good engineering practices for a Tech Leader position.

⸻

3. Test Pyramid

The recommended structure is:

Many domain unit tests
Some application tests
Some infrastructure integration tests
Few E2E tests

Visual representation:

        E2E tests
      Integration tests
    Application tests
  Domain unit tests

The lower the test is in the pyramid, the faster and more isolated it should be.

⸻

4. Test Types

4.1 Unit Tests

Unit tests verify one unit of behavior in isolation.

They should be:

* Fast.
* Deterministic.
* Independent from Spring.
* Independent from MongoDB.
* Independent from HTTP.
* Focused on business behavior.

Examples

StockTest
SalesUnitsCriterionTest
StockRatioCriterionTest
ProductRankingServiceTest

⸻

4.2 Application Tests

Application tests verify use case orchestration.

They should check that:

* The use case loads products from the output port.
* The use case delegates ranking to the domain service.
* The use case returns the ranking result.

They should not use real MongoDB or HTTP.

Examples

RankProductsServiceTest

⸻

4.3 Integration Tests

Integration tests verify the collaboration between the application code and an external technology.

In this project, the main integration tests are for MongoDB.

They should use Testcontainers.

Examples

MongoProductRepositoryIntegrationTest

These tests must not call the REST endpoint.

⸻

4.4 E2E Tests

E2E tests verify the full flow from the outside of the application.

They should:

* Start the Spring Boot application.
* Use Rest Assured.
* Use MongoDB through Testcontainers.
* Call the REST endpoint.
* Verify the HTTP response.

Examples

ProductRankingE2ETest

⸻

5. Difference Between Integration And E2E Tests

This distinction is important for the interview.

Integration test

MongoProductRepositoryIntegrationTest

Tests:

MongoProductRepository
ProductMongoMapper
Spring Data MongoDB
MongoDB container

Does not test:

HTTP
Controller
Rest mapper
Full use case flow

⸻

E2E test

ProductRankingE2ETest

Tests:

HTTP request
Controller
REST mapper
Application use case
Domain ranking service
MongoDB adapter
MongoDB container
HTTP response

This is the full user-visible behavior.

⸻

6. Recommended Test Package Structure

src/test/java/com/backendtools/productranking
  domain
    model
      ProductIdTest.java
      ProductNameTest.java
      SalesUnitsTest.java
      SizeStockTest.java
      StockTest.java
      ScoreTest.java
      CriterionWeightTest.java
      RankingWeightsTest.java
    ranking
      SalesUnitsCriterionTest.java
      StockRatioCriterionTest.java
      ProductRankingServiceTest.java
  application
    usecase
      RankProductsServiceTest.java
  infrastructure
    persistence
      mongo
        MongoProductRepositoryIntegrationTest.java
    rest
      ProductRankingControllerTest.java
  e2e
    ProductRankingE2ETest.java
  architecture
    HexagonalArchitectureTest.java

⸻

7. Test Naming Convention

Test names should describe behavior.

Use:

shouldDoSomethingWhenCondition

Examples:

shouldCalculatePartialStockAvailabilityRatio
shouldReturnProductsOrderedByHighestWeightedScore
shouldRejectNegativeCriterionWeight
shouldReturnBadRequestWhenWeightIsNegative

Avoid:

test1
rankingTest
shouldWork
ok
badRequest

⸻

8. Domain Unit Tests

8.1 ProductIdTest

Test cases

shouldCreateProductIdWhenValueIsValid
shouldRejectNullProductId
shouldRejectBlankProductId
shouldBeEqualWhenValuesAreEqual

Example

@Test
void shouldRejectBlankProductId() {
    assertThatThrownBy(() -> new ProductId(" "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Product id cannot be empty");
}

@Test
void shouldBeEqualWhenValuesAreEqual() {
    assertThat(new ProductId("1")).isEqualTo(new ProductId("1"));
}

Note: value equality must be tested for every value object, since equals and hashCode are part of the value object contract (see 001 section 5.0).

⸻

8.2 ProductNameTest

Test cases

shouldCreateProductNameWhenValueIsValid
shouldRejectNullProductName
shouldRejectBlankProductName

⸻

8.3 SalesUnitsTest

Test cases

shouldCreateSalesUnitsWhenValueIsZero
shouldCreateSalesUnitsWhenValueIsPositive
shouldRejectNegativeSalesUnits

Example

@Test
void shouldRejectNegativeSalesUnits() {
    assertThatThrownBy(() -> new SalesUnits(-1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Sales units cannot be negative");
}

⸻

8.4 SizeStockTest

Test cases

shouldDetectAvailableStockWhenUnitsAreGreaterThanZero
shouldDetectUnavailableStockWhenUnitsAreZero
shouldRejectNegativeStockUnits

Example

@Test
void shouldDetectAvailableStockWhenUnitsAreGreaterThanZero() {
    SizeStock sizeStock = new SizeStock(Size.S, 4);
    assertThat(sizeStock.hasAvailableUnits()).isTrue();
}

⸻

8.5 StockTest

Test cases

shouldCalculateFullAvailabilityRatio
shouldCalculatePartialAvailabilityRatio
shouldCalculateZeroAvailabilityRatio
shouldRejectEmptyStock
shouldRejectDuplicatedSizes

Example

@Test
void shouldCalculatePartialAvailabilityRatio() {
    Stock stock = new Stock(Arrays.asList(
        new SizeStock(Size.S, 4),
        new SizeStock(Size.M, 9),
        new SizeStock(Size.L, 0)
    ));
    assertThat(stock.availableSizesCount()).isEqualTo(2);
    assertThat(stock.totalSizesCount()).isEqualTo(3);
    assertThat(stock.availabilityRatio()).isEqualTo(2.0 / 3.0);
}

⸻

8.6 ScoreTest

Test cases

shouldAddTwoScores
shouldMultiplyScoreByCriterionWeight
shouldRejectNanScore
shouldRejectInfiniteScore

Example

@Test
void shouldMultiplyScoreByCriterionWeight() {
    Score score = new Score(100);
    CriterionWeight weight = new CriterionWeight(0.7);
    Score weightedScore = score.multiplyBy(weight);
    assertThat(weightedScore.value()).isEqualTo(70);
}

⸻

8.7 CriterionWeightTest

Test cases

shouldCreateCriterionWeightWhenValueIsZero
shouldCreateCriterionWeightWhenValueIsPositive
shouldRejectNegativeCriterionWeight

⸻

8.8 RankingWeightsTest

Test cases

shouldReturnConfiguredCriterionWeight
shouldReturnZeroWhenCriterionWeightIsMissing
shouldRejectEmptyWeights

Example

@Test
void shouldReturnZeroWhenCriterionWeightIsMissing() {
    Map<CriterionName, CriterionWeight> weights = new EnumMap<>(CriterionName.class);
    weights.put(CriterionName.SALES_UNITS, new CriterionWeight(1));
    RankingWeights rankingWeights = new RankingWeights(weights);
    assertThat(rankingWeights.weightFor(CriterionName.STOCK_RATIO).value())
        .isZero();
}

⸻

9. Ranking Criteria Tests

9.1 SalesUnitsCriterionTest

Test cases

shouldCalculateScoreUsingProductSalesUnits
shouldReturnSalesUnitsCriterionName

Example

@Test
void shouldCalculateScoreUsingProductSalesUnits() {
    SalesUnitsCriterion criterion = new SalesUnitsCriterion();
    Product product = ProductTestMother.productWithSalesUnits(100);
    Score score = criterion.calculateScore(product);
    assertThat(score.value()).isEqualTo(100);
}

⸻

9.2 StockRatioCriterionTest

Test cases

shouldCalculateFullStockRatioScore
shouldCalculatePartialStockRatioScore
shouldCalculateZeroStockRatioScore
shouldReturnStockRatioCriterionName

Example

@Test
void shouldCalculatePartialStockRatioScore() {
    StockRatioCriterion criterion = new StockRatioCriterion();
    Product product = ProductTestMother.productWithStock(4, 9, 0);
    Score score = criterion.calculateScore(product);
    assertThat(score.value()).isEqualTo(66.66666666666666);
}

Preferred with precision tolerance:

assertThat(score.value()).isCloseTo(66.67, within(0.01));

⸻

10. ProductRankingServiceTest

This is one of the most important domain tests.

Test cases

shouldReturnProductsOrderedByHighestWeightedScore
shouldRankProductsOnlyBySalesUnitsWhenStockRatioWeightIsZero
shouldRankProductsOnlyByStockRatioWhenSalesUnitsWeightIsZero
shouldUseZeroWhenCriterionWeightIsMissing
shouldApplyDeterministicTieBreakerUsingProductId
shouldOrderByTieBreakerWhenAllWeightsAreZero
shouldRejectEmptyCriteria

⸻

10.1 Main Ranking Test

@Test
void shouldReturnProductsOrderedByHighestWeightedScore() {
    ProductRankingService rankingService = new ProductRankingService(Arrays.asList(
        new SalesUnitsCriterion(),
        new StockRatioCriterion()
    ));
    RankingWeights weights = RankingWeightsTestMother.weights(0.7, 0.3);
    ProductRanking ranking = rankingService.rank(
        ProductTestMother.initialDataset(),
        weights
    );
    assertThat(ranking.products())
        .extracting(rankedProduct -> rankedProduct.product().id().value())
        .containsExactly("5", "1", "3", "2", "6", "4");
}

This test proves the core business requirement.

⸻

10.2 Stock Only Ranking Test

@Test
void shouldRankProductsOnlyByStockRatioWhenSalesUnitsWeightIsZero() {
    ProductRankingService rankingService = ProductRankingServiceTestMother.defaultService();
    RankingWeights weights = RankingWeightsTestMother.weights(0, 1);
    ProductRanking ranking = rankingService.rank(
        ProductTestMother.initialDataset(),
        weights
    );
    assertThat(ranking.products())
        .extracting(rankedProduct -> rankedProduct.product().id().value())
        .containsExactly("2", "3", "4", "6", "1", "5");
}

⸻

10.3 Tie-Breaker Test Warning

The input list for shouldApplyDeterministicTieBreakerUsingProductId must not be pre-ordered by product id.

Java sorting is stable: if the input is already ordered by id, the test passes even when the comparator has no tie-breaker. Provide the tied products in reverse id order, for example product 4 before product 2, and assert that the output is 2 before 4.

@Test
void shouldApplyDeterministicTieBreakerUsingProductId() {
    ProductRankingService rankingService = ProductRankingServiceTestMother.defaultService();
    List<Product> tiedProductsInReverseIdOrder = Arrays.asList(
        ProductTestMother.product("4", "PLEATED T-SHIRT", 50, 1, 1, 1),
        ProductTestMother.product("2", "CONTRASTING FABRIC T-SHIRT", 50, 1, 1, 1)
    );
    ProductRanking ranking = rankingService.rank(
        tiedProductsInReverseIdOrder,
        RankingWeightsTestMother.weights(1, 1)
    );
    assertThat(ranking.products())
        .extracting(rankedProduct -> rankedProduct.product().id().value())
        .containsExactly("2", "4");
}

⸻

11. Application Test

11.1 RankProductsServiceTest

This test verifies orchestration.

It should not use Spring or MongoDB.

Use a fake repository.

@Test
void shouldLoadProductsAndDelegateRankingToDomainService() {
    ProductRepository productRepository =
        new InMemoryProductRepository(ProductTestMother.initialDataset());
    ProductRankingService rankingService = ProductRankingServiceTestMother.defaultService();
    RankProductsService service = new RankProductsService(
        productRepository,
        rankingService
    );
    RankProductsCommand command = new RankProductsCommand(
        RankingWeightsTestMother.weights(0.7, 0.3)
    );
    ProductRanking ranking = service.rankProducts(command);
    assertThat(ranking.products())
        .extracting(rankedProduct -> rankedProduct.product().id().value())
        .containsExactly("5", "1", "3", "2", "6", "4");
}

⸻

11.2 InMemoryProductRepository

For application tests:

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

This is preferable to using MongoDB in application tests.

⸻

12. MongoDB Integration Tests

12.1 MongoProductRepositoryIntegrationTest

This test verifies that the MongoDB adapter works.

Use:

@DataMongoTest
@Testcontainers
MongoDBContainer

Test cases

shouldFindAllProductsFromMongoAndMapThemToDomain
shouldReturnEmptyListWhenNoProductsExist
shouldFailWhenPersistedProductContainsInvalidDomainData

⸻

12.2 Example

@Testcontainers
@DataMongoTest
class MongoProductRepositoryIntegrationTest {
    @Container
    static MongoDBContainer mongoDBContainer =
        new MongoDBContainer("mongo:7");
    @DynamicPropertySource
    static void configureMongoProperties(
        DynamicPropertyRegistry registry
    ) {
        registry.add(
            "spring.data.mongodb.uri",
            mongoDBContainer::getReplicaSetUrl
        );
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
            ProductDocumentTestMother.product(
                "1",
                "V-NECH BASIC SHIRT",
                100,
                4,
                9,
                0
            )
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
}

⸻

13. REST Controller Tests

Controller tests are optional if E2E tests are strong, but they can be useful for validating request mapping and error handling.

Use:

@WebMvcTest
MockMvc
@MockitoBean RankProductsUseCase

Note: @MockBean is deprecated since Spring Boot 3.4 in favor of @MockitoBean. Use @MockBean only with earlier Boot versions.

Test cases

shouldReturnOkWhenRequestIsValid
shouldReturnBadRequestWhenWeightsAreMissing
shouldReturnBadRequestWhenJsonIsMalformed

Important:

Controller tests should not verify the ranking algorithm.

That belongs to domain tests.

⸻

14. E2E Tests With Rest Assured

E2E tests verify the full behavior from HTTP to MongoDB and back.

Use:

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
RestAssured
MongoDBContainer

⸻

14.1 ProductRankingE2ETest

Main test cases

shouldReturnRankedProductsUsingSalesUnitsAndStockRatioWeights
shouldReturnRankedProductsUsingOnlyStockRatioWeight
shouldReturnBadRequestWhenWeightIsNegative
shouldReturnBadRequestWhenWeightsAreEmpty
shouldReturnBadRequestWhenJsonIsMalformed
shouldReturnEmptyProductListWhenNoProductsExist
shouldIgnoreUnknownCriterionWeights
shouldReturnBadRequestWhenOnlyUnknownCriteriaAreProvided

The last three cases cover AC6, AC7 and the only-unknown-criteria edge case from the REST API specification (004 sections 17 and 5.6).

⸻

14.2 Example

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductRankingE2ETest {
    @Container
    static MongoDBContainer mongoDBContainer =
        new MongoDBContainer("mongo:7");
    @LocalServerPort
    private int port;
    @Autowired
    private SpringDataMongoProductRepository repository;
    @DynamicPropertySource
    static void configureMongoProperties(
        DynamicPropertyRegistry registry
    ) {
        registry.add(
            "spring.data.mongodb.uri",
            mongoDBContainer::getReplicaSetUrl
        );
    }
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
            .body("{\"weights\":{\"salesUnits\":0.7,\"stockRatio\":0.3}}")
        .when()
            .post("/api/products/ranking")
        .then()
            .statusCode(200)
            .body("products.id", contains("5", "1", "3", "2", "6", "4"))
            .body("products[0].score", equalTo(465.0f));
    }
}

Note about the request body:

The project targets Java 17 (see 007 section 3), so text blocks can be used for JSON bodies:

.body("""
    {"weights":{"salesUnits":0.7,"stockRatio":0.3}}
    """)

Note about the startup seed:

MongoSeedConfiguration runs at application startup and seeds the dataset when the collection is empty. E2E tests must not rely on it: each test prepares its own data with deleteAll and saveAll, which keeps tests independent from the seed. Alternatively, the seed bean can be excluded in tests with a Spring profile.

⸻

14.3 Negative Weight E2E Test

@Test
void shouldReturnBadRequestWhenWeightIsNegative() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"weights\":{\"salesUnits\":-1,\"stockRatio\":0.3}}")
    .when()
        .post("/api/products/ranking")
    .then()
        .statusCode(400)
        .body("message", equalTo("Criterion weight cannot be negative"));
}

⸻

15. Test Data Builders

To avoid duplication, create test mothers or builders.

Recommended classes:

ProductTestMother
RankingWeightsTestMother
ProductRankingServiceTestMother
ProductDocumentTestMother

⸻

15.1 ProductTestMother

public final class ProductTestMother {
    private ProductTestMother() {
    }
    public static Product product(
        String id,
        String name,
        int salesUnits,
        int smallStock,
        int mediumStock,
        int largeStock
    ) {
        return new Product(
            new ProductId(id),
            new ProductName(name),
            new SalesUnits(salesUnits),
            stock(smallStock, mediumStock, largeStock)
        );
    }
    public static Product productWithSalesUnits(int salesUnits) {
        return product(
            "1",
            "Product",
            salesUnits,
            1,
            1,
            1
        );
    }
    public static Product productWithStock(
        int smallStock,
        int mediumStock,
        int largeStock
    ) {
        return product(
            "1",
            "Product",
            100,
            smallStock,
            mediumStock,
            largeStock
        );
    }
    public static List<Product> initialDataset() {
        return Arrays.asList(
            product("1", "V-NECH BASIC SHIRT", 100, 4, 9, 0),
            product("2", "CONTRASTING FABRIC T-SHIRT", 50, 35, 9, 9),
            product("3", "RAISED PRINT T-SHIRT", 80, 20, 2, 20),
            product("4", "PLEATED T-SHIRT", 3, 25, 30, 10),
            product("5", "CONTRASTING LACE T-SHIRT", 650, 0, 1, 0),
            product("6", "SLOGAN T-SHIRT", 20, 9, 2, 5)
        );
    }
    private static Stock stock(
        int smallStock,
        int mediumStock,
        int largeStock
    ) {
        return new Stock(Arrays.asList(
            new SizeStock(Size.S, smallStock),
            new SizeStock(Size.M, mediumStock),
            new SizeStock(Size.L, largeStock)
        ));
    }
}

⸻

15.2 RankingWeightsTestMother

public final class RankingWeightsTestMother {
    private RankingWeightsTestMother() {
    }
    public static RankingWeights weights(
        double salesUnitsWeight,
        double stockRatioWeight
    ) {
        Map<CriterionName, CriterionWeight> weights =
            new EnumMap<>(CriterionName.class);
        weights.put(
            CriterionName.SALES_UNITS,
            new CriterionWeight(salesUnitsWeight)
        );
        weights.put(
            CriterionName.STOCK_RATIO,
            new CriterionWeight(stockRatioWeight)
        );
        return new RankingWeights(weights);
    }
    public static RankingWeights salesOnly() {
        return weights(1, 0);
    }
    public static RankingWeights stockOnly() {
        return weights(0, 1);
    }
}

⸻

16. Assertions

Use AssertJ for readable assertions.

Recommended:

assertThat(ranking.products())
    .extracting(rankedProduct -> rankedProduct.product().id().value())
    .containsExactly("5", "1", "3", "2", "6", "4");

Avoid:

assertEquals("5", ranking.products().get(0).product().id().value());
assertEquals("1", ranking.products().get(1).product().id().value());
assertEquals("3", ranking.products().get(2).product().id().value());
assertEquals("2", ranking.products().get(3).product().id().value());
assertEquals("6", ranking.products().get(4).product().id().value());
assertEquals("4", ranking.products().get(5).product().id().value());

The second example is harder to read and more prone to Assertion Roulette.

⸻

17. Test Smells To Avoid

17.1 Assertion Roulette

Bad:

assertEquals("5", ids.get(0));
assertEquals("1", ids.get(1));
assertEquals("3", ids.get(2));

Good:

assertThat(ids).containsExactly("5", "1", "3");

⸻

17.2 Mystery Guest

Bad:

List<Product> products = loadProductsFromSomeFile();

Good:

List<Product> products = ProductTestMother.initialDataset();

The test data should be visible and understandable.

⸻

17.3 General Fixture

Bad:

@BeforeEach
void setUp() {
    // create many objects used by only one test
}

Good:

@Test
void shouldCalculatePartialStockAvailabilityRatio() {
    Stock stock = stock(4, 9, 0);
    assertThat(stock.availabilityRatio()).isEqualTo(2.0 / 3.0);
}

Each test should build only what it needs.

⸻

17.4 Excessive Mocking

Bad:

mock Product
mock Stock
mock SalesUnits
mock Score

Good:

Use real domain objects in domain tests.

Mock external dependencies only.

⸻

17.5 Testing Implementation Details

Bad:

verify(rankingService).weightedScoreFor(...)

Good:

assertThat(result.products())
    .extracting(...)
    .containsExactly(...)

Test behavior, not private implementation.

⸻

17.6 Sleepy Test

Avoid:

Thread.sleep(1000);

There is no asynchronous behavior in this exercise, so sleeps are unnecessary.

⸻

17.7 Conditional Test Logic

Avoid:

if (result.size() > 0) {
    assertThat(...)
}

A test should have a clear expected result.

⸻

18. Architecture Tests

Architecture tests are highly recommended for a Tech Leader exercise.

Use ArchUnit.

Maven dependency

<dependency>
    <groupId>com.tngtech.archunit</groupId>
    <artifactId>archunit-junit5</artifactId>
    <version>1.3.0</version>
    <scope>test</scope>
</dependency>

⸻

18.1 HexagonalArchitectureTest

Recommended rules:

domainShouldNotDependOnSpring
domainShouldNotDependOnMongo
applicationShouldNotDependOnInfrastructure
controllersShouldBeLocatedInInfrastructureRest
mongoDocumentsShouldBeLocatedInInfrastructurePersistenceMongo

Example

@AnalyzeClasses(packages = "com.backendtools.productranking")
class HexagonalArchitectureTest {
    @ArchTest
    static final ArchRule domainShouldNotDependOnSpring =
        noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("org.springframework..");
    @ArchTest
    static final ArchRule applicationShouldNotDependOnInfrastructure =
        noClasses()
            .that()
            .resideInAPackage("..application..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..infrastructure..");
}

This is a strong signal of architectural discipline.

⸻

19. Required Dependencies

Unit tests

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>

Spring Boot starter test includes JUnit 5, AssertJ and Mockito.

⸻

Rest Assured

<dependency>
    <groupId>io.rest-assured</groupId>
    <artifactId>rest-assured</artifactId>
    <scope>test</scope>
</dependency>

⸻

Testcontainers MongoDB

<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>mongodb</artifactId>
    <scope>test</scope>
</dependency>

⸻

Testcontainers JUnit Jupiter

<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>

⸻

ArchUnit

<dependency>
    <groupId>com.tngtech.archunit</groupId>
    <artifactId>archunit-junit5</artifactId>
    <scope>test</scope>
</dependency>

⸻

20. CI Testing Command

The project should run all tests with:

./mvnw test

If using Maven wrapper is not included:

mvn test

⸻

21. Acceptance Test Matrix

Test type	Class	Purpose	Uses Spring	Uses MongoDB	Uses HTTP
Unit	StockTest	Stock rules	No	No	No
Unit	SalesUnitsCriterionTest	Sales criterion	No	No	No
Unit	ProductRankingServiceTest	Ranking algorithm	No	No	No
Application	RankProductsServiceTest	Use case orchestration	No	No	No
Integration	MongoProductRepositoryIntegrationTest	Mongo adapter	Partial	Yes	No
E2E	ProductRankingE2ETest	Full flow	Yes	Yes	Yes
Architecture	HexagonalArchitectureTest	Dependency rules	No	No	No

⸻

22. Minimum Test Suite

If time is limited, the minimum valuable test suite should include:

ProductRankingServiceTest
StockRatioCriterionTest
RankingWeightsTest
RankProductsServiceTest
MongoProductRepositoryIntegrationTest
ProductRankingE2ETest
HexagonalArchitectureTest

This covers:

* Core algorithm.
* Criteria.
* Weight behavior.
* Application orchestration.
* Mongo persistence.
* REST full flow.
* Architecture boundaries.

⸻

23. Interview Defense

A good explanation would be:

I separated the tests according to the architecture. The domain tests validate business rules without Spring or MongoDB. The application tests verify use case orchestration with a fake repository. MongoDB integration tests verify the persistence adapter with Testcontainers. Finally, E2E tests use Rest Assured to validate the full HTTP flow.

About test smells:

I avoided Assertion Roulette by asserting collections with containsExactly, avoided excessive mocking by using real domain objects, and kept test data explicit through test mothers.

About E2E:

The E2E test proves that the real endpoint returns the expected product order for the exercise dataset and the requested weights.

⸻

24. Final Summary

The test strategy is based on:

Unit tests for domain correctness
Application tests for use case orchestration
Integration tests for MongoDB adapter behavior
E2E tests for full REST behavior
Architecture tests for dependency rules

Main testing principle:

Test business behavior, not implementation details.
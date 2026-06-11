005 - MongoDB Persistence Specification

Product Ranking Backend Tools

1. Purpose

This document defines how product data is persisted and retrieved using MongoDB.

MongoDB belongs to the infrastructure layer and must be treated as a secondary adapter in the Hexagonal Architecture.

The domain and application layers must not depend on MongoDB classes, annotations or document structures.

⸻

2. Persistence Responsibility

The persistence layer is responsible for:

* Storing product data in MongoDB.
* Loading products from MongoDB.
* Mapping MongoDB documents into domain objects.
* Seeding the initial exercise dataset.
* Providing an implementation of the application output port ProductRepository.

The persistence layer is not responsible for:

* Calculating scores.
* Sorting products.
* Applying ranking criteria.
* Validating domain business rules beyond mapping consistency.
* Exposing REST responses.

⸻

3. Architectural Position

MongoDB is a secondary adapter.

Application Use Case
    ↓
ProductRepository output port
    ↓
MongoProductRepository adapter
    ↓
SpringDataMongoProductRepository
    ↓
MongoDB

The dependency direction is:

infrastructure.persistence.mongo → application.port.out → domain

The application layer defines the contract.

The infrastructure layer implements it.

⸻

4. Output Port

The application layer defines the repository port.

public interface ProductRepository {
    List<Product> findAll();
}

This port returns domain objects, not MongoDB documents.

⸻

5. MongoDB Collection

The products will be stored in the following collection:

products

⸻

6. Product Document Structure

A MongoDB product document should have this structure:

{
  "_id": "1",
  "name": "V-NECH BASIC SHIRT",
  "salesUnits": 100,
  "stock": {
    "S": 4,
    "M": 9,
    "L": 0
  }
}

⸻

7. ProductDocument

ProductDocument is an infrastructure model.

It represents how product data is stored in MongoDB.

It is not a domain entity.

@Document(collection = "products")
public class ProductDocument {
    @Id
    private String id;
    private String name;
    private int salesUnits;
    private Map<String, Integer> stock;
    protected ProductDocument() {
    }
    public ProductDocument(
        String id,
        String name,
        int salesUnits,
        Map<String, Integer> stock
    ) {
        this.id = id;
        this.name = name;
        this.salesUnits = salesUnits;
        this.stock = stock;
    }
    public String getId() {
        return id;
    }
    public String getName() {
        return name;
    }
    public int getSalesUnits() {
        return salesUnits;
    }
    public Map<String, Integer> getStock() {
        return stock;
    }
}

⸻

8. Design Decision: Separate Document From Domain

Avoid this:

@Document(collection = "products")
public class Product {
}

Reason:

* It couples the domain to MongoDB.
* It introduces persistence concerns into business logic.
* It makes the domain harder to test.
* It makes future persistence replacement harder.
* It violates Hexagonal Architecture.

Preferred approach:

ProductDocument  → infrastructure
Product          → domain

⸻

9. Spring Data Mongo Repository

Spring Data MongoDB is used only inside infrastructure.

public interface SpringDataMongoProductRepository
    extends MongoRepository<ProductDocument, String> {
}

This interface must not be injected into application services.

It should only be used by MongoProductRepository.

⸻

10. MongoProductRepository Adapter

MongoProductRepository implements the output port required by the application.

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

Responsibility

* Retrieve ProductDocument objects from MongoDB.
* Convert them to domain Product objects.
* Return domain objects to the application layer.

Non-responsibility

It must not:

* Sort products.
* Calculate scores.
* Apply criterion weights.
* Return REST response objects.
* Expose ProductDocument outside infrastructure.

⸻

11. ProductMongoMapper

The mapper converts between MongoDB documents and domain objects.

@Component
public class ProductMongoMapper {
    public Product toDomain(ProductDocument document) {
        return new Product(
            new ProductId(document.getId()),
            new ProductName(document.getName()),
            new SalesUnits(document.getSalesUnits()),
            toDomainStock(document.getStock())
        );
    }
    private Stock toDomainStock(Map<String, Integer> stock) {
        List<SizeStock> sizeStocks = stock.entrySet()
            .stream()
            .map(entry -> new SizeStock(
                Size.valueOf(entry.getKey()),
                entry.getValue()
            ))
            .collect(Collectors.toList());
        return new Stock(sizeStocks);
    }
    public ProductDocument toDocument(Product product) {
        return new ProductDocument(
            product.id().value(),
            product.name().value(),
            product.salesUnits().value(),
            toDocumentStock(product.stock())
        );
    }
    private Map<String, Integer> toDocumentStock(Stock stock) {
        return stock.sizes()
            .stream()
            .collect(Collectors.toMap(
                sizeStock -> sizeStock.size().name(),
                SizeStock::units,
                (first, second) -> first,
                LinkedHashMap::new
            ));
    }
}

⸻

12. Mapper Validation Behavior

The mapper relies on domain constructors to validate loaded data.

Example:

new SalesUnits(document.getSalesUnits())

If MongoDB contains invalid data, for example:

{
  "salesUnits": -10
}

The domain constructor rejects it.

This is acceptable because invalid persisted data should not silently enter the domain model.

⸻

13. Unknown Size Handling

The initial exercise only supports:

S
M
L

If MongoDB contains:

{
  "XL": 10
}

Size.valueOf("XL") will fail unless XL exists in the enum.

Recommended behavior for this exercise

Fail fast.

Reason:

* The exercise dataset is controlled.
* Unknown sizes indicate invalid persisted data.
* Silent ignoring could hide data problems.

⸻

14. Initial Dataset

The initial data must be inserted into MongoDB.

Id	Name	Sales Units	S	M	L
1	V-NECH BASIC SHIRT	100	4	9	0
2	CONTRASTING FABRIC T-SHIRT	50	35	9	9
3	RAISED PRINT T-SHIRT	80	20	2	20
4	PLEATED T-SHIRT	3	25	30	10
5	CONTRASTING LACE T-SHIRT	650	0	1	0
6	SLOGAN T-SHIRT	20	9	2	5

⸻

15. Mongo Seed Configuration

For the exercise, the database can be seeded at application startup.

@Configuration
public class MongoSeedConfiguration {
    @Bean
    CommandLineRunner seedProducts(
        SpringDataMongoProductRepository repository
    ) {
        return args -> {
            if (repository.count() > 0) {
                return;
            }
            repository.saveAll(Arrays.asList(
                product("1", "V-NECH BASIC SHIRT", 100, 4, 9, 0),
                product("2", "CONTRASTING FABRIC T-SHIRT", 50, 35, 9, 9),
                product("3", "RAISED PRINT T-SHIRT", 80, 20, 2, 20),
                product("4", "PLEATED T-SHIRT", 3, 25, 30, 10),
                product("5", "CONTRASTING LACE T-SHIRT", 650, 0, 1, 0),
                product("6", "SLOGAN T-SHIRT", 20, 9, 2, 5)
            ));
        };
    }
    private ProductDocument product(
        String id,
        String name,
        int salesUnits,
        int smallStock,
        int mediumStock,
        int largeStock
    ) {
        Map<String, Integer> stock = new LinkedHashMap<>();
        stock.put("S", smallStock);
        stock.put("M", mediumStock);
        stock.put("L", largeStock);
        return new ProductDocument(id, name, salesUnits, stock);
    }
}

⸻

16. Design Decision: Seed Only When Empty

The seed process should only insert data when the collection is empty.

Reason:

* Avoid duplicating data.
* Avoid overwriting manually inserted data.
* Keep startup deterministic.
* Make local development easier.

⸻

17. Docker Compose

A local MongoDB instance can be provided with Docker Compose.

The version attribute is intentionally omitted: it is obsolete in Docker Compose v2.

services:
  mongodb:
    image: mongo:7
    container_name: product-ranking-mongodb
    ports:
      - "27017:27017"
    environment:
      MONGO_INITDB_DATABASE: product_ranking

⸻

18. Spring Configuration

application.yml:

spring:
  data:
    mongodb:
      uri: mongodb://localhost:27017/product_ranking
server:
  port: 8080

⸻

19. Maven Dependencies

Required dependency:

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-mongodb</artifactId>
</dependency>

For integration tests:

<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>mongodb</artifactId>
    <scope>test</scope>
</dependency>

⸻

20. Persistence Package Structure

Recommended structure:

infrastructure
  persistence
    mongo
      ProductDocument.java
      SpringDataMongoProductRepository.java
      MongoProductRepository.java
      ProductMongoMapper.java

⸻

21. Integration Testing Strategy

The MongoDB adapter must be tested with integration tests.

The goal is to verify that:

* Documents are stored correctly.
* Documents are loaded correctly.
* Documents are mapped to domain objects.
* The adapter implements the ProductRepository port correctly.

⸻

22. MongoProductRepositoryIntegrationTest

Recommended test name:

MongoProductRepositoryIntegrationTest

Recommended test cases:

shouldFindAllProductsFromMongoAndMapThemToDomain
shouldReturnEmptyListWhenNoProductsExist
shouldFailWhenPersistedProductContainsInvalidDomainData

⸻

23. Testcontainers Setup

Example:

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
        ProductMongoMapper mapper = new ProductMongoMapper();
        productRepository = new MongoProductRepository(
            springDataRepository,
            mapper
        );
        springDataRepository.deleteAll();
    }
    @Test
    void shouldFindAllProductsFromMongoAndMapThemToDomain() {
        springDataRepository.save(
            new ProductDocument(
                "1",
                "V-NECH BASIC SHIRT",
                100,
                stock(4, 9, 0)
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
    private Map<String, Integer> stock(
        int small,
        int medium,
        int large
    ) {
        Map<String, Integer> stock = new LinkedHashMap<>();
        stock.put("S", small);
        stock.put("M", medium);
        stock.put("L", large);
        return stock;
    }
}

⸻

24. Difference Between Integration And E2E Tests

A MongoDB repository integration test:

Tests MongoProductRepository + MongoDB
Does not call REST endpoint
Does not start the full application flow

An E2E test:

Calls POST /api/products/ranking
Goes through controller, mapper, use case, domain service, repository and MongoDB

Both test types are useful and should not be confused.

⸻

25. Repository Adapter Acceptance Criteria

AC1 - Load all products

Given products exist in MongoDB.

When:

productRepository.findAll()

Then it returns all products as domain Product objects.

⸻

AC2 - Return empty list

Given no products exist in MongoDB.

When:

productRepository.findAll()

Then it returns an empty list.

⸻

AC3 - Map stock correctly

Given a Mongo document with stock:

{
  "S": 4,
  "M": 9,
  "L": 0
}

Then the domain stock availability ratio is:

2 / 3

⸻

AC4 - Reject invalid persisted data

Given a Mongo document with:

{
  "salesUnits": -1
}

When the document is mapped to domain.

Then the domain rejects the product.

⸻

26. Persistence Smells To Avoid

26.1 Using Mongo document as domain

Bad:

@Document(collection = "products")
public class Product {
}

Good:

public final class Product {
}

and:

@Document(collection = "products")
public class ProductDocument {
}

⸻

26.2 Injecting Spring Data repository into use case

Bad:

public class RankProductsService {
    private final SpringDataMongoProductRepository repository;
}

Good:

public class RankProductsService {
    private final ProductRepository productRepository;
}

⸻

26.3 Returning ProductDocument from output port

Bad:

public interface ProductRepository {
    List<ProductDocument> findAll();
}

Good:

public interface ProductRepository {
    List<Product> findAll();
}

⸻

26.4 Calculating ranking in repository

Bad:

public List<Product> findAllRanked(...) {
    // score calculation here
}

Good:

public List<Product> findAll() {
    // only loading and mapping
}

Ranking belongs to the domain service.

⸻

27. Interview Defense

A good explanation would be:

MongoDB is implemented as a secondary adapter. The application layer depends on the ProductRepository output port, while the MongoProductRepository adapter implements that port using Spring Data MongoDB.
The domain model is not annotated with MongoDB annotations. Instead, I use a ProductDocument for persistence and a mapper to convert documents into rich domain objects.

About mapping:

The mapper is important because the persistence model and the domain model have different responsibilities. The document is optimized for storage, while the domain object protects business invariants and exposes behavior.

About testing:

I would test MongoProductRepository with Testcontainers to verify the real MongoDB integration without confusing it with E2E tests. The E2E tests are reserved for the full HTTP flow.

⸻

28. Final Summary

MongoDB persistence is implemented with:

ProductRepository output port
MongoProductRepository adapter
SpringDataMongoProductRepository
ProductDocument
ProductMongoMapper

Main rule:

MongoDB is an implementation detail of infrastructure.

The domain remains:

Persistence-agnostic
Framework-independent
Business-focused
Easy to test
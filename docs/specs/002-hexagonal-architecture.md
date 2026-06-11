002 - Hexagonal Architecture Specification

Product Ranking Backend Tools

1. Purpose

This document defines the architectural structure of the product ranking service using Hexagonal Architecture, also known as Ports and Adapters.

The objective is to clearly separate:

* Domain logic
* Application use cases
* Infrastructure concerns
* REST API concerns
* Persistence concerns

The implementation must keep business rules independent from frameworks such as Spring Boot and MongoDB.

⸻

2. Architectural Goal

The service must expose a REST endpoint that ranks products using weighted criteria.

However, the ranking algorithm itself must not depend on:

* REST controllers
* JSON request bodies
* MongoDB documents
* Spring Data repositories
* Spring annotations
* HTTP concepts

The core business logic must be reusable from other adapters in the future, such as:

* CLI
* Batch job
* Kafka consumer
* GraphQL API
* Another REST endpoint
* Different database technology

⸻

3. Architectural Style

The project follows Hexagonal Architecture.

The application is divided into three main areas:

domain
application
infrastructure

Dependency direction

Dependencies must always point inward.

infrastructure  --->  application  --->  domain

The domain must not depend on anything outside itself.

The application layer may depend on the domain.

The infrastructure layer may depend on application and domain.

⸻

4. Layer Responsibilities

4.1 Domain Layer

The domain layer contains the business model and business rules.

Responsibilities

* Represent products.
* Represent stock.
* Represent ranking criteria.
* Represent scores and weights.
* Calculate product ranking.
* Protect business invariants.

Must contain

Product
ProductId
ProductName
SalesUnits
Stock
SizeStock
Size
Score
CriterionWeight
RankingWeights
RankingCriterion
SalesUnitsCriterion
StockRatioCriterion
ProductRankingService
RankedProduct
ProductRanking

Must not contain

@RestController
@Service
@Repository
@Component
@Document
@Id
MongoRepository
ResponseEntity
HttpStatus
RequestBody
Spring annotations
JSON annotations

Rule

The domain must be plain Java.

No Spring.
No MongoDB.
No HTTP.

⸻

4.2 Application Layer

The application layer coordinates use cases.

It does not contain low-level infrastructure details.

Responsibilities

* Expose input ports.
* Define output ports required by use cases.
* Receive application commands.
* Load products through output ports.
* Call domain services.
* Return domain results.

Must contain

RankProductsUseCase
RankProductsCommand
RankProductsService
ProductRepository

Must not contain

@RestController
@Document
MongoRepository
HTTP request objects
HTTP response objects
Mongo documents
JSON field names

Rule

The application layer orchestrates.
The domain layer decides.

⸻

4.3 Infrastructure Layer

The infrastructure layer adapts external technologies to the application core.

Responsibilities

* Expose REST API.
* Persist and retrieve data from MongoDB.
* Map HTTP requests to application commands.
* Map domain results to HTTP responses.
* Map MongoDB documents to domain models.
* Configure Spring beans.
* Seed initial data.

Must contain

ProductRankingController
ProductRankingRequest
RankingWeightsRequest
ProductRankingResponse
RankedProductResponse
ProductRankingRestMapper
ProductDocument
SpringDataMongoProductRepository
MongoProductRepository
ProductMongoMapper
ProductRankingConfiguration
MongoSeedConfiguration
RestExceptionHandler
ErrorResponse

Rule

Infrastructure depends on the application core, never the opposite.

⸻

5. Ports

Ports are interfaces that define how the outside world interacts with the application or how the application interacts with external systems.

There are two types of ports:

Input ports
Output ports

⸻

5.1 Input Port

Input ports define use cases offered by the application.

RankProductsUseCase

public interface RankProductsUseCase {
    ProductRanking rankProducts(RankProductsCommand command);
}

Responsibility

RankProductsUseCase represents the ability to rank products.

It does not know whether the request comes from:

* REST
* CLI
* Scheduler
* Test
* Message consumer

Design decision

The input port returns a domain object, ProductRanking.

The REST adapter is responsible for converting that domain result into an HTTP response.

⸻

5.2 Output Port

Output ports define what the application needs from the outside world.

ProductRepository

public interface ProductRepository {
    List<Product> findAll();
}

Responsibility

ProductRepository allows the use case to retrieve products.

It does not expose MongoDB concepts.

Forbidden examples

The output port must not look like this:

public interface ProductRepository extends MongoRepository<ProductDocument, String> {
}

Nor like this:

public interface ProductRepository {
    List<ProductDocument> findAll();
}

The application should work even if MongoDB is replaced by PostgreSQL, files, memory storage or an external API.

⸻

6. Adapters

Adapters implement the connection between external technologies and ports.

⸻

6.1 Primary Adapter: REST

The REST adapter is a primary adapter because it starts the interaction with the application.

Main classes

ProductRankingController
ProductRankingRequest
RankingWeightsRequest
ProductRankingResponse
RankedProductResponse
ProductRankingRestMapper
RestExceptionHandler
ErrorResponse

Flow

HTTP request
    ↓
ProductRankingController
    ↓
ProductRankingRestMapper
    ↓
RankProductsUseCase
    ↓
ProductRankingRestMapper
    ↓
HTTP response

Controller responsibility

The controller should only:

* Receive HTTP request.
* Delegate request mapping.
* Call the use case.
* Return HTTP response.

Controller must not:

* Calculate scores.
* Sort products.
* Access MongoDB.
* Know domain internals more than necessary.
* Create MongoDB documents.
* Contain complex business rules.

⸻

6.2 Secondary Adapter: MongoDB

The MongoDB adapter is a secondary adapter because it is used by the application to fulfill a dependency.

Main classes

ProductDocument
SpringDataMongoProductRepository
MongoProductRepository
ProductMongoMapper

Flow

RankProductsService
    ↓
ProductRepository
    ↓
MongoProductRepository
    ↓
SpringDataMongoProductRepository
    ↓
MongoDB

MongoProductRepository

MongoProductRepository implements the application output port:

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

Design decision

The adapter translates infrastructure data structures into domain objects.

The rest of the system does not know that MongoDB exists.

⸻

7. Application Use Case

7.1 RankProductsService

RankProductsService is the use case implementation.

public final class RankProductsService implements RankProductsUseCase {
    private final ProductRepository productRepository;
    private final ProductRankingService productRankingService;
    public RankProductsService(
        ProductRepository productRepository,
        ProductRankingService productRankingService
    ) {
        this.productRepository = Objects.requireNonNull(productRepository);
        this.productRankingService = Objects.requireNonNull(productRankingService);
    }
    @Override
    public ProductRanking rankProducts(RankProductsCommand command) {
        List<Product> products = productRepository.findAll();
        return productRankingService.rank(products, command.rankingWeights());
    }
}

Responsibilities

* Load products from repository.
* Delegate ranking to domain service.
* Return ranking result.

Non-responsibilities

* It does not know about HTTP.
* It does not know about MongoDB.
* It does not calculate individual criterion scores.
* It does not sort products manually.
* It does not parse JSON.

⸻

8. Command Object

8.1 RankProductsCommand

public final class RankProductsCommand {
    private final RankingWeights rankingWeights;
    public RankProductsCommand(RankingWeights rankingWeights) {
        this.rankingWeights = Objects.requireNonNull(rankingWeights);
    }
    public RankingWeights rankingWeights() {
        return rankingWeights;
    }
}

Responsibility

The command represents the input required by the use case.

Design decision

The command receives domain value objects instead of raw primitive values.

This keeps validation close to domain concepts.

⸻

9. Package Structure

Recommended package structure:

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

Note about CriterionName location

CriterionName lives in domain.ranking next to the criteria, while RankingWeights lives in domain.model and depends on it. This dependency between domain subpackages is deliberate and harmless: architecture rules are enforced at layer level (domain, application, infrastructure), not at subpackage level.

⸻

10. Dependency Rules By Package

domain

May depend on:

java.util
java.lang

Must not depend on:

application
infrastructure
springframework
mongodb
jackson
javax.validation
jakarta.validation

⸻

application

May depend on:

domain
java.util

Must not depend on:

infrastructure
springframework.web
mongodb
jackson

⸻

infrastructure

May depend on:

application
domain
springframework
mongodb
jackson

Note: Bean Validation is intentionally not used in this project. Request validation is handled by the REST mapper and the domain (see 004 section 9.1).

⸻

11. Dependency Verification

It is recommended to add architecture tests with ArchUnit.

Example rules:

Domain classes should not depend on Spring.
Domain classes should not depend on MongoDB.
Application classes should not depend on infrastructure.
Controllers should not be accessed from domain or application.
Repositories from Spring Data should stay in infrastructure.

Example test names:

domainShouldNotDependOnSpringFramework
applicationShouldNotDependOnInfrastructure
infrastructureShouldBeAllowedToDependOnApplicationAndDomain

These tests are not mandatory but are highly valuable for a Tech Leader position.

⸻

12. Runtime Flow

The main flow is:

1. Client sends POST /api/products/ranking with criterion weights.
2. ProductRankingController receives the request.
3. ProductRankingRestMapper converts the request into RankProductsCommand.
4. RankProductsUseCase is called.
5. RankProductsService loads products through ProductRepository.
6. MongoProductRepository retrieves documents from MongoDB.
7. ProductMongoMapper converts ProductDocument into Product.
8. ProductRankingService calculates final ranking.
9. ProductRanking is returned to the use case.
10. Controller maps ProductRanking to ProductRankingResponse.
11. Client receives ordered products.

⸻

13. Sequence Diagram

Client
  |
  | POST /api/products/ranking
  v
ProductRankingController
  |
  | toCommand(request)
  v
ProductRankingRestMapper
  |
  | rankProducts(command)
  v
RankProductsUseCase
  |
  | findAll()
  v
ProductRepository
  |
  | findAll()
  v
MongoProductRepository
  |
  | findAll()
  v
SpringDataMongoProductRepository
  |
  | documents
  v
ProductMongoMapper
  |
  | products
  v
ProductRankingService
  |
  | ranking
  v
ProductRankingController
  |
  | toResponse(ranking)
  v
Client

⸻

14. Spring Bean Configuration

The domain and application services should be registered explicitly from infrastructure configuration.

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

Design decision

Domain classes are not annotated with @Service.

Spring is used only as an assembly mechanism.

This keeps the core model independent from the framework.

⸻

15. REST Adapter Mapping

The REST request should not be passed directly to the application use case.

Avoid:

rankProductsUseCase.rankProducts(request);

Prefer:

RankProductsCommand command = mapper.toCommand(request);
ProductRanking ranking = rankProductsUseCase.rankProducts(command);

Reason

REST models are transport models.

Application commands are use case models.

They should evolve independently.

⸻

16. Mongo Adapter Mapping

Mongo documents should not be used directly as domain objects.

Avoid:

List<ProductDocument> products = repository.findAll();
rankingService.rank(products, weights);

Prefer:

List<Product> products = repository.findAll()
    .stream()
    .map(mapper::toDomain)
    .collect(Collectors.toList());

Reason

Persistence models are optimized for storage.

Domain models are optimized for business behavior.

⸻

17. Error Handling

Error handling belongs to the REST adapter.

Domain and application layers may throw meaningful exceptions such as:

IllegalArgumentException

The REST adapter converts those exceptions into HTTP responses.

Example:

@RestControllerAdvice
public class RestExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleIllegalArgumentException(
        IllegalArgumentException exception
    ) {
        return new ErrorResponse(exception.getMessage());
    }
}

The domain should not throw HTTP-specific exceptions.

Avoid:

throw new ResponseStatusException(HttpStatus.BAD_REQUEST);

inside domain or application layers.

⸻

18. Testing Strategy By Layer

Domain tests

Target:

domain

Characteristics:

* No Spring.
* No MongoDB.
* No HTTP.
* Fast.
* Pure unit tests.

Examples:

StockTest
SalesUnitsCriterionTest
ProductRankingServiceTest

⸻

Application tests

Target:

application

Characteristics:

* Use fake or mock ProductRepository.
* Verify orchestration.
* No MongoDB.
* No HTTP.

Examples:

RankProductsServiceTest

⸻

Infrastructure integration tests

Target:

infrastructure.persistence.mongo

Characteristics:

* Use MongoDB Testcontainers.
* Verify document mapping.
* Verify repository adapter.
* No HTTP.

Examples:

MongoProductRepositoryIntegrationTest

⸻

E2E tests

Target:

whole application

Characteristics:

* Start Spring Boot application.
* Use Rest Assured.
* Use MongoDB Testcontainers.
* Call REST endpoint.
* Verify full flow.

Examples:

ProductRankingE2ETest

⸻

19. Architecture Smells To Avoid

19.1 Domain depending on Spring

Bad:

@Service
public class ProductRankingService {
}

Good:

public final class ProductRankingService {
}

⸻

19.2 Application depending on MongoDB

Bad:

public class RankProductsService {
    private final MongoRepository<ProductDocument, String> repository;
}

Good:

public class RankProductsService {
    private final ProductRepository productRepository;
}

⸻

19.3 Controller doing business logic

Bad:

products.sort(...)

inside controller.

Good:

ProductRanking ranking = rankProductsUseCase.rankProducts(command);

⸻

19.4 Passing DTOs into domain

Bad:

rankingService.rank(ProductRankingRequest request);

Good:

rankingService.rank(products, rankingWeights);

⸻

19.5 Exposing Mongo documents through REST

Bad:

public List<ProductDocument> rankProducts() {
}

Good:

public ProductRankingResponse rankProducts() {
}

⸻

20. Architectural Benefits

This architecture provides:

* Clear separation of concerns.
* Testable domain logic.
* Framework-independent business rules.
* Easy replacement of MongoDB.
* Easy addition of ranking criteria.
* REST API isolated from domain model.
* Better maintainability.
* Better interview defensibility.

⸻

21. Interview Defense

A good explanation for this architecture would be:

I separated the solution into domain, application and infrastructure following Hexagonal Architecture.
The domain contains the rich business model and the ranking algorithm. The application layer exposes the rank products use case and depends on a repository port, not on MongoDB. The infrastructure layer contains REST and MongoDB adapters.
This allows the ranking logic to be tested without Spring or MongoDB, keeps the code extensible, and makes it easy to add new ranking criteria using the Strategy pattern.

Another important point:

The controller does not calculate rankings. MongoDB documents are not domain objects. The domain is independent from frameworks.

⸻

22. Final Architecture Summary

REST API
  ↓
REST Adapter
  ↓
Input Port
  ↓
Use Case
  ↓
Domain Service
  ↓
Output Port
  ↓
MongoDB Adapter
  ↓
MongoDB

Main dependency rule:

Infrastructure depends on Application.
Application depends on Domain.
Domain depends on nothing.
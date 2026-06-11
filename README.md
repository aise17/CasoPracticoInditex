# Product Ranking Service

REST service that ranks a list of products using a weighted combination of ranking criteria. Built with Java 17, Spring Boot 3 and MongoDB, following Hexagonal Architecture and tactical DDD.

## Problem

Given a list of t-shirts, the service must order them by a score computed as the weighted sum of two criteria:

- **Sales units**: score based on the number of units sold.
- **Stock ratio**: score based on the proportion of sizes with available stock.

The weights for each criterion are provided by the API consumer on every request. New criteria may be added in the future.

## Requirements

- JDK 17+
- Docker (for MongoDB and for integration/E2E tests via Testcontainers)

## Running the application

```bash
# 1. Start MongoDB
docker compose up -d

# 2. Run the application (seeds the initial dataset on first start)
./mvnw spring-boot:run
```

## API

### POST /api/products/ranking

Request:

```json
{
  "weights": {
    "salesUnits": 0.7,
    "stockRatio": 0.3
  }
}
```

```bash
curl -s -X POST http://localhost:8080/api/products/ranking \
  -H "Content-Type: application/json" \
  -d '{"weights":{"salesUnits":0.7,"stockRatio":0.3}}'
```

Response (200):

```json
{
  "products": [
    {
      "id": "5",
      "name": "CONTRASTING LACE T-SHIRT",
      "salesUnits": 650,
      "stock": { "S": 0, "M": 1, "L": 0 },
      "score": 465.0
    }
  ]
}
```

Errors return 400 with a body like `{"message": "Criterion weight cannot be negative"}`.

| Case | Status | Message |
| --- | --- | --- |
| Missing or empty `weights` | 400 | `Ranking weights cannot be empty` |
| Negative weight | 400 | `Criterion weight cannot be negative` |
| Malformed JSON / non-numeric weight | 400 | `Malformed JSON request` |
| Only unknown criteria provided | 400 | `Ranking weights cannot be empty` |

Unknown criterion fields are ignored. Missing weights for known criteria default to zero.

A Postman collection with examples of every possible call (valid and error cases, with response assertions) is available at `docs/postman/ProductRanking.postman_collection.json`.

## Architecture

Hexagonal Architecture (Ports & Adapters) with three layers:

```
com.backendtools.productranking
├── domain                  # Pure business model. No framework dependencies.
│   ├── model               # Aggregate (Product) and value objects
│   └── ranking             # Ranking criteria (Strategy) and domain service
├── application             # Use cases orchestrating the domain
│   ├── port.in             # RankProductsUseCase (primary port)
│   ├── port.out            # ProductRepository (secondary port)
│   └── usecase             # RankProductsService, RankProductsCommand
└── infrastructure          # Adapters and configuration
    ├── rest                # Primary adapter: controller, DTOs, mapper, error handling
    ├── persistence.mongo   # Secondary adapter: documents, Spring Data repository, mapper
    └── configuration       # Bean wiring and Mongo seed
```

Dependency rule: `infrastructure -> application -> domain`. The domain depends on nothing. Enforced by `HexagonalArchitectureTest` (ArchUnit).

### Tactical DDD

- **Aggregate root**: `Product` (identity-based equality on `ProductId`).
- **Value objects**: `ProductId`, `ProductName`, `SalesUnits`, `SizeStock`, `Stock`, `Score`, `CriterionWeight`, `RankingWeights`, `RankedProduct`, `ProductRanking`. All immutable, self-validating, with value-based `equals`/`hashCode`.
- **Domain service**: `ProductRankingService` computes the weighted ranking, logic that does not belong to a single aggregate.
- **Rich model**: invariants live in constructors (no negative sales, no duplicated stock sizes, no negative weights), behavior lives next to the data (`Stock.availabilityRatio()`, `Score.add()`...). No setters, no anemic DTOs in the core.

## Design decisions

- **Strategy pattern for criteria**: each criterion implements `RankingCriterion` (`name()` + `calculateScore(Product)`). Adding a criterion means adding one class and registering it, without touching the ranking algorithm (Open/Closed). Inheritance was discarded because criteria do not share behavior worth inheriting; decorators were discarded because criteria are combined by weighted sum, not by wrapping each other.
- **Weighted sum with tie-breaker**: final score = `sum(criterionScore * weight)`. Ties are broken deterministically by ascending product id, so responses are reproducible.
- **`double` for scores**: scores are relative ordering values, not monetary amounts; IEEE 754 precision is sufficient and keeps the model simple. `BigDecimal` would add noise without changing any ordering in this domain. `Score` rejects `NaN`/`Infinity` and the REST mapper rounds to 2 decimals at the boundary.
- **No Bean Validation**: transport-level validation is done explicitly in the REST mapper and the domain value objects, which keeps a single source of truth for error messages and avoids duplicating invariants in annotations.
- **Typed weight fields instead of a generic map**: `RankingWeightsRequest` exposes explicit `salesUnits`/`stockRatio` fields, making the API contract self-documenting. The trade-off is that new criteria require a DTO change, which is acceptable: a new criterion is a deliberate API evolution.
- **Java 17 + Spring Boot 3**: the exercise requires Java 8 or superior; Java 17 is the LTS baseline of Spring Boot 3 and enables text blocks and modern APIs.
- **Seed data**: the dataset from the exercise statement is inserted on startup if the collection is empty. Tests never rely on the seed; each test prepares its own data.

## Testing

```bash
./mvnw test
```

71 tests in four levels (names state behavior, one logical assertion block per test):

| Level | Location | Tooling |
| --- | --- | --- |
| Unit (domain & application) | `domain/*`, `application/*` | JUnit 5, AssertJ, test mothers |
| REST slice | `infrastructure/rest` | `@WebMvcTest`, MockMvc, `@MockitoBean` |
| Integration (persistence) | `infrastructure/persistence/mongo` | `@DataMongoTest`, Testcontainers |
| End-to-end | `e2e` | `@SpringBootTest`, Rest Assured, Testcontainers |
| Architecture | `architecture` | ArchUnit |

Note: `src/test/resources/docker-java.properties` pins the Docker API version to 1.44 because Docker Engine 29+ rejects the 1.32 default used by Testcontainers 1.x.

001 - Domain Model Specification

Product Ranking Backend Tools

1. Purpose

This document defines the domain model for the product ranking service.

The goal of the system is to rank a list of products displayed in a t-shirt category using a weighted scoring algorithm based on configurable ranking criteria.

The initial ranking criteria are:

* Sales units
* Stock ratio

The domain model must be expressive, maintainable and aligned with tactical DDD principles. The implementation must avoid an anemic domain model and keep business rules inside domain objects and domain services.

⸻

2. Bounded Context

The system belongs to the following bounded context:

Product Ranking Context

This bounded context is responsible for:

* Representing products that can be ranked.
* Representing stock availability by size.
* Representing ranking criteria.
* Calculating weighted product scores.
* Returning products ordered by their final ranking score.

This context does not manage:

* Product catalog creation.
* Product pricing.
* Product categories.
* Checkout.
* Orders.
* Users.
* Promotions.

Those concepts are outside the current exercise.

⸻

3. Ubiquitous Language

Business concept	Code name	Type
Product	Product	Aggregate Root
Product identifier	ProductId	Value Object
Product name	ProductName	Value Object
Sales units	SalesUnits	Value Object
Product stock	Stock	Value Object
Stock by size	SizeStock	Value Object
Size	Size	Enum
Ranking criterion	RankingCriterion	Domain interface (Strategy pattern)
Criterion name	CriterionName	Enum
Criterion weight	CriterionWeight	Value Object
Ranking weights	RankingWeights	Value Object
Score	Score	Value Object
Ranked product	RankedProduct	Value Object
Product ranking	ProductRanking	Value Object
Ranking service	ProductRankingService	Domain Service

⸻

4. Aggregate

4.1 Product

Product is the main aggregate root.

A product contains all the information required by the ranking algorithm:

* Identifier
* Name
* Sales units
* Stock by size

Responsibilities

The Product aggregate is responsible for:

* Protecting product consistency.
* Exposing meaningful domain information.
* Avoiding primitive obsession in the ranking process.

Invariants

A product must always satisfy the following rules:

* Product id cannot be null.
* Product name cannot be null or blank.
* Sales units cannot be negative.
* Stock cannot be null.
* Stock must contain at least one size.

Suggested implementation

public final class Product {
    private final ProductId id;
    private final ProductName name;
    private final SalesUnits salesUnits;
    private final Stock stock;
    public Product(
        ProductId id,
        ProductName name,
        SalesUnits salesUnits,
        Stock stock
    ) {
        this.id = Objects.requireNonNull(id);
        this.name = Objects.requireNonNull(name);
        this.salesUnits = Objects.requireNonNull(salesUnits);
        this.stock = Objects.requireNonNull(stock);
    }
    public ProductId id() {
        return id;
    }
    public ProductName name() {
        return name;
    }
    public SalesUnits salesUnits() {
        return salesUnits;
    }
    public Stock stock() {
        return stock;
    }
}

Design decision

Product should not be annotated with MongoDB, JPA or Spring annotations.

The domain model must be persistence-agnostic.

MongoDB persistence will be handled by infrastructure adapters.

⸻

5. Value Objects

5.0 Cross-Cutting Value Object Rules

Every value object in this domain must:

* Be immutable.
* Implement equals and hashCode based on its value.
* Implement toString for debugging and error messages.

Value equality is the defining characteristic of a value object. Two ProductId instances with the same value must be equal. Without equals and hashCode, the model degrades into anemic wrappers and value objects cannot be used safely in collections, maps or assertions.

Example for ProductId:

@Override
public boolean equals(Object other) {
    if (this == other) {
        return true;
    }
    if (!(other instanceof ProductId)) {
        return false;
    }
    return value.equals(((ProductId) other).value);
}

@Override
public int hashCode() {
    return value.hashCode();
}

@Override
public String toString() {
    return value;
}

The suggested implementations below omit equals, hashCode and toString for brevity, but the real implementation must include them in every value object.

⸻

5.1 ProductId

Represents the unique identity of a product.

Rules

* Cannot be null.
* Cannot be blank.

Suggested implementation

public final class ProductId {
    private final String value;
    public ProductId(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Product id cannot be empty");
        }
        this.value = value;
    }
    public String value() {
        return value;
    }
}

Design decision

Although the exercise uses numeric ids, the domain uses String to avoid coupling the model to a specific persistence format.

MongoDB ids are naturally string-compatible.

⸻

5.2 ProductName

Represents the product commercial name.

Rules

* Cannot be null.
* Cannot be blank.

Suggested implementation

public final class ProductName {
    private final String value;
    public ProductName(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Product name cannot be empty");
        }
        this.value = value;
    }
    public String value() {
        return value;
    }
}

⸻

5.3 SalesUnits

Represents the number of units sold by a product.

Rules

* Cannot be negative.
* Can be zero.

Suggested implementation

public final class SalesUnits {
    private final int value;
    public SalesUnits(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("Sales units cannot be negative");
        }
        this.value = value;
    }
    public int value() {
        return value;
    }
}

⸻

5.4 Size

Represents a t-shirt size.

Initial values

public enum Size {
    S,
    M,
    L
}

Design decision

For the initial exercise, the available sizes are S, M and L.

If the business later adds new sizes, the model can evolve by adding new enum values or replacing the enum with a richer value object.

⸻

5.5 SizeStock

Represents the stock available for a specific size.

Rules

* Size cannot be null.
* Units cannot be negative.
* Units can be zero.

Behavior

SizeStock must know whether a size has available stock.

Suggested implementation

public final class SizeStock {
    private final Size size;
    private final int units;
    public SizeStock(Size size, int units) {
        if (units < 0) {
            throw new IllegalArgumentException("Stock units cannot be negative");
        }
        this.size = Objects.requireNonNull(size);
        this.units = units;
    }
    public Size size() {
        return size;
    }
    public int units() {
        return units;
    }
    public boolean hasAvailableUnits() {
        return units > 0;
    }
}

Design decision

The rule units > 0 is inside the value object instead of being duplicated in services or controllers.

This avoids an anemic model.

⸻

5.6 Stock

Represents the complete stock situation of a product.

Rules

* Stock cannot be null.
* Stock must contain at least one size.
* Stock cannot contain duplicated sizes.
* Stock entries must be immutable from outside the object.

Behavior

Stock is responsible for calculating:

* Number of available sizes.
* Number of total sizes.
* Stock availability ratio.

Formula

availabilityRatio = availableSizesCount / totalSizesCount

Example:

S: 4, M: 9, L: 0
availableSizesCount = 2
totalSizesCount = 3
availabilityRatio = 2 / 3 = 0.6667

Suggested implementation

public final class Stock {
    private final List<SizeStock> sizes;
    public Stock(List<SizeStock> sizes) {
        if (sizes == null || sizes.isEmpty()) {
            throw new IllegalArgumentException("Stock must contain at least one size");
        }
        if (containsDuplicatedSizes(sizes)) {
            throw new IllegalArgumentException("Stock cannot contain duplicated sizes");
        }
        this.sizes = Collections.unmodifiableList(new ArrayList<>(sizes));
    }
    private static boolean containsDuplicatedSizes(List<SizeStock> sizes) {
        return sizes.stream()
            .map(SizeStock::size)
            .distinct()
            .count() != sizes.size();
    }
    public List<SizeStock> sizes() {
        return sizes;
    }
    public long availableSizesCount() {
        return sizes.stream()
            .filter(SizeStock::hasAvailableUnits)
            .count();
    }
    public int totalSizesCount() {
        return sizes.size();
    }
    public double availabilityRatio() {
        return (double) availableSizesCount() / totalSizesCount();
    }
}

Design decision

A duplicated size, for example S appearing twice, would silently distort the availability ratio. The invariant is protected in the constructor so an inconsistent Stock can never exist.

The stock ratio calculation belongs to Stock, not to the ranking criterion itself.

The ranking criterion may decide how to transform this ratio into a score, but the concept of stock availability belongs to the stock model.

⸻

5.7 Score

Represents a calculated score.

Rules

* Cannot be NaN.
* Cannot be infinite.

Behavior

Score supports domain operations:

* Add another score.
* Multiply by a criterion weight.
* Compare with another score.

Suggested implementation

public final class Score implements Comparable<Score> {
    private final double value;
    public Score(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException("Score must be a valid number");
        }
        this.value = value;
    }
    public double value() {
        return value;
    }
    public Score multiplyBy(CriterionWeight weight) {
        return new Score(value * weight.value());
    }
    public Score add(Score other) {
        return new Score(value + other.value);
    }
    @Override
    public int compareTo(Score other) {
        return Double.compare(value, other.value);
    }
}

Design decision

Score is modeled as a value object instead of using raw double values across the domain.

This improves readability and prevents score-related logic from being scattered.

⸻

5.8 CriterionWeight

Represents the weight assigned to a ranking criterion.

Rules

* Cannot be negative.
* Can be zero.
* Can be decimal.

Suggested implementation

public final class CriterionWeight {
    private final double value;
    public CriterionWeight(double value) {
        if (value < 0) {
            throw new IllegalArgumentException("Criterion weight cannot be negative");
        }
        this.value = value;
    }
    public double value() {
        return value;
    }
}

Design decision

Weight 0 is valid because it allows clients to disable a criterion without removing it from the system.

Example:

{
  "weights": {
    "salesUnits": 1,
    "stockRatio": 0
  }
}

In this case, ranking is based only on sales units.

⸻

5.9 CriterionName

Represents the supported ranking criteria.

Initial values

public enum CriterionName {
    SALES_UNITS,
    STOCK_RATIO
}

⸻

5.10 RankingWeights

Represents the collection of weights used to rank products.

Rules

* Cannot be null.
* Cannot be empty.
* Missing criterion weights are treated as zero.

Suggested implementation

public final class RankingWeights {
    private final Map<CriterionName, CriterionWeight> weights;
    public RankingWeights(Map<CriterionName, CriterionWeight> weights) {
        if (weights == null || weights.isEmpty()) {
            throw new IllegalArgumentException("Ranking weights cannot be empty");
        }
        this.weights = Collections.unmodifiableMap(new EnumMap<>(weights));
    }
    public CriterionWeight weightFor(CriterionName criterionName) {
        return weights.getOrDefault(criterionName, new CriterionWeight(0));
    }
}

Design decision

If a new criterion is added in the future and the client does not send a weight for it, the system treats its weight as zero.

This makes the API backward compatible.

⸻

6. Ranking Criteria

Ranking criteria are modeled using the Strategy pattern.

Each criterion encapsulates its own scoring rule.

6.1 RankingCriterion

public interface RankingCriterion {
    CriterionName name();
    Score calculateScore(Product product);
}

Responsibilities

A ranking criterion must:

* Identify itself using a CriterionName.
* Calculate an unweighted score for a given product.

The weighting process is not performed by the criterion itself.

⸻

6.2 SalesUnitsCriterion

Scores a product based on its sales units.

Formula

score = product.salesUnits

Suggested implementation

public final class SalesUnitsCriterion implements RankingCriterion {
    @Override
    public CriterionName name() {
        return CriterionName.SALES_UNITS;
    }
    @Override
    public Score calculateScore(Product product) {
        return new Score(product.salesUnits().value());
    }
}

Example

Product sales units = 100
Sales units score = 100

⸻

6.3 StockRatioCriterion

Scores a product based on the ratio of sizes with available stock.

Formula

score = product.stock.availabilityRatio * 100

Examples

S: 4, M: 9, L: 0
available sizes = 2
total sizes = 3
availability ratio = 2 / 3
score = 66.67
S: 35, M: 9, L: 9
available sizes = 3
total sizes = 3
availability ratio = 1
score = 100

Suggested implementation

public final class StockRatioCriterion implements RankingCriterion {
    private static final int MAX_SCORE = 100;
    @Override
    public CriterionName name() {
        return CriterionName.STOCK_RATIO;
    }
    @Override
    public Score calculateScore(Product product) {
        return new Score(product.stock().availabilityRatio() * MAX_SCORE);
    }
}

Design decision

The stock ratio is scaled to 0..100 so that it can be combined more naturally with sales units.

Without scaling, stock ratio values would be between 0 and 1, making them almost irrelevant unless very large weights were used.

⸻

7. Domain Service

7.1 ProductRankingService

ProductRankingService is a domain service responsible for ranking products using the configured criteria and weights.

It exists because ranking is a business operation involving multiple products and multiple criteria.

Responsibilities

* Apply all registered ranking criteria to each product.
* Multiply each criterion score by its configured weight.
* Sum weighted scores into a final score.
* Sort products by final score descending.
* Return a ProductRanking.

Suggested implementation

public final class ProductRankingService {
    private final List<RankingCriterion> criteria;
    public ProductRankingService(List<RankingCriterion> criteria) {
        if (criteria == null || criteria.isEmpty()) {
            throw new IllegalArgumentException("At least one ranking criterion is required");
        }
        this.criteria = Collections.unmodifiableList(new ArrayList<>(criteria));
    }
    public ProductRanking rank(
        List<Product> products,
        RankingWeights rankingWeights
    ) {
        List<RankedProduct> rankedProducts = products.stream()
            .map(product -> rankProduct(product, rankingWeights))
            .sorted(RankedProduct.highestScoreFirst())
            .collect(Collectors.toList());
        return new ProductRanking(rankedProducts);
    }
    private RankedProduct rankProduct(
        Product product,
        RankingWeights rankingWeights
    ) {
        Score finalScore = criteria.stream()
            .map(criterion -> weightedScoreFor(product, criterion, rankingWeights))
            .reduce(new Score(0), Score::add);
        return new RankedProduct(product, finalScore);
    }
    private Score weightedScoreFor(
        Product product,
        RankingCriterion criterion,
        RankingWeights rankingWeights
    ) {
        CriterionWeight weight = rankingWeights.weightFor(criterion.name());
        return criterion.calculateScore(product).multiplyBy(weight);
    }
}

Design decision

The ranking service receives criteria through constructor injection.

This makes the ranking algorithm extensible and testable.

A new criterion can be added without modifying the ranking algorithm itself.

⸻

8. Ranked Product

8.1 RankedProduct

Represents a product with its calculated final score.

public final class RankedProduct {
    private final Product product;
    private final Score score;
    public RankedProduct(Product product, Score score) {
        this.product = Objects.requireNonNull(product);
        this.score = Objects.requireNonNull(score);
    }
    public Product product() {
        return product;
    }
    public Score score() {
        return score;
    }
    public static Comparator<RankedProduct> highestScoreFirst() {
        return Comparator.comparing(RankedProduct::score).reversed()
            .thenComparing(rankedProduct -> rankedProduct.product().id().value());
    }
}

Design decision

The comparator is exposed by the domain model using a meaningful name.

This avoids leaking comparison logic into services, controllers or tests.

The comparator includes a deterministic tie-breaker: when two products have the same score, the lower product id goes first. This keeps the ordering stable between executions and is aligned with the tie-breaking rule defined in the ranking algorithm specification (003).

Since ProductId is a String, the tie-breaker uses lexicographic order. With the exercise dataset (ids 1 to 6) this is equivalent to numeric order. See 003 section 9 for the documented trade-off.

⸻

9. Product Ranking

9.1 ProductRanking

Represents the final ordered result.

public final class ProductRanking {
    private final List<RankedProduct> products;
    public ProductRanking(List<RankedProduct> products) {
        this.products = Collections.unmodifiableList(
            new ArrayList<>(Objects.requireNonNull(products))
        );
    }
    public List<RankedProduct> products() {
        return products;
    }
}

Design decision

The ranking result is immutable.

Consumers cannot reorder or mutate the result accidentally.

⸻

10. Expected Ranking Example

Given the following products:

Id	Name	Sales Units	Stock
1	V-NECH BASIC SHIRT	100	S:4, M:9, L:0
2	CONTRASTING FABRIC T-SHIRT	50	S:35, M:9, L:9
3	RAISED PRINT T-SHIRT	80	S:20, M:2, L:20
4	PLEATED T-SHIRT	3	S:25, M:30, L:10
5	CONTRASTING LACE T-SHIRT	650	S:0, M:1, L:0
6	SLOGAN T-SHIRT	20	S:9, M:2, L:5

And the following weights:

{
  "salesUnits": 0.7,
  "stockRatio": 0.3
}

The expected score calculation is:

Id	Sales score	Stock ratio	Stock score	Final score
1	100	2/3	66.67	90.00
2	50	3/3	100.00	65.00
3	80	3/3	100.00	86.00
4	3	3/3	100.00	32.10
5	650	1/3	33.33	465.00
6	20	3/3	100.00	44.00

Expected order:

5, 1, 3, 2, 6, 4

⸻

11. Domain Test Cases

The following unit tests should be implemented for the domain layer.

ProductIdTest

* shouldCreateProductIdWhenValueIsValid
* shouldRejectNullProductId
* shouldRejectBlankProductId
* shouldBeEqualWhenValuesAreEqual

Note: value equality should be tested at least once per value object, since equals and hashCode are part of the value object contract.

ProductNameTest

* shouldCreateProductNameWhenValueIsValid
* shouldRejectNullProductName
* shouldRejectBlankProductName

SalesUnitsTest

* shouldCreateSalesUnitsWhenValueIsZero
* shouldCreateSalesUnitsWhenValueIsPositive
* shouldRejectNegativeSalesUnits

SizeStockTest

* shouldDetectAvailableStockWhenUnitsAreGreaterThanZero
* shouldDetectUnavailableStockWhenUnitsAreZero
* shouldRejectNegativeStockUnits

StockTest

* shouldCalculateFullAvailabilityRatio
* shouldCalculatePartialAvailabilityRatio
* shouldRejectEmptyStock
* shouldRejectDuplicatedSizes

ScoreTest

* shouldAddTwoScores
* shouldMultiplyScoreByCriterionWeight
* shouldRejectNanScore
* shouldRejectInfiniteScore

CriterionWeightTest

* shouldCreateCriterionWeightWhenValueIsZero
* shouldCreateCriterionWeightWhenValueIsPositive
* shouldRejectNegativeCriterionWeight

SalesUnitsCriterionTest

* shouldCalculateScoreUsingProductSalesUnits

StockRatioCriterionTest

* shouldCalculateScoreUsingAvailableSizeRatio

RankingWeightsTest

* shouldReturnConfiguredCriterionWeight
* shouldReturnZeroWhenCriterionWeightIsMissing
* shouldRejectEmptyWeights

ProductRankingServiceTest

* shouldRankProductsByWeightedScore
* shouldCalculateFinalScoreUsingAllConfiguredCriteria
* shouldUseZeroWeightWhenCriterionWeightIsMissing
* shouldReturnProductsOrderedByHighestScoreFirst
* shouldApplyDeterministicTieBreakerUsingProductId

⸻

12. Anti-Patterns To Avoid

12.1 Anemic Product

Avoid this:

public class Product {
    public String id;
    public String name;
    public int salesUnits;
    public Map<String, Integer> stock;
}

This object has data but no behavior.

Preferred approach:

product.stock().availabilityRatio();
product.salesUnits().value();

⸻

12.2 Ranking Logic In Controller

Avoid this:

@PostMapping("/ranking")
public List<ProductResponse> rankProducts(@RequestBody Request request) {
    return repository.findAll()
        .stream()
        .sorted(...)
        .collect(Collectors.toList());
}

Controllers must not contain business logic.

⸻

12.3 Mongo Annotations In Domain

Avoid this:

@Document(collection = "products")
public class Product {
}

The domain must not depend on MongoDB.

Use a separate ProductDocument in the infrastructure layer.

⸻

12.4 Primitive Obsession

Avoid spreading raw values everywhere:

double score;
double weight;
int sales;
Map<String, Integer> stock;

Prefer explicit value objects:

Score
CriterionWeight
SalesUnits
Stock
SizeStock

⸻

13. Extensibility

Adding a new ranking criterion should require minimal changes.

Example: MarginCriterion

Required changes:

1. Add new enum value:

MARGIN

2. Create a new criterion:

public final class MarginCriterion implements RankingCriterion {
    // implementation
}

3. Register it in configuration:

new MarginCriterion()

4. Optionally accept its weight in the REST request:

{
  "weights": {
    "salesUnits": 0.5,
    "stockRatio": 0.3,
    "margin": 0.2
  }
}

The following classes should not need to change:

* ProductRankingController
* RankProductsService
* ProductRankingService
* MongoProductRepository
* ProductRepository

The following classes are expected to change, and that is acceptable because they are small, localized infrastructure changes:

* CriterionName (new enum value)
* RankingWeightsRequest (new typed field)
* ProductRankingRestMapper (map the new field)

See 004 section 9.2 for the trade-off between typed request fields and a generic weights map.

⸻

14. Final Domain Design Summary

The domain model is composed of:

Product
 ├── ProductId
 ├── ProductName
 ├── SalesUnits
 └── Stock
      └── SizeStock
           └── Size

Ranking is handled by:

ProductRankingService
 ├── RankingCriterion
 │    ├── SalesUnitsCriterion
 │    └── StockRatioCriterion
 ├── RankingWeights
 ├── CriterionWeight
 ├── Score
 ├── RankedProduct
 └── ProductRanking

The domain layer is:

* Independent from Spring.
* Independent from MongoDB.
* Independent from REST.
* Rich in behavior.
* Easy to unit test.
* Open to new ranking criteria.
* Aligned with Hexagonal Architecture and tactical DDD principles.
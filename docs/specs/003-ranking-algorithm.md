003 - Ranking Algorithm Specification

Product Ranking Backend Tools

1. Purpose

This document defines the ranking algorithm used to order products in the product ranking service.

The algorithm calculates a final weighted score for each product using a set of ranking criteria and the weight assigned to each criterion.

The initial ranking criteria are:

* Sales units
* Stock ratio

The algorithm must be extensible so new criteria can be added in the future without modifying the ranking flow.

⸻

2. Business Requirement

Given a list of products displayed in a t-shirt category, the system must sort those products according to a weighted score.

Each ranking criterion produces an individual score.

Each criterion has an associated weight.

The final product score is calculated as the sum of all weighted criterion scores.

Products must be returned ordered by final score in descending order.

⸻

3. Formula Overview

The general formula is:

finalScore(product) =
    score(product, criterion1) * weight(criterion1)
  + score(product, criterion2) * weight(criterion2)
  + ...
  + score(product, criterionN) * weight(criterionN)

For the initial exercise:

finalScore(product) =
    salesUnitsScore(product) * salesUnitsWeight
  + stockRatioScore(product) * stockRatioWeight

⸻

4. Ranking Criteria

4.1 Sales Units Criterion

Business meaning

A product with more sold units should receive a higher score.

Input

Product.salesUnits

Formula

salesUnitsScore = product.salesUnits

Example

Product sales units = 100
salesUnitsScore = 100

Design decision

The sales units score is not normalized in the initial implementation.

Reason:

* The requirement states that the criterion gives a score based on the number of units sold.
* Using the sales value directly is simple and transparent.
* The weight allows clients to control the influence of this criterion.

If the business later requires a different scale, a normalized criterion can be added or the existing criterion can be changed after agreement.

⸻

4.2 Stock Ratio Criterion

Business meaning

A product available in more sizes should receive a higher score.

The criterion does not reward the total number of stock units.
It rewards size availability.

Example:

S: 100, M: 0, L: 0

This product has stock, but only in one size.

S: 1, M: 1, L: 1

This product has less total stock, but better size coverage.

The stock ratio criterion rewards the second case more highly.

⸻

Input

Product.stock

Each stock contains sizes and units:

S: units
M: units
L: units

⸻

Availability rule

A size is considered available when:

units > 0

A size is considered unavailable when:

units == 0

Negative stock is not allowed by the domain model.

⸻

Ratio formula

availabilityRatio = availableSizesCount / totalSizesCount

For the initial dataset:

totalSizesCount = 3

because the sizes are:

S, M, L

⸻

Score formula

stockRatioScore = availabilityRatio * 100

This produces a score between:

0 and 100

⸻

Examples

Example 1

S: 4
M: 9
L: 0
availableSizesCount = 2
totalSizesCount = 3
availabilityRatio = 2 / 3 = 0.6667
stockRatioScore = 0.6667 * 100 = 66.67

Example 2

S: 35
M: 9
L: 9
availableSizesCount = 3
totalSizesCount = 3
availabilityRatio = 3 / 3 = 1
stockRatioScore = 1 * 100 = 100

Example 3

S: 0
M: 1
L: 0
availableSizesCount = 1
totalSizesCount = 3
availabilityRatio = 1 / 3 = 0.3333
stockRatioScore = 0.3333 * 100 = 33.33

⸻

5. Weight Rules

Each criterion can receive a weight.

Rules

* Weight cannot be negative.
* Weight can be zero.
* Weight can be decimal.
* Missing criterion weight is treated as zero.
* The sum of weights does not need to be exactly 1.

⸻

5.1 Why weights do not need to sum 1

The system does not require this:

salesUnitsWeight + stockRatioWeight = 1

Reason:

* The requirement only says that each criterion has an associated weight.
* It does not state that weights must be percentages.
* Allowing arbitrary weights gives more flexibility.
* A client can amplify or disable criteria freely.

Valid example:

{
  "weights": {
    "salesUnits": 2,
    "stockRatio": 1
  }
}

Valid example:

{
  "weights": {
    "salesUnits": 1,
    "stockRatio": 0
  }
}

Invalid example:

{
  "weights": {
    "salesUnits": -1,
    "stockRatio": 0.5
  }
}

⸻

6. Missing Weight Behavior

If the request does not include a weight for a criterion, its weight is considered 0.

Example request:

{
  "weights": {
    "salesUnits": 1
  }
}

Equivalent internal weights:

SALES_UNITS = 1
STOCK_RATIO = 0

Design decision

This behavior makes the system backward compatible.

If new criteria are added in the future, existing clients do not break.

⸻

7. Final Score Calculation

For each product:

weightedSalesUnitsScore = salesUnitsScore * salesUnitsWeight
weightedStockRatioScore = stockRatioScore * stockRatioWeight
finalScore = weightedSalesUnitsScore + weightedStockRatioScore

⸻

8. Sorting Rule

Products are sorted by:

finalScore descending

Highest score first.

⸻

9. Tie-Breaking Rule

The exercise does not explicitly define a tie-breaking rule.

The recommended deterministic tie-breaker is:

1. Higher final score first
2. Lower product id first

Example:

Product 2 score = 100
Product 4 score = 100

Expected order:

Product 2 before Product 4

because product id 2 is lower than product id 4.

Design decision

A deterministic tie-breaker prevents unstable ordering between executions.

This is especially useful for tests and API consumers.

Note about ProductId comparison

ProductId is a String, so the tie-breaker uses lexicographic order. With the exercise dataset (ids 1 to 6) this is equivalent to numeric order. If ids with different lengths were introduced, for example 10 and 2, lexicographic order would differ from numeric order ("10" sorts before "2"). This is a documented and accepted trade-off for this exercise, since product ids are kept as opaque strings on purpose.

Note about sorting stability

Java sorting is stable. If the input list is already ordered by product id, a missing tie-breaker would go unnoticed because tied products would keep their input order. Tests for the tie-breaker must provide tied products in an order different from the expected output order.

⸻

10. Initial Dataset

The initial product dataset is:

Id	Name	Sales Units	Stock
1	V-NECH BASIC SHIRT	100	S:4, M:9, L:0
2	CONTRASTING FABRIC T-SHIRT	50	S:35, M:9, L:9
3	RAISED PRINT T-SHIRT	80	S:20, M:2, L:20
4	PLEATED T-SHIRT	3	S:25, M:30, L:10
5	CONTRASTING LACE T-SHIRT	650	S:0, M:1, L:0
6	SLOGAN T-SHIRT	20	S:9, M:2, L:5

⸻

11. Expected Calculation With Example Weights

Given this request:

{
  "weights": {
    "salesUnits": 0.7,
    "stockRatio": 0.3
  }
}

⸻

Product 1

Name: V-NECH BASIC SHIRT
Sales units: 100
Stock: S:4, M:9, L:0
salesUnitsScore = 100
stockRatioScore = 2 / 3 * 100 = 66.67
finalScore = 100 * 0.7 + 66.67 * 0.3
finalScore = 70 + 20
finalScore = 90.00

⸻

Product 2

Name: CONTRASTING FABRIC T-SHIRT
Sales units: 50
Stock: S:35, M:9, L:9
salesUnitsScore = 50
stockRatioScore = 3 / 3 * 100 = 100
finalScore = 50 * 0.7 + 100 * 0.3
finalScore = 35 + 30
finalScore = 65.00

⸻

Product 3

Name: RAISED PRINT T-SHIRT
Sales units: 80
Stock: S:20, M:2, L:20
salesUnitsScore = 80
stockRatioScore = 3 / 3 * 100 = 100
finalScore = 80 * 0.7 + 100 * 0.3
finalScore = 56 + 30
finalScore = 86.00

⸻

Product 4

Name: PLEATED T-SHIRT
Sales units: 3
Stock: S:25, M:30, L:10
salesUnitsScore = 3
stockRatioScore = 3 / 3 * 100 = 100
finalScore = 3 * 0.7 + 100 * 0.3
finalScore = 2.1 + 30
finalScore = 32.10

⸻

Product 5

Name: CONTRASTING LACE T-SHIRT
Sales units: 650
Stock: S:0, M:1, L:0
salesUnitsScore = 650
stockRatioScore = 1 / 3 * 100 = 33.33
finalScore = 650 * 0.7 + 33.33 * 0.3
finalScore = 455 + 10
finalScore = 465.00

⸻

Product 6

Name: SLOGAN T-SHIRT
Sales units: 20
Stock: S:9, M:2, L:5
salesUnitsScore = 20
stockRatioScore = 3 / 3 * 100 = 100
finalScore = 20 * 0.7 + 100 * 0.3
finalScore = 14 + 30
finalScore = 44.00

⸻

12. Expected Ordered Result

Expected order by final score descending:

Position	Product Id	Name	Final Score
1	5	CONTRASTING LACE T-SHIRT	465.00
2	1	V-NECH BASIC SHIRT	90.00
3	3	RAISED PRINT T-SHIRT	86.00
4	2	CONTRASTING FABRIC T-SHIRT	65.00
5	6	SLOGAN T-SHIRT	44.00
6	4	PLEATED T-SHIRT	32.10

Expected ids:

5, 1, 3, 2, 6, 4

⸻

13. Alternative Ranking Examples

13.1 Ranking only by sales units

Request:

{
  "weights": {
    "salesUnits": 1,
    "stockRatio": 0
  }
}

Expected order:

5, 1, 3, 2, 6, 4

Because sales units are:

650, 100, 80, 50, 20, 3

⸻

13.2 Ranking only by stock ratio

Request:

{
  "weights": {
    "salesUnits": 0,
    "stockRatio": 1
  }
}

Scores:

Product Id	Stock Ratio Score
1	66.67
2	100.00
3	100.00
4	100.00
5	33.33
6	100.00

Expected order using product id as tie-breaker:

2, 3, 4, 6, 1, 5

⸻

13.3 Ranking with missing stock ratio weight

Request:

{
  "weights": {
    "salesUnits": 1
  }
}

Internal interpretation:

salesUnitsWeight = 1
stockRatioWeight = 0

Expected order:

5, 1, 3, 2, 6, 4

⸻

14. Precision and Rounding

The domain should keep score values as double.

The REST response may expose scores rounded to two decimal places if desired.

Recommendation

Keep full precision internally.

Round only at the API response level.

Example:

Internal score: 89.999999999
Response score: 90.00

Design decision

Rounding is a presentation concern.

The domain should avoid formatting decisions.

⸻

15. Invalid Inputs

The algorithm must reject invalid values through domain validation.

Invalid sales units

salesUnits < 0

Expected behavior:

IllegalArgumentException

⸻

Invalid stock units

stock units < 0

Expected behavior:

IllegalArgumentException

⸻

Duplicated stock sizes

stock contains the same size twice

Expected behavior:

IllegalArgumentException

⸻

Invalid weight

criterionWeight < 0

Expected behavior:

IllegalArgumentException

⸻

Invalid score

score is NaN
score is infinite

Expected behavior:

IllegalArgumentException

⸻

15.1 Valid Edge Cases

The following inputs are valid and must not be rejected.

Empty product list

The ranking of an empty product list is an empty ranking.

The REST endpoint returns 200 OK with an empty products array.

All weights set to zero

{
  "weights": {
    "salesUnits": 0,
    "stockRatio": 0
  }
}

All final scores are 0 and products are ordered by the tie-breaker, product id ascending.

This is valid because the weights object is not empty: RankingWeights rejects an empty map, not a map whose values are zero. Disabling all criteria explicitly is a legitimate client decision.

⸻

16. Extensibility Rules

The ranking algorithm must not depend on concrete criteria.

Avoid:

double finalScore =
    product.salesUnits().value() * salesUnitsWeight
  + product.stock().availabilityRatio() * stockRatioWeight;

This would make the algorithm harder to extend.

Prefer:

Score finalScore = criteria.stream()
    .map(criterion -> weightedScoreFor(product, criterion, rankingWeights))
    .reduce(new Score(0), Score::add);

The algorithm should depend on:

RankingCriterion

not on:

SalesUnitsCriterion
StockRatioCriterion

⸻

17. Adding New Criteria

A new criterion must implement:

public interface RankingCriterion {
    CriterionName name();
    Score calculateScore(Product product);
}

Example:

public final class MarginCriterion implements RankingCriterion {
    @Override
    public CriterionName name() {
        return CriterionName.MARGIN;
    }
    @Override
    public Score calculateScore(Product product) {
        return new Score(calculateMarginScore(product));
    }
}

The ranking service should not require modification.

⸻

18. Algorithm Pseudocode

function rankProducts(products, criteria, weights):
    rankedProducts = []
    for each product in products:
        finalScore = 0
        for each criterion in criteria:
            criterionScore = criterion.calculateScore(product)
            criterionWeight = weights.weightFor(criterion.name)
            weightedScore = criterionScore * criterionWeight
            finalScore = finalScore + weightedScore
        rankedProducts.add(product, finalScore)
    sort rankedProducts by:
        finalScore descending
        productId ascending
    return rankedProducts

⸻

19. Java-Oriented Implementation

Recommended domain service structure:

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

⸻

20. Test Cases

20.1 SalesUnitsCriterionTest

shouldCalculateScoreUsingProductSalesUnits

Given:

Product sales units = 100

Expected:

Score = 100

⸻

20.2 StockRatioCriterionTest

shouldCalculateFullStockRatioScore

Given:

S: 1
M: 2
L: 3

Expected:

Score = 100

⸻

shouldCalculatePartialStockRatioScore

Given:

S: 1
M: 0
L: 3

Expected:

Score = 66.67

⸻

shouldCalculateMinimumStockRatioScore

Given:

S: 0
M: 0
L: 0

Expected:

Score = 0

⸻

20.3 ProductRankingServiceTest

shouldReturnProductsOrderedByHighestWeightedScore

Given:

products = initial dataset
salesUnitsWeight = 0.7
stockRatioWeight = 0.3

Expected order:

5, 1, 3, 2, 6, 4

⸻

shouldRankProductsOnlyBySalesUnitsWhenStockRatioWeightIsZero

Given:

salesUnitsWeight = 1
stockRatioWeight = 0

Expected order:

5, 1, 3, 2, 6, 4

⸻

shouldRankProductsOnlyByStockRatioWhenSalesUnitsWeightIsZero

Given:

salesUnitsWeight = 0
stockRatioWeight = 1

Expected order:

2, 3, 4, 6, 1, 5

⸻

shouldUseZeroWhenCriterionWeightIsMissing

Given:

weights = SALES_UNITS: 1

Expected:

STOCK_RATIO weight is treated as 0

⸻

shouldApplyDeterministicTieBreakerUsingProductId

Given:

Product 4 finalScore = 100
Product 2 finalScore = 100

And the input list provides product 4 before product 2.

Expected order:

2, 4

Important: the input order must differ from the expected output order. Java sorting is stable, so a pre-ordered input would make this test pass even without a tie-breaker.

⸻

21. Design Trade-Offs

21.1 Direct sales score vs normalized sales score

The chosen implementation uses:

salesUnitsScore = salesUnits

Alternative:

salesUnitsScore = productSales / maxSales * 100

The alternative would normalize all criteria to 0..100.

However, it requires knowing the full product set inside the sales criterion, which makes each criterion less independent.

For this exercise, direct sales score is simpler, clearer and aligned with the statement.

⸻

21.2 Stock ratio score scaling

The chosen implementation uses:

stockRatioScore = availabilityRatio * 100

Alternative:

stockRatioScore = availabilityRatio

The alternative would produce very small values between 0 and 1.

The chosen scaling makes weights easier to reason about.

⸻

21.3 Missing weights as zero

The chosen implementation treats missing weights as zero.

Alternative:

Reject request when a known criterion weight is missing.

The chosen behavior is more backward compatible and extensible.

⸻

21.4 double vs BigDecimal for Score

The chosen implementation uses double.

Reason:

* Scores are used only for ordering, not for monetary calculations.
* double precision is more than enough to sort products reliably.
* Rounding is applied only at the REST response level.

BigDecimal would add verbosity without a business benefit in this context. If scores ever acquire monetary semantics, the Score value object is the single place to change.

⸻

21.5 Strategy vs inheritance vs decorator

The exercise statement hints that ordering criteria can be approached with inheritance or decorators. The alternatives were considered explicitly.

Inheritance

An AbstractRankingCriterion base class with a template method. Rejected because there is no shared algorithm skeleton between criteria: each criterion is a self-contained scoring rule. Inheritance would create coupling without reuse.

Decorator

A WeightedCriterion decorator that wraps a criterion and applies its weight:

public final class WeightedCriterion implements RankingCriterion {
    private final RankingCriterion delegate;
    private final CriterionWeight weight;
    public WeightedCriterion(RankingCriterion delegate, CriterionWeight weight) {
        this.delegate = Objects.requireNonNull(delegate);
        this.weight = Objects.requireNonNull(weight);
    }
    @Override
    public CriterionName name() {
        return delegate.name();
    }
    @Override
    public Score calculateScore(Product product) {
        return delegate.calculateScore(product).multiplyBy(weight);
    }
}

This is a valid design. It was not chosen because weights arrive at runtime in each request, while criteria are assembled once at configuration time. Decorating criteria per request would force rebuilding the criterion list on every call, mixing request-scoped data with singleton strategies.

Chosen approach

Strategy for the scoring rules, plus weighting applied by the domain service using RankingWeights. Criteria remain stateless singletons and weights remain request-scoped data.

⸻

22. Interview Defense

A good explanation would be:

The ranking algorithm is implemented as a domain service. It does not know concrete scoring rules directly. Instead, it receives a list of RankingCriterion strategies. Each criterion calculates an unweighted score and the ranking service applies the configured weights.
This keeps the algorithm open to extension. If a new criterion is required, I only need to implement a new RankingCriterion and register it. The ranking service does not need to change.

About stock ratio:

The stock ratio criterion measures size coverage, not total stock units. A size counts as available when it has more than zero units. The ratio is then scaled to 0..100 so it can be combined more naturally with the sales units criterion.

About tests:

The expected ranking for the provided dataset and weights 0.7 and 0.3 is 5, 1, 3, 2, 6, 4. This becomes one of the main domain tests and also one of the E2E acceptance tests.

⸻

23. Final Summary

The algorithm is based on:

Final weighted score = sum of criterion score * criterion weight

Initial criteria:

Sales units score = product sales units
Stock ratio score = available sizes / total sizes * 100

Sorting:

Highest final score first
Tie-breaker by product id ascending

Expected result for the main example:

5, 1, 3, 2, 6, 4
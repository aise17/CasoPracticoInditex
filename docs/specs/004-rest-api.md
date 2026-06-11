004 - REST API Specification

Product Ranking Backend Tools

1. Purpose

This document defines the REST API contract for the product ranking service.

The API allows clients to request a ranked list of products by sending the weights assigned to each ranking criterion.

The REST layer must remain thin and must not contain business logic.

Its responsibility is to:

* Receive HTTP requests.
* Validate transport-level input.
* Map requests to application commands.
* Call the application use case.
* Map domain results to HTTP responses.
* Convert application or domain validation errors into HTTP responses.

⸻

2. REST Design Overview

The service exposes one endpoint:

POST /api/products/ranking

This endpoint receives criterion weights and returns products ordered by final weighted score.

⸻

3. HTTP Method

The endpoint uses POST.

Reason

Although the operation is read-oriented, the client sends a ranking configuration in the request body.

Using POST avoids encoding potentially complex ranking configuration in query parameters.

⸻

4. Endpoint

POST /api/products/ranking

Description

Returns all products ordered by their ranking score using the provided criterion weights.

⸻

5. Request Body

5.1 JSON Structure

{
  "weights": {
    "salesUnits": 0.7,
    "stockRatio": 0.3
  }
}

⸻

5.2 Fields

Field	Type	Required	Description
weights	object	yes	Contains the weights for each ranking criterion
weights.salesUnits	number	no	Weight for the sales units criterion
weights.stockRatio	number	no	Weight for the stock ratio criterion

⸻

5.3 Weight Rules

Each weight must follow these rules:

* Must be numeric.
* Must be greater than or equal to 0.
* Can be decimal.
* Can be 0.
* Does not need to sum to 1.

⸻

5.4 Missing Weights

If a known criterion is missing from the request, the application treats its weight as 0.

Example:

{
  "weights": {
    "salesUnits": 1
  }
}

Internal interpretation:

salesUnits = 1
stockRatio = 0

⸻

5.5 Empty Weights

An empty weights object is invalid.

Invalid example:

{
  "weights": {}
}

Expected result:

400 Bad Request

⸻

5.6 Unknown Criteria

If the request contains a weight for a criterion that the system does not support:

{
  "weights": {
    "salesUnits": 0.7,
    "margin": 0.3
  }
}

The unknown field is ignored and the ranking only uses the supported criteria.

Design decision

The request DTO declares one field per supported criterion, so unknown JSON fields are ignored by Jackson by default.

Trade-off:

* Ignoring unknown fields keeps the API tolerant and backward compatible.
* Rejecting unknown fields (fail-on-unknown-properties) would give earlier feedback for client typos.

The tolerant behavior is chosen and documented. Switching to strict validation is a one-line Jackson configuration if preferred.

Edge case: only unknown criteria

If after ignoring unknown fields no supported weight remains:

{
  "weights": {
    "margin": 0.3
  }
}

The effective weights map is empty, so the request is rejected:

400 Bad Request
{
  "message": "Ranking weights cannot be empty"
}

This is coherent: ignoring unknown criteria must not silently turn an unusable request into a default ranking.

⸻

6. Response Body

6.1 Successful Response

Status:

200 OK

Example:

{
  "products": [
    {
      "id": "5",
      "name": "CONTRASTING LACE T-SHIRT",
      "salesUnits": 650,
      "stock": {
        "S": 0,
        "M": 1,
        "L": 0
      },
      "score": 465.0
    },
    {
      "id": "1",
      "name": "V-NECH BASIC SHIRT",
      "salesUnits": 100,
      "stock": {
        "S": 4,
        "M": 9,
        "L": 0
      },
      "score": 90.0
    }
  ]
}

⸻

6.2 Response Fields

Field	Type	Description
products	array	Ranked products ordered by final score descending
products[].id	string	Product identifier
products[].name	string	Product name
products[].salesUnits	number	Number of units sold
products[].stock	object	Stock units by size
products[].stock.S	number	Stock units for size S
products[].stock.M	number	Stock units for size M
products[].stock.L	number	Stock units for size L
products[].score	number	Final weighted score

⸻

7. Example Requests

7.1 Ranking by sales and stock ratio

Request:

POST /api/products/ranking
Content-Type: application/json
{
  "weights": {
    "salesUnits": 0.7,
    "stockRatio": 0.3
  }
}

Expected product order:

5, 1, 3, 2, 6, 4

⸻

7.2 Ranking only by sales units

Request:

{
  "weights": {
    "salesUnits": 1,
    "stockRatio": 0
  }
}

Expected product order:

5, 1, 3, 2, 6, 4

⸻

7.3 Ranking only by stock ratio

Request:

{
  "weights": {
    "salesUnits": 0,
    "stockRatio": 1
  }
}

Expected product order:

2, 3, 4, 6, 1, 5

⸻

7.4 Ranking with missing stock ratio weight

Request:

{
  "weights": {
    "salesUnits": 1
  }
}

Expected product order:

5, 1, 3, 2, 6, 4

⸻

8. Error Responses

8.1 Generic Error Structure

All REST errors should follow this structure:

{
  "message": "Criterion weight cannot be negative"
}

Optional extended version:

{
  "message": "Criterion weight cannot be negative",
  "code": "INVALID_REQUEST"
}

For this exercise, the simple version is enough.

⸻

8.2 Negative Weight

Request:

{
  "weights": {
    "salesUnits": -1,
    "stockRatio": 0.3
  }
}

Response:

400 Bad Request
{
  "message": "Criterion weight cannot be negative"
}

⸻

8.3 Missing Weights Object

Request:

{}

Response:

400 Bad Request
{
  "message": "Ranking weights cannot be empty"
}

⸻

8.4 Empty Weights Object

Request:

{
  "weights": {}
}

Response:

400 Bad Request
{
  "message": "Ranking weights cannot be empty"
}

⸻

8.5 Malformed JSON

Request:

{
  "weights": {
    "salesUnits": 0.7,
    "stockRatio": 
  }
}

Response:

400 Bad Request
{
  "message": "Malformed JSON request"
}

⸻

8.6 Non-Numeric Weight

Request:

{
  "weights": {
    "salesUnits": "high",
    "stockRatio": 0.3
  }
}

Response:

400 Bad Request
{
  "message": "Malformed JSON request"
}

Note: a type mismatch inside the request body raises HttpMessageNotReadableException (Jackson fails to deserialize the value), so it is handled by the same handler as malformed JSON.

⸻

9. REST Models

REST models belong to infrastructure.

They must not be used in the domain layer.

⸻

9.1 ProductRankingRequest

Suggested implementation:

public class ProductRankingRequest {
    private RankingWeightsRequest weights;
    public RankingWeightsRequest getWeights() {
        return weights;
    }
    public void setWeights(RankingWeightsRequest weights) {
        this.weights = weights;
    }
}

Design decision

Bean Validation annotations are intentionally not used on this request.

The missing weights case is validated by the REST mapper, which throws IllegalArgumentException with the same message as the domain rule ("Ranking weights cannot be empty"). This keeps a single validation path and a single source for the error message, instead of duplicating validation between Bean Validation and the mapper. It also avoids dead code: a @NotNull annotation would intercept the request before the mapper null-check could ever run.

⸻

9.2 RankingWeightsRequest

Suggested implementation:

public class RankingWeightsRequest {
    private BigDecimal salesUnits;
    private BigDecimal stockRatio;
    public BigDecimal getSalesUnits() {
        return salesUnits;
    }
    public void setSalesUnits(BigDecimal salesUnits) {
        this.salesUnits = salesUnits;
    }
    public BigDecimal getStockRatio() {
        return stockRatio;
    }
    public void setStockRatio(BigDecimal stockRatio) {
        this.stockRatio = stockRatio;
    }
}

Design decision

The REST request uses BigDecimal to receive numeric JSON values safely.

The mapper converts them into domain CriterionWeight.

Design decision: typed fields vs generic map

RankingWeightsRequest declares one typed field per criterion instead of a generic Map<String, BigDecimal>.

Trade-off:

* Typed fields make the contract explicit, self-documenting and easy to expose in OpenAPI.
* A generic map would allow adding new criteria without touching the REST DTO and mapper.

Typed fields were chosen because the API contract should be explicit. Adding a new criterion already requires a new enum value and a new criterion class, so extending the DTO and the mapper is a small, localized change inside the infrastructure layer.

⸻

9.3 ProductRankingResponse

public class ProductRankingResponse {
    private final List<RankedProductResponse> products;
    public ProductRankingResponse(List<RankedProductResponse> products) {
        this.products = products;
    }
    public List<RankedProductResponse> getProducts() {
        return products;
    }
}

⸻

9.4 RankedProductResponse

public class RankedProductResponse {
    private final String id;
    private final String name;
    private final int salesUnits;
    private final Map<String, Integer> stock;
    private final double score;
    public RankedProductResponse(
        String id,
        String name,
        int salesUnits,
        Map<String, Integer> stock,
        double score
    ) {
        this.id = id;
        this.name = name;
        this.salesUnits = salesUnits;
        this.stock = stock;
        this.score = score;
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
    public double getScore() {
        return score;
    }
}

⸻

10. REST Mapper

10.1 Responsibility

ProductRankingRestMapper converts:

ProductRankingRequest -> RankProductsCommand
ProductRanking -> ProductRankingResponse

It isolates the REST model from the application and domain model.

⸻

10.2 Suggested implementation

@Component
public class ProductRankingRestMapper {
    public RankProductsCommand toCommand(ProductRankingRequest request) {
        Map<CriterionName, CriterionWeight> weights = new EnumMap<>(CriterionName.class);
        if (request.getWeights() == null) {
            throw new IllegalArgumentException("Ranking weights cannot be empty");
        }
        addWeightIfPresent(
            weights,
            CriterionName.SALES_UNITS,
            request.getWeights().getSalesUnits()
        );
        addWeightIfPresent(
            weights,
            CriterionName.STOCK_RATIO,
            request.getWeights().getStockRatio()
        );
        return new RankProductsCommand(new RankingWeights(weights));
    }
    private void addWeightIfPresent(
        Map<CriterionName, CriterionWeight> weights,
        CriterionName criterionName,
        BigDecimal value
    ) {
        if (value != null) {
            weights.put(
                criterionName,
                new CriterionWeight(value.doubleValue())
            );
        }
    }
    public ProductRankingResponse toResponse(ProductRanking ranking) {
        List<RankedProductResponse> products = ranking.products()
            .stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
        return new ProductRankingResponse(products);
    }
    private RankedProductResponse toResponse(RankedProduct rankedProduct) {
        Product product = rankedProduct.product();
        return new RankedProductResponse(
            product.id().value(),
            product.name().value(),
            product.salesUnits().value(),
            toStockResponse(product.stock()),
            roundScore(rankedProduct.score().value())
        );
    }
    private Map<String, Integer> toStockResponse(Stock stock) {
        return stock.sizes()
            .stream()
            .collect(Collectors.toMap(
                sizeStock -> sizeStock.size().name(),
                SizeStock::units,
                (first, second) -> first,
                LinkedHashMap::new
            ));
    }
    private double roundScore(double score) {
        return BigDecimal.valueOf(score)
            .setScale(2, RoundingMode.HALF_UP)
            .doubleValue();
    }
}

⸻

11. Controller

11.1 Responsibility

The controller exposes the REST endpoint.

It should not calculate ranking scores.

Suggested implementation:

@RestController
@RequestMapping("/api/products")
public class ProductRankingController {
    private final RankProductsUseCase rankProductsUseCase;
    private final ProductRankingRestMapper mapper;
    public ProductRankingController(
        RankProductsUseCase rankProductsUseCase,
        ProductRankingRestMapper mapper
    ) {
        this.rankProductsUseCase = rankProductsUseCase;
        this.mapper = mapper;
    }
    @PostMapping("/ranking")
    public ProductRankingResponse rankProducts(
        @RequestBody ProductRankingRequest request
    ) {
        RankProductsCommand command = mapper.toCommand(request);
        ProductRanking ranking = rankProductsUseCase.rankProducts(command);
        return mapper.toResponse(ranking);
    }
}

⸻

12. Error Handling

12.1 RestExceptionHandler

Suggested implementation:

@RestControllerAdvice
public class RestExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleIllegalArgumentException(
        IllegalArgumentException exception
    ) {
        return new ErrorResponse(exception.getMessage());
    }
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleHttpMessageNotReadableException() {
        return new ErrorResponse("Malformed JSON request");
    }
}

Note: there is no MethodArgumentNotValidException handler because Bean Validation is not used. Missing weights are detected by the REST mapper and surface as IllegalArgumentException. Every handler in this class is exercised by at least one test, so there is no unused code.

⸻

12.2 ErrorResponse

public class ErrorResponse {
    private final String message;
    public ErrorResponse(String message) {
        this.message = message;
    }
    public String getMessage() {
        return message;
    }
}

⸻

13. Validation Strategy

There are two validation levels:

Transport validation
Domain validation

⸻

13.1 Transport Validation

Transport validation checks whether the HTTP request is structurally valid.

Examples:

* JSON is readable.
* Numeric fields are parseable.
* weights object exists.

Handled by:

RestExceptionHandler (unreadable JSON, type mismatches)
ProductRankingRestMapper (missing weights object)

⸻

13.2 Domain Validation

Domain validation checks business rules.

Examples:

* Weight cannot be negative.
* Ranking weights cannot be empty.
* Score cannot be NaN.
* Product stock cannot be empty.

Handled by:

CriterionWeight
RankingWeights
Score
Stock
SalesUnits

⸻

14. API Should Not Expose Domain Internals

The response can expose useful information such as:

id
name
salesUnits
stock
score

But it should not expose internal domain classes or implementation details such as:

CriterionName enum values
Value object wrappers
Internal Java class names
MongoDB document structure

⸻

15. OpenAPI Documentation

Optionally, the project can include OpenAPI using Springdoc.

Dependency:

<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.5.0</version>
</dependency>

Access:

GET /swagger-ui.html

This is optional for the exercise but useful for presentation.

⸻

16. cURL Examples

16.1 Main example

curl -X POST http://localhost:8080/api/products/ranking \
  -H "Content-Type: application/json" \
  -d '{
    "weights": {
      "salesUnits": 0.7,
      "stockRatio": 0.3
    }
  }'

⸻

16.2 Sales only

curl -X POST http://localhost:8080/api/products/ranking \
  -H "Content-Type: application/json" \
  -d '{
    "weights": {
      "salesUnits": 1,
      "stockRatio": 0
    }
  }'

⸻

16.3 Stock only

curl -X POST http://localhost:8080/api/products/ranking \
  -H "Content-Type: application/json" \
  -d '{
    "weights": {
      "salesUnits": 0,
      "stockRatio": 1
    }
  }'

⸻

17. E2E Acceptance Criteria

The endpoint must satisfy the following acceptance criteria.

⸻

AC1 - Rank products using sales and stock ratio weights

Given:

{
  "weights": {
    "salesUnits": 0.7,
    "stockRatio": 0.3
  }
}

When:

POST /api/products/ranking

Then:

200 OK

And product ids are returned in this order:

5, 1, 3, 2, 6, 4

⸻

AC2 - Rank products only by stock ratio

Given:

{
  "weights": {
    "salesUnits": 0,
    "stockRatio": 1
  }
}

Then product ids are returned in this order:

2, 3, 4, 6, 1, 5

⸻

AC3 - Reject negative weight

Given:

{
  "weights": {
    "salesUnits": -1
  }
}

Then:

400 Bad Request

⸻

AC4 - Reject empty weights

Given:

{
  "weights": {}
}

Then:

400 Bad Request

⸻

AC5 - Reject malformed JSON

Given a malformed JSON body.

Then:

400 Bad Request

⸻

AC6 - Return empty product list when no products exist

Given no products are stored in the database.

And a valid weights request.

When:

POST /api/products/ranking

Then:

200 OK

And products is an empty array.

⸻

AC7 - Ignore unknown criterion weights

Given:

{
  "weights": {
    "salesUnits": 1,
    "margin": 5
  }
}

Then:

200 OK

And the ranking uses only the sales units criterion, producing the order:

5, 1, 3, 2, 6, 4

⸻

18. REST Layer Smells To Avoid

18.1 Business logic in controller

Avoid:

products.sort(...)

inside the controller.

⸻

18.2 Returning Mongo documents

Avoid:

public List<ProductDocument> rankProducts(...)

⸻

18.3 Passing request DTOs into application service

Avoid:

rankProductsUseCase.rankProducts(request);

⸻

18.4 Using Spanish names in code

Avoid:

ProductoResponse
unidadesVendidas
ordenarProductos

Prefer:

ProductRankingResponse
salesUnits
rankProducts

⸻

19. Interview Defense

A good explanation would be:

The REST layer is only an adapter. It receives the HTTP request, maps it into an application command and delegates the ranking operation to the input port.
The controller does not calculate scores, does not sort products and does not know anything about MongoDB. This keeps the REST adapter thin and aligned with Hexagonal Architecture.

About request and command separation:

I do not pass the REST request object directly into the use case because it is a transport model. The application receives a command expressed in application and domain terms.

About errors:

Domain validation errors are converted into HTTP 400 responses by a REST exception handler. The domain does not throw HTTP-specific exceptions.

⸻

20. Final Summary

The REST API exposes:

POST /api/products/ranking

Input:

{
  "weights": {
    "salesUnits": 0.7,
    "stockRatio": 0.3
  }
}

Output:

{
  "products": [
    {
      "id": "5",
      "name": "CONTRASTING LACE T-SHIRT",
      "salesUnits": 650,
      "stock": {
        "S": 0,
        "M": 1,
        "L": 0
      },
      "score": 465.0
    }
  ]
}

Main architectural rule:

REST is an adapter, not the place where business decisions are made.
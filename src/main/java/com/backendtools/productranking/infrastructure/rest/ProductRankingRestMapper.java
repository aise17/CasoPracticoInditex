package com.backendtools.productranking.infrastructure.rest;

import com.backendtools.productranking.application.usecase.RankProductsCommand;
import com.backendtools.productranking.domain.model.CriterionWeight;
import com.backendtools.productranking.domain.model.Product;
import com.backendtools.productranking.domain.model.ProductRanking;
import com.backendtools.productranking.domain.model.RankedProduct;
import com.backendtools.productranking.domain.model.RankingWeights;
import com.backendtools.productranking.domain.model.SizeStock;
import com.backendtools.productranking.domain.model.Stock;
import com.backendtools.productranking.domain.ranking.CriterionName;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ProductRankingRestMapper {

    public RankProductsCommand toCommand(ProductRankingRequest request) {
        if (request.getWeights() == null) {
            throw new IllegalArgumentException("Ranking weights cannot be empty");
        }
        Map<CriterionName, CriterionWeight> weights = new EnumMap<>(CriterionName.class);
        addWeightIfPresent(weights, CriterionName.SALES_UNITS, request.getWeights().getSalesUnits());
        addWeightIfPresent(weights, CriterionName.STOCK_RATIO, request.getWeights().getStockRatio());
        return new RankProductsCommand(new RankingWeights(weights));
    }

    private void addWeightIfPresent(
        Map<CriterionName, CriterionWeight> weights,
        CriterionName criterionName,
        BigDecimal value
    ) {
        if (value != null) {
            weights.put(criterionName, new CriterionWeight(value.doubleValue()));
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

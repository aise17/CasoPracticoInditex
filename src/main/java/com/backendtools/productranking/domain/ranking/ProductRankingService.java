package com.backendtools.productranking.domain.ranking;

import com.backendtools.productranking.domain.model.CriterionWeight;
import com.backendtools.productranking.domain.model.Product;
import com.backendtools.productranking.domain.model.ProductRanking;
import com.backendtools.productranking.domain.model.RankedProduct;
import com.backendtools.productranking.domain.model.RankingWeights;
import com.backendtools.productranking.domain.model.Score;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public final class ProductRankingService {

    private final List<RankingCriterion> criteria;

    public ProductRankingService(List<RankingCriterion> criteria) {
        if (criteria == null || criteria.isEmpty()) {
            throw new IllegalArgumentException("At least one ranking criterion is required");
        }
        this.criteria = Collections.unmodifiableList(new ArrayList<>(criteria));
    }

    public ProductRanking rank(List<Product> products, RankingWeights rankingWeights) {
        List<RankedProduct> rankedProducts = products.stream()
            .map(product -> rankProduct(product, rankingWeights))
            .sorted(RankedProduct.highestScoreFirst())
            .collect(Collectors.toList());
        return new ProductRanking(rankedProducts);
    }

    private RankedProduct rankProduct(Product product, RankingWeights rankingWeights) {
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

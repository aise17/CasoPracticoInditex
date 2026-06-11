package com.backendtools.productranking.domain.ranking;

import com.backendtools.productranking.domain.model.Product;
import com.backendtools.productranking.domain.model.Score;

public interface RankingCriterion {

    CriterionName name();

    Score calculateScore(Product product);
}

package com.backendtools.productranking.application.port.in;

import com.backendtools.productranking.application.usecase.RankProductsCommand;
import com.backendtools.productranking.domain.model.ProductRanking;

public interface RankProductsUseCase {

    ProductRanking rankProducts(RankProductsCommand command);
}

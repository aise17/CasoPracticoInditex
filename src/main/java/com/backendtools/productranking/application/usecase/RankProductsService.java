package com.backendtools.productranking.application.usecase;

import com.backendtools.productranking.application.port.in.RankProductsUseCase;
import com.backendtools.productranking.application.port.out.ProductRepository;
import com.backendtools.productranking.domain.model.Product;
import com.backendtools.productranking.domain.model.ProductRanking;
import com.backendtools.productranking.domain.ranking.ProductRankingService;

import java.util.List;
import java.util.Objects;

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

package com.backendtools.productranking.infrastructure.configuration;

import com.backendtools.productranking.application.port.in.RankProductsUseCase;
import com.backendtools.productranking.application.port.out.ProductRepository;
import com.backendtools.productranking.application.usecase.RankProductsService;
import com.backendtools.productranking.domain.ranking.ProductRankingService;
import com.backendtools.productranking.domain.ranking.SalesUnitsCriterion;
import com.backendtools.productranking.domain.ranking.StockRatioCriterion;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

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
        return new RankProductsService(productRepository, productRankingService);
    }
}

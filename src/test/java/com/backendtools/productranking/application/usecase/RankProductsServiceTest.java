package com.backendtools.productranking.application.usecase;

import com.backendtools.productranking.domain.model.ProductRanking;
import com.backendtools.productranking.domain.model.ProductTestMother;
import com.backendtools.productranking.domain.model.RankingWeightsTestMother;
import com.backendtools.productranking.domain.ranking.ProductRankingServiceTestMother;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class RankProductsServiceTest {

    @Test
    void shouldLoadProductsAndDelegateRankingToDomainService() {
        RankProductsService service = new RankProductsService(
            new InMemoryProductRepository(ProductTestMother.initialDataset()),
            ProductRankingServiceTestMother.defaultService()
        );
        RankProductsCommand command = new RankProductsCommand(
            RankingWeightsTestMother.weights(0.7, 0.3)
        );

        ProductRanking ranking = service.rankProducts(command);

        assertThat(ranking.products())
            .extracting(rankedProduct -> rankedProduct.product().id().value())
            .containsExactly("5", "1", "3", "2", "6", "4");
    }

    @Test
    void shouldReturnEmptyRankingWhenRepositoryHasNoProducts() {
        RankProductsService service = new RankProductsService(
            new InMemoryProductRepository(Collections.emptyList()),
            ProductRankingServiceTestMother.defaultService()
        );
        RankProductsCommand command = new RankProductsCommand(
            RankingWeightsTestMother.weights(0.7, 0.3)
        );

        ProductRanking ranking = service.rankProducts(command);

        assertThat(ranking.products()).isEmpty();
    }
}

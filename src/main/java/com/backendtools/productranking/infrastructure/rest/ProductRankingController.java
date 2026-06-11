package com.backendtools.productranking.infrastructure.rest;

import com.backendtools.productranking.application.port.in.RankProductsUseCase;
import com.backendtools.productranking.application.usecase.RankProductsCommand;
import com.backendtools.productranking.domain.model.ProductRanking;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public ProductRankingResponse rankProducts(@RequestBody ProductRankingRequest request) {
        RankProductsCommand command = mapper.toCommand(request);
        ProductRanking ranking = rankProductsUseCase.rankProducts(command);
        return mapper.toResponse(ranking);
    }
}

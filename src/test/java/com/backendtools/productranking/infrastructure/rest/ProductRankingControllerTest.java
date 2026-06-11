package com.backendtools.productranking.infrastructure.rest;

import com.backendtools.productranking.application.port.in.RankProductsUseCase;
import com.backendtools.productranking.domain.model.ProductRanking;
import com.backendtools.productranking.domain.model.ProductTestMother;
import com.backendtools.productranking.domain.model.RankedProduct;
import com.backendtools.productranking.domain.model.Score;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Objects;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductRankingController.class)
@Import(ProductRankingRestMapper.class)
class ProductRankingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RankProductsUseCase rankProductsUseCase;

    @Test
    void shouldReturnOkWhenRequestIsValid() throws Exception {
        when(rankProductsUseCase.rankProducts(any())).thenReturn(new ProductRanking(
            Collections.singletonList(new RankedProduct(
                ProductTestMother.product("1", "V-NECH BASIC SHIRT", 100, 4, 9, 0),
                new Score(90)
            ))
        ));

        mockMvc.perform(post("/api/products/ranking")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content("""
                    {"weights":{"salesUnits":0.7,"stockRatio":0.3}}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.products[0].id").value("1"))
            .andExpect(jsonPath("$.products[0].score").value(90.0));
    }

    @Test
    void shouldReturnBadRequestWhenWeightsAreMissing() throws Exception {
        mockMvc.perform(post("/api/products/ranking")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Ranking weights cannot be empty"));
    }

    @Test
    void shouldReturnBadRequestWhenJsonIsMalformed() throws Exception {
        mockMvc.perform(post("/api/products/ranking")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content("{\"weights\":{\"salesUnits\":}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Malformed JSON request"));
    }
}

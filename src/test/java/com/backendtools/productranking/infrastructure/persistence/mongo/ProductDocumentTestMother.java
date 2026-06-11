package com.backendtools.productranking.infrastructure.persistence.mongo;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ProductDocumentTestMother {

    private ProductDocumentTestMother() {
    }

    public static ProductDocument product(
        String id,
        String name,
        int salesUnits,
        int smallStock,
        int mediumStock,
        int largeStock
    ) {
        Map<String, Integer> stock = new LinkedHashMap<>();
        stock.put("S", smallStock);
        stock.put("M", mediumStock);
        stock.put("L", largeStock);
        return new ProductDocument(id, name, salesUnits, stock);
    }

    public static List<ProductDocument> initialDataset() {
        return Arrays.asList(
            product("1", "V-NECH BASIC SHIRT", 100, 4, 9, 0),
            product("2", "CONTRASTING FABRIC T-SHIRT", 50, 35, 9, 9),
            product("3", "RAISED PRINT T-SHIRT", 80, 20, 2, 20),
            product("4", "PLEATED T-SHIRT", 3, 25, 30, 10),
            product("5", "CONTRASTING LACE T-SHIRT", 650, 0, 1, 0),
            product("6", "SLOGAN T-SHIRT", 20, 9, 2, 5)
        );
    }
}

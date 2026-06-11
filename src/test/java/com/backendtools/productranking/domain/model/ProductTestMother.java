package com.backendtools.productranking.domain.model;

import java.util.Arrays;
import java.util.List;

public final class ProductTestMother {

    private ProductTestMother() {
    }

    public static Product product(
        String id,
        String name,
        int salesUnits,
        int smallStock,
        int mediumStock,
        int largeStock
    ) {
        return new Product(
            new ProductId(id),
            new ProductName(name),
            new SalesUnits(salesUnits),
            stock(smallStock, mediumStock, largeStock)
        );
    }

    public static Product productWithSalesUnits(int salesUnits) {
        return product("1", "Product", salesUnits, 1, 1, 1);
    }

    public static Product productWithStock(int smallStock, int mediumStock, int largeStock) {
        return product("1", "Product", 100, smallStock, mediumStock, largeStock);
    }

    public static List<Product> initialDataset() {
        return Arrays.asList(
            product("1", "V-NECH BASIC SHIRT", 100, 4, 9, 0),
            product("2", "CONTRASTING FABRIC T-SHIRT", 50, 35, 9, 9),
            product("3", "RAISED PRINT T-SHIRT", 80, 20, 2, 20),
            product("4", "PLEATED T-SHIRT", 3, 25, 30, 10),
            product("5", "CONTRASTING LACE T-SHIRT", 650, 0, 1, 0),
            product("6", "SLOGAN T-SHIRT", 20, 9, 2, 5)
        );
    }

    private static Stock stock(int smallStock, int mediumStock, int largeStock) {
        return new Stock(Arrays.asList(
            new SizeStock(Size.S, smallStock),
            new SizeStock(Size.M, mediumStock),
            new SizeStock(Size.L, largeStock)
        ));
    }
}

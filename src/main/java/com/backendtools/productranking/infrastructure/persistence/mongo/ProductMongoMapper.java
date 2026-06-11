package com.backendtools.productranking.infrastructure.persistence.mongo;

import com.backendtools.productranking.domain.model.Product;
import com.backendtools.productranking.domain.model.ProductId;
import com.backendtools.productranking.domain.model.ProductName;
import com.backendtools.productranking.domain.model.SalesUnits;
import com.backendtools.productranking.domain.model.Size;
import com.backendtools.productranking.domain.model.SizeStock;
import com.backendtools.productranking.domain.model.Stock;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ProductMongoMapper {

    public Product toDomain(ProductDocument document) {
        return new Product(
            new ProductId(document.getId()),
            new ProductName(document.getName()),
            new SalesUnits(document.getSalesUnits()),
            toDomainStock(document.getStock())
        );
    }

    private Stock toDomainStock(Map<String, Integer> stock) {
        List<SizeStock> sizeStocks = stock.entrySet()
            .stream()
            .map(entry -> new SizeStock(Size.valueOf(entry.getKey()), entry.getValue()))
            .collect(Collectors.toList());
        return new Stock(sizeStocks);
    }

    public ProductDocument toDocument(Product product) {
        return new ProductDocument(
            product.id().value(),
            product.name().value(),
            product.salesUnits().value(),
            toDocumentStock(product.stock())
        );
    }

    private Map<String, Integer> toDocumentStock(Stock stock) {
        return stock.sizes()
            .stream()
            .collect(Collectors.toMap(
                sizeStock -> sizeStock.size().name(),
                SizeStock::units,
                (first, second) -> first,
                LinkedHashMap::new
            ));
    }
}

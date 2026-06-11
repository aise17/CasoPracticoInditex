package com.backendtools.productranking.infrastructure.persistence.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;

@Document(collection = "products")
public class ProductDocument {

    @Id
    private String id;
    private String name;
    private int salesUnits;
    private Map<String, Integer> stock;

    protected ProductDocument() {
    }

    public ProductDocument(String id, String name, int salesUnits, Map<String, Integer> stock) {
        this.id = id;
        this.name = name;
        this.salesUnits = salesUnits;
        this.stock = stock;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getSalesUnits() {
        return salesUnits;
    }

    public Map<String, Integer> getStock() {
        return stock;
    }
}

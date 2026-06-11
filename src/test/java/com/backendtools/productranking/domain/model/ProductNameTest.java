package com.backendtools.productranking.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductNameTest {

    @Test
    void shouldCreateProductNameWhenValueIsValid() {
        ProductName productName = new ProductName("V-NECH BASIC SHIRT");

        assertThat(productName.value()).isEqualTo("V-NECH BASIC SHIRT");
    }

    @Test
    void shouldRejectNullProductName() {
        assertThatThrownBy(() -> new ProductName(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Product name cannot be empty");
    }

    @Test
    void shouldRejectBlankProductName() {
        assertThatThrownBy(() -> new ProductName(" "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Product name cannot be empty");
    }

    @Test
    void shouldBeEqualWhenValuesAreEqual() {
        assertThat(new ProductName("Product")).isEqualTo(new ProductName("Product"));
    }
}

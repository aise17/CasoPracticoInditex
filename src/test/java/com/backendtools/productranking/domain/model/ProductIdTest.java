package com.backendtools.productranking.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductIdTest {

    @Test
    void shouldCreateProductIdWhenValueIsValid() {
        ProductId productId = new ProductId("1");

        assertThat(productId.value()).isEqualTo("1");
    }

    @Test
    void shouldRejectNullProductId() {
        assertThatThrownBy(() -> new ProductId(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Product id cannot be empty");
    }

    @Test
    void shouldRejectBlankProductId() {
        assertThatThrownBy(() -> new ProductId(" "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Product id cannot be empty");
    }

    @Test
    void shouldBeEqualWhenValuesAreEqual() {
        assertThat(new ProductId("1")).isEqualTo(new ProductId("1"));
    }
}

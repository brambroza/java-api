package com.enterprise.oms.catalog.application;

import com.enterprise.oms.catalog.domain.Product;
import com.enterprise.oms.shared.domain.Money;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class ProductDtos {

    private ProductDtos() {
    }

    public record CreateProductRequest(
            @NotBlank @Size(max = 64) @Pattern(regexp = "[A-Za-z0-9-]+") String sku,
            @NotBlank @Size(max = 200) String name,
            @Size(max = 2000) String description,
            @NotNull @DecimalMin("0.0") @Digits(integer = 15, fraction = 4) BigDecimal price,
            @NotBlank @Pattern(regexp = "[A-Za-z]{3}") String currency,
            @PositiveOrZero int initialStock) {
    }

    public record ChangePriceRequest(
            @NotNull @DecimalMin("0.0") @Digits(integer = 15, fraction = 4) BigDecimal price,
            @NotBlank @Pattern(regexp = "[A-Za-z]{3}") String currency) {
    }

    public record ProductResponse(UUID id, String sku, String name, String description, Money price, boolean active,
                                  Instant createdAt, Instant updatedAt) {

        public static ProductResponse from(Product product) {
            return new ProductResponse(product.getId(), product.getSku(), product.getName(), product.getDescription(),
                    product.getPrice(), product.isActive(), product.getCreatedAt(), product.getUpdatedAt());
        }
    }

    /** Read model handed to other modules (ordering) so they never touch the Product entity directly. */
    public record ProductSnapshot(UUID id, String sku, String name, Money price, boolean active) {

        public static ProductSnapshot from(Product product) {
            return new ProductSnapshot(product.getId(), product.getSku(), product.getName(), product.getPrice(), product.isActive());
        }
    }
}

package ma.bench.rest.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record ItemRequest(
        @NotBlank String sku,
        @NotBlank String name,
        @NotNull BigDecimal price,
        @NotNull @PositiveOrZero Integer stock,
        @NotNull Long categoryId) {
}

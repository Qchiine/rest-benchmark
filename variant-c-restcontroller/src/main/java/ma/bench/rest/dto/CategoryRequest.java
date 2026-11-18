package ma.bench.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record CategoryRequest(
        @NotBlank String code,
        @NotBlank String name) {
}

package ma.bench.jersey.dto;

import java.math.BigDecimal;

public record ItemPayload(
        String sku,
        String name,
        BigDecimal price,
        Integer stock,
        Long categoryId) {

    public boolean estComplet() {
        return sku != null && !sku.isBlank()
                && name != null && !name.isBlank()
                && price != null
                && stock != null && stock >= 0
                && categoryId != null;
    }
}

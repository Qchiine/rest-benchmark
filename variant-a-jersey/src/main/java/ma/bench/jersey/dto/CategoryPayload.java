package ma.bench.jersey.dto;

public record CategoryPayload(String code, String name) {

    public boolean estComplet() {
        return code != null && !code.isBlank() && name != null && !name.isBlank();
    }
}

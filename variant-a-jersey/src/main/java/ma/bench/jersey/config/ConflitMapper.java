package ma.bench.jersey.config;

import java.sql.SQLException;

import org.hibernate.exception.ConstraintViolationException;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Traduit une violation de contrainte d'unicite en 409, comme les variantes C et D.
 * Le type declare est ConstraintViolationException pour ne pas intercepter les
 * autres exceptions, qui doivent conserver leur traitement par defaut.
 */
@Provider
public class ConflitMapper implements ExceptionMapper<ConstraintViolationException> {

    @Override
    public Response toResponse(ConstraintViolationException conflit) {
        SQLException sql = conflit.getSQLException();
        String detail = sql != null && sql.getMessage() != null
                ? sql.getMessage()
                : "Contrainte d'unicite violee";

        String corps = "{\"status\":409,\"title\":\"Conflit de donnees\",\"detail\":\""
                + detail.replace("\\", "\\\\").replace("\"", "'").replace("\n", " ")
                + "\"}";

        return Response.status(Response.Status.CONFLICT)
                .type("application/problem+json")
                .entity(corps)
                .build();
    }
}
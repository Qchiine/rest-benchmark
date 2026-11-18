package ma.bench.rest.config;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Une violation de contrainte unique (sku ou code deja present) est un conflit
 * client, pas une erreur serveur. Sans ce mapping, Spring renvoie 500 alors que
 * 409 est la reponse attendue par le protocole de benchmark.
 */
@RestControllerAdvice
public class GestionErreurs {

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail conflit(DataIntegrityViolationException exception) {
        ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        detail.setTitle("Conflit de donnees");
        detail.setDetail("Une contrainte d'unicite est violee (sku ou code deja utilise).");
        return detail;
    }
}
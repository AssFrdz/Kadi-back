package fr.miage.kadi.prix.adapter.in.web;

import fr.miage.kadi.prix.domain.exception.DonneeInvalideException;
import fr.miage.kadi.prix.domain.exception.MagasinInconnuException;
import fr.miage.kadi.prix.domain.exception.ProduitInconnuException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduit les exceptions du domaine en erreurs HTTP au format RFC 9457 (application/problem+json).
 * Limité aux contrôleurs du module prix ; les erreurs de validation des DTO sont traitées par Spring.
 */
@RestControllerAdvice(assignableTypes = PrixFiableController.class)
class ErreursPrixHandler {

    @ExceptionHandler({ProduitInconnuException.class, MagasinInconnuException.class})
    ProblemDetail introuvable(RuntimeException exception) {
        ProblemDetail probleme = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        probleme.setTitle("Introuvable");
        return probleme;
    }

    @ExceptionHandler(DonneeInvalideException.class)
    ProblemDetail invalide(DonneeInvalideException exception) {
        ProblemDetail probleme = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        probleme.setTitle("Saisie invalide");
        return probleme;
    }
}

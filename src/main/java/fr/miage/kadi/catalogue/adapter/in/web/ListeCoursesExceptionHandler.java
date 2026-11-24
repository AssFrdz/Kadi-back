package fr.miage.kadi.catalogue.adapter.in.web;

import fr.miage.kadi.catalogue.application.exception.FusionLigneImpossibleException;
import fr.miage.kadi.catalogue.application.exception.LigneListeIntrouvableException;
import fr.miage.kadi.catalogue.application.exception.ReferenceListeIntrouvableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Erreurs de la liste au format RFC 9457 (problem+json), avec un message compréhensible par l'utilisateur. */
@RestControllerAdvice(assignableTypes = ListeCoursesController.class)
class ListeCoursesExceptionHandler {

    @ExceptionHandler({IllegalArgumentException.class, NullPointerException.class})
    ProblemDetail requeteInvalide(RuntimeException erreur) {
        return probleme(HttpStatus.BAD_REQUEST, "Demande invalide", erreur.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail corpsIllisible(HttpMessageNotReadableException erreur) {
        return probleme(HttpStatus.BAD_REQUEST, "Demande invalide",
                "Le corps de la requête est absent, n'est pas un JSON valide ou contient une valeur du mauvais type.");
    }

    @ExceptionHandler({LigneListeIntrouvableException.class, ReferenceListeIntrouvableException.class})
    ProblemDetail introuvable(RuntimeException erreur) {
        return probleme(HttpStatus.NOT_FOUND, "Introuvable", erreur.getMessage());
    }

    @ExceptionHandler(FusionLigneImpossibleException.class)
    ProblemDetail conflit(FusionLigneImpossibleException erreur) {
        return probleme(HttpStatus.CONFLICT, "Conflit", erreur.getMessage());
    }

    private static ProblemDetail probleme(HttpStatus statut, String titre, String detail) {
        ProblemDetail probleme = ProblemDetail.forStatusAndDetail(statut,
                detail == null || detail.isBlank() ? titre : detail);
        probleme.setTitle(titre);
        return probleme;
    }
}

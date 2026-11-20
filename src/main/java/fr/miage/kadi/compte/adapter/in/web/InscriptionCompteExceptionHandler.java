package fr.miage.kadi.compte.adapter.in.web;

import fr.miage.kadi.compte.application.exception.AdresseEmailDejaUtiliseeException;
import fr.miage.kadi.compte.application.exception.EnvoiEmailIndisponibleException;
import fr.miage.kadi.compte.application.exception.JetonVerificationInvalideException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackageClasses = InscriptionCompteController.class)
public class InscriptionCompteExceptionHandler {

    @ExceptionHandler(AdresseEmailDejaUtiliseeException.class)
    ProblemDetail adresseEmailDejaUtilisee(AdresseEmailDejaUtiliseeException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(JetonVerificationInvalideException.class)
    ProblemDetail jetonVerificationInvalide(JetonVerificationInvalideException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(EnvoiEmailIndisponibleException.class)
    ProblemDetail envoiEmailIndisponible(EnvoiEmailIndisponibleException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage());
    }
}

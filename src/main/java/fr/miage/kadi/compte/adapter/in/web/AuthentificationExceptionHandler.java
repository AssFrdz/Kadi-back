package fr.miage.kadi.compte.adapter.in.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AuthentificationExceptionHandler {

    @ExceptionHandler(AuthenticationException.class)
    ProblemDetail identifiantsInvalides(AuthenticationException exception) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                "Adresse e-mail ou mot de passe incorrect.");
    }
}

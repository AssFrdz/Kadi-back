package fr.miage.kadi.compte.application.exception;

public class AdresseEmailDejaUtiliseeException extends RuntimeException {

    public AdresseEmailDejaUtiliseeException() {
        super("Cette adresse e-mail est déjà utilisée.");
    }
}

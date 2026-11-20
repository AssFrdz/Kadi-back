package fr.miage.kadi.compte.application.exception;

public class JetonVerificationInvalideException extends RuntimeException {

    public JetonVerificationInvalideException() {
        super("Le lien de vérification est invalide ou a expiré.");
    }
}

package fr.miage.kadi.catalogue.application.exception;

/** La liste a changé, ou plusieurs lignes sont compatibles : l'opération demandée n'est plus possible telle quelle. */
public class FusionLigneImpossibleException extends RuntimeException {
    public FusionLigneImpossibleException(String message, Throwable cause) {
        super(message, cause);
    }

    public FusionLigneImpossibleException(String message) {
        super(message);
    }
}

package fr.miage.kadi.catalogue.application.exception;

public class ProduitNonAjoutableException extends RuntimeException {
    public ProduitNonAjoutableException(String message) {
        super(message);
    }

    public ProduitNonAjoutableException(String message, Throwable cause) {
        super(message, cause);
    }
}

package fr.miage.kadi.catalogue.application.exception;

public class PropositionIntrouvableException extends RuntimeException {
    public PropositionIntrouvableException(long id) {
        super("La proposition " + id + " n'existe pas.");
    }
}

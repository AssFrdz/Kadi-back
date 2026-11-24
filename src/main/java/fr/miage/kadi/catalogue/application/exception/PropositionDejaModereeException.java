package fr.miage.kadi.catalogue.application.exception;

public class PropositionDejaModereeException extends RuntimeException {
    public PropositionDejaModereeException(long id) {
        super("La proposition " + id + " a déjà été traitée.");
    }
}

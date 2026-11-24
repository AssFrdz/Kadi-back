package fr.miage.kadi.catalogue.application.exception;

public class LigneListeIntrouvableException extends RuntimeException {
    public LigneListeIntrouvableException() {
        super("La ligne demandée n'existe pas dans cette liste.");
    }
}

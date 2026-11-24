package fr.miage.kadi.prix.domain.exception;

/** Levée quand un relevé ou une consultation désigne un magasin que Kadi ne connaît pas. */
public class MagasinInconnuException extends RuntimeException {

    public MagasinInconnuException(long magasinId) {
        super("Magasin inconnu : " + magasinId);
    }
}

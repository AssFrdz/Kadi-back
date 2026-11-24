package fr.miage.kadi.prix.domain.exception;

/** Levée quand un objet du domaine refuse une valeur absurde dès sa construction. */
public class DonneeInvalideException extends RuntimeException {

    public DonneeInvalideException(String message) {
        super(message);
    }
}

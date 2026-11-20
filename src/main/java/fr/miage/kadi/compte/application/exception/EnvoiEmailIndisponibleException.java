package fr.miage.kadi.compte.application.exception;

public class EnvoiEmailIndisponibleException extends RuntimeException {

    public EnvoiEmailIndisponibleException(String message) {
        super(message);
    }

    public EnvoiEmailIndisponibleException(String message, Throwable cause) {
        super(message, cause);
    }
}

package fr.miage.kadi.compte.application.port.out;

@FunctionalInterface
public interface MotDePasseHasher {

    String hasher(String motDePasse);
}

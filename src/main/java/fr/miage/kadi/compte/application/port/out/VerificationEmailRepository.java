package fr.miage.kadi.compte.application.port.out;

import java.time.Instant;

public interface VerificationEmailRepository {

    void enregistrer(long compteId, String empreinte, Instant expireLe);

    boolean verifierEtConsommer(String empreinte, Instant maintenant);
}

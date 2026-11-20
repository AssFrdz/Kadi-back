package fr.miage.kadi.compte.application.port.out;

import fr.miage.kadi.compte.domain.model.AdresseEmail;
import java.net.URI;

public interface ExpediteurEmail {

    void verifierDisponibilite();

    void envoyerLienVerification(AdresseEmail destinataire, URI lien);
}

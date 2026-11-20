package fr.miage.kadi.compte.application.port.out;

import fr.miage.kadi.compte.domain.model.AdresseEmail;
import fr.miage.kadi.compte.domain.model.CompteUtilisateur;
import java.util.Optional;

public interface CompteUtilisateurRepository {

    CompteUtilisateur enregistrerCompte(AdresseEmail adresseEmail, String motDePasseHash);

    Optional<CompteUtilisateur> trouverParAdresseEmail(AdresseEmail adresseEmail);
}

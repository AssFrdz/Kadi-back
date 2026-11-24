package fr.miage.kadi.catalogue.application.port.out;

import fr.miage.kadi.catalogue.domain.model.IdentifiantCompte;
import fr.miage.kadi.catalogue.domain.model.Quantite;

public interface AjoutProduitListeRepository {
    void ajouter(IdentifiantCompte proprietaire, long produitId, Quantite quantite);
}

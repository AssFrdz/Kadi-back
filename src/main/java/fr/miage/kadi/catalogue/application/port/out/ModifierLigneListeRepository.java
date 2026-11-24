package fr.miage.kadi.catalogue.application.port.out;

import fr.miage.kadi.catalogue.domain.model.IdentifiantCompte;
import fr.miage.kadi.catalogue.domain.model.Quantite;
import java.util.UUID;

public interface ModifierLigneListeRepository {
    void modifierQuantite(IdentifiantCompte proprietaire, UUID ligneId, Quantite quantite);

    void supprimer(IdentifiantCompte proprietaire, UUID ligneId);
}

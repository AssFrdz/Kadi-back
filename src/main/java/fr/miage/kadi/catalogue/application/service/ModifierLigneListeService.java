package fr.miage.kadi.catalogue.application.service;

import fr.miage.kadi.catalogue.application.port.out.ModifierLigneListeRepository;
import fr.miage.kadi.catalogue.domain.model.IdentifiantCompte;
import fr.miage.kadi.catalogue.domain.model.Quantite;
import java.util.Objects;
import java.util.UUID;

public class ModifierLigneListeService {
    private final ModifierLigneListeRepository lignes;

    public ModifierLigneListeService(ModifierLigneListeRepository lignes) {
        this.lignes = Objects.requireNonNull(lignes);
    }

    public void modifierQuantite(IdentifiantCompte proprietaire, UUID ligneId, Quantite quantite) {
        lignes.modifierQuantite(Objects.requireNonNull(proprietaire), Objects.requireNonNull(ligneId),
                Objects.requireNonNull(quantite));
    }

    public void supprimer(IdentifiantCompte proprietaire, UUID ligneId) {
        lignes.supprimer(Objects.requireNonNull(proprietaire), Objects.requireNonNull(ligneId));
    }
}

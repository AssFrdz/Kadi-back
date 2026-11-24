package fr.miage.kadi.catalogue.application.service;

import fr.miage.kadi.catalogue.application.port.out.AjoutProduitListeRepository;
import fr.miage.kadi.catalogue.domain.model.IdentifiantCompte;
import fr.miage.kadi.catalogue.domain.model.Quantite;
import java.util.Objects;

public class AjouterProduitListeService {
    private final AjoutProduitListeRepository listes;

    public AjouterProduitListeService(AjoutProduitListeRepository listes) {
        this.listes = Objects.requireNonNull(listes);
    }

    public void ajouter(IdentifiantCompte proprietaire, long produitId, Quantite quantite) {
        Objects.requireNonNull(proprietaire, "Le propriétaire ne peut pas être null.");
        Objects.requireNonNull(quantite, "La quantité ne peut pas être nulle.");
        if (produitId <= 0) {
            throw new IllegalArgumentException("L'identifiant du produit doit être positif.");
        }
        listes.ajouter(proprietaire, produitId, quantite);
    }
}

package fr.miage.kadi.prix.application.port.in;

import fr.miage.kadi.prix.domain.model.PrixDeReference;
import java.util.Optional;

/** Donner le prix fiable d'un produit dans un magasin, avec la raison d'y croire. */
public interface ConsulterPrixDeReference {

    /**
     * @return vide si aucun relevé n'est exploitable, même au niveau de l'enseigne
     * @throws fr.miage.kadi.prix.domain.exception.ProduitInconnuException si le produit n'existe pas
     * @throws fr.miage.kadi.prix.domain.exception.MagasinInconnuException si le magasin n'existe pas
     */
    Optional<PrixDeReference> consulter(long produitId, long magasinId);
}

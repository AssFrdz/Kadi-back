package fr.miage.kadi.prix.application.port.out;

import fr.miage.kadi.prix.domain.model.ReleveDePrix;
import java.time.Instant;
import java.util.List;

/** Les relevés de prix, d'où qu'ils viennent (base, import, catalogue de démonstration). */
public interface RelevesDePrix {

    /** Relevés du produit dans ce magasin, datés de {@code depuis} ou après. */
    List<ReleveDePrix> duProduitDansLeMagasin(long produitId, long magasinId, Instant depuis);

    /** Relevés du produit dans tous les magasins de l'enseigne de ce magasin, lui compris, datés de {@code depuis} ou après. */
    List<ReleveDePrix> duProduitDansLEnseigneDuMagasin(long produitId, long magasinId, Instant depuis);

    /** @return le relevé enregistré, avec son identifiant */
    ReleveDePrix enregistrer(ReleveDePrix releve, boolean confirmeMalgreAlerte);
}

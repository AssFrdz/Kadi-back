package fr.miage.kadi.catalogue.application.port.out;

import fr.miage.kadi.catalogue.domain.model.IdentifiantCompte;
import fr.miage.kadi.catalogue.domain.model.NatureBesoin;
import java.util.Optional;

/** Références qu'une ligne de liste peut désigner. */
public interface ReferencesListeRepository {

    /** Produit précis ou besoin générique du catalogue. */
    Optional<ReferenceCatalogue> trouverDansCatalogue(long produitId);

    boolean besoinPersonnelAppartientA(IdentifiantCompte proprietaire, long besoinId);

    /** Crée le besoin personnel en attente de validation, ou renvoie celui que ce compte a déjà créé avec ce libellé. */
    long enregistrerBesoinPersonnel(IdentifiantCompte proprietaire, String libelle);

    /** {@code nature} est nulle pour un produit précis. */
    record ReferenceCatalogue(long id, boolean generique, NatureBesoin nature) {
    }
}

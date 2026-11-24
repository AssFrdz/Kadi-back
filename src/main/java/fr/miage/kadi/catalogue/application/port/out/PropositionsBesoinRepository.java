package fr.miage.kadi.catalogue.application.port.out;

import fr.miage.kadi.catalogue.domain.model.NatureBesoin;
import fr.miage.kadi.catalogue.domain.model.PropositionBesoin;
import fr.miage.kadi.catalogue.domain.model.StatutProposition;
import java.util.List;
import java.util.Optional;

public interface PropositionsBesoinRepository {

    /** Propositions du statut demandé, de la plus ancienne à la plus récente. */
    List<PropositionBesoin> lister(StatutProposition statut);

    Optional<PropositionBesoin> trouver(long id);

    /** Besoins génériques du catalogue commun, pour un rattachement. */
    List<BesoinGenerique> listerBesoinsGeneriques();

    /** Besoin générique de même libellé, sans tenir compte de la casse. */
    Optional<BesoinGenerique> trouverBesoinGenerique(String libelle);

    Optional<BesoinGenerique> trouverBesoinGenerique(long id);

    /**
     * Crée le besoin générique commun et valide la proposition en une seule transaction. Renvoie l'identifiant
     * du besoin créé, ou vide, sans rien créer, si la proposition n'est plus en attente.
     */
    Optional<Long> validerEnCreant(long propositionId, String libelle, NatureBesoin nature);

    /** Valide la proposition en la reliant à un besoin générique existant ; {@code false} si elle n'est plus en attente. */
    boolean validerEnRattachant(long propositionId, long besoinGeneriqueId);

    /** {@code false} si la proposition n'est plus en attente. */
    boolean refuser(long propositionId);

    record BesoinGenerique(long id, String libelle, NatureBesoin nature) {
    }
}

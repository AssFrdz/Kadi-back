package fr.miage.kadi.catalogue.application.port.out;

import fr.miage.kadi.catalogue.domain.model.IdentifiantCompte;
import fr.miage.kadi.catalogue.domain.model.ListeCourses;
import java.util.List;
import java.util.function.Function;

/** Liste personnelle unique de chaque compte. Toutes les opérations sont limitées au propriétaire indiqué. */
public interface ListePersonnelleRepository {

    /** Lignes de la liste du propriétaire, dans l'ordre d'ajout ; vide s'il n'a encore rien ajouté. */
    List<LigneListeVue> consulter(IdentifiantCompte proprietaire);

    /**
     * Charge la liste du propriétaire en la verrouillant, applique l'opération puis enregistre la liste obtenue,
     * le tout dans une seule transaction : deux modifications simultanées du même compte s'exécutent l'une après l'autre.
     */
    <T> T modifier(IdentifiantCompte proprietaire, Function<ListeCourses, Modification<T>> operation);

    record Modification<T>(ListeCourses liste, T resultat) {
    }
}

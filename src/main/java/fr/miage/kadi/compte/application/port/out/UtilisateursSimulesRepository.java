package fr.miage.kadi.compte.application.port.out;

import fr.miage.kadi.compte.domain.model.UtilisateurSimule;
import java.util.List;
import java.util.Optional;

public interface UtilisateursSimulesRepository {

    /** Profils simulés, dans l'ordre de leur identifiant. */
    List<UtilisateurSimule> lister();

    Optional<UtilisateurSimule> trouver(long id);
}

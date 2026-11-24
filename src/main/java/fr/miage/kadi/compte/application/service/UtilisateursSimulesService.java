package fr.miage.kadi.compte.application.service;

import fr.miage.kadi.compte.application.port.out.UtilisateursSimulesRepository;
import fr.miage.kadi.compte.domain.model.UtilisateurSimule;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Fournit les profils proposés dans la liste déroulante et vérifie celui que le front a sélectionné. */
public class UtilisateursSimulesService {

    private final UtilisateursSimulesRepository utilisateurs;

    public UtilisateursSimulesService(UtilisateursSimulesRepository utilisateurs) {
        this.utilisateurs = Objects.requireNonNull(utilisateurs);
    }

    public List<UtilisateurSimule> lister() {
        return utilisateurs.lister();
    }

    public Optional<UtilisateurSimule> trouver(long id) {
        return id <= 0 ? Optional.empty() : utilisateurs.trouver(id);
    }
}

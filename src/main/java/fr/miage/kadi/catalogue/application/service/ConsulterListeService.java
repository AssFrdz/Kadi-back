package fr.miage.kadi.catalogue.application.service;

import fr.miage.kadi.catalogue.application.port.out.ListesCoursesRepository;
import fr.miage.kadi.catalogue.domain.model.IdentifiantCompte;
import fr.miage.kadi.catalogue.domain.model.ListeCourses;
import java.util.List;
import java.util.Objects;

public class ConsulterListeService {
    private final ListesCoursesRepository listes;

    public ConsulterListeService(ListesCoursesRepository listes) {
        this.listes = Objects.requireNonNull(listes, "Le dépôt des listes ne peut pas être null.");
    }

    public ListeCourses consulter(IdentifiantCompte proprietaire) {
        Objects.requireNonNull(proprietaire, "Le propriétaire ne peut pas être null.");
        ListeCourses liste = listes.trouverPourProprietaire(proprietaire)
                .orElseGet(() -> new ListeCourses(proprietaire, List.of()));
        if (!liste.proprietaireId().equals(proprietaire)) {
            throw new IllegalStateException("Le dépôt a renvoyé la liste d'un autre compte.");
        }
        return liste;
    }
}

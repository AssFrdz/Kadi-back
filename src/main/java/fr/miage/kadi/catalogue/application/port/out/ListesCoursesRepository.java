package fr.miage.kadi.catalogue.application.port.out;

import fr.miage.kadi.catalogue.domain.model.IdentifiantCompte;
import fr.miage.kadi.catalogue.domain.model.ListeCourses;
import java.util.Optional;

public interface ListesCoursesRepository {
    Optional<ListeCourses> trouverPourProprietaire(IdentifiantCompte proprietaire);
}

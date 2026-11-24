package fr.miage.kadi.catalogue.config;

import fr.miage.kadi.catalogue.application.port.out.ListePersonnelleRepository;
import fr.miage.kadi.catalogue.application.port.out.ProduitsCatalogueRepository;
import fr.miage.kadi.catalogue.application.port.out.ReferencesListeRepository;
import fr.miage.kadi.catalogue.application.port.out.RechercheProduitsCompatiblesRepository;
import fr.miage.kadi.catalogue.application.service.ConsulterCatalogueService;
import fr.miage.kadi.catalogue.application.service.GererListeCoursesService;
import fr.miage.kadi.catalogue.application.service.RechercherCatalogueService;
import fr.miage.kadi.catalogue.application.service.RechercherProduitsCompatiblesService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class CatalogueConfiguration {

    @Bean
    ConsulterCatalogueService consulterCatalogueService(ProduitsCatalogueRepository produits) {
        return new ConsulterCatalogueService(produits);
    }

    @Bean
    RechercherCatalogueService rechercherCatalogueService(ProduitsCatalogueRepository produits) {
        return new RechercherCatalogueService(produits);
    }

    @Bean
    RechercherProduitsCompatiblesService rechercherProduitsCompatiblesService(
            RechercheProduitsCompatiblesRepository produits, RechercherCatalogueService rechercheCatalogue) {
        return new RechercherProduitsCompatiblesService(produits, rechercheCatalogue);
    }

    @Bean
    GererListeCoursesService gererListeCoursesService(ListePersonnelleRepository listes,
            ReferencesListeRepository references) {
        return new GererListeCoursesService(listes, references);
    }
}

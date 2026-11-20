package fr.miage.kadi.catalogue.config;

import fr.miage.kadi.catalogue.application.port.out.ProduitsCatalogueRepository;
import fr.miage.kadi.catalogue.application.service.ConsulterCatalogueService;
import fr.miage.kadi.catalogue.application.service.RechercherCatalogueService;
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
}

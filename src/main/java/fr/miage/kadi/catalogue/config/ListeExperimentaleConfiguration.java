package fr.miage.kadi.catalogue.config;

import fr.miage.kadi.catalogue.application.port.out.AjoutProduitListeRepository;
import fr.miage.kadi.catalogue.application.port.out.ListesCoursesRepository;
import fr.miage.kadi.catalogue.application.port.out.ModifierLigneListeRepository;
import fr.miage.kadi.catalogue.application.service.AjouterProduitListeService;
import fr.miage.kadi.catalogue.application.service.ConsulterListeService;
import fr.miage.kadi.catalogue.application.service.ModifierLigneListeService;
import fr.miage.kadi.catalogue.application.service.VerifierVenteProduitService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("liste-experimentale")
class ListeExperimentaleConfiguration {
    @Bean
    VerifierVenteProduitService verifierVenteProduitService() {
        return new VerifierVenteProduitService();
    }

    @Bean
    ConsulterListeService consulterListeService(ListesCoursesRepository listes) {
        return new ConsulterListeService(listes);
    }

    @Bean
    AjouterProduitListeService ajouterProduitListeService(AjoutProduitListeRepository listes) {
        return new AjouterProduitListeService(listes);
    }

    @Bean
    ModifierLigneListeService modifierLigneListeService(ModifierLigneListeRepository lignes) {
        return new ModifierLigneListeService(lignes);
    }
}

package fr.miage.kadi.compte.config;

import fr.miage.kadi.compte.application.port.out.UtilisateursSimulesRepository;
import fr.miage.kadi.compte.application.service.UtilisateursSimulesService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class CompteConfiguration {

    @Bean
    UtilisateursSimulesService utilisateursSimulesService(UtilisateursSimulesRepository utilisateurs) {
        return new UtilisateursSimulesService(utilisateurs);
    }
}

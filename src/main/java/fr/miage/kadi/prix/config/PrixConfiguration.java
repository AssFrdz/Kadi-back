package fr.miage.kadi.prix.config;

import fr.miage.kadi.prix.application.port.out.Referentiel;
import fr.miage.kadi.prix.application.port.out.RelevesDePrix;
import fr.miage.kadi.prix.application.port.out.StockageDesImports;
import fr.miage.kadi.prix.application.service.ImportService;
import fr.miage.kadi.prix.application.service.PrixFiableService;
import fr.miage.kadi.prix.domain.algo.ParametresPrixFiable;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Branche le module prix : le service, écrit sans Spring, reçoit ici ses ports de sortie,
 * ses réglages et l'horloge réelle. Les tests, eux, lui donnent de faux ports et une horloge figée.
 */
@Configuration
class PrixConfiguration {

    @Bean
    Clock horloge() {
        return Clock.systemUTC();
    }

    @Bean
    ParametresPrixFiable parametresPrixFiable() {
        return ParametresPrixFiable.PAR_DEFAUT;
    }

    @Bean
    ImportService importService(StockageDesImports stockage) {
        return new ImportService(stockage);
    }

    @Bean
    PrixFiableService prixFiableService(
            RelevesDePrix releves, Referentiel referentiel, ParametresPrixFiable parametres, Clock horloge) {
        return new PrixFiableService(releves, referentiel, parametres, horloge);
    }
}

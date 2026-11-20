package fr.miage.kadi.compte;

import fr.miage.kadi.compte.application.port.out.ExpediteurEmail;
import fr.miage.kadi.compte.domain.model.AdresseEmail;
import java.net.URI;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration(proxyBeanMethods = false)
public class EmailTestConfiguration {

    @Bean
    @Primary
    public EnvoiEmailFictif envoiEmailFictif() {
        return new EnvoiEmailFictif();
    }

    public static class EnvoiEmailFictif implements ExpediteurEmail {

        private URI dernierLien;

        @Override
        public void verifierDisponibilite() {
        }

        @Override
        public void envoyerLienVerification(AdresseEmail destinataire, URI lien) {
            dernierLien = lien;
        }

        public URI dernierLien() {
            return dernierLien;
        }
    }
}

package fr.miage.kadi.compte.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.miage.kadi.compte.application.exception.JetonVerificationInvalideException;
import fr.miage.kadi.compte.application.port.out.ExpediteurEmail;
import fr.miage.kadi.compte.application.port.out.VerificationEmailRepository;
import fr.miage.kadi.compte.domain.model.AdresseEmail;
import fr.miage.kadi.compte.domain.model.CompteUtilisateur;
import fr.miage.kadi.compte.domain.model.RoleCompte;
import java.net.URI;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class VerificationEmailServiceTest {

    private static final Instant MAINTENANT = Instant.parse("2026-11-18T18:00:00Z");
    private static final CompteUtilisateur COMPTE =
            new CompteUtilisateur(7, new AdresseEmail("assia@example.test"), "{bcrypt}hash", false,
                    RoleCompte.UTILISATEUR);

    @Test
    void envoieUnLienAvecUnJetonBrutMaisNePersisteQueSonEmpreinte() {
        DepotJetonFictif depot = new DepotJetonFictif();
        ExpediteurFictif expediteur = new ExpediteurFictif();
        VerificationEmailService service = service(depot, expediteur);

        service.envoyerLien(COMPTE);

        assertThat(expediteur.destinataire).isEqualTo(COMPTE.adresseEmail());
        assertThat(expediteur.lien).startsWith("http://localhost:4200/verification-email?token=");
        assertThat(expediteur.lien).doesNotContain("%");
        assertThat(depot.compteId).isEqualTo(COMPTE.id());
        assertThat(depot.empreinte).hasSize(64).doesNotContain(expediteur.lien.substring(
                expediteur.lien.indexOf("token=") + "token=".length()));
        assertThat(depot.expireLe).isEqualTo(MAINTENANT.plus(Duration.ofHours(24)));
    }

    @Test
    void confirmeLeJetonEnEmpreinteEtRefuseUnJetonInvalideOuConsomme() {
        DepotJetonFictif depot = new DepotJetonFictif();
        ExpediteurFictif expediteur = new ExpediteurFictif();
        VerificationEmailService service = service(depot, expediteur);
        service.envoyerLien(COMPTE);
        String jeton = URI.create(expediteur.lien).getQuery().substring("token=".length());

        service.confirmer(jeton);

        assertThat(depot.empreinteVerifiee).isEqualTo(depot.empreinte);
        depot.jetonActif = false;
        assertThatThrownBy(() -> service.confirmer(jeton))
                .isInstanceOf(JetonVerificationInvalideException.class);
        assertThatThrownBy(() -> service.confirmer("jeton-invalide"))
                .isInstanceOf(JetonVerificationInvalideException.class);
    }

    @Test
    void refuseUnJetonExpire() {
        DepotJetonFictif depot = new DepotJetonFictif();
        ExpediteurFictif expediteur = new ExpediteurFictif();
        VerificationEmailService service = service(depot, expediteur);
        service.envoyerLien(COMPTE);
        String jeton = URI.create(expediteur.lien).getQuery().substring("token=".length());
        depot.expireLe = MAINTENANT.minusSeconds(1);

        assertThatThrownBy(() -> service.confirmer(jeton))
                .isInstanceOf(JetonVerificationInvalideException.class);
    }

    private VerificationEmailService service(DepotJetonFictif depot, ExpediteurFictif expediteur) {
        return new VerificationEmailService(
                depot,
                expediteur,
                Clock.fixed(MAINTENANT, ZoneOffset.UTC),
                new SecureRandom(),
                "http://localhost:4200/verification-email");
    }

    private static class DepotJetonFictif implements VerificationEmailRepository {

        private long compteId;
        private String empreinte;
        private Instant expireLe;
        private String empreinteVerifiee;
        private boolean jetonActif = true;

        @Override
        public void enregistrer(long compteId, String empreinte, Instant expireLe) {
            this.compteId = compteId;
            this.empreinte = empreinte;
            this.expireLe = expireLe;
        }

        @Override
        public boolean verifierEtConsommer(String empreinte, Instant maintenant) {
            empreinteVerifiee = empreinte;
            return jetonActif && this.empreinte.equals(empreinte) && expireLe.isAfter(maintenant);
        }
    }

    private static class ExpediteurFictif implements ExpediteurEmail {

        private AdresseEmail destinataire;
        private String lien;

        @Override
        public void verifierDisponibilite() {
        }

        @Override
        public void envoyerLienVerification(AdresseEmail destinataire, URI lien) {
            this.destinataire = destinataire;
            this.lien = lien.toString();
        }
    }
}

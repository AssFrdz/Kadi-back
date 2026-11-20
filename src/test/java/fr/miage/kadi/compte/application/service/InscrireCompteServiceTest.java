package fr.miage.kadi.compte.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.miage.kadi.compte.application.exception.AdresseEmailDejaUtiliseeException;
import fr.miage.kadi.compte.application.port.out.CompteUtilisateurRepository;
import fr.miage.kadi.compte.application.port.out.ExpediteurEmail;
import fr.miage.kadi.compte.application.port.out.MotDePasseHasher;
import fr.miage.kadi.compte.application.port.out.VerificationEmailRepository;
import fr.miage.kadi.compte.domain.model.AdresseEmail;
import fr.miage.kadi.compte.domain.model.CompteUtilisateur;
import fr.miage.kadi.compte.domain.model.RoleCompte;
import java.net.URI;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class InscrireCompteServiceTest {

    private static final AdresseEmail EMAIL = new AdresseEmail("assia@example.test");

    @Test
    void enregistreUnCompteAvecLeHashEtLeRoleParDefaut() {
        DepotCompteFictif depot = new DepotCompteFictif();
        InscrireCompteService service = service(depot, motDePasse -> "hash:" + motDePasse);

        CompteUtilisateur compte = service.inscrire(EMAIL, "mot-de-passe");

        assertThat(compte.id()).isEqualTo(1);
        assertThat(compte.adresseEmail()).isEqualTo(EMAIL);
        assertThat(compte.motDePasseHash()).isEqualTo("hash:mot-de-passe");
        assertThat(compte.emailVerifie()).isFalse();
        assertThat(compte.role()).isEqualTo(RoleCompte.UTILISATEUR);
        assertThat(depot.compteEnregistre).isEqualTo(compte);
    }

    @Test
    void refuseUneAdresseDejaUtiliseeSansHasheNiEnregistrerLeMotDePasse() {
        DepotCompteFictif depot = new DepotCompteFictif();
        depot.compteExistant = new CompteUtilisateur(2, EMAIL, "hash:existant", false, RoleCompte.UTILISATEUR);
        boolean[] hasherAppele = {false};
        InscrireCompteService service = service(depot, motDePasse -> {
            hasherAppele[0] = true;
            return "hash:" + motDePasse;
        });

        assertThatThrownBy(() -> service.inscrire(EMAIL, "mot-de-passe"))
                .isInstanceOf(AdresseEmailDejaUtiliseeException.class)
                .hasMessage("Cette adresse e-mail est déjà utilisée.");
        assertThat(hasherAppele[0]).isFalse();
        assertThat(depot.compteEnregistre).isNull();
    }

    @Test
    void refuseUnMotDePasseAbsentAvantToutAccesAuDepot() {
        boolean[] depotInterroge = {false};
        CompteUtilisateurRepository depot = new DepotCompteFictif() {
            @Override
            public Optional<CompteUtilisateur> trouverParAdresseEmail(AdresseEmail adresseEmail) {
                depotInterroge[0] = true;
                return super.trouverParAdresseEmail(adresseEmail);
            }
        };
        InscrireCompteService service = service(depot, motDePasse -> "hash:" + motDePasse);

        assertThatThrownBy(() -> service.inscrire(EMAIL, " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Le mot de passe ne peut pas être vide.");
        assertThat(depotInterroge[0]).isFalse();
    }

    @Test
    void refuseUneAdresseEmailNulle() {
        InscrireCompteService service = service(new DepotCompteFictif(), motDePasse -> "hash:" + motDePasse);

        assertThatThrownBy(() -> service.inscrire(null, "mot-de-passe"))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("L'adresse e-mail ne peut pas être null.");
    }

    private InscrireCompteService service(CompteUtilisateurRepository depot, MotDePasseHasher hasher) {
        VerificationEmailRepository depotJetons = new VerificationEmailRepository() {
            @Override
            public void enregistrer(long compteId, String empreinte, Instant expireLe) {
            }

            @Override
            public boolean verifierEtConsommer(String empreinte, Instant maintenant) {
                return false;
            }
        };
        ExpediteurEmail expediteur = new ExpediteurEmail() {
            @Override
            public void verifierDisponibilite() {
            }

            @Override
            public void envoyerLienVerification(AdresseEmail destinataire, URI lien) {
            }
        };
        VerificationEmailService verification = new VerificationEmailService(
                depotJetons,
                expediteur,
                Clock.fixed(Instant.EPOCH, ZoneOffset.UTC),
                new SecureRandom(),
                "https://example.test/verification-email");
        return new InscrireCompteService(depot, hasher, verification);
    }

    private static class DepotCompteFictif implements CompteUtilisateurRepository {

        private CompteUtilisateur compteExistant;
        private CompteUtilisateur compteEnregistre;

        @Override
        public CompteUtilisateur enregistrerCompte(AdresseEmail adresseEmail, String motDePasseHash) {
            compteEnregistre = new CompteUtilisateur(1, adresseEmail, motDePasseHash, false, RoleCompte.UTILISATEUR);
            return compteEnregistre;
        }

        @Override
        public Optional<CompteUtilisateur> trouverParAdresseEmail(AdresseEmail adresseEmail) {
            return Optional.ofNullable(compteExistant);
        }
    }
}

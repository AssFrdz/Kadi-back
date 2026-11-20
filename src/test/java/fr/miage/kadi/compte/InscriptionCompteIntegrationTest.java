package fr.miage.kadi.compte;

import static org.assertj.core.api.Assertions.assertThat;

import fr.miage.kadi.compte.application.port.out.CompteUtilisateurRepository;
import fr.miage.kadi.compte.application.service.InscrireCompteService;
import fr.miage.kadi.compte.domain.model.AdresseEmail;
import fr.miage.kadi.compte.domain.model.CompteUtilisateur;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@Import(EmailTestConfiguration.class)
class InscriptionCompteIntegrationTest {

    @Autowired
    private InscrireCompteService inscrireCompteService;

    @Autowired
    private CompteUtilisateurRepository compteUtilisateurRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void inscritEtPersisteUnCompteAvecUnMotDePasseVerifiableMaisNonLisible() {
        AdresseEmail adresseEmail = new AdresseEmail("assia-" + UUID.randomUUID() + "@example.test");
        String motDePasse = "MotDePasse-de-test-2026";

        CompteUtilisateur inscrit = inscrireCompteService.inscrire(adresseEmail, motDePasse);
        CompteUtilisateur relu = compteUtilisateurRepository.trouverParAdresseEmail(adresseEmail).orElseThrow();

        assertThat(inscrit.id()).isPositive();
        assertThat(inscrit.motDePasseHash()).isNotEqualTo(motDePasse);
        assertThat(passwordEncoder.matches(motDePasse, inscrit.motDePasseHash())).isTrue();
        assertThat(relu).isEqualTo(inscrit);
        assertThat(relu.emailVerifie()).isFalse();
    }
}

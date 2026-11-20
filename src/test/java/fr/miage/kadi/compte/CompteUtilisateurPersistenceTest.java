package fr.miage.kadi.compte;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.miage.kadi.compte.application.port.out.CompteUtilisateurRepository;
import fr.miage.kadi.compte.domain.model.AdresseEmail;
import fr.miage.kadi.compte.domain.model.CompteUtilisateur;
import fr.miage.kadi.compte.domain.model.RoleCompte;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class CompteUtilisateurPersistenceTest {

    @Autowired
    private CompteUtilisateurRepository compteUtilisateurRepository;

    @Test
    void enregistreUnCompteNonVerifieAvecLeRoleUtilisateurPuisLeRelit() {
        AdresseEmail adresseEmail = nouvelleAdresseEmail();
        String motDePasseHash = "{bcrypt}empreinte-de-test";

        CompteUtilisateur enregistre = compteUtilisateurRepository.enregistrerCompte(adresseEmail, motDePasseHash);
        CompteUtilisateur relu = compteUtilisateurRepository.trouverParAdresseEmail(adresseEmail).orElseThrow();

        assertThat(relu)
                .isEqualTo(new CompteUtilisateur(
                        enregistre.id(), adresseEmail, motDePasseHash, false, RoleCompte.UTILISATEUR));
    }

    @Test
    void rechercheUneAdresseSansTenirCompteDeLaCasse() {
        AdresseEmail adresseEnregistree = nouvelleAdresseEmail();
        compteUtilisateurRepository.enregistrerCompte(adresseEnregistree, "{bcrypt}empreinte-de-test");

        AdresseEmail adresseRecherchee = new AdresseEmail(adresseEnregistree.valeur().toUpperCase());

        assertThat(compteUtilisateurRepository.trouverParAdresseEmail(adresseRecherchee))
                .isPresent()
                .get()
                .extracting(CompteUtilisateur::adresseEmail)
                .isEqualTo(adresseEnregistree);
    }

    @Test
    void laBaseRefuseUnDoublonInsensibleALaCasse() {
        AdresseEmail adresseEmail = nouvelleAdresseEmail();
        compteUtilisateurRepository.enregistrerCompte(adresseEmail, "{bcrypt}empreinte-de-test");

        assertThatThrownBy(() -> compteUtilisateurRepository.enregistrerCompte(
                new AdresseEmail(adresseEmail.valeur().toUpperCase()), "{bcrypt}autre-empreinte"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private AdresseEmail nouvelleAdresseEmail() {
        return new AdresseEmail("assia-" + UUID.randomUUID() + "@example.test");
    }
}

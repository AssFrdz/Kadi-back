package fr.miage.kadi.compte.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CompteUtilisateurTest {

    @Test
    void normaliseAdresseEmailPourLaRendreInsensibleAuxEspacesEtALaCasse() {
        AdresseEmail email = new AdresseEmail("  Assia.Exemple@Mail.test  ");

        assertThat(email.valeur()).isEqualTo("assia.exemple@mail.test");
    }

    @Test
    void refuseUneAdresseEmailNulleVideOuTropLongue() {
        assertThatThrownBy(() -> new AdresseEmail(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("L'adresse e-mail ne peut pas être null.");
        assertThatThrownBy(() -> new AdresseEmail("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("L'adresse e-mail ne peut pas être vide.");
        assertThatThrownBy(() -> new AdresseEmail("a".repeat(321)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("L'adresse e-mail ne peut pas dépasser 320 caractères.");
    }

    @Test
    void conserveLeHashLeStatutDeVerificationEtLeRole() {
        AdresseEmail email = new AdresseEmail("assia@example.test");
        CompteUtilisateur compte = new CompteUtilisateur(
                7, email, "{bcrypt}empreinte", false, RoleCompte.UTILISATEUR);

        assertThat(compte.id()).isEqualTo(7);
        assertThat(compte.adresseEmail()).isEqualTo(email);
        assertThat(compte.motDePasseHash()).isEqualTo("{bcrypt}empreinte");
        assertThat(compte.emailVerifie()).isFalse();
        assertThat(compte.role()).isEqualTo(RoleCompte.UTILISATEUR);
    }

    @Test
    void refuseUnIdentifiantUnHashOuDesChampsObligatoiresInvalides() {
        AdresseEmail email = new AdresseEmail("assia@example.test");

        assertThatThrownBy(() -> new CompteUtilisateur(
                0, email, "{bcrypt}empreinte", false, RoleCompte.UTILISATEUR))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("L'identifiant du compte doit être strictement positif.");
        assertThatThrownBy(() -> new CompteUtilisateur(
                7, null, "{bcrypt}empreinte", false, RoleCompte.UTILISATEUR))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("L'adresse e-mail du compte ne peut pas être null.");
        assertThatThrownBy(() -> new CompteUtilisateur(
                7, email, " ", false, RoleCompte.UTILISATEUR))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Le hash du mot de passe ne peut pas être absent.");
        assertThatThrownBy(() -> new CompteUtilisateur(
                7, email, "{bcrypt}empreinte", false, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Le rôle du compte ne peut pas être null.");
    }
}

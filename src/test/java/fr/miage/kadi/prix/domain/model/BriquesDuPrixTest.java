package fr.miage.kadi.prix.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.miage.kadi.prix.domain.exception.DonneeInvalideException;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Les objets valeur refusent l'absurde dès leur construction (fiche F2, section Modèle). */
class BriquesDuPrixTest {

    private static final Instant HIER = Instant.parse("2026-11-13T12:00:00Z");

    @Test
    void un_montant_est_au_centime_et_ne_depend_pas_de_son_ecriture() {
        assertThat(Montant.de("1.5")).isEqualTo(Montant.de("1.50"));
        assertThat(Montant.de("1.50").valeur()).isEqualByComparingTo("1.50");
        assertThat(Montant.de("0.99")).isLessThan(Montant.de("1.00"));
    }

    @Test
    void un_montant_refuse_le_vide_le_negatif_et_les_fractions_de_centime() {
        assertThatThrownBy(() -> new Montant(null)).isInstanceOf(DonneeInvalideException.class);
        assertThatThrownBy(() -> Montant.de("-0.01")).isInstanceOf(DonneeInvalideException.class);
        assertThatThrownBy(() -> Montant.de("1.995")).isInstanceOf(DonneeInvalideException.class);
        // un zéro final n'est pas une fraction de centime
        assertThat(Montant.de("1.990").valeur()).isEqualByComparingTo(new BigDecimal("1.99"));
    }

    @Test
    void un_releve_valide_se_construit() {
        ReleveDePrix releve = releve(1, 1, "1.09", "Karim");

        assertThat(releve.prix()).isEqualTo(Montant.de("1.09"));
        assertThat(releve.id()).isNull();
    }

    @Test
    void un_releve_refuse_un_prix_nul_et_un_contributeur_vide_ou_trop_long() {
        assertThatThrownBy(() -> releve(1, 1, "0.00", "Karim")).isInstanceOf(DonneeInvalideException.class);
        assertThatThrownBy(() -> releve(1, 1, "1.09", " ")).isInstanceOf(DonneeInvalideException.class);
        assertThatThrownBy(() -> releve(1, 1, "1.09", "x".repeat(65))).isInstanceOf(DonneeInvalideException.class);
    }

    @Test
    void un_releve_designe_un_produit_et_un_magasin() {
        assertThatThrownBy(() -> releve(0, 1, "1.09", "Karim")).isInstanceOf(DonneeInvalideException.class);
        assertThatThrownBy(() -> releve(1, -3, "1.09", "Karim")).isInstanceOf(DonneeInvalideException.class);
        assertThatThrownBy(() -> new ReleveDePrix(null, 1, 1, Montant.de("1.09"), UniteDePrix.PIECE, null, TypeDePreuve.SAISIE, "Karim", false))
                .isInstanceOf(DonneeInvalideException.class);
    }

    @Test
    void la_confiance_est_comprise_entre_0_et_1() {
        assertThatThrownBy(() -> new Confiance(-0.01)).isInstanceOf(DonneeInvalideException.class);
        assertThatThrownBy(() -> new Confiance(1.01)).isInstanceOf(DonneeInvalideException.class);
        assertThatThrownBy(() -> new Confiance(Double.NaN)).isInstanceOf(DonneeInvalideException.class);
    }

    @Test
    void le_niveau_de_confiance_se_deduit_du_score_aux_seuils_de_la_fiche() {
        assertThat(new Confiance(0.60).niveau()).isEqualTo(NiveauDeConfiance.ELEVEE);
        assertThat(new Confiance(0.5999).niveau()).isEqualTo(NiveauDeConfiance.MOYENNE);
        assertThat(new Confiance(0.35).niveau()).isEqualTo(NiveauDeConfiance.MOYENNE);
        assertThat(new Confiance(0.3499).niveau()).isEqualTo(NiveauDeConfiance.FAIBLE);
    }

    @Test
    void un_prix_inhabituel_demande_une_confirmation_les_autres_non() {
        Montant reference = Montant.de("1.09");

        assertThat(new ControleDeSaisie(StatutDeSaisie.INHABITUEL, reference, -45.4, -90.8).demandeConfirmation()).isTrue();
        assertThat(new ControleDeSaisie(StatutDeSaisie.NORMAL, reference, 0.5, 0.9).demandeConfirmation()).isFalse();
        assertThat(ControleDeSaisie.sansReference().demandeConfirmation()).isFalse();
    }

    @Test
    void un_controle_ne_compare_a_rien_sans_reference_et_a_tout_avec() {
        assertThatThrownBy(() -> new ControleDeSaisie(StatutDeSaisie.INHABITUEL, null, null, null))
                .isInstanceOf(DonneeInvalideException.class);
        assertThatThrownBy(() -> new ControleDeSaisie(StatutDeSaisie.SANS_REFERENCE, Montant.de("1.09"), 1.0, 1.0))
                .isInstanceOf(DonneeInvalideException.class);
    }

    private static ReleveDePrix releve(long produit, long magasin, String prix, String contributeur) {
        return new ReleveDePrix(null, produit, magasin, Montant.de(prix), UniteDePrix.PIECE, HIER, TypeDePreuve.SAISIE, contributeur, false);
    }
}

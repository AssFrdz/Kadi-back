package fr.miage.kadi.prix.domain.algo;

import static fr.miage.kadi.prix.domain.algo.JeuDeReleves.MAINTENANT;
import static fr.miage.kadi.prix.domain.algo.JeuDeReleves.exempleLactel;
import static fr.miage.kadi.prix.domain.algo.JeuDeReleves.releve;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.miage.kadi.prix.domain.exception.DonneeInvalideException;
import fr.miage.kadi.prix.domain.model.ControleDeSaisie;
import fr.miage.kadi.prix.domain.model.Montant;
import fr.miage.kadi.prix.domain.model.ReleveDePrix;
import fr.miage.kadi.prix.domain.model.StatutDeSaisie;
import fr.miage.kadi.prix.domain.model.TypeDePreuve;
import fr.miage.kadi.prix.domain.model.UniteDePrix;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Le scénario de Karim (dossier, section 03) : une faute de frappe arrêtée à la saisie. */
class ControleurDeSaisieTest {

    private final ControleurDeSaisie controleur = new ControleurDeSaisie(ParametresPrixFiable.PAR_DEFAUT);

    @Test
    void karim_tape_0_10_au_lieu_de_1_10_et_doit_confirmer() {
        ControleDeSaisie controle = controler("0.10", UniteDePrix.PIECE, exempleLactel());

        assertThat(controle.statut()).isEqualTo(StatutDeSaisie.INHABITUEL);
        assertThat(controle.reference()).isEqualTo(Montant.de("1.09"));
        assertThat(controle.ecartPourcent()).isLessThan(-90);
        assertThat(controle.demandeConfirmation()).isTrue();
    }

    @Test
    void un_prix_proche_de_la_reference_passe_sans_question() {
        ControleDeSaisie controle = controler("1.12", UniteDePrix.PIECE, exempleLactel());

        assertThat(controle.statut()).isEqualTo(StatutDeSaisie.NORMAL);
        assertThat(controle.demandeConfirmation()).isFalse();
    }

    @Test
    void avec_moins_de_3_temoins_il_n_y_a_pas_de_reference_pour_juger() {
        List<ReleveDePrix> deuxTemoins = List.of(
                releve("1.09", TypeDePreuve.TICKET, 3, "A"),
                releve("1.09", TypeDePreuve.TICKET, 4, "B"));

        assertThat(controler("0.10", UniteDePrix.PIECE, deuxTemoins).statut()).isEqualTo(StatutDeSaisie.SANS_REFERENCE);
    }

    @Test
    void un_prix_au_kilo_ne_se_compare_pas_a_des_prix_a_la_piece() {
        assertThat(controler("13.96", UniteDePrix.KILOGRAMME, exempleLactel()).statut())
                .isEqualTo(StatutDeSaisie.SANS_REFERENCE);
    }

    @Test
    void sans_aucun_releve_la_saisie_est_acceptee_sans_reference() {
        assertThat(controler("1.09", UniteDePrix.PIECE, List.of()).statut()).isEqualTo(StatutDeSaisie.SANS_REFERENCE);
    }

    @Test
    void un_prix_saisi_nul_est_refuse() {
        assertThatThrownBy(() -> controler("0.00", UniteDePrix.PIECE, exempleLactel()))
                .isInstanceOf(DonneeInvalideException.class);
    }

    private ControleDeSaisie controler(String prix, UniteDePrix unite, List<ReleveDePrix> releves) {
        return controleur.controler(Montant.de(prix), unite, releves, releves, MAINTENANT);
    }
}

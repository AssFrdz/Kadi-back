package fr.miage.kadi.prix.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.miage.kadi.prix.application.port.in.DemandeDeReleve;
import fr.miage.kadi.prix.application.port.in.ResultatEnregistrement;
import fr.miage.kadi.prix.application.port.out.Referentiel;
import fr.miage.kadi.prix.application.port.out.RelevesDePrix;
import fr.miage.kadi.prix.domain.algo.ParametresPrixFiable;
import fr.miage.kadi.prix.domain.exception.DonneeInvalideException;
import fr.miage.kadi.prix.domain.exception.MagasinInconnuException;
import fr.miage.kadi.prix.domain.exception.ProduitInconnuException;
import fr.miage.kadi.prix.domain.model.Montant;
import fr.miage.kadi.prix.domain.model.ReleveDePrix;
import fr.miage.kadi.prix.domain.model.StatutDeSaisie;
import fr.miage.kadi.prix.domain.model.TypeDePreuve;
import fr.miage.kadi.prix.domain.model.UniteDePrix;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Le service avec de faux ports de sortie (en mémoire) et une horloge figée : aucune base, aucun Spring.
 * Fiche F2 : existence du produit et du magasin, date future refusée, prix inhabituel enregistré seulement après confirmation.
 */
class PrixFiableServiceTest {

    private static final Instant MAINTENANT = Instant.parse("2026-11-14T12:00:00Z");
    private static final long LAIT = 1;
    private static final long CLICHY = 1;

    private FauxReleves releves;
    private PrixFiableService service;

    @BeforeEach
    void preparer() {
        releves = new FauxReleves();
        Referentiel referentiel = new Referentiel() {
            @Override
            public boolean produitExiste(long produitId) {
                return produitId == LAIT;
            }

            @Override
            public boolean magasinExiste(long magasinId) {
                return magasinId == CLICHY;
            }
        };
        service = new PrixFiableService(releves, referentiel, ParametresPrixFiable.PAR_DEFAUT, Clock.fixed(MAINTENANT, ZoneOffset.UTC));
        // l'exemple Lactel de la fiche
        releves.ajouter("1.09", TypeDePreuve.TICKET, 3, "A");
        releves.ajouter("1.09", TypeDePreuve.ETIQUETTE, 7, "B");
        releves.ajouter("1.12", TypeDePreuve.ETIQUETTE, 12, "C");
        releves.ajouter("0.10", TypeDePreuve.SAISIE, 13, "D");
        for (int jour = 1; jour <= 5; jour++) {
            releves.ajouter("1.15", TypeDePreuve.ETIQUETTE, jour, "E");
        }
    }

    @Test
    void consulter_le_lait_donne_le_prix_fiable_de_la_fiche() {
        assertThat(service.consulter(LAIT, CLICHY)).hasValueSatisfying(prix ->
                assertThat(prix.prix()).isEqualTo(Montant.de("1.09")));
    }

    @Test
    void la_consultation_ne_lit_que_la_fenetre_de_180_jours() {
        service.consulter(LAIT, CLICHY);

        assertThat(releves.dernierDepuis).isEqualTo(MAINTENANT.minus(Duration.ofDays(180)));
    }

    @Test
    void un_produit_ou_un_magasin_inconnu_est_signale() {
        assertThatThrownBy(() -> service.consulter(9999, CLICHY)).isInstanceOf(ProduitInconnuException.class);
        assertThatThrownBy(() -> service.consulter(LAIT, 9999)).isInstanceOf(MagasinInconnuException.class);
        assertThatThrownBy(() -> service.enregistrer(demande("1.10", null, false, 9999)))
                .isInstanceOf(ProduitInconnuException.class);
        assertThat(releves.enregistres).isEmpty();
    }

    @Test
    void karim_tape_0_10_le_prix_n_est_pas_enregistre_sans_confirmation() {
        ResultatEnregistrement resultat = service.enregistrer(demande("0.10", null, false, LAIT));

        assertThat(resultat.controle().statut()).isEqualTo(StatutDeSaisie.INHABITUEL);
        assertThat(resultat.estEnregistre()).isFalse();
        assertThat(releves.enregistres).isEmpty();
    }

    @Test
    void apres_confirmation_le_prix_inhabituel_est_enregistre_et_marque() {
        ResultatEnregistrement resultat = service.enregistrer(demande("0.10", null, true, LAIT));

        assertThat(resultat.estEnregistre()).isTrue();
        assertThat(releves.enregistres).hasSize(1);
        assertThat(releves.confirmesMalgreAlerte).containsExactly(true);
    }

    @Test
    void un_prix_normal_est_enregistre_sans_question_et_date_de_maintenant() {
        ResultatEnregistrement resultat = service.enregistrer(demande("1.10", null, false, LAIT));

        assertThat(resultat.controle().statut()).isEqualTo(StatutDeSaisie.NORMAL);
        assertThat(resultat.releve().id()).isNotNull();
        assertThat(resultat.releve().releveLe()).isEqualTo(MAINTENANT);
        assertThat(releves.confirmesMalgreAlerte).containsExactly(false);
    }

    @Test
    void un_releve_date_dans_le_futur_est_refuse() {
        Instant dansSixJours = MAINTENANT.plus(Duration.ofDays(6));

        assertThatThrownBy(() -> service.enregistrer(demande("1.10", dansSixJours, false, LAIT)))
                .isInstanceOf(DonneeInvalideException.class);
        assertThat(releves.enregistres).isEmpty();
    }

    @Test
    void une_saisie_absente_ou_absurde_est_refusee() {
        assertThatThrownBy(() -> service.enregistrer(null)).isInstanceOf(DonneeInvalideException.class);
        assertThatThrownBy(() -> service.enregistrer(new DemandeDeReleve(
                LAIT, CLICHY, Montant.de("1.10"), UniteDePrix.PIECE, TypeDePreuve.SAISIE, " ", false, null, false)))
                .isInstanceOf(DonneeInvalideException.class);
    }

    private static DemandeDeReleve demande(String prix, Instant date, boolean confirmer, long produit) {
        return new DemandeDeReleve(produit, CLICHY, Montant.de(prix), UniteDePrix.PIECE, TypeDePreuve.ETIQUETTE,
                "Karim", false, date, confirmer);
    }

    /** Un faux port de sortie : les relevés en mémoire, et la trace de ce que le service lui demande. */
    private static final class FauxReleves implements RelevesDePrix {

        private final List<ReleveDePrix> existants = new ArrayList<>();
        private final List<ReleveDePrix> enregistres = new ArrayList<>();
        private final List<Boolean> confirmesMalgreAlerte = new ArrayList<>();
        private Instant dernierDepuis;
        private long prochainId = 1;

        void ajouter(String prix, TypeDePreuve preuve, int ageEnJours, String contributeur) {
            existants.add(new ReleveDePrix(prochainId++, LAIT, CLICHY, Montant.de(prix), UniteDePrix.PIECE,
                    MAINTENANT.minus(Duration.ofDays(ageEnJours)), preuve, contributeur, false));
        }

        @Override
        public List<ReleveDePrix> duProduitDansLeMagasin(long produitId, long magasinId, Instant depuis) {
            dernierDepuis = depuis;
            return existants.stream().filter(r -> !r.releveLe().isBefore(depuis)).toList();
        }

        @Override
        public List<ReleveDePrix> duProduitDansLEnseigneDuMagasin(long produitId, long magasinId, Instant depuis) {
            return duProduitDansLeMagasin(produitId, magasinId, depuis);
        }

        @Override
        public ReleveDePrix enregistrer(ReleveDePrix releve, boolean confirmeMalgreAlerte) {
            ReleveDePrix avecId = new ReleveDePrix(prochainId++, releve.produitId(), releve.magasinId(), releve.prix(),
                    releve.unite(), releve.releveLe(), releve.preuve(), releve.contributeur(), releve.enPromotion());
            enregistres.add(avecId);
            confirmesMalgreAlerte.add(confirmeMalgreAlerte);
            return avecId;
        }
    }
}

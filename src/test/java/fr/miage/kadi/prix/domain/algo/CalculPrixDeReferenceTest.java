package fr.miage.kadi.prix.domain.algo;

import static fr.miage.kadi.prix.domain.algo.JeuDeReleves.GENNEVILLIERS;
import static fr.miage.kadi.prix.domain.algo.JeuDeReleves.MAINTENANT;
import static fr.miage.kadi.prix.domain.algo.JeuDeReleves.auKilo;
import static fr.miage.kadi.prix.domain.algo.JeuDeReleves.enPromotion;
import static fr.miage.kadi.prix.domain.algo.JeuDeReleves.exempleLactel;
import static fr.miage.kadi.prix.domain.algo.JeuDeReleves.releve;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import fr.miage.kadi.prix.domain.model.Montant;
import fr.miage.kadi.prix.domain.model.MotifExclusion;
import fr.miage.kadi.prix.domain.model.NiveauDeCalcul;
import fr.miage.kadi.prix.domain.model.NiveauDeConfiance;
import fr.miage.kadi.prix.domain.model.PrixDeReference;
import fr.miage.kadi.prix.domain.model.ReleveDePrix;
import fr.miage.kadi.prix.domain.model.ReleveEcarte;
import fr.miage.kadi.prix.domain.model.TypeDePreuve;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;
import org.junit.jupiter.api.Test;

/** Les critères d'acceptation de la fiche F2 sur le calcul du prix de référence. */
class CalculPrixDeReferenceTest {

    private final CalculPrixDeReference calcul = new CalculPrixDeReference(ParametresPrixFiable.PAR_DEFAUT);

    @Test
    void exemple_lactel_de_la_fiche() {
        PrixDeReference resultat = auMagasin(exempleLactel());

        assertThat(resultat.prix()).isEqualTo(Montant.de("1.09"));
        assertThat(resultat.niveau()).isEqualTo(NiveauDeCalcul.MAGASIN);
        assertThat(motifs(resultat)).containsExactly("0.10:ABERRANT");
        assertThat(resultat.retenus()).hasSize(8);
        assertThat(resultat.confiance().score()).isCloseTo(0.646, within(0.001));
        assertThat(resultat.confiance().niveau()).isEqualTo(NiveauDeConfiance.ELEVEE);
    }

    @Test
    void un_contributeur_qui_saisit_sans_fin_ne_peut_pas_imposer_son_prix() {
        List<ReleveDePrix> releves = new ArrayList<>(List.of(
                releve("1.09", TypeDePreuve.TICKET, 3, "A"),
                releve("1.09", TypeDePreuve.TICKET, 4, "B")));
        for (int jour = 1; jour <= 40; jour++) {
            releves.add(releve("1.15", TypeDePreuve.TICKET, jour, "E"));
        }

        assertThat(auMagasin(releves).prix()).isEqualTo(Montant.de("1.09"));
    }

    @Test
    void avec_moins_de_3_contributeurs_rien_n_est_ecarte() {
        List<ReleveDePrix> deuxTemoins = List.of(
                releve("1.09", TypeDePreuve.TICKET, 3, "A"),
                releve("0.10", TypeDePreuve.TICKET, 3, "B"));

        PrixDeReference resultat = auMagasin(deuxTemoins);

        assertThat(resultat.ecartes()).isEmpty();
        assertThat(resultat.retenus()).hasSize(2);
    }

    @Test
    void un_doublon_ne_compte_qu_une_fois() {
        ReleveDePrix premier = releve("1.09", TypeDePreuve.ETIQUETTE, 3, "A");
        ReleveDePrix memeJourMemePrix = releve("1.09", TypeDePreuve.ETIQUETTE, 3, "A");

        PrixDeReference resultat = auMagasin(List.of(premier, memeJourMemePrix));

        assertThat(resultat.retenus()).hasSize(1);
        assertThat(resultat.ecartes()).extracting(ReleveEcarte::motif).containsExactly(MotifExclusion.DOUBLON);
    }

    @Test
    void promotion_releve_trop_ancien_et_autre_unite_sont_exclus_avec_leur_motif() {
        List<ReleveDePrix> releves = List.of(
                releve("3.49", TypeDePreuve.ETIQUETTE, 6, "A"),
                releve("3.59", TypeDePreuve.TICKET, 8, "B"),
                enPromotion("2.49", 4, "C"),
                releve("2.99", TypeDePreuve.TICKET, 181, "D"),
                auKilo("13.96", 5, "F"));

        PrixDeReference resultat = auMagasin(releves);

        assertThat(resultat.ecartes()).extracting(ReleveEcarte::motif)
                .containsExactlyInAnyOrder(MotifExclusion.EN_PROMOTION, MotifExclusion.TROP_ANCIEN, MotifExclusion.UNITE_INCOMPATIBLE);
        assertThat(resultat.retenus()).hasSize(2);
    }

    @Test
    void quand_tous_les_prix_sont_identiques_le_plancher_tolere_un_centime_mais_pas_dix_pour_cent() {
        List<ReleveDePrix> unCentime = List.of(
                releve("1.09", TypeDePreuve.TICKET, 1, "A"),
                releve("1.09", TypeDePreuve.TICKET, 2, "B"),
                releve("1.09", TypeDePreuve.TICKET, 3, "C"),
                releve("1.10", TypeDePreuve.TICKET, 4, "D"));
        List<ReleveDePrix> dixPourCent = List.of(
                releve("1.09", TypeDePreuve.TICKET, 1, "A"),
                releve("1.09", TypeDePreuve.TICKET, 2, "B"),
                releve("1.09", TypeDePreuve.TICKET, 3, "C"),
                releve("1.20", TypeDePreuve.TICKET, 4, "D"));

        assertThat(auMagasin(unCentime).ecartes()).isEmpty();
        assertThat(motifs(auMagasin(dixPourCent))).containsExactly("1.20:ABERRANT");
    }

    @Test
    void avec_un_seul_temoin_au_magasin_on_se_replie_sur_l_enseigne_qui_en_a_davantage() {
        ReleveDePrix seulAClichy = releve("0.95", TypeDePreuve.SAISIE, 2, "F");
        List<ReleveDePrix> enseigne = List.of(seulAClichy,
                releve(GENNEVILLIERS, "0.89", TypeDePreuve.TICKET, 4, "G"),
                releve(GENNEVILLIERS, "0.89", TypeDePreuve.ETIQUETTE, 9, "H"),
                releve(GENNEVILLIERS, "0.90", TypeDePreuve.ETIQUETTE, 15, "I"));

        PrixDeReference resultat = calcul.calculer(List.of(seulAClichy), enseigne, MAINTENANT).orElseThrow();

        assertThat(resultat.niveau()).isEqualTo(NiveauDeCalcul.ENSEIGNE);
        assertThat(resultat.prix()).isEqualTo(Montant.de("0.89"));
        // la pénalité de repli garantit qu'un prix d'enseigne n'est jamais affiché en confiance élevée
        assertThat(resultat.confiance().score()).isLessThanOrEqualTo(0.5);
    }

    @Test
    void sans_temoin_de_plus_dans_l_enseigne_on_reste_au_magasin() {
        List<ReleveDePrix> seulTemoin = List.of(releve("1.30", TypeDePreuve.SAISIE, 2, "X"));

        assertThat(calcul.calculer(seulTemoin, seulTemoin, MAINTENANT).orElseThrow().niveau())
                .isEqualTo(NiveauDeCalcul.MAGASIN);
    }

    @Test
    void sans_aucun_releve_il_n_y_a_pas_de_prix_plutot_qu_un_prix_invente() {
        assertThat(calcul.calculer(List.of(), List.of(), MAINTENANT)).isEmpty();
        assertThat(calcul.calculer(null, null, MAINTENANT)).isEmpty();
    }

    @Test
    void la_confiance_baisse_quand_les_releves_vieillissent() {
        double recents = auMagasin(troisTemoins("1.09", "1.09", "1.09", 1)).confiance().score();
        double anciens = auMagasin(troisTemoins("1.09", "1.09", "1.09", 60)).confiance().score();

        assertThat(anciens).isLessThan(recents);
    }

    @Test
    void la_confiance_baisse_quand_les_temoins_sont_en_desaccord() {
        double daccord = auMagasin(troisTemoins("1.09", "1.09", "1.10", 1)).confiance().score();
        double partages = auMagasin(troisTemoins("0.99", "1.09", "1.19", 1)).confiance().score();

        assertThat(partages).isLessThan(daccord);
    }

    @Test
    void la_confiance_baisse_quand_il_y_a_moins_de_temoins_independants() {
        double troisPersonnes = auMagasin(troisTemoins("1.09", "1.09", "1.09", 1)).confiance().score();
        double unePersonne = auMagasin(List.of(
                releve("1.09", TypeDePreuve.TICKET, 1, "A"),
                releve("1.09", TypeDePreuve.TICKET, 2, "A"),
                releve("1.09", TypeDePreuve.TICKET, 3, "A"))).confiance().score();

        assertThat(unePersonne).isLessThan(troisPersonnes);
    }

    @Test
    void a_entrees_egales_le_resultat_est_identique_malgre_le_pivot_aleatoire() {
        List<ReleveDePrix> releves = exempleLactel();

        assertThat(auMagasin(releves)).isEqualTo(auMagasin(releves));
    }

    @Property(tries = 300)
    void une_valeur_aberrante_ne_deplace_pas_un_prix_sur_lequel_au_moins_3_temoins_s_accordent(
            @ForAll @IntRange(min = 50, max = 2000) int centimes,
            @ForAll @IntRange(min = 3, max = 10) int temoins,
            @ForAll @IntRange(min = 2, max = 50) int facteur,
            @ForAll boolean tropBas) {
        String prix = new BigDecimal(centimes).movePointLeft(2).toPlainString();
        List<ReleveDePrix> accord = new ArrayList<>();
        for (int i = 0; i < temoins; i++) {
            accord.add(releve(prix, TypeDePreuve.TICKET, 1 + i, "temoin-" + i));
        }
        int centimesAberrants = tropBas ? Math.max(1, centimes / facteur) : centimes * facteur;
        List<ReleveDePrix> avecErreur = new ArrayList<>(accord);
        avecErreur.add(releve(new BigDecimal(centimesAberrants).movePointLeft(2).toPlainString(), TypeDePreuve.SAISIE, 1, "distrait"));

        assertThat(auMagasin(avecErreur).prix()).isEqualTo(auMagasin(accord).prix());
    }

    private PrixDeReference auMagasin(List<ReleveDePrix> releves) {
        return calcul.calculer(releves, releves, MAINTENANT).orElseThrow();
    }

    private static List<ReleveDePrix> troisTemoins(String a, String b, String c, int age) {
        return List.of(
                releve(a, TypeDePreuve.TICKET, age, "A"),
                releve(b, TypeDePreuve.TICKET, age, "B"),
                releve(c, TypeDePreuve.TICKET, age, "C"));
    }

    private static List<String> motifs(PrixDeReference resultat) {
        return resultat.ecartes().stream().map(e -> e.releve().prix().valeur() + ":" + e.motif()).toList();
    }
}

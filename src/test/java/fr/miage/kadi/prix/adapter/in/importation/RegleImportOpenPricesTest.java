package fr.miage.kadi.prix.adapter.in.importation;

import static org.assertj.core.api.Assertions.assertThat;

import fr.miage.kadi.prix.adapter.in.importation.RegleImportOpenPrices.Lecture;
import fr.miage.kadi.prix.adapter.in.importation.RegleImportOpenPrices.Rejet;
import fr.miage.kadi.prix.application.port.in.ReleveAImporter;
import fr.miage.kadi.prix.domain.model.Montant;
import fr.miage.kadi.prix.domain.model.TypeDePreuve;
import fr.miage.kadi.prix.domain.model.UniteDePrix;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Les règles qui transforment une ligne Open Prices en relevé (fiche F2, Impact, Import Open Prices). */
class RegleImportOpenPricesTest {

    @Test
    void une_ligne_correcte_devient_un_releve_d_open_prices() {
        ReleveAImporter releve = lire(ligne("1.250", "RECEIPT", "Carrefour Market", "Carrefour Market")).releve();

        assertThat(releve.source()).isEqualTo("OPEN_PRICES");
        assertThat(releve.referenceExterne()).isEqualTo("42629");
        assertThat(releve.codeBarres()).isEqualTo("3560070283484");
        assertThat(releve.prix()).isEqualTo(Montant.de("1.25"));
        assertThat(releve.preuve()).isEqualTo(TypeDePreuve.TICKET);
        assertThat(releve.unite()).isEqualTo(UniteDePrix.PIECE);
        assertThat(releve.referenceMagasin()).isEqualTo("node/4966187139");
        assertThat(releve.contributeur()).isEqualTo("openprices:a91f5c8b");
        // le 28 octobre 2024 à midi, heure de Paris (UTC+1 en hiver)
        assertThat(releve.releveLe()).isEqualTo(Instant.parse("2024-10-28T11:00:00Z"));
    }

    @ParameterizedTest
    @CsvSource({
            "E.Leclerc, E.Leclerc",
            "E. Leclerc, E.Leclerc",
            "Centre Commercial E.Leclerc, E.Leclerc",
            "Centre Commercial E. Leclerc, E.Leclerc",
            "centre commercial  E.  Leclerc, E.Leclerc",
            "Carrefour City, Carrefour City",
            "E.Leclerc Drive, E.Leclerc Drive"
    })
    void une_meme_marque_ecrite_differemment_donne_une_seule_enseigne_et_les_formats_restent_distincts(
            String marque, String enseigneAttendue) {
        assertThat(RegleImportOpenPrices.enseigne(marque, "un magasin", "node/1")).isEqualTo(enseigneAttendue);
    }

    @Test
    void un_magasin_sans_marque_est_sa_propre_enseigne() {
        assertThat(RegleImportOpenPrices.enseigne("", "Épicerie du marché", "node/7")).isEqualTo("Épicerie du marché (node/7)");
        assertThat(RegleImportOpenPrices.enseigne("", "Épicerie du marché", "node/8")).isEqualTo("Épicerie du marché (node/8)");
    }

    @ParameterizedTest
    @CsvSource({
            "RECEIPT, TICKET",
            "PRICE_TAG, ETIQUETTE",
            "GDPR_REQUEST, EXPORT_FIDELITE",
            "SHOP_IMPORT, IMPORT_ENSEIGNE",
            "'', SAISIE"
    })
    void les_preuves_d_open_prices_sont_traduites(String openPrices, TypeDePreuve kadi) {
        assertThat(RegleImportOpenPrices.preuve(openPrices)).isEqualTo(kadi);
    }

    @ParameterizedTest
    @CsvSource({
            "1.995, PRIX_INVALIDE",
            "0, PRIX_INVALIDE",
            "-1.00, PRIX_INVALIDE",
            "gratuit, PRIX_INVALIDE"
    })
    void un_prix_inexploitable_est_rejete_plutot_qu_arrondi(String prix, Rejet motif) {
        assertThat(lire(ligne(prix, "PRICE_TAG", "Lidl", "Lidl")).rejet()).isEqualTo(motif);
    }

    @Test
    void les_autres_lignes_inexploitables_sont_rejetees_avec_leur_motif() {
        assertThat(lire("42629\ttrop\tcourte").rejet()).isEqualTo(Rejet.LIGNE_INCOMPLETE);
        assertThat(lire(ligne("1.25", "PRICE_TAG", "Lidl", "Lidl").replace("3560070283484", "123456789012345")).rejet())
                .isEqualTo(Rejet.CODE_BARRES_INVALIDE);
        assertThat(lire(ligne("1.25", "PRICE_TAG", "Lidl", "Lidl").replace("NODE", "POINT")).rejet())
                .isEqualTo(Rejet.MAGASIN_INVALIDE);
        assertThat(lire(ligne("1.25", "PRICE_TAG", "Lidl", "Lidl").replace("2024-10-28", "hier")).rejet())
                .isEqualTo(Rejet.DATE_INVALIDE);
    }

    @Test
    void un_produit_sans_libelle_garde_son_code_barres_comme_nom() {
        String sansLibelle = ligne("1.25", "PRICE_TAG", "Lidl", "Lidl").replace("Épinards", "");

        assertThat(lire(sansLibelle).releve().libelleProduit()).isEqualTo("Produit 3560070283484");
    }

    private static Lecture lire(String ligne) {
        return RegleImportOpenPrices.lire(ligne);
    }

    /** Une ligne au format de extraire_open_prices.py, inspirée d'un vrai relevé de l'export. */
    private static String ligne(String prix, String preuve, String magasin, String marque) {
        return String.join("\t", "42629", "3560070283484", "Épinards", prix, "", "false", "2024-10-28",
                "a91f5c8b", preuve, "NODE", "4966187139", magasin, marque);
    }
}

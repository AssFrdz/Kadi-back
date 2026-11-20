package fr.miage.kadi.prix.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import fr.miage.kadi.prix.domain.model.Montant;
import fr.miage.kadi.prix.domain.model.ReleveDePrix;
import fr.miage.kadi.prix.domain.model.TypeDePreuve;
import fr.miage.kadi.prix.domain.model.UniteDePrix;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * Les requêtes du module prix sur un vrai PostgreSQL (migrations V1 et V2 appliquées par Flyway).
 * Chaque test est annulé à la fin : les tests ne se voient pas entre eux.
 */
@SpringBootTest
@Transactional
class PersistancePrixAdapterIT {

    private static final Instant MAINTENANT = Instant.now().truncatedTo(ChronoUnit.SECONDS);

    @Autowired
    private PersistancePrixAdapter adaptateur;

    @Autowired
    private JdbcTemplate jdbc;

    private long lait;
    private long pates;
    private long clichy;
    private long gennevilliers;
    private long carrefourCity;

    @BeforeEach
    void preparer() {
        long leclerc = inserer("INSERT INTO enseigne (nom) VALUES ('E.Leclerc (test)') RETURNING id");
        long carrefour = inserer("INSERT INTO enseigne (nom) VALUES ('Carrefour City (test)') RETURNING id");
        clichy = inserer("INSERT INTO magasin (enseigne_id, nom) VALUES (" + leclerc + ", 'Leclerc Clichy') RETURNING id");
        gennevilliers = inserer("INSERT INTO magasin (enseigne_id, nom) VALUES (" + leclerc + ", 'Leclerc Gennevilliers') RETURNING id");
        carrefourCity = inserer("INSERT INTO magasin (enseigne_id, nom) VALUES (" + carrefour + ", 'Carrefour City Clichy') RETURNING id");
        lait = inserer("INSERT INTO produit (libelle, code_barres) VALUES ('Lait test', '2999999999991') RETURNING id");
        pates = inserer("INSERT INTO produit (libelle, code_barres) VALUES ('Pâtes test', '2999999999992') RETURNING id");
    }

    @Test
    void un_releve_enregistre_recoit_un_identifiant_et_se_relit_a_l_identique() {
        ReleveDePrix enregistre = adaptateur.enregistrer(releve(lait, clichy, "1.09", 3, "A"), false);

        assertThat(enregistre.id()).isNotNull();
        assertThat(adaptateur.duProduitDansLeMagasin(lait, clichy, MAINTENANT.minus(Duration.ofDays(180))))
                .containsExactly(enregistre);
    }

    @Test
    void la_lecture_du_magasin_respecte_la_fenetre_le_produit_et_le_magasin() {
        ReleveDePrix recent = adaptateur.enregistrer(releve(lait, clichy, "1.09", 3, "A"), false);
        adaptateur.enregistrer(releve(lait, clichy, "0.99", 200, "B"), false);
        adaptateur.enregistrer(releve(pates, clichy, "0.89", 3, "C"), false);
        adaptateur.enregistrer(releve(lait, gennevilliers, "1.10", 3, "D"), false);

        assertThat(adaptateur.duProduitDansLeMagasin(lait, clichy, MAINTENANT.minus(Duration.ofDays(180))))
                .containsExactly(recent);
    }

    @Test
    void la_lecture_de_l_enseigne_reunit_les_magasins_de_la_meme_enseigne_et_seulement_eux() {
        ReleveDePrix aClichy = adaptateur.enregistrer(releve(lait, clichy, "1.09", 3, "A"), false);
        ReleveDePrix aGennevilliers = adaptateur.enregistrer(releve(lait, gennevilliers, "1.10", 4, "B"), false);
        adaptateur.enregistrer(releve(lait, carrefourCity, "1.35", 2, "C"), false);

        assertThat(adaptateur.duProduitDansLEnseigneDuMagasin(lait, clichy, MAINTENANT.minus(Duration.ofDays(180))))
                .containsExactlyInAnyOrder(aClichy, aGennevilliers);
    }

    @Test
    void la_confirmation_malgre_l_alerte_est_conservee_et_la_source_est_kadi() {
        ReleveDePrix enregistre = adaptateur.enregistrer(releve(lait, clichy, "0.10", 0, "Karim"), true);

        assertThat(jdbc.queryForObject("SELECT confirme_malgre_alerte FROM releve_de_prix WHERE id = ?", Boolean.class, enregistre.id()))
                .isTrue();
        assertThat(jdbc.queryForObject("SELECT source FROM releve_de_prix WHERE id = ?", String.class, enregistre.id()))
                .isEqualTo("KADI");
    }

    @Test
    void l_existence_d_un_produit_et_d_un_magasin_se_verifie_en_base() {
        assertThat(adaptateur.produitExiste(lait)).isTrue();
        assertThat(adaptateur.produitExiste(-1)).isFalse();
        assertThat(adaptateur.magasinExiste(clichy)).isTrue();
        assertThat(adaptateur.magasinExiste(-1)).isFalse();
    }

    private ReleveDePrix releve(long produit, long magasin, String prix, int ageEnJours, String contributeur) {
        return new ReleveDePrix(null, produit, magasin, Montant.de(prix), UniteDePrix.PIECE,
                MAINTENANT.minus(Duration.ofDays(ageEnJours)), TypeDePreuve.ETIQUETTE, contributeur, false);
    }

    private long inserer(String sql) {
        return jdbc.queryForObject(sql, Long.class);
    }
}

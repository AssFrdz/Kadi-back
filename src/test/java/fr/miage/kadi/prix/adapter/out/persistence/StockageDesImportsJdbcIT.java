package fr.miage.kadi.prix.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import fr.miage.kadi.prix.application.port.in.ReleveAImporter;
import fr.miage.kadi.prix.application.port.out.StockageDesImports.ReleveAEcrire;
import fr.miage.kadi.prix.domain.model.Montant;
import fr.miage.kadi.prix.domain.model.TypeDePreuve;
import fr.miage.kadi.prix.domain.model.UniteDePrix;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** L'écriture de l'import sur un vrai PostgreSQL : relancer l'import ne crée aucun doublon (contraintes de V1 et V2). */
@SpringBootTest
@Transactional
class StockageDesImportsJdbcIT {

    @Autowired
    private StockageDesImportsJdbc stockage;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void enseigne_magasin_et_produit_sont_crees_une_fois_puis_retrouves() {
        long enseigne = stockage.enseigne("Lidl (test)");
        long magasin = stockage.magasin("node/999999001", "Lidl Clichy", enseigne);
        long produit = stockage.produit("2999999999993", "Café test");

        assertThat(stockage.enseigne("Lidl (test)")).isEqualTo(enseigne);
        assertThat(stockage.magasin("node/999999001", "Lidl Clichy", enseigne)).isEqualTo(magasin);
        assertThat(stockage.produit("2999999999993", "Café test")).isEqualTo(produit);
    }

    @Test
    void relancer_l_import_n_ajoute_aucun_releve_en_double() {
        long enseigne = stockage.enseigne("Lidl (test)");
        long magasin = stockage.magasin("node/999999001", "Lidl Clichy", enseigne);
        long produit = stockage.produit("2999999999993", "Café test");
        List<ReleveAEcrire> lot = List.of(
                new ReleveAEcrire(produit, magasin, releve("test-1", "3.49")),
                new ReleveAEcrire(produit, magasin, releve("test-2", "3.59")));

        assertThat(stockage.ajouterReleves(lot)).isEqualTo(2);
        assertThat(stockage.ajouterReleves(lot)).isZero();
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM releve_de_prix WHERE source = 'OPEN_PRICES' AND reference_externe LIKE 'test-%'", Long.class))
                .isEqualTo(2);
    }

    private static ReleveAImporter releve(String reference, String prix) {
        return new ReleveAImporter("OPEN_PRICES", reference, "2999999999993", "Café test", "node/999999001", "Lidl Clichy",
                "Lidl (test)", Montant.de(prix), UniteDePrix.PIECE, Instant.parse("2026-11-01T11:00:00Z"),
                TypeDePreuve.ETIQUETTE, "openprices:abc", false);
    }
}

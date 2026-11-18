package fr.miage.kadi.prix.adapter.out.persistence;

import fr.miage.kadi.prix.application.port.in.ReleveAImporter;
import fr.miage.kadi.prix.application.port.out.StockageDesImports;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * L'import en SQL direct (JDBC) plutôt qu'en JPA : des centaines de milliers de lignes, écrites par lots.
 * « ON CONFLICT DO NOTHING » s'appuie sur les contraintes d'unicité de V1 et V2 : relancer l'import n'ajoute aucun doublon.
 */
@Component
class StockageDesImportsJdbc implements StockageDesImports {

    private final JdbcTemplate jdbc;

    StockageDesImportsJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public long enseigne(String nom) {
        jdbc.update("INSERT INTO enseigne (nom) VALUES (?) ON CONFLICT (nom) DO NOTHING", nom);
        return jdbc.queryForObject("SELECT id FROM enseigne WHERE nom = ?", Long.class, nom);
    }

    @Override
    public long magasin(String referenceExterne, String nom, long enseigneId) {
        jdbc.update("""
                INSERT INTO magasin (enseigne_id, nom, reference_externe) VALUES (?, ?, ?)
                ON CONFLICT (reference_externe) DO NOTHING
                """, enseigneId, nom, referenceExterne);
        return jdbc.queryForObject("SELECT id FROM magasin WHERE reference_externe = ?", Long.class, referenceExterne);
    }

    @Override
    public long produit(String codeBarres, String libelle) {
        jdbc.update("INSERT INTO produit (libelle, code_barres) VALUES (?, ?) ON CONFLICT (code_barres) DO NOTHING",
                libelle, codeBarres);
        return jdbc.queryForObject("SELECT id FROM produit WHERE code_barres = ?", Long.class, codeBarres);
    }

    @Override
    @Transactional
    public int ajouterReleves(List<ReleveAEcrire> releves) {
        int[] lignesAjoutees = jdbc.batchUpdate("""
                INSERT INTO releve_de_prix
                    (produit_id, magasin_id, prix, releve_le, source, type_preuve, contributeur, unite, en_promotion, reference_externe)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (source, reference_externe) DO NOTHING
                """, releves, releves.size(), (requete, aEcrire) -> {
            ReleveAImporter releve = aEcrire.releve();
            requete.setLong(1, aEcrire.produitId());
            requete.setLong(2, aEcrire.magasinId());
            requete.setBigDecimal(3, releve.prix().valeur());
            requete.setTimestamp(4, Timestamp.from(releve.releveLe()));
            requete.setString(5, releve.source());
            requete.setString(6, releve.preuve().name());
            requete.setString(7, releve.contributeur());
            requete.setString(8, releve.unite().name());
            requete.setBoolean(9, releve.enPromotion());
            requete.setString(10, releve.referenceExterne());
        })[0];
        return Arrays.stream(lignesAjoutees).sum();
    }
}

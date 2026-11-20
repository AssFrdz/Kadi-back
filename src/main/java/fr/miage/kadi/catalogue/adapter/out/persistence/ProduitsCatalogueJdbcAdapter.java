package fr.miage.kadi.catalogue.adapter.out.persistence;

import fr.miage.kadi.catalogue.application.port.out.ProduitsCatalogueRepository;
import fr.miage.kadi.catalogue.domain.model.ProduitCatalogue;
import fr.miage.kadi.catalogue.domain.model.FiltresCatalogue;
import java.util.List;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.util.function.Consumer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
class ProduitsCatalogueJdbcAdapter implements ProduitsCatalogueRepository {

    private static final String CANDIDATS_SQL = """
            SELECT p.id, p.libelle, p.code_barres, p.marque, p.format, off.produit_id AS off_id,
                   off.image_url, off.image_auteur
            FROM produit p
            LEFT JOIN produit_open_food_facts off ON off.produit_id = p.id
            CROSS JOIN LATERAL (
                SELECT lower(translate(concat_ws(' ', p.libelle, p.marque, p.format),
                    'ÀÂÄÁÃÅÇÉÈÊËÍÌÎÏÑÓÒÔÖÕÚÙÛÜÝŸàâäáãåçéèêëíìîïñóòôöõúùûüýÿ',
                    'AAAAAACEEEEIIIINOOOOOUUUUYYaaaaaaceeeeiiiinooooouuuuyy')) AS nom
            ) normalise
            WHERE word_similarity(?, normalise.nom) >= 0.2
            """;

    private static final String FILTRES_SQL = """
            AND (? = '' OR EXISTS (
                SELECT 1 FROM unnest(string_to_array(p.marque, ',')) AS marque(valeur)
                WHERE lower(trim(marque.valeur)) = lower(?)
            ))
            AND (? = '' OR position(lower(?) IN lower(p.format)) > 0)
            """;

    private final JdbcTemplate jdbc;

    ProduitsCatalogueJdbcAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<ProduitCatalogue> trouverApres(long dernierId, int limite) {
        return trouverApres(dernierId, limite, new FiltresCatalogue(null, null));
    }

    @Override
    public List<ProduitCatalogue> trouverApres(long dernierId, int limite, FiltresCatalogue filtres) {
        return jdbc.query("""
                SELECT p.id, p.libelle, p.code_barres, p.marque, p.format, off.produit_id AS off_id,
                       off.image_url, off.image_auteur
                FROM produit p
                LEFT JOIN produit_open_food_facts off ON off.produit_id = p.id
                WHERE p.id > ?
                """ + FILTRES_SQL + """
                ORDER BY p.id
                LIMIT ?
                """,
                (resultat, ligne) -> lireProduit(resultat),
                dernierId, filtres.marque(), filtres.marque(), filtres.format(), filtres.format(), limite);
    }

    @Override
    public List<ProduitCatalogue> presSelectionner(String recherche, int limite) {
        return jdbc.query(CANDIDATS_SQL + " ORDER BY word_similarity(?, normalise.nom) DESC, p.id LIMIT ?",
                (resultat, ligne) -> lireProduit(resultat), recherche, recherche, limite);
    }

    @Override
    @Transactional(readOnly = true)
    public void parcourirCandidats(String recherche, FiltresCatalogue filtres, Consumer<ProduitCatalogue> consommateur) {
        jdbc.query(connexion -> {
            PreparedStatement requete = connexion.prepareStatement(CANDIDATS_SQL + FILTRES_SQL);
            requete.setString(1, recherche);
            requete.setString(2, filtres.marque());
            requete.setString(3, filtres.marque());
            requete.setString(4, filtres.format());
            requete.setString(5, filtres.format());
            requete.setFetchSize(256);
            return requete;
        }, resultat -> {
            consommateur.accept(lireProduit(resultat));
        });
    }

    private ProduitCatalogue lireProduit(ResultSet resultat) throws SQLException {
        return new ProduitCatalogue(
                resultat.getLong("id"),
                resultat.getString("libelle"),
                resultat.getString("code_barres"),
                resultat.getObject("off_id") == null ? null
                        : "https://world.openfoodfacts.org/product/" + resultat.getString("code_barres"),
                resultat.getString("image_url"),
                resultat.getString("image_auteur"),
                resultat.getString("marque"),
                resultat.getString("format"));
    }
}

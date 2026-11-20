package fr.miage.kadi.prix.adapter.out.persistence;

import fr.miage.kadi.prix.application.port.in.RechercherPourLaSaisie.MagasinTrouve;
import fr.miage.kadi.prix.application.port.in.RechercherPourLaSaisie.ProduitTrouve;
import fr.miage.kadi.prix.application.port.out.CatalogueDeRecherche;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Recherche provisoire en SQL : chaque mot doit apparaître dans le nom (LIKE, insensible à la casse).
 * Les mots sont passés en paramètres, jamais concaténés dans la requête (pas d'injection SQL).
 */
@Component
class CatalogueDeRechercheJdbc implements CatalogueDeRecherche {

    private final JdbcTemplate jdbc;

    CatalogueDeRechercheJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<ProduitTrouve> produitsContenant(List<String> mots, int limite) {
        String conditions = String.join(" AND ", mots.stream().map(mot -> "lower(libelle) LIKE ?").toList());
        return jdbc.query(
                "SELECT id, libelle, code_barres FROM produit WHERE " + conditions + " ORDER BY libelle LIMIT ?",
                (ligne, numero) -> new ProduitTrouve(ligne.getLong("id"), ligne.getString("libelle"), ligne.getString("code_barres")),
                parametres(mots, limite));
    }

    @Override
    public List<ProduitTrouve> produitDeCodeBarres(String codeBarres) {
        return jdbc.query(
                "SELECT id, libelle, code_barres FROM produit WHERE code_barres = ?",
                (ligne, numero) -> new ProduitTrouve(ligne.getLong("id"), ligne.getString("libelle"), ligne.getString("code_barres")),
                codeBarres);
    }

    @Override
    public List<MagasinTrouve> magasinsContenant(List<String> mots, int limite) {
        String conditions = String.join(" AND ",
                mots.stream().map(mot -> "lower(m.nom || ' ' || e.nom) LIKE ?").toList());
        return jdbc.query("""
                        SELECT m.id, m.nom, e.nom AS enseigne
                        FROM magasin m JOIN enseigne e ON e.id = m.enseigne_id
                        WHERE %s
                        ORDER BY m.nom LIMIT ?
                        """.formatted(conditions),
                (ligne, numero) -> new MagasinTrouve(ligne.getLong("id"), ligne.getString("nom"), ligne.getString("enseigne")),
                parametres(mots, limite));
    }

    /** Chaque mot devient « %mot% » ; les caractères spéciaux de LIKE sont neutralisés. */
    private static Object[] parametres(List<String> mots, int limite) {
        List<Object> valeurs = new ArrayList<>();
        for (String mot : mots) {
            valeurs.add("%" + mot.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%");
        }
        valeurs.add(limite);
        return valeurs.toArray();
    }
}

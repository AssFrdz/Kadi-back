package fr.miage.kadi.catalogue.adapter.out.persistence;

import fr.miage.kadi.catalogue.application.port.out.RechercheProduitsCompatiblesRepository;
import fr.miage.kadi.catalogue.domain.model.IdentifiantCompte;
import fr.miage.kadi.catalogue.domain.model.NatureBesoin;
import fr.miage.kadi.catalogue.domain.model.ProduitRechercheCompatible;
import fr.miage.kadi.catalogue.domain.model.TypeReference;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
class RechercheProduitsCompatiblesJdbcAdapter implements RechercheProduitsCompatiblesRepository {
    /** Les mots saisis peuvent figurer dans le libellé, la marque ou le format (« spaghetti barilla »). */
    private static final String TEXTE = "concat_ws(' ', libelle, marque, format)";
    private static final String NOM_NORMALISE = NormalisationCatalogueSql.EXPRESSION.formatted(TEXTE);
    private static final String COLONNES =
            "SELECT id, libelle, code_barres, marque, format, type_reference, nature_besoin";
    private static final Pattern QUANTITE = Pattern.compile("([0-9]+)(kg|mg|g|ml|cl|dl|l)");

    private final JdbcTemplate jdbc;

    RechercheProduitsCompatiblesJdbcAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<ProduitRechercheCompatible> trouverParCodeBarres(String codeBarres) {
        return jdbc.query(COLONNES + " FROM produit WHERE type_reference = 'PRECIS' AND code_barres = ?",
                (resultat, ligne) -> lire(resultat), codeBarres);
    }

    @Override
    @Transactional(readOnly = true)
    public void parcourirCorrespondances(List<String> mots, List<String> motsNormalises, boolean inclureGeneriques,
            Consumer<ProduitRechercheCompatible> consommateur) {
        List<String> parametres = new ArrayList<>();
        String conditions = conditions(mots, "lower(" + TEXTE + ")", false, parametres);
        if (!motsNormalises.isEmpty()) {
            conditions = "(" + conditions + ") OR ("
                    + conditions(motsNormalises, NOM_NORMALISE, true, parametres) + ")";
        }
        String types = inclureGeneriques ? "type_reference IN ('PRECIS', 'GENERIQUE')" : "type_reference = 'PRECIS'";
        String sql = COLONNES + " FROM produit WHERE " + types + " AND (" + conditions + ")";
        jdbc.query(connexion -> {
            PreparedStatement requete = connexion.prepareStatement(sql);
            int position = 1;
            for (String parametre : parametres) {
                requete.setString(position++, parametre);
            }
            requete.setFetchSize(256);
            return requete;
        }, resultat -> {
            consommateur.accept(lire(resultat));
        });
    }

    @Override
    public List<ProduitRechercheCompatible> listerBesoinsGeneriques() {
        return jdbc.query(COLONNES + " FROM produit WHERE type_reference = 'GENERIQUE' ORDER BY id",
                (resultat, ligne) -> lire(resultat));
    }

    @Override
    public List<ProduitRechercheCompatible> listerBesoinsPersonnels(IdentifiantCompte proprietaire) {
        return jdbc.query("SELECT id, libelle FROM besoin_personnalise WHERE compte_id = ? AND statut <> 'VALIDE' ORDER BY id",
                (resultat, ligne) -> new ProduitRechercheCompatible(resultat.getLong("id"),
                        resultat.getString("libelle"), null, null, null, TypeReference.BESOIN_PERSONNALISE, null),
                proprietaire.valeur());
    }

    private static ProduitRechercheCompatible lire(ResultSet resultat) throws SQLException {
        boolean generique = "GENERIQUE".equals(resultat.getString("type_reference"));
        return new ProduitRechercheCompatible(resultat.getLong("id"), resultat.getString("libelle"),
                resultat.getString("code_barres"), resultat.getString("marque"), resultat.getString("format"),
                generique ? TypeReference.BESOIN_GENERIQUE : TypeReference.PRODUIT,
                generique ? NatureBesoin.valueOf(resultat.getString("nature_besoin")) : null);
    }

    private static String conditions(List<String> mots, String colonne, boolean variantesUnites,
            List<String> parametres) {
        StringJoiner conditions = new StringJoiner(" AND ");
        for (String mot : mots) {
            parametres.add(mot);
            String condition = "position(lower(?) in " + colonne + ") > 0";
            Matcher quantite = QUANTITE.matcher(mot);
            if (variantesUnites && quantite.matches()) {
                parametres.add("\\m" + quantite.group(1) + "[[:space:]]*" + quantite.group(2) + "\\M");
                condition = "(" + condition + " OR " + colonne + " ~* ?)";
            }
            conditions.add(condition);
        }
        return conditions.toString();
    }
}

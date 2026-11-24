package fr.miage.kadi.catalogue.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import fr.miage.kadi.catalogue.application.service.RechercherProduitsCompatiblesService;
import fr.miage.kadi.catalogue.domain.algo.SimilariteRecherche;
import fr.miage.kadi.catalogue.domain.model.ProduitRechercheCompatible;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@EnabledIfEnvironmentVariable(named = "KADI_TEST_RECHERCHE_VOLUMINEUX", matches = "true")
class VolumeRechercheCompatibleIT {
    private static final int ECHANTILLONS = 5;

    @Autowired private JdbcTemplate jdbc;
    @Autowired private DataSource dataSource;
    @Autowired private RechercherProduitsCompatiblesService recherche;

    @BeforeEach
    void preparerCatalogueFictif() {
        jdbc.execute("CREATE SCHEMA volume_recherche_test");
        jdbc.execute("SET LOCAL search_path TO volume_recherche_test, public");
        new ResourceDatabasePopulator(
                new ClassPathResource("db/migration/V1__modele_commun.sql"),
                new ClassPathResource("db/migration/V4__comptes_utilisateur.sql")).execute(dataSource);
        jdbc.update("""
                INSERT INTO produit(libelle, code_barres)
                SELECT 'Produit fictif ' || lpad(i::text, 6, '0') || ' ' ||
                    CASE i % 5
                        WHEN 0 THEN 'Lait demi-ecreme 1 L'
                        WHEN 1 THEN 'Yaourt nature 500 g'
                        WHEN 2 THEN 'Pates alimentaires 500 g'
                        WHEN 3 THEN 'Savon solide 100 g'
                        ELSE 'Riz long 1 kg'
                    END,
                    '2' || lpad(i::text, 12, '0')
                FROM generate_series(1, 80000) AS nombres(i)
                """);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM produit", Integer.class)).isEqualTo(80_000);
        jdbc.execute("ANALYZE produit");
    }

    @Test
    void mesureLaRechercheSurDesFichesFictivesSansSeuilDePerformance() {
        mesurer("deux_caracteres", "la", 20);
        mesurer("mots", "lait", 20);
        mesurer("faute", "yaort", 20);
        mesurer("code_barres", "2000000000012", 1);
        mesurer("sans_resultat", "zzzzzzzzzzzz", 0);
        afficherPlans();
    }

    @Test
    void compareDesIndexEtUneRequeteAlternativeSansModifierLaRechercheApplicative() {
        jdbc.update("UPDATE produit SET libelle = 'Produit fictif 100%_naturel' WHERE code_barres = ?",
                "2000000000012");
        String normalise = NormalisationCatalogueSql.EXPRESSION.formatted("libelle");
        String original = """
                SELECT id FROM produit
                WHERE (position(lower(?) in lower(libelle)) > 0)
                   OR (position(lower(?) in %s) > 0)
                ORDER BY id
                """.formatted(normalise);
        String alternatif = """
                SELECT id FROM produit
                WHERE (lower(libelle) LIKE lower(?) ESCAPE E'\\\\')
                   OR (%s LIKE lower(?) ESCAPE E'\\\\')
                ORDER BY id
                """.formatted(normalise);
        long debut = System.nanoTime();
        jdbc.execute("CREATE INDEX volume_nom_brut_trgm ON produit USING gin (lower(libelle) gin_trgm_ops)");
        jdbc.execute("CREATE INDEX volume_nom_normalise_trgm ON produit USING gin (("
                + normalise + ") gin_trgm_ops)");
        double creationMs = (System.nanoTime() - debut) / 1_000_000.0;
        jdbc.execute("ANALYZE produit");
        Long taille = jdbc.queryForObject("""
                SELECT pg_relation_size('volume_nom_brut_trgm') + pg_relation_size('volume_nom_normalise_trgm')
                """, Long.class);
        System.out.printf(Locale.ROOT, "INDEX : creation %.1f ms, taille %d octets%n", creationMs, taille);

        for (String texte : List.of("la", "lait", "100%", "_naturel", "absentxyz")) {
            String normalisee = SimilariteRecherche.normaliser(texte);
            List<Long> attendus = jdbc.queryForList(original, Long.class, texte, normalisee);
            String brut = motif(texte);
            String sansAccents = motif(normalisee);
            List<Long> trouves = jdbc.queryForList(alternatif, Long.class, brut, sansAccents);
            assertThat(trouves).as("Correspondances pour %s", texte).containsExactlyElementsOf(attendus);
            System.out.printf("VARIANTE %s : %d resultats identiques%n", texte, trouves.size());
            afficherPlan("variante_" + texte, alternatif, brut, sansAccents);
        }
        afficherPlan("position_avec_index", original, "lait", "lait");
    }

    @Test
    void compareUnePreselectionTrigrammeIndexeeAvecLeSeuilActuel() {
        jdbc.update("""
                UPDATE produit SET libelle = 'Yaourt nature', marque = 'Marque Été', format = '1 L'
                WHERE code_barres = ?
                """, "2000000000012");
        jdbc.update("""
                UPDATE produit SET libelle = 'Pâtes fines', marque = '', format = NULL
                WHERE code_barres = ?
                """, "2000000000013");
        String limite = jdbc.queryForObject("""
                SELECT a.v || b.v || c.v || d.v || e.v
                FROM unnest(ARRAY['l', 'a', 'i', 't', 'x']) AS a(v)
                CROSS JOIN unnest(ARRAY['l', 'a', 'i', 't', 'x']) AS b(v)
                CROSS JOIN unnest(ARRAY['l', 'a', 'i', 't', 'x']) AS c(v)
                CROSS JOIN unnest(ARRAY['l', 'a', 'i', 't', 'x']) AS d(v)
                CROSS JOIN unnest(ARRAY['l', 'a', 'i', 't', 'x']) AS e(v)
                WHERE word_similarity('lait', a.v || b.v || c.v || d.v || e.v) = 0.2::real
                LIMIT 1
                """, String.class);
        jdbc.update("""
                UPDATE produit SET libelle = ?, marque = NULL, format = NULL WHERE code_barres = ?
                """, limite, "2000000000014");
        Long idLimite = jdbc.queryForObject("SELECT id FROM produit WHERE code_barres = ?",
                Long.class, "2000000000014");
        String concatene = """
                libelle || CASE WHEN marque IS NULL THEN '' ELSE ' ' || marque END
                        || CASE WHEN format IS NULL THEN '' ELSE ' ' || format END
                """;
        String normalise = NormalisationCatalogueSql.EXPRESSION.formatted(concatene);
        long debut = System.nanoTime();
        jdbc.execute("CREATE INDEX volume_candidats_trgm ON produit USING gin (("
                + normalise + ") gin_trgm_ops)");
        double creationMs = (System.nanoTime() - debut) / 1_000_000.0;
        Long taille = jdbc.queryForObject("SELECT pg_relation_size('volume_candidats_trgm')", Long.class);
        jdbc.execute("ANALYZE produit");
        jdbc.execute("SET LOCAL pg_trgm.word_similarity_threshold = 0.1999");
        System.out.printf(Locale.ROOT, "INDEX CANDIDATS : creation %.1f ms, taille %d octets%n", creationMs, taille);

        String original = ProduitsCatalogueJdbcAdapter.CANDIDATS_SQL
                + ProduitsCatalogueJdbcAdapter.FILTRES_SQL + " ORDER BY p.id";
        String alternatif = """
                SELECT id FROM produit
                WHERE ? <%% (%s) AND word_similarity(?, %s) >= 0.2
                ORDER BY id
                """.formatted(normalise, normalise);
        for (String texte : List.of("yaort", "pates", "ete", "1 l", "lait", "zzzzzzzzzzzz")) {
            List<Long> attendus = jdbc.query(original, (resultat, ligne) -> resultat.getLong("id"),
                    texte, "", "", "", "");
            List<Long> trouves = jdbc.queryForList(alternatif, Long.class, texte, texte);
            assertThat(trouves).as("Candidats pour %s", texte).containsExactlyElementsOf(attendus);
            if (texte.equals("lait")) {
                assertThat(attendus).as("Candidat exactement au seuil").contains(idLimite);
                assertThat(trouves).as("Candidat indexé exactement au seuil").contains(idLimite);
            }
            System.out.printf("CANDIDATS %s : %d resultats identiques%n", texte, trouves.size());
        }
        String expressionOriginale = NormalisationCatalogueSql.EXPRESSION
                .formatted("concat_ws(' ', libelle, marque, format)");
        afficherPlan("candidats_sans_index_yaort",
                "SELECT id FROM produit WHERE word_similarity(?, " + expressionOriginale
                        + ") >= 0.2 ORDER BY id",
                "yaort");
        afficherPlan("candidats_indexes_yaort", alternatif, "yaort", "yaort");
    }

    @Test
    void etudieLaLimiteAnticipeePourDeuxCaracteresSansChangerLeClassement() {
        String normalise = NormalisationCatalogueSql.EXPRESSION.formatted("libelle");
        long debut = System.nanoTime();
        jdbc.execute("CREATE INDEX volume_ordre_nom ON produit ((" + normalise + ") COLLATE \"C\", id)");
        double creationMs = (System.nanoTime() - debut) / 1_000_000.0;
        Long taille = jdbc.queryForObject("SELECT pg_relation_size('volume_ordre_nom')", Long.class);
        jdbc.execute("ANALYZE produit");
        String limite = """
                SELECT id FROM produit
                WHERE (position(lower(?) in lower(libelle)) > 0)
                   OR (position(lower(?) in %s) > 0)
                ORDER BY (%s) COLLATE "C", id
                LIMIT 20
                """.formatted(normalise, normalise);
        List<Long> service = recherche.rechercher("la").stream().map(ProduitRechercheCompatible::id).toList();
        List<Long> resultatSql = jdbc.queryForList(limite, Long.class, "la", "la");
        assertThat(resultatSql).containsExactlyElementsOf(service);
        System.out.printf(Locale.ROOT, "INDEX ORDRE : creation %.1f ms, taille %d octets%n", creationMs, taille);
        System.out.println("ORDRE SIMPLE : identique sur le jeu regulier");
        afficherPlan("deux_caracteres_limite", limite, "la", "la");

        jdbc.update("UPDATE produit SET libelle = 'La!x' WHERE code_barres = ?", "2000000000012");
        jdbc.update("UPDATE produit SET libelle = 'La x' WHERE code_barres = ?", "2000000000013");
        List<Long> avecPonctuation = recherche.rechercher("la").stream().map(ProduitRechercheCompatible::id).toList();
        List<Long> ordreSql = jdbc.queryForList(limite, Long.class, "la", "la");
        assertThat(ordreSql).isNotEqualTo(avecPonctuation);
        System.out.println("ORDRE PONCTUATION : different du classement du service");
    }

    private String motif(String texte) {
        return "%" + texte.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
    }

    private void mesurer(String cas, String requete, int nombreAttendu) {
        verifier(recherche.rechercher(requete), nombreAttendu);
        List<Double> durees = new ArrayList<>();
        for (int i = 0; i < ECHANTILLONS; i++) {
            long debut = System.nanoTime();
            List<ProduitRechercheCompatible> resultats = recherche.rechercher(requete);
            durees.add((System.nanoTime() - debut) / 1_000_000.0);
            verifier(resultats, nombreAttendu);
        }
        durees.sort(Comparator.naturalOrder());
        System.out.printf(Locale.ROOT, "%s : %d mesures, mediane %.1f ms, maximum %.1f ms%n",
                cas, ECHANTILLONS, durees.get(ECHANTILLONS / 2), durees.get(ECHANTILLONS - 1));
    }

    private void verifier(List<ProduitRechercheCompatible> resultats, int nombreAttendu) {
        assertThat(resultats).hasSize(nombreAttendu).doesNotHaveDuplicates();
    }

    private void afficherPlans() {
        String normalise = NormalisationCatalogueSql.EXPRESSION.formatted("libelle");
        String correspondances = """
                SELECT id, libelle, code_barres FROM produit
                WHERE (position(lower(?) in lower(libelle)) > 0)
                   OR (position(lower(?) in %s) > 0)
                """.formatted(normalise);
        afficherPlan("correspondances_lait", correspondances, "lait", "lait");
        afficherPlan("candidats_yaort",
                ProduitsCatalogueJdbcAdapter.CANDIDATS_SQL + ProduitsCatalogueJdbcAdapter.FILTRES_SQL,
                "yaort", "", "", "", "");
    }

    private void afficherPlan(String nom, String requete, Object... parametres) {
        System.out.println("PLAN " + nom);
        jdbc.queryForList("EXPLAIN (ANALYZE, BUFFERS) " + requete, String.class, parametres)
                .forEach(System.out::println);
    }
}

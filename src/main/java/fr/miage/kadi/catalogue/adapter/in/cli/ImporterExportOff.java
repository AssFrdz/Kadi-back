package fr.miage.kadi.catalogue.adapter.in.cli;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Objects;

public final class ImporterExportOff {
    private static final long LIMITE_MAX = 100_000;

    private ImporterExportOff() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage : --officiel ou fichier CSV, puis nombre maximal de lignes (1 à 100000).");
        }
        long limite = Long.parseLong(args[1]);
        if (limite < 1 || limite > LIMITE_MAX) {
            throw new IllegalArgumentException("La limite doit être comprise entre 1 et 100000 lignes.");
        }
        String url = variable("KADI_IMPORT_DB_URL");
        String utilisateur = variable("KADI_IMPORT_DB_USER");
        String motDePasse = variable("KADI_IMPORT_DB_PASSWORD");
        try (Connection connexion = DriverManager.getConnection(url, utilisateur, motDePasse)) {
            connexion.setAutoCommit(false);
            try {
                Resultat resultat = importer(connexion, args[0], limite);
                connexion.commit();
                System.out.printf("""
                        Lignes lues : %d
                        Fiches admissibles : %d
                        Codes distincts admissibles : %d
                        Produits créés : %d
                        Produits OFF actualisés : %d
                        Codes déjà présents hors OFF, ignorés : %d
                        Versions antérieures ou non datées ignorées : %d
                        Images du CSV sans crédit vérifiable : %d
                        Lecture interrompue par la limite : %s
                        """, resultat.rapport().lues(), resultat.rapport().admissibles(), resultat.distincts(),
                        resultat.crees(), resultat.actualises(), resultat.conflits(), resultat.anciens(),
                        resultat.rapport().imagesSansCredit(),
                        resultat.rapport().limiteAtteinte() ? "oui" : "non");
            } catch (Exception erreur) {
                try {
                    connexion.rollback();
                } catch (SQLException echecRollback) {
                    erreur.addSuppressed(echecRollback);
                }
                throw erreur;
            }
        }
    }

    static Resultat importer(Connection connexion, String source, long limite) throws SQLException, IOException, InterruptedException {
        if (connexion.getAutoCommit()) {
            throw new IllegalArgumentException("L'import doit utiliser une transaction explicite.");
        }
        if (limite < 1 || limite > LIMITE_MAX) {
            throw new IllegalArgumentException("La limite doit être comprise entre 1 et 100000 lignes.");
        }
        try (var commande = connexion.createStatement()) {
            commande.execute("SELECT pg_advisory_xact_lock(1262568516, 1179013961)");
            commande.execute("""
                    CREATE TEMP TABLE off_import_staging (
                        code VARCHAR(14) PRIMARY KEY,
                        libelle VARCHAR(255) NOT NULL,
                        marque VARCHAR(255),
                        format VARCHAR(255),
                        categories TEXT,
                        modifie TIMESTAMP WITH TIME ZONE
                    ) ON COMMIT DROP
                    """);
        }

        EvaluerExportOff.Rapport rapport;
        try (PreparedStatement insertion = connexion.prepareStatement("""
                INSERT INTO off_import_staging(code, libelle, marque, format, categories, modifie)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT (code) DO UPDATE SET libelle = EXCLUDED.libelle, marque = EXCLUDED.marque,
                    format = EXCLUDED.format, categories = EXCLUDED.categories, modifie = EXCLUDED.modifie
                WHERE off_import_staging.modifie IS NULL
                   OR (EXCLUDED.modifie IS NOT NULL AND EXCLUDED.modifie > off_import_staging.modifie)
                """)) {
            EvaluerExportOff.Consommateur accepter = fiche -> {
                try {
                    insertion.setString(1, fiche.code());
                    insertion.setString(2, fiche.libelle());
                    insertion.setString(3, fiche.marque());
                    insertion.setString(4, fiche.format());
                    insertion.setString(5, fiche.categories());
                    insertion.setTimestamp(6, fiche.modifie() == null ? null : Timestamp.from(fiche.modifie()));
                    insertion.executeUpdate();
                } catch (SQLException erreur) {
                    throw new IOException("Impossible de préparer la fiche " + fiche.code(), erreur);
                }
            };
            rapport = source.equals("--officiel")
                    ? EvaluerExportOff.lireOfficiel(limite, accepter)
                    : EvaluerExportOff.lire(Path.of(source), limite, accepter);
        }
        long distincts = compter(connexion, "SELECT count(*) FROM off_import_staging");
        long conflits = compter(connexion, """
                SELECT count(*) FROM off_import_staging s JOIN produit p ON p.code_barres = s.code
                LEFT JOIN produit_open_food_facts off ON off.produit_id = p.id AND off.identifiant_externe = s.code
                WHERE off.produit_id IS NULL
                """);
        long anciens = compter(connexion, """
                SELECT count(*) FROM off_import_staging s
                JOIN produit_open_food_facts off ON off.identifiant_externe = s.code
                WHERE off.modifie_source_le IS NOT NULL
                  AND (s.modifie IS NULL OR s.modifie < off.modifie_source_le)
                """);
        int crees;
        int actualises;
        try (var commande = connexion.createStatement()) {
            crees = commande.executeUpdate("""
                    WITH ajoutes AS (
                        INSERT INTO produit(libelle, code_barres, marque, format)
                        SELECT libelle, code, marque, format FROM off_import_staging
                        ON CONFLICT (code_barres) DO NOTHING RETURNING id, code_barres
                    )
                    INSERT INTO produit_open_food_facts(produit_id, identifiant_externe, categorie_source,
                                                       modifie_source_le, importe_le)
                    SELECT a.id, s.code, s.categories, s.modifie, CURRENT_TIMESTAMP
                    FROM ajoutes a JOIN off_import_staging s ON s.code = a.code_barres
                    """);
            actualises = commande.executeUpdate("""
                    UPDATE produit p SET libelle = s.libelle, marque = s.marque, format = s.format
                    FROM off_import_staging s JOIN produit_open_food_facts off ON off.identifiant_externe = s.code
                    WHERE p.id = off.produit_id AND p.code_barres = s.code
                      AND (off.modifie_source_le IS NULL OR s.modifie >= off.modifie_source_le)
                    """);
            commande.executeUpdate("""
                    UPDATE produit_open_food_facts off SET categorie_source = s.categories,
                        modifie_source_le = s.modifie, importe_le = CURRENT_TIMESTAMP
                    FROM off_import_staging s JOIN produit p ON p.code_barres = s.code
                    WHERE off.produit_id = p.id AND off.identifiant_externe = s.code
                      AND (off.modifie_source_le IS NULL OR s.modifie >= off.modifie_source_le)
                    """);
            commande.execute("DROP TABLE off_import_staging");
        }
        return new Resultat(rapport, distincts, crees, actualises - crees, conflits, anciens);
    }

    private static long compter(Connection connexion, String sql) throws SQLException {
        try (var commande = connexion.createStatement(); ResultSet resultat = commande.executeQuery(sql)) {
            if (!resultat.next()) {
                throw new SQLException("Le comptage de l'import n'a pas renvoyé de résultat.");
            }
            return resultat.getLong(1);
        }
    }

    private static String variable(String nom) {
        String valeur = System.getenv(nom);
        if (valeur == null || valeur.isBlank()) {
            throw new IllegalArgumentException("La variable " + nom + " est obligatoire.");
        }
        return valeur;
    }

    record Resultat(EvaluerExportOff.Rapport rapport, long distincts, int crees, int actualises,
            long conflits, long anciens) {
        Resultat {
            Objects.requireNonNull(rapport);
        }
    }
}

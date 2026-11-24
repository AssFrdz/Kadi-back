package fr.miage.kadi.catalogue.adapter.in.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.time.Duration;
import java.time.Instant;
import java.time.DateTimeException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

public final class EvaluerExportOff {
    private static final List<String> COLONNES = List.of(
            "code", "product_name", "brands", "quantity", "countries_tags", "categories_tags",
            "last_modified_t", "image_url");
    private static final URI EXPORT_OFFICIEL =
            URI.create("https://static.openfoodfacts.org/data/en.openfoodfacts.org.products.csv.gz");

    private EvaluerExportOff() {
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage : --officiel ou chemin-export.csv[.gz], puis nombre-maximal-de-lignes");
        }
        long limite;
        try {
            limite = Long.parseLong(args[1]);
        } catch (NumberFormatException erreur) {
            throw new IllegalArgumentException("La limite doit être un entier strictement positif.", erreur);
        }
        Rapport rapport = args[0].equals("--officiel")
                ? analyserOfficiel(limite) : analyser(Path.of(args[0]), limite);
        System.out.printf("""
                Lignes lues : %d
                Sans pays France : %d
                France, code invalide : %d
                France, nom manquant ou trop long : %d
                France, marque ou format trop long : %d
                Fiches admissibles : %d
                Dont URL d'image non créditable avec ce CSV : %d
                Lecture interrompue par la limite : %s
                Aucune donnée n'a été importée.
                """, rapport.lues(), rapport.horsFrance(), rapport.codesInvalides(), rapport.nomsInvalides(),
                rapport.champsTropLongs(), rapport.admissibles(), rapport.imagesSansCredit(),
                rapport.limiteAtteinte() ? "oui" : "non");
    }

    public static Rapport analyser(Path fichier, long limite) throws IOException {
        return lire(fichier, limite, fiche -> {});
    }

    static Rapport lire(Path fichier, long limite, Consommateur consommateur) throws IOException {
        try (InputStream brut = Files.newInputStream(fichier)) {
            return analyser(brut, fichier.getFileName().toString().endsWith(".gz"), limite, consommateur);
        }
    }

    public static Rapport analyserOfficiel(long limite) throws IOException, InterruptedException {
        return lireOfficiel(limite, fiche -> {});
    }

    static Rapport lireOfficiel(long limite, Consommateur consommateur) throws IOException, InterruptedException {
        if (limite <= 0) {
            throw new IllegalArgumentException("La limite doit être strictement positive.");
        }
        HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(30)).build();
        HttpRequest requete = HttpRequest.newBuilder(EXPORT_OFFICIEL)
                .header("User-Agent", "Kadi/0.1 (catalogue local; export CSV)")
                .timeout(Duration.ofMinutes(10)).GET().build();
        HttpResponse<InputStream> reponse = client.send(requete, HttpResponse.BodyHandlers.ofInputStream());
        try (InputStream corps = reponse.body()) {
            if (reponse.statusCode() != 200) {
                throw new IOException("Téléchargement Open Food Facts : HTTP " + reponse.statusCode());
            }
            return analyser(corps, true, limite, consommateur);
        }
    }

    private static Rapport analyser(InputStream brut, boolean compresse, long limite, Consommateur consommateur) throws IOException {
        if (limite <= 0) {
            throw new IllegalArgumentException("La limite doit être strictement positive.");
        }
        long lues = 0;
        long horsFrance = 0;
        long codesInvalides = 0;
        long nomsInvalides = 0;
        long champsTropLongs = 0;
        long admissibles = 0;
        long imagesSansCredit = 0;
        try (InputStream entree = compresse ? new GZIPInputStream(brut) : brut;
                BufferedReader lecteur = new BufferedReader(new InputStreamReader(entree,
                        StandardCharsets.UTF_8.newDecoder()
                                .onMalformedInput(CodingErrorAction.REPORT)
                                .onUnmappableCharacter(CodingErrorAction.REPORT)));
                CSVParser csv = CSVFormat.TDF.builder().setHeader().setSkipHeaderRecord(true).get().parse(lecteur)) {
            for (String colonne : COLONNES) {
                if (!csv.getHeaderMap().containsKey(colonne)) {
                    throw new IllegalArgumentException("Colonne obligatoire absente : " + colonne);
                }
            }
            Iterator<CSVRecord> lignes = csv.iterator();
            while (lues < limite && lignes.hasNext()) {
                CSVRecord ligne = lignes.next();
                lues++;
                if (!ligne.isConsistent()) {
                    throw new IllegalArgumentException("Nombre de colonnes invalide à la ligne " + ligne.getRecordNumber());
                }
                if (Arrays.stream(ligne.get("countries_tags").split(","))
                        .map(String::trim).noneMatch("en:france"::equals)) {
                    horsFrance++;
                } else if (!ligne.get("code").matches("[0-9]{1,14}")) {
                    codesInvalides++;
                } else if (ligne.get("product_name").isBlank() || ligne.get("product_name").length() > 255) {
                    nomsInvalides++;
                } else if (ligne.get("brands").length() > 255 || ligne.get("quantity").length() > 255) {
                    champsTropLongs++;
                } else {
                    Instant modifie = null;
                    if (!ligne.get("last_modified_t").isBlank()) {
                        try {
                            modifie = Instant.ofEpochSecond(Long.parseLong(ligne.get("last_modified_t")));
                        } catch (NumberFormatException | DateTimeException erreur) {
                            throw new IOException("Date source invalide à la ligne " + ligne.getRecordNumber(), erreur);
                        }
                    }
                    consommateur.accepter(new Fiche(ligne.get("code"), ligne.get("product_name").trim(),
                            valeurOuNull(ligne.get("brands")), valeurOuNull(ligne.get("quantity")),
                            valeurOuNull(ligne.get("categories_tags")), modifie));
                    admissibles++;
                    if (!ligne.get("image_url").isBlank()) {
                        imagesSansCredit++;
                    }
                }
            }
            return new Rapport(lues, horsFrance, codesInvalides, nomsInvalides, champsTropLongs,
                    admissibles, imagesSansCredit, lues == limite && lignes.hasNext());
        }
    }

    private static String valeurOuNull(String texte) {
        return texte.isBlank() ? null : texte.trim();
    }

    @FunctionalInterface
    interface Consommateur {
        void accepter(Fiche fiche) throws IOException;
    }

    record Fiche(String code, String libelle, String marque, String format, String categories, Instant modifie) {
    }

    public record Rapport(long lues, long horsFrance, long codesInvalides, long nomsInvalides,
            long champsTropLongs, long admissibles, long imagesSansCredit, boolean limiteAtteinte) {
    }
}

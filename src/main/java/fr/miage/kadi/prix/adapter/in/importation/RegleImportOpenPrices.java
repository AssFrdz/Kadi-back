package fr.miage.kadi.prix.adapter.in.importation;

import fr.miage.kadi.prix.application.port.in.ReleveAImporter;
import fr.miage.kadi.prix.domain.model.Montant;
import fr.miage.kadi.prix.domain.model.TypeDePreuve;
import fr.miage.kadi.prix.domain.model.UniteDePrix;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/**
 * Les règles qui transforment une ligne Open Prices (outils/open-prices) en relevé à importer, en Java pur pour être testées.
 * Une ligne inexploitable est rejetée avec son motif plutôt que corrigée au hasard.
 */
final class RegleImportOpenPrices {

    static final String SOURCE = "OPEN_PRICES";

    /** Colonnes du fichier écrit par extraire_open_prices.py, dans l'ordre. */
    private static final int ID = 0;
    private static final int CODE_BARRES = 1;
    private static final int LIBELLE = 2;
    private static final int PRIX = 3;
    private static final int PRIX_PAR = 4;
    private static final int EN_PROMOTION = 5;
    private static final int DATE = 6;
    private static final int CONTRIBUTEUR = 7;
    private static final int TYPE_PREUVE = 8;
    private static final int OSM_TYPE = 9;
    private static final int OSM_ID = 10;
    private static final int MAGASIN = 11;
    private static final int MARQUE = 12;
    static final int NOMBRE_DE_COLONNES = 13;

    /** Open Prices ne donne que le jour : le relevé est daté de midi, heure de Paris. */
    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");

    enum Rejet {
        LIGNE_INCOMPLETE,
        PRIX_INVALIDE,
        CODE_BARRES_INVALIDE,
        MAGASIN_INVALIDE,
        DATE_INVALIDE
    }

    /** Soit un relevé, soit le motif de son rejet. */
    record Lecture(ReleveAImporter releve, Rejet rejet) {
    }

    private RegleImportOpenPrices() {
    }

    static Lecture lire(String ligne) {
        String[] c = ligne.split("\t", -1);
        if (c.length != NOMBRE_DE_COLONNES || c[ID].isBlank()) {
            return rejet(Rejet.LIGNE_INCOMPLETE);
        }
        String codeBarres = c[CODE_BARRES].strip();
        if (!codeBarres.matches("[0-9]{1,14}")) {
            return rejet(Rejet.CODE_BARRES_INVALIDE);
        }
        Montant prix = montant(c[PRIX]);
        if (prix == null) {
            return rejet(Rejet.PRIX_INVALIDE);
        }
        String type = c[OSM_TYPE].strip().toLowerCase(Locale.ROOT);
        String osmId = c[OSM_ID].strip();
        if (!type.matches("node|way|relation") || !osmId.matches("[0-9]+")) {
            return rejet(Rejet.MAGASIN_INVALIDE);
        }
        LocalDate jour;
        try {
            jour = LocalDate.parse(c[DATE].strip());
        } catch (DateTimeParseException e) {
            return rejet(Rejet.DATE_INVALIDE);
        }

        String referenceMagasin = type + "/" + osmId;
        String nomMagasin = premierNonVide(c[MAGASIN], c[MARQUE], "Magasin " + referenceMagasin);
        String libelle = premierNonVide(c[LIBELLE], "Produit " + codeBarres);
        String contributeur = "openprices:" + premierNonVide(c[CONTRIBUTEUR], "inconnu");
        return new Lecture(new ReleveAImporter(
                SOURCE,
                c[ID].strip(),
                codeBarres,
                tronquer(libelle, 255),
                referenceMagasin,
                tronquer(nomMagasin, 150),
                enseigne(c[MARQUE], nomMagasin, referenceMagasin),
                prix,
                unite(c[PRIX_PAR]),
                jour.atTime(LocalTime.NOON).atZone(PARIS).toInstant(),
                preuve(c[TYPE_PREUVE]),
                tronquer(contributeur, 64),
                Boolean.parseBoolean(c[EN_PROMOTION].strip())), null);
    }

    /**
     * Même marque, écriture différente : une seule enseigne (« Centre Commercial E. Leclerc » devient « E.Leclerc »).
     * Les formats restent distincts (« Carrefour City » n'est pas « Carrefour ») : leurs prix diffèrent.
     * Sans marque, le magasin est sa propre enseigne : il n'a pas de repli plutôt qu'un repli faux.
     */
    static String enseigne(String marque, String nomMagasin, String referenceMagasin) {
        String nettoyee = marque == null ? "" : marque.strip()
                .replaceFirst("(?i)^centre\\s+commercial\\s+", "")
                .replaceAll("\\.\\s+", ".")
                .replaceAll("\\s+", " ");
        if (!nettoyee.isBlank()) {
            return tronquer(nettoyee, 100);
        }
        String suffixe = " (" + referenceMagasin + ")";
        return tronquer(nomMagasin, 100 - suffixe.length()) + suffixe;
    }

    static TypeDePreuve preuve(String typeOpenPrices) {
        return switch (typeOpenPrices.strip()) {
            case "RECEIPT" -> TypeDePreuve.TICKET;
            case "PRICE_TAG" -> TypeDePreuve.ETIQUETTE;
            case "GDPR_REQUEST" -> TypeDePreuve.EXPORT_FIDELITE;
            case "SHOP_IMPORT" -> TypeDePreuve.IMPORT_ENSEIGNE;
            default -> TypeDePreuve.SAISIE;
        };
    }

    static UniteDePrix unite(String prixPar) {
        return switch (prixPar.strip()) {
            case "KILOGRAM" -> UniteDePrix.KILOGRAMME;
            case "LITER", "LITRE" -> UniteDePrix.LITRE;
            default -> UniteDePrix.PIECE;
        };
    }

    /** Un prix strictement positif, au centime : 1.990 est accepté, 1.995 est refusé plutôt qu'arrondi. */
    private static Montant montant(String texte) {
        try {
            BigDecimal valeur = new BigDecimal(texte.strip());
            if (valeur.signum() <= 0 || valeur.stripTrailingZeros().scale() > 2) {
                return null;
            }
            return new Montant(valeur);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String premierNonVide(String... valeurs) {
        for (String valeur : valeurs) {
            if (valeur != null && !valeur.isBlank()) {
                return valeur.strip();
            }
        }
        return "";
    }

    private static String tronquer(String texte, int longueur) {
        return texte.length() <= longueur ? texte : texte.substring(0, longueur);
    }

    private static Lecture rejet(Rejet motif) {
        return new Lecture(null, motif);
    }
}

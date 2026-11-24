package fr.miage.kadi.catalogue.domain.algo;

import fr.miage.kadi.catalogue.domain.model.ProduitCatalogue;
import java.text.Normalizer;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class SimilariteRecherche {
    private SimilariteRecherche() {
    }

    public static String normaliser(String texte) {
        return Normalizer.normalize(texte.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replaceAll("[^\\p{L}\\p{N}]+", " ")
                .trim()
                .replaceAll(" +", " ");
    }

    public static double score(String recherche, String libelle) {
        String nom = normaliser(libelle);
        if (nom.contains(recherche)) {
            return 1.0;
        }

        double score = dice(recherche, nom);
        if (!recherche.contains(" ")) {
            for (String mot : nom.split(" ")) {
                score = Math.max(score, dice(recherche, mot));
            }
        }
        return score;
    }

    /**
     * Ressemblance de Dice entre la saisie normalisée et le libellé entier, sans le bonus des sous-chaînes :
     * sert à départager les libellés qui contiennent tous la saisie (« Lait » avant « Boisson au lait »).
     */
    public static double proximiteLibelle(String recherche, String libelle) {
        return dice(recherche, normaliser(libelle));
    }

    public static double scoreProduit(String recherche, ProduitCatalogue produit) {
        String marque = produit.marque() == null ? "" : produit.marque();
        String format = produit.format() == null ? "" : produit.format();
        double score = score(recherche, produit.libelle());
        if (!marque.isBlank()) {
            score = Math.max(score, score(recherche, marque));
        }
        if (!format.isBlank()) {
            score = Math.max(score, score(recherche, format));
        }
        return Math.max(score, score(recherche, produit.libelle() + " " + marque + " " + format));
    }

    private static double dice(String premier, String second) {
        Set<String> a = trigrammes(premier);
        Set<String> b = trigrammes(second);
        if (a.isEmpty() || b.isEmpty()) {
            return 0;
        }
        Set<String> communs = new HashSet<>(a);
        communs.retainAll(b);
        return 2.0 * communs.size() / (a.size() + b.size());
    }

    private static Set<String> trigrammes(String texte) {
        Set<String> resultat = new HashSet<>();
        String entoure = "  " + texte + " ";
        for (int i = 0; i + 3 <= entoure.length(); i++) {
            resultat.add(entoure.substring(i, i + 3));
        }
        return resultat;
    }
}

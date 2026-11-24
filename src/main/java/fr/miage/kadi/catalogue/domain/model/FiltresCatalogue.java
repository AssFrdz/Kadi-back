package fr.miage.kadi.catalogue.domain.model;

import java.util.Set;
import java.util.stream.Collectors;

public record FiltresCatalogue(String marque, String format, Set<NutriScore> nutriScores) {
    public FiltresCatalogue(String marque, String format) {
        this(marque, format, Set.of());
    }

    public FiltresCatalogue {
        marque = nettoyer(marque);
        format = nettoyer(format);
        if (nutriScores == null || nutriScores.stream().anyMatch(score -> score == null)) {
            throw new IllegalArgumentException("La sélection Nutri-Score est invalide.");
        }
        nutriScores = Set.copyOf(nutriScores);
    }

    public String cleNutriScores() {
        return nutriScores.stream().sorted().map(Enum::name).collect(Collectors.joining(","));
    }

    private static String nettoyer(String valeur) {
        if (valeur == null) {
            return "";
        }
        if (valeur.length() > 255) {
            throw new IllegalArgumentException("Un filtre ne peut pas dépasser 255 caractères.");
        }
        return valeur.strip();
    }
}

package fr.miage.kadi.catalogue.domain.model;

public record FiltresCatalogue(String marque, String format) {
    public FiltresCatalogue {
        marque = nettoyer(marque);
        format = nettoyer(format);
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

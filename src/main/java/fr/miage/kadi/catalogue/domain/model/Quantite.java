package fr.miage.kadi.catalogue.domain.model;

import java.math.BigDecimal;

public record Quantite(BigDecimal value, UniteQuantite unite) {

    public Quantite {
        if (unite == null) {
            throw new NullPointerException("L'unité ne peut pas être nulle");
        }

        if (value == null) {
            throw new NullPointerException("La valeur ne peut pas être nulle");
        }

        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                "La valeur doit être strictement supérieure à 0"
            );
        }

        if (unite == UniteQuantite.ARTICLE
                && value.stripTrailingZeros().scale() > 0) {
            throw new IllegalArgumentException(
                "Une quantité d'articles doit être entière"
            );
        }
    }

    public Quantite ajouter(Quantite autre) {

        if (autre == null) {
            throw new NullPointerException(
                "La quantité à ajouter ne peut pas être nulle"
            );
        }

        // mêmes unités
        if (unite == autre.unite()) {
            return new Quantite(
                value.add(autre.value()),
                unite
            );
        }

        // kg + g
        if (unite == UniteQuantite.KILOGRAMME
                && autre.unite() == UniteQuantite.GRAMME) {

            BigDecimal autreEnKg =
                autre.value().movePointLeft(3);

            return new Quantite(
                value.add(autreEnKg),
                unite
            );
        }

        // g + kg
        if (unite == UniteQuantite.GRAMME
                && autre.unite() == UniteQuantite.KILOGRAMME) {

            BigDecimal autreEnGrammes =
                autre.value().movePointRight(3);

            return new Quantite(
                value.add(autreEnGrammes),
                unite
            );
        }

        // L + cl
        if (unite == UniteQuantite.LITRE
                && autre.unite() == UniteQuantite.CENTILITRE) {
            return new Quantite(
                value.add(autre.value().movePointLeft(2)),
                unite
            );
        }
        // cl + L
        if (unite == UniteQuantite.CENTILITRE
                && autre.unite() == UniteQuantite.LITRE) {
            return new Quantite(
                value.add(autre.value().movePointRight(2)),
                unite
            );
        }
        throw new IllegalArgumentException(
            "Unités incompatibles : " + unite + " et " + autre.unite()
        );
    }

    public boolean estCompatibleAvec(Quantite autre){
        if(autre==null){
            throw new NullPointerException("La quantite de comparaison ne peut être nulle");
        }

        if(autre.unite()==unite){
            return true;
        }

        if(autre.unite()== UniteQuantite.KILOGRAMME && unite==UniteQuantite.GRAMME || autre.unite()== UniteQuantite.GRAMME && unite==UniteQuantite.KILOGRAMME){
            return true;
        }
        if(autre.unite()== UniteQuantite.LITRE && unite==UniteQuantite.CENTILITRE || autre.unite()== UniteQuantite.CENTILITRE && unite==UniteQuantite.LITRE){
            return true;
        }

        return false;

    }
}
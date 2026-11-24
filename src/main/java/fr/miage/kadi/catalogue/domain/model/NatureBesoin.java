package fr.miage.kadi.catalogue.domain.model;

import java.util.List;
import java.util.Objects;

/**
 * Nature d'un besoin générique du catalogue : elle fixe les unités que l'utilisateur peut choisir.
 * Un solide se compte ou se pèse, un liquide se compte ou se mesure en litres ou centilitres,
 * un article unitaire (gobelets, lessive en bidon...) se compte seulement.
 */
public enum NatureBesoin {
    SOLIDE(List.of(UniteQuantite.ARTICLE, UniteQuantite.GRAMME, UniteQuantite.KILOGRAMME)),
    LIQUIDE(List.of(UniteQuantite.ARTICLE, UniteQuantite.LITRE, UniteQuantite.CENTILITRE)),
    UNITAIRE(List.of(UniteQuantite.ARTICLE));

    private final List<UniteQuantite> unitesAutorisees;

    NatureBesoin(List<UniteQuantite> unitesAutorisees) {
        this.unitesAutorisees = unitesAutorisees;
    }

    public List<UniteQuantite> unitesAutorisees() {
        return unitesAutorisees;
    }

    public void verifier(Quantite quantite) {
        Objects.requireNonNull(quantite, "La quantité ne peut pas être nulle.");
        if (!unitesAutorisees.contains(quantite.unite())) {
            throw new IllegalArgumentException("L'unité " + quantite.unite() + " n'est pas proposée pour ce besoin.");
        }
    }
}

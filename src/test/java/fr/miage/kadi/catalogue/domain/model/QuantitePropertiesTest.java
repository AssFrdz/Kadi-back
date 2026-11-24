package fr.miage.kadi.catalogue.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;

/**
 * Propriétés des quantités : l'addition avec conversion donne la même masse ou le même volume total
 * qu'un calcul de référence dans l'unité la plus fine (grammes, centilitres).
 */
class QuantitePropertiesTest {

    @Property
    void kilogrammesEtGrammesSAdditionnentSansPerte(@ForAll @IntRange(min = 1, max = 100_000) int grammesA,
            @ForAll @IntRange(min = 1, max = 100_000) int grammesB, @ForAll boolean premierEnKilos) {
        Quantite a = premierEnKilos ? kilos(grammesA) : grammes(grammesA);
        Quantite somme = a.ajouter(grammes(grammesB));
        assertThat(enGrammes(somme)).isEqualByComparingTo(BigDecimal.valueOf((long) grammesA + grammesB));
        assertThat(somme.unite()).isEqualTo(a.unite());
    }

    @Property
    void litresEtCentilitresSAdditionnentSansPerte(@ForAll @IntRange(min = 1, max = 100_000) int clA,
            @ForAll @IntRange(min = 1, max = 100_000) int clB, @ForAll boolean premierEnLitres) {
        Quantite a = premierEnLitres
                ? new Quantite(BigDecimal.valueOf(clA).movePointLeft(2), UniteQuantite.LITRE)
                : new Quantite(BigDecimal.valueOf(clA), UniteQuantite.CENTILITRE);
        Quantite somme = a.ajouter(new Quantite(BigDecimal.valueOf(clB), UniteQuantite.CENTILITRE));
        BigDecimal enCl = somme.unite() == UniteQuantite.LITRE ? somme.value().movePointRight(2) : somme.value();
        assertThat(enCl).isEqualByComparingTo(BigDecimal.valueOf((long) clA + clB));
    }

    @Property
    void lAdditionDArticlesResteEntiereEtCommutative(@ForAll @IntRange(min = 1, max = 1_000_000) int a,
            @ForAll @IntRange(min = 1, max = 1_000_000) int b) {
        Quantite x = articles(a);
        Quantite y = articles(b);
        assertThat(x.ajouter(y)).isEqualTo(y.ajouter(x));
        assertThat(x.ajouter(y).value().stripTrailingZeros().scale()).isLessThanOrEqualTo(0);
    }

    @Property
    void unPoidsEtUnNombreDArticlesNeSAdditionnentJamais(@ForAll @IntRange(min = 1, max = 1000) int valeur) {
        assertThat(articles(valeur).estCompatibleAvec(grammes(valeur))).isFalse();
        assertThatThrownBy(() -> articles(valeur).ajouter(kilos(valeur))).isInstanceOf(IllegalArgumentException.class);
    }

    private static Quantite grammes(int valeur) {
        return new Quantite(BigDecimal.valueOf(valeur), UniteQuantite.GRAMME);
    }

    private static Quantite kilos(int grammes) {
        return new Quantite(BigDecimal.valueOf(grammes).movePointLeft(3), UniteQuantite.KILOGRAMME);
    }

    private static Quantite articles(int valeur) {
        return new Quantite(BigDecimal.valueOf(valeur), UniteQuantite.ARTICLE);
    }

    private static BigDecimal enGrammes(Quantite quantite) {
        return quantite.unite() == UniteQuantite.KILOGRAMME ? quantite.value().movePointRight(3) : quantite.value();
    }
}

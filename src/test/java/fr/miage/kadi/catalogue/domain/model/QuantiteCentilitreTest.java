package fr.miage.kadi.catalogue.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** Conversions litre / centilitre ajoutées pour les liquides (A5 de la conception). */
class QuantiteCentilitreTest {

    @Test
    void ajouteDesCentilitresAUnVolumeEnLitresDansLUniteDeLaPremiereQuantite() {
        Quantite somme = quantite("1", UniteQuantite.LITRE).ajouter(quantite("50", UniteQuantite.CENTILITRE));
        assertThat(somme.unite()).isEqualTo(UniteQuantite.LITRE);
        assertThat(somme.value()).isEqualByComparingTo("1.5");
    }

    @Test
    void ajouteDesLitresAUnVolumeEnCentilitres() {
        Quantite somme = quantite("20", UniteQuantite.CENTILITRE).ajouter(quantite("1", UniteQuantite.LITRE));
        assertThat(somme.unite()).isEqualTo(UniteQuantite.CENTILITRE);
        assertThat(somme.value()).isEqualByComparingTo("120");
    }

    @Test
    void litresEtCentilitresSontCompatiblesMaisPasAvecLesPoidsNiLesArticles() {
        Quantite litre = quantite("1", UniteQuantite.LITRE);
        assertThat(litre.estCompatibleAvec(quantite("33", UniteQuantite.CENTILITRE))).isTrue();
        assertThat(quantite("33", UniteQuantite.CENTILITRE).estCompatibleAvec(litre)).isTrue();
        assertThat(litre.estCompatibleAvec(quantite("1", UniteQuantite.KILOGRAMME))).isFalse();
        assertThat(quantite("33", UniteQuantite.CENTILITRE).estCompatibleAvec(quantite("2", UniteQuantite.ARTICLE)))
                .isFalse();
        assertThatThrownBy(() -> litre.ajouter(quantite("2", UniteQuantite.ARTICLE)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void accepteUnVolumeDecimalEnCentilitres() {
        assertThat(quantite("12.5", UniteQuantite.CENTILITRE).value()).isEqualByComparingTo("12.5");
    }

    private static Quantite quantite(String valeur, UniteQuantite unite) {
        return new Quantite(new BigDecimal(valeur), unite);
    }
}

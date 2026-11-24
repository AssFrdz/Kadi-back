package fr.miage.kadi.catalogue.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class QuantiteTest {

    @Test
    void accepteUnNombreEntierDArticles() {
        Quantite quantite = new Quantite(new BigDecimal("2"), UniteQuantite.ARTICLE);

        assertThat(quantite.value()).isEqualByComparingTo("2");
        assertThat(quantite.unite()).isEqualTo(UniteQuantite.ARTICLE);
    }

    @Test
    void accepteUnNombreEntierEcritAvecUneDecimale() {
        Quantite quantite = new Quantite(new BigDecimal("2.0"), UniteQuantite.ARTICLE);

        assertThat(quantite.value()).isEqualByComparingTo("2");
    }

    @Test
    void accepteDesPoidsEtVolumesDecimaux() {
        assertThat(new Quantite(new BigDecimal("1.5"), UniteQuantite.KILOGRAMME).value())
                .isEqualByComparingTo("1.5");
        assertThat(new Quantite(new BigDecimal("0.5"), UniteQuantite.LITRE).value())
                .isEqualByComparingTo("0.5");
        assertThat(new Quantite(new BigDecimal("0.25"), UniteQuantite.GRAMME).value())
                .isEqualByComparingTo("0.25");
    }

    @Test
    void refuseUneQuantiteEgaleAZero() {
        assertThatThrownBy(() -> new Quantite(BigDecimal.ZERO, UniteQuantite.ARTICLE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La valeur doit être strictement supérieure à 0");
    }

    @Test
    void refuseUneQuantiteNegative() {
        assertThatThrownBy(() -> new Quantite(new BigDecimal("-0.5"), UniteQuantite.KILOGRAMME))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La valeur doit être strictement supérieure à 0");
    }

    @Test
    void refuseUneFractionDArticle() {
        assertThatThrownBy(() -> new Quantite(new BigDecimal("1.5"), UniteQuantite.ARTICLE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Une quantité d'articles doit être entière");
    }

    @Test
    void additionneDesQuantitesDeMemeUniteSansModifierLesOperandes() {
        Quantite premiere = new Quantite(new BigDecimal("2"), UniteQuantite.ARTICLE);
        Quantite seconde = new Quantite(new BigDecimal("3"), UniteQuantite.ARTICLE);

        Quantite somme = premiere.ajouter(seconde);

        assertThat(somme.value()).isEqualByComparingTo("5");
        assertThat(somme.unite()).isEqualTo(UniteQuantite.ARTICLE);
        assertThat(premiere.value()).isEqualByComparingTo("2");
        assertThat(seconde.value()).isEqualByComparingTo("3");
    }

    @Test
    void additionneDesGrammesAuxKilogrammes() {
        Quantite kilogramme = new Quantite(BigDecimal.ONE, UniteQuantite.KILOGRAMME);
        Quantite grammes = new Quantite(new BigDecimal("500"), UniteQuantite.GRAMME);

        Quantite somme = kilogramme.ajouter(grammes);

        assertThat(somme.value()).isEqualByComparingTo("1.5");
        assertThat(somme.unite()).isEqualTo(UniteQuantite.KILOGRAMME);
    }

    @Test
    void additionneDesKilogrammesAuxGrammes() {
        Quantite grammes = new Quantite(new BigDecimal("500"), UniteQuantite.GRAMME);
        Quantite kilogramme = new Quantite(BigDecimal.ONE, UniteQuantite.KILOGRAMME);

        Quantite somme = grammes.ajouter(kilogramme);

        assertThat(somme.value()).isEqualByComparingTo("1500");
        assertThat(somme.unite()).isEqualTo(UniteQuantite.GRAMME);
    }

    @Test
    void conserveLaPrecisionDesConversions() {
        Quantite kilogrammes = new Quantite(new BigDecimal("0.001"), UniteQuantite.KILOGRAMME);
        Quantite grammes = new Quantite(new BigDecimal("0.25"), UniteQuantite.GRAMME);

        assertThat(kilogrammes.ajouter(grammes).value()).isEqualByComparingTo("0.00125");
        assertThat(grammes.ajouter(kilogrammes).value()).isEqualByComparingTo("1.25");
    }

    @Test
    void refuseLAdditionDUnitesIncompatibles() {
        Quantite articles = new Quantite(new BigDecimal("3"), UniteQuantite.ARTICLE);
        Quantite kilogrammes = new Quantite(BigDecimal.ONE, UniteQuantite.KILOGRAMME);

        assertThatThrownBy(() -> articles.ajouter(kilogrammes))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unités incompatibles : ARTICLE et KILOGRAMME");
    }

    @Test
    void considereChaqueUniteCompatibleAvecElleMeme() {
        for (UniteQuantite unite : UniteQuantite.values()) {
            Quantite premiere = new Quantite(BigDecimal.ONE, unite);
            Quantite seconde = new Quantite(new BigDecimal("2"), unite);

            assertThat(premiere.estCompatibleAvec(seconde))
                    .as("Compatibilité de %s avec elle-même", unite)
                    .isTrue();
        }
    }

    @Test
    void considereLesGrammesEtLesKilogrammesCompatiblesDansLesDeuxSens() {
        Quantite grammes = new Quantite(new BigDecimal("500"), UniteQuantite.GRAMME);
        Quantite kilogrammes = new Quantite(BigDecimal.ONE, UniteQuantite.KILOGRAMME);

        assertThat(grammes.estCompatibleAvec(kilogrammes)).isTrue();
        assertThat(kilogrammes.estCompatibleAvec(grammes)).isTrue();
    }

    @Test
    void considereLesAutresUnitesDifferentesIncompatiblesDansLesDeuxSens() {
        UniteQuantite[][] paires = {
            {UniteQuantite.ARTICLE, UniteQuantite.GRAMME},
            {UniteQuantite.ARTICLE, UniteQuantite.KILOGRAMME},
            {UniteQuantite.ARTICLE, UniteQuantite.LITRE},
            {UniteQuantite.GRAMME, UniteQuantite.LITRE},
            {UniteQuantite.KILOGRAMME, UniteQuantite.LITRE}
        };

        for (UniteQuantite[] paire : paires) {
            Quantite premiere = new Quantite(BigDecimal.ONE, paire[0]);
            Quantite seconde = new Quantite(BigDecimal.ONE, paire[1]);

            assertThat(premiere.estCompatibleAvec(seconde))
                    .as("%s et %s", paire[0], paire[1])
                    .isFalse();
            assertThat(seconde.estCompatibleAvec(premiere))
                    .as("%s et %s", paire[1], paire[0])
                    .isFalse();
        }
    }

}

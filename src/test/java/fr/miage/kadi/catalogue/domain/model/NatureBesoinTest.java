package fr.miage.kadi.catalogue.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class NatureBesoinTest {

    @Test
    void unSolideSeCompteOuSePese() {
        assertThat(NatureBesoin.SOLIDE.unitesAutorisees())
                .containsExactly(UniteQuantite.ARTICLE, UniteQuantite.GRAMME, UniteQuantite.KILOGRAMME);
        assertThatCode(() -> NatureBesoin.SOLIDE.verifier(quantite("1.5", UniteQuantite.KILOGRAMME)))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> NatureBesoin.SOLIDE.verifier(quantite("2", UniteQuantite.LITRE)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void unLiquideSeCompteOuSeMesureEnLitresEtCentilitres() {
        assertThat(NatureBesoin.LIQUIDE.unitesAutorisees())
                .containsExactly(UniteQuantite.ARTICLE, UniteQuantite.LITRE, UniteQuantite.CENTILITRE);
        assertThatCode(() -> NatureBesoin.LIQUIDE.verifier(quantite("20", UniteQuantite.CENTILITRE)))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> NatureBesoin.LIQUIDE.verifier(quantite("500", UniteQuantite.GRAMME)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void unArticleUnitaireSeCompteSeulement() {
        assertThat(NatureBesoin.UNITAIRE.unitesAutorisees()).containsExactly(UniteQuantite.ARTICLE);
        assertThatThrownBy(() -> NatureBesoin.UNITAIRE.verifier(quantite("1", UniteQuantite.KILOGRAMME)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void refuseUneQuantiteAbsente() {
        assertThatThrownBy(() -> NatureBesoin.SOLIDE.verifier(null)).isInstanceOf(NullPointerException.class);
    }

    private static Quantite quantite(String valeur, UniteQuantite unite) {
        return new Quantite(new BigDecimal(valeur), unite);
    }
}

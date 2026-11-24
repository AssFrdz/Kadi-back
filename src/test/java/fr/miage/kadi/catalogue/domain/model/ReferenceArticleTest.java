package fr.miage.kadi.catalogue.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ReferenceArticleTest {

    @Test
    void accepteChaqueTypeDeReference() {
        for (TypeReference type : TypeReference.values()) {
            ReferenceArticle reference = new ReferenceArticle(type, 42);

            assertThat(reference.type()).isEqualTo(type);
            assertThat(reference.id()).isEqualTo(42);
        }
    }

    @Test
    void distingueDeuxTypesAvecLeMemeIdentifiant() {
        ReferenceArticle produit = new ReferenceArticle(TypeReference.PRODUIT, 42);
        ReferenceArticle besoin = new ReferenceArticle(TypeReference.BESOIN_GENERIQUE, 42);

        assertThat(produit).isNotEqualTo(besoin);
    }

    @Test
    void refuseUnTypeAbsent() {
        assertThatThrownBy(() -> new ReferenceArticle(null, 42))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Le type ne peut pas être null");
    }

    @Test
    void refuseUnIdentifiantNulOuNegatif() {
        assertThatThrownBy(() -> new ReferenceArticle(TypeReference.PRODUIT, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ReferenceArticle(TypeReference.PRODUIT, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void uneReferenceNestPasEgaleANull() {
        ReferenceArticle reference = new ReferenceArticle(TypeReference.PRODUIT, 42);

        assertThat(reference.equals(null)).isFalse();
    }
}

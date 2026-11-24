package fr.miage.kadi.catalogue.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class LigneListeTest {

    @Test
    void conserveLIdentifiantEtLeContenuFournis() {
        UUID id = UUID.fromString("6b0a7f19-11cb-4ad4-afd3-68e9ebd0e33c");
        ContenuLigne contenu = new ContenuLigne(
                new ReferenceArticle(TypeReference.BESOIN_GENERIQUE, 42),
                new Quantite(BigDecimal.ONE, UniteQuantite.ARTICLE), null);

        LigneListe ligne = new LigneListe(id, contenu);

        assertThat(ligne.id()).isEqualTo(id);
        assertThat(ligne.contenu()).isEqualTo(contenu);
    }

    @Test
    void refuseUnIdentifiantAbsent() {
        ContenuLigne contenu = new ContenuLigne(
                new ReferenceArticle(TypeReference.PRODUIT, 42),
                new Quantite(BigDecimal.ONE, UniteQuantite.ARTICLE), null);

        assertThatThrownBy(() -> new LigneListe(null, contenu))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("L'identifiant de la ligne ne peut pas être null.");
    }

    @Test
    void refuseUnContenuAbsent() {
        UUID id = UUID.fromString("6b0a7f19-11cb-4ad4-afd3-68e9ebd0e33c");

        assertThatThrownBy(() -> new LigneListe(id, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Le contenu de la ligne ne peut pas être null.");
    }

    @Test
    void remplaceLeContenuSansChangerLIdentifiantNiLaLigneInitiale() {
        UUID id = UUID.fromString("6b0a7f19-11cb-4ad4-afd3-68e9ebd0e33c");
        ReferenceArticle reference = new ReferenceArticle(TypeReference.BESOIN_GENERIQUE, 42);
        ContenuLigne initial = new ContenuLigne(
                reference, new Quantite(new BigDecimal("2"), UniteQuantite.ARTICLE), null);
        ContenuLigne nouveau = new ContenuLigne(
                reference, new Quantite(new BigDecimal("3"), UniteQuantite.ARTICLE), null);
        LigneListe ligne = new LigneListe(id, initial);

        LigneListe modifiee = ligne.avecContenu(nouveau);

        assertThat(modifiee).isNotSameAs(ligne);
        assertThat(modifiee.id()).isEqualTo(id);
        assertThat(modifiee.contenu()).isEqualTo(nouveau);
        assertThat(ligne.id()).isEqualTo(id);
        assertThat(ligne.contenu()).isEqualTo(initial);
        assertThat(ligne.contenu().quantite().value()).isEqualByComparingTo("2");
    }

    @Test
    void refuseDeRemplacerParUnContenuAbsent() {
        LigneListe ligne = new LigneListe(
                UUID.fromString("6b0a7f19-11cb-4ad4-afd3-68e9ebd0e33c"),
                new ContenuLigne(new ReferenceArticle(TypeReference.PRODUIT, 42),
                        new Quantite(BigDecimal.ONE, UniteQuantite.ARTICLE), null));

        assertThatThrownBy(() -> ligne.avecContenu(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Le contenu de la ligne ne peut pas être null.");
    }
}

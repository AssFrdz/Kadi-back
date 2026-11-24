package fr.miage.kadi.catalogue.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class ContenuLigneTest {

    @Test
    void accepteChaqueTypeDeReferenceSansFormat() {
        Quantite quantite = new Quantite(BigDecimal.ONE, UniteQuantite.ARTICLE);

        for (TypeReference type : TypeReference.values()) {
            ReferenceArticle reference = new ReferenceArticle(type, 42);
            ContenuLigne contenu = new ContenuLigne(reference, quantite, null);

            assertThat(contenu.reference()).isEqualTo(reference);
            assertThat(contenu.quantite()).isEqualTo(quantite);
            assertThat(contenu.formatSouhaite()).isNull();
        }
    }

    @Test
    void conserveLeFormatTelQueSaisi() {
        ReferenceArticle reference = new ReferenceArticle(TypeReference.BESOIN_GENERIQUE, 42);
        Quantite quantite = new Quantite(BigDecimal.ONE, UniteQuantite.ARTICLE);

        ContenuLigne contenu = new ContenuLigne(reference, quantite, "  sachet de 1 kg  ");

        assertThat(contenu.formatSouhaite()).isEqualTo("  sachet de 1 kg  ");
    }

    @Test
    void reconnaitLaMemeReferenceEtUnFormatAbsentOuIdentique() {
        Quantite quantite = new Quantite(BigDecimal.ONE, UniteQuantite.ARTICLE);
        ReferenceArticle premiere = new ReferenceArticle(TypeReference.BESOIN_GENERIQUE, 42);
        ReferenceArticle seconde = new ReferenceArticle(TypeReference.BESOIN_GENERIQUE, 42);

        assertThat(new ContenuLigne(premiere, quantite, null)
                .aMemeReferenceEtFormatQue(new ContenuLigne(seconde, quantite, null))).isTrue();
        assertThat(new ContenuLigne(premiere, quantite, "1 kg")
                .aMemeReferenceEtFormatQue(new ContenuLigne(seconde, quantite, "1 kg"))).isTrue();
    }

    @Test
    void distingueDesReferencesDeTypeOuIdentifiantDifferents() {
        Quantite quantite = new Quantite(BigDecimal.ONE, UniteQuantite.ARTICLE);
        ContenuLigne contenu = new ContenuLigne(
                new ReferenceArticle(TypeReference.PRODUIT, 42), quantite, null);

        assertThat(contenu.aMemeReferenceEtFormatQue(new ContenuLigne(
                new ReferenceArticle(TypeReference.BESOIN_GENERIQUE, 42), quantite, null))).isFalse();
        assertThat(contenu.aMemeReferenceEtFormatQue(new ContenuLigne(
                new ReferenceArticle(TypeReference.PRODUIT, 43), quantite, null))).isFalse();
    }

    @Test
    void distingueUnFormatAbsentEtDesFormatsDifferents() {
        Quantite quantite = new Quantite(BigDecimal.ONE, UniteQuantite.ARTICLE);
        ReferenceArticle reference = new ReferenceArticle(TypeReference.BESOIN_GENERIQUE, 42);
        ContenuLigne sansFormat = new ContenuLigne(reference, quantite, null);
        ContenuLigne unKilo = new ContenuLigne(reference, quantite, "1 kg");

        assertThat(sansFormat.aMemeReferenceEtFormatQue(unKilo)).isFalse();
        assertThat(unKilo.aMemeReferenceEtFormatQue(sansFormat)).isFalse();
        assertThat(unKilo.aMemeReferenceEtFormatQue(
                new ContenuLigne(reference, quantite, "1000 g"))).isFalse();
        assertThat(unKilo.aMemeReferenceEtFormatQue(
                new ContenuLigne(reference, quantite, " 1 kg "))).isFalse();
    }

    @Test
    void compareLaReferenceEtLeFormatIndependammentDesQuantites() {
        ReferenceArticle reference = new ReferenceArticle(TypeReference.BESOIN_GENERIQUE, 42);
        ContenuLigne articles = new ContenuLigne(
                reference, new Quantite(BigDecimal.ONE, UniteQuantite.ARTICLE), null);
        ContenuLigne poids = new ContenuLigne(
                reference, new Quantite(BigDecimal.ONE, UniteQuantite.KILOGRAMME), null);

        assertThat(articles.aMemeReferenceEtFormatQue(poids)).isTrue();
    }

    @Test
    void permetLaFusionPourLaMemeReferenceLeMemeFormatEtDesUnitesCompatibles() {
        ReferenceArticle reference = new ReferenceArticle(TypeReference.BESOIN_GENERIQUE, 42);
        ContenuLigne kilogramme = new ContenuLigne(
                reference, new Quantite(BigDecimal.ONE, UniteQuantite.KILOGRAMME), "sachet");
        ContenuLigne grammes = new ContenuLigne(
                new ReferenceArticle(TypeReference.BESOIN_GENERIQUE, 42),
                new Quantite(new BigDecimal("500"), UniteQuantite.GRAMME), "sachet");
        ContenuLigne sansFormat = new ContenuLigne(
                reference, new Quantite(BigDecimal.ONE, UniteQuantite.ARTICLE), null);
        ContenuLigne autreSansFormat = new ContenuLigne(
                reference, new Quantite(new BigDecimal("2"), UniteQuantite.ARTICLE), null);

        assertThat(kilogramme.peutFusionnerAutomatiquementAvec(grammes)).isTrue();
        assertThat(grammes.peutFusionnerAutomatiquementAvec(kilogramme)).isTrue();
        assertThat(sansFormat.peutFusionnerAutomatiquementAvec(autreSansFormat)).isTrue();
    }

    @Test
    void refuseLaFusionPourDesUnitesIncompatibles() {
        ReferenceArticle reference = new ReferenceArticle(TypeReference.BESOIN_GENERIQUE, 42);
        ContenuLigne articles = new ContenuLigne(
                reference, new Quantite(BigDecimal.ONE, UniteQuantite.ARTICLE), null);
        ContenuLigne poids = new ContenuLigne(
                reference, new Quantite(BigDecimal.ONE, UniteQuantite.KILOGRAMME), null);

        assertThat(articles.peutFusionnerAutomatiquementAvec(poids)).isFalse();
        assertThat(poids.peutFusionnerAutomatiquementAvec(articles)).isFalse();
    }

    @Test
    void refuseLaFusionPourDesReferencesOuFormatsDifferents() {
        Quantite quantite = new Quantite(BigDecimal.ONE, UniteQuantite.ARTICLE);
        ContenuLigne original = new ContenuLigne(
                new ReferenceArticle(TypeReference.BESOIN_GENERIQUE, 42), quantite, "1 kg");

        assertThat(original.peutFusionnerAutomatiquementAvec(new ContenuLigne(
                new ReferenceArticle(TypeReference.PRODUIT, 42), quantite, "1 kg"))).isFalse();
        assertThat(original.peutFusionnerAutomatiquementAvec(new ContenuLigne(
                new ReferenceArticle(TypeReference.BESOIN_GENERIQUE, 43), quantite, "1 kg"))).isFalse();
        assertThat(original.peutFusionnerAutomatiquementAvec(new ContenuLigne(
                original.reference(), quantite, "1000 g"))).isFalse();
        assertThat(original.peutFusionnerAutomatiquementAvec(new ContenuLigne(
                original.reference(), quantite, null))).isFalse();
    }

    @Test
    void fusionneSansModifierLesContenusEtConserveLaReferenceEtLeFormatDuPremier() {
        ReferenceArticle reference = new ReferenceArticle(TypeReference.BESOIN_GENERIQUE, 42);
        ContenuLigne premier = new ContenuLigne(
                reference, new Quantite(BigDecimal.ONE, UniteQuantite.KILOGRAMME), "sachet");
        ContenuLigne second = new ContenuLigne(
                new ReferenceArticle(TypeReference.BESOIN_GENERIQUE, 42),
                new Quantite(new BigDecimal("500"), UniteQuantite.GRAMME), "sachet");

        ContenuLigne resultat = premier.fusionnerAutomatiquementAvec(second);

        assertThat(resultat).isNotSameAs(premier).isNotSameAs(second);
        assertThat(resultat.reference()).isSameAs(reference);
        assertThat(resultat.formatSouhaite()).isEqualTo("sachet");
        assertThat(resultat.quantite().unite()).isEqualTo(UniteQuantite.KILOGRAMME);
        assertThat(resultat.quantite().value()).isEqualByComparingTo("1.5");
        assertThat(premier.quantite().value()).isEqualByComparingTo("1");
        assertThat(premier.quantite().unite()).isEqualTo(UniteQuantite.KILOGRAMME);
        assertThat(second.quantite().value()).isEqualByComparingTo("500");
        assertThat(second.quantite().unite()).isEqualTo(UniteQuantite.GRAMME);
    }

    @Test
    void fusionneDesArticlesSansFormatEtConserveLUniteDuPremier() {
        ReferenceArticle reference = new ReferenceArticle(TypeReference.PRODUIT, 42);
        ContenuLigne premier = new ContenuLigne(
                reference, new Quantite(new BigDecimal("2"), UniteQuantite.ARTICLE), null);
        ContenuLigne second = new ContenuLigne(
                reference, new Quantite(new BigDecimal("3"), UniteQuantite.ARTICLE), null);

        ContenuLigne resultat = premier.fusionnerAutomatiquementAvec(second);

        assertThat(resultat.quantite().value()).isEqualByComparingTo("5");
        assertThat(resultat.quantite().unite()).isEqualTo(UniteQuantite.ARTICLE);
        assertThat(resultat.formatSouhaite()).isNull();
    }

    @Test
    void refuseLaFusionQuandUneConditionNestPasRespectee() {
        ReferenceArticle reference = new ReferenceArticle(TypeReference.BESOIN_GENERIQUE, 42);
        Quantite articles = new Quantite(BigDecimal.ONE, UniteQuantite.ARTICLE);
        ContenuLigne premier = new ContenuLigne(reference, articles, "1 kg");
        ContenuLigne autreReference = new ContenuLigne(
                new ReferenceArticle(TypeReference.PRODUIT, 42), articles, "1 kg");
        ContenuLigne autreFormat = new ContenuLigne(reference, articles, "1000 g");
        ContenuLigne autreUnite = new ContenuLigne(
                reference, new Quantite(BigDecimal.ONE, UniteQuantite.KILOGRAMME), "1 kg");

        assertThatThrownBy(() -> premier.fusionnerAutomatiquementAvec(autreReference))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Les contenus ne peuvent pas être fusionnés automatiquement");
        assertThatThrownBy(() -> premier.fusionnerAutomatiquementAvec(autreFormat))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Les contenus ne peuvent pas être fusionnés automatiquement");
        assertThatThrownBy(() -> premier.fusionnerAutomatiquementAvec(autreUnite))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Les contenus ne peuvent pas être fusionnés automatiquement");
    }

    @Test
    void detecteLesFormatsDifferentsEquivalentsApresNormalisation() {
        ReferenceArticle reference = new ReferenceArticle(TypeReference.BESOIN_GENERIQUE, 42);
        Quantite quantite = new Quantite(BigDecimal.ONE, UniteQuantite.ARTICLE);
        ContenuLigne premier = new ContenuLigne(reference, quantite, "  Crème   fraîche ");
        ContenuLigne autreEcriture = new ContenuLigne(
                reference, quantite, "creme fraîche");

        assertThat(premier.pourraitFusionnerApresNormalisationAvec(autreEcriture)).isTrue();
        assertThat(autreEcriture.pourraitFusionnerApresNormalisationAvec(premier)).isTrue();
        assertThat(premier.peutFusionnerAutomatiquementAvec(autreEcriture)).isFalse();
    }

    @Test
    void neProposePasDeConfirmationPourDesTextesDejaIdentiques() {
        ReferenceArticle reference = new ReferenceArticle(TypeReference.BESOIN_GENERIQUE, 42);
        Quantite quantite = new Quantite(BigDecimal.ONE, UniteQuantite.ARTICLE);
        ContenuLigne premier = new ContenuLigne(reference, quantite, "1 kg");
        ContenuLigne memeTexte = new ContenuLigne(reference, quantite, "1 kg");

        assertThat(premier.pourraitFusionnerApresNormalisationAvec(memeTexte)).isFalse();
    }

    @Test
    void neProposePasDeConfirmationSiLaReferenceOuLeFormatEstAbsentOuDifferent() {
        Quantite quantite = new Quantite(BigDecimal.ONE, UniteQuantite.ARTICLE);
        ContenuLigne premier = new ContenuLigne(
                new ReferenceArticle(TypeReference.BESOIN_GENERIQUE, 42), quantite, "Crème fraîche");

        assertThat(premier.pourraitFusionnerApresNormalisationAvec(null)).isFalse();
        assertThat(premier.pourraitFusionnerApresNormalisationAvec(new ContenuLigne(
                new ReferenceArticle(TypeReference.PRODUIT, 42), quantite, "creme fraiche"))).isFalse();
        assertThat(premier.pourraitFusionnerApresNormalisationAvec(new ContenuLigne(
                premier.reference(), quantite, null))).isFalse();
        assertThat(premier.pourraitFusionnerApresNormalisationAvec(new ContenuLigne(
                premier.reference(), quantite, ""))).isFalse();
    }

    @Test
    void neConfondPasDesFormatsDontLesCaracteresRestentDifferentsApresNormalisation() {
        ReferenceArticle reference = new ReferenceArticle(TypeReference.BESOIN_GENERIQUE, 42);
        Quantite quantite = new Quantite(BigDecimal.ONE, UniteQuantite.ARTICLE);
        ContenuLigne unKilo = new ContenuLigne(reference, quantite, "sachet de 1 kg");
        ContenuLigne cinqCentsGrammes = new ContenuLigne(reference, quantite, "sachet de 500 g");

        assertThat(unKilo.pourraitFusionnerApresNormalisationAvec(cinqCentsGrammes)).isFalse();
    }

    @Test
    void proposeLaConfirmationPourFormatsNormalisesEtQuantitesEnGrammesEtKilogrammes() {
        ReferenceArticle reference = new ReferenceArticle(TypeReference.BESOIN_GENERIQUE, 42);
        ContenuLigne kilogrammes = new ContenuLigne(
                reference, new Quantite(BigDecimal.ONE, UniteQuantite.KILOGRAMME),
                "  Crème   fraîche ");
        ContenuLigne grammes = new ContenuLigne(
                reference, new Quantite(new BigDecimal("500"), UniteQuantite.GRAMME),
                "creme fraîche");

        assertThat(kilogrammes.pourraitFusionnerApresNormalisationAvec(grammes)).isTrue();
        assertThat(grammes.pourraitFusionnerApresNormalisationAvec(kilogrammes)).isTrue();
    }

    @Test
    void neProposePasLaConfirmationPourDesQuantitesAuxUnitesIncompatibles() {
        ReferenceArticle reference = new ReferenceArticle(TypeReference.BESOIN_GENERIQUE, 42);
        ContenuLigne articles = new ContenuLigne(
                reference, new Quantite(BigDecimal.ONE, UniteQuantite.ARTICLE),
                "  Crème   fraîche ");
        ContenuLigne kilogrammes = new ContenuLigne(
                reference, new Quantite(BigDecimal.ONE, UniteQuantite.KILOGRAMME),
                "creme fraîche");

        assertThat(articles.pourraitFusionnerApresNormalisationAvec(kilogrammes)).isFalse();
        assertThat(kilogrammes.pourraitFusionnerApresNormalisationAvec(articles)).isFalse();
    }
}

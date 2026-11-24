package fr.miage.kadi.catalogue.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class ListeCoursesTest {

    private static final IdentifiantCompte COMPTE_TEST = new IdentifiantCompte(101);

    private ListeCourses liste(List<LigneListe> lignes) {
        return new ListeCourses(COMPTE_TEST, lignes);
    }

    private LigneListe ligne(UUID id, long referenceId) {
        return new LigneListe(id, new ContenuLigne(
                new ReferenceArticle(TypeReference.BESOIN_GENERIQUE, referenceId),
                new Quantite(BigDecimal.ONE, UniteQuantite.ARTICLE), null));
    }

    @Test
    void accepteUneListeVideEtConserveLOrdreDesLignes() {
        LigneListe premiere = ligne(UUID.randomUUID(), 1);
        LigneListe seconde = ligne(UUID.randomUUID(), 2);

        assertThat(liste(List.of()).lignes()).isEmpty();
        assertThat(liste(List.of(premiere, seconde)).lignes())
                .containsExactly(premiere, seconde);
    }

    @Test
    void refuseDeuxLignesAyantLeMemeIdentifiant() {
        UUID id = UUID.fromString("6b0a7f19-11cb-4ad4-afd3-68e9ebd0e33c");

        assertThatThrownBy(() -> liste(List.of(ligne(id, 1), ligne(id, 2))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Deux lignes ne peuvent pas avoir le même identifiant : " + id);
    }

    @Test
    void copieLaListeEtEmpecheSaModificationDepuisLAccesseur() {
        LigneListe premiere = ligne(UUID.randomUUID(), 1);
        LigneListe seconde = ligne(UUID.randomUUID(), 2);
        List<LigneListe> source = new ArrayList<>(List.of(premiere));
        ListeCourses liste = liste(source);

        source.add(seconde);

        assertThat(liste.lignes()).containsExactly(premiere);
        assertThatThrownBy(() -> liste.lignes().add(seconde))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThat(liste.lignes()).containsExactly(premiere);
    }

    @Test
    void trouveToutesLesLignesFusionnablesDansLeurOrdreSansModifierLaListe() {
        ReferenceArticle reference = new ReferenceArticle(TypeReference.BESOIN_GENERIQUE, 1);
        ContenuLigne recherche = new ContenuLigne(
                reference, new Quantite(BigDecimal.ONE, UniteQuantite.KILOGRAMME), "sachet");
        LigneListe premiere = new LigneListe(UUID.randomUUID(), new ContenuLigne(
                reference, new Quantite(new BigDecimal("500"), UniteQuantite.GRAMME), "sachet"));
        LigneListe autreReference = new LigneListe(UUID.randomUUID(), new ContenuLigne(
                new ReferenceArticle(TypeReference.PRODUIT, 1),
                new Quantite(BigDecimal.ONE, UniteQuantite.KILOGRAMME), "sachet"));
        LigneListe autreFormat = new LigneListe(UUID.randomUUID(), new ContenuLigne(
                reference, new Quantite(BigDecimal.ONE, UniteQuantite.KILOGRAMME), null));
        LigneListe autreUnite = new LigneListe(UUID.randomUUID(), new ContenuLigne(
                reference, new Quantite(BigDecimal.ONE, UniteQuantite.ARTICLE), "sachet"));
        LigneListe seconde = new LigneListe(UUID.randomUUID(), new ContenuLigne(
                reference, new Quantite(BigDecimal.ONE, UniteQuantite.KILOGRAMME), "sachet"));
        ListeCourses liste = liste(
                List.of(premiere, autreReference, autreFormat, autreUnite, seconde));

        List<LigneListe> resultat = liste.lignesFusionnablesAvec(recherche);

        assertThat(resultat).containsExactly(premiere, seconde);
        assertThat(liste.lignes())
                .containsExactly(premiere, autreReference, autreFormat, autreUnite, seconde);
        assertThatThrownBy(() -> resultat.add(autreReference))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void trouveLesLignesDontLeFormatNormaliseDemandeConfirmationDansLeurOrdre() {
        ReferenceArticle reference = new ReferenceArticle(TypeReference.BESOIN_GENERIQUE, 42);
        ContenuLigne recherche = new ContenuLigne(
                reference, new Quantite(BigDecimal.ONE, UniteQuantite.KILOGRAMME),
                "creme fraîche");
        LigneListe premiere = new LigneListe(UUID.randomUUID(), new ContenuLigne(
                reference, new Quantite(new BigDecimal("500"), UniteQuantite.GRAMME),
                "  Crème   fraîche "));
        LigneListe autreReference = new LigneListe(UUID.randomUUID(), new ContenuLigne(
                new ReferenceArticle(TypeReference.PRODUIT, 42),
                new Quantite(BigDecimal.ONE, UniteQuantite.KILOGRAMME), "Crème fraîche"));
        LigneListe formatDifferent = new LigneListe(UUID.randomUUID(), new ContenuLigne(
                reference, new Quantite(BigDecimal.ONE, UniteQuantite.KILOGRAMME),
                "sachet de 1 kg"));
        LigneListe uniteIncompatible = new LigneListe(UUID.randomUUID(), new ContenuLigne(
                reference, new Quantite(BigDecimal.ONE, UniteQuantite.ARTICLE),
                "Crème fraîche"));
        LigneListe seconde = new LigneListe(UUID.randomUUID(), new ContenuLigne(
                reference, new Quantite(new BigDecimal("250"), UniteQuantite.GRAMME),
                "CREME FRAICHE"));
        ListeCourses liste = liste(
                List.of(premiere, autreReference, formatDifferent, uniteIncompatible, seconde));

        List<LigneListe> resultat = liste.lignesAConfirmerAvec(recherche);

        assertThat(resultat).containsExactly(premiere, seconde);
        assertThat(liste.lignes())
                .containsExactly(premiere, autreReference, formatDifferent, uniteIncompatible, seconde);
        assertThatThrownBy(() -> resultat.add(autreReference))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void ajouteUneLigneEnFinSansModifierLaListeInitiale() {
        LigneListe existante = ligne(UUID.randomUUID(), 1);
        LigneListe nouvelle = ligne(UUID.randomUUID(), 2);
        ListeCourses initiale = liste(List.of(existante));

        ListeCourses resultat = initiale.ajouter(nouvelle);

        assertThat(resultat).isNotSameAs(initiale);
        assertThat(resultat.lignes()).containsExactly(existante, nouvelle);
        assertThat(initiale.lignes()).containsExactly(existante);
        assertThat(liste(List.of()).ajouter(nouvelle).lignes()).containsExactly(nouvelle);
    }

    @Test
    void fusionneLaLigneCompatibleEnConservantSonIdentifiantEtSaPosition() {
        ReferenceArticle reference = new ReferenceArticle(TypeReference.BESOIN_GENERIQUE, 1);
        LigneListe avant = ligne(UUID.randomUUID(), 2);
        LigneListe existante = new LigneListe(UUID.randomUUID(), new ContenuLigne(
                reference, new Quantite(BigDecimal.ONE, UniteQuantite.KILOGRAMME), "sachet"));
        LigneListe apres = ligne(UUID.randomUUID(), 3);
        LigneListe nouvelle = new LigneListe(UUID.randomUUID(), new ContenuLigne(
                reference, new Quantite(new BigDecimal("500"), UniteQuantite.GRAMME), "sachet"));
        ListeCourses initiale = liste(List.of(avant, existante, apres));

        ListeCourses resultat = initiale.ajouter(nouvelle);

        assertThat(resultat).isNotSameAs(initiale);
        assertThat(resultat.lignes()).hasSize(3);
        assertThat(resultat.lignes().get(0)).isEqualTo(avant);
        LigneListe fusionnee = resultat.lignes().get(1);
        assertThat(fusionnee.id()).isEqualTo(existante.id()).isNotEqualTo(nouvelle.id());
        assertThat(fusionnee.contenu().reference()).isEqualTo(reference);
        assertThat(fusionnee.contenu().formatSouhaite()).isEqualTo("sachet");
        assertThat(fusionnee.contenu().quantite().value()).isEqualByComparingTo("1.5");
        assertThat(fusionnee.contenu().quantite().unite()).isEqualTo(UniteQuantite.KILOGRAMME);
        assertThat(resultat.lignes().get(2)).isEqualTo(apres);
        assertThat(initiale.lignes()).containsExactly(avant, existante, apres);
        assertThat(existante.contenu().quantite().value()).isEqualByComparingTo("1");
    }

    @Test
    void gardeDistinctsLesTypesFormatsEtUnitesIncompatibles() {
        ReferenceArticle reference = new ReferenceArticle(TypeReference.BESOIN_GENERIQUE, 1);
        LigneListe existante = new LigneListe(UUID.randomUUID(), new ContenuLigne(
                reference, new Quantite(BigDecimal.ONE, UniteQuantite.ARTICLE), "1 kg"));
        LigneListe autreType = new LigneListe(UUID.randomUUID(), new ContenuLigne(
                new ReferenceArticle(TypeReference.PRODUIT, 1),
                new Quantite(BigDecimal.ONE, UniteQuantite.ARTICLE), "1 kg"));
        LigneListe autreFormat = new LigneListe(UUID.randomUUID(), new ContenuLigne(
                reference, new Quantite(BigDecimal.ONE, UniteQuantite.ARTICLE), "1000 g"));
        LigneListe autreUnite = new LigneListe(UUID.randomUUID(), new ContenuLigne(
                reference, new Quantite(BigDecimal.ONE, UniteQuantite.KILOGRAMME), "1 kg"));

        ListeCourses resultat = liste(List.of(existante))
                .ajouter(autreType).ajouter(autreFormat).ajouter(autreUnite);

        assertThat(resultat.lignes()).containsExactly(existante, autreType, autreFormat, autreUnite);
    }

    @Test
    void refuseUnAjoutNulOuUnIdentifiantDejaPresentMemeSiLaFusionEstPossible() {
        LigneListe existante = ligne(UUID.randomUUID(), 1);
        ListeCourses initiale = liste(List.of(existante));

        assertThatThrownBy(() -> initiale.ajouter(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La ligne à ajouter ne peut pas être null.");
        assertThatThrownBy(() -> initiale.ajouter(ligne(existante.id(), 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(existante.id().toString());
        assertThatThrownBy(() -> initiale.ajouter(ligne(existante.id(), 2)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(existante.id().toString());
        assertThat(initiale.lignes()).containsExactly(existante);
    }

    @Test
    void refuseLajoutSiPlusieursLignesSontCompatiblesSansModifierLaListe() {
        LigneListe premiere = ligne(UUID.randomUUID(), 1);
        LigneListe seconde = ligne(UUID.randomUUID(), 1);
        LigneListe nouvelle = ligne(UUID.randomUUID(), 1);
        ListeCourses initiale = liste(List.of(premiere, seconde));

        assertThatThrownBy(() -> initiale.ajouter(nouvelle))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("plusieurs lignes");
        assertThat(initiale.lignes()).containsExactly(premiere, seconde);
    }

    @Test
    void supprimeLaLigneDemandeeEnConservantLOrdreEtLaListeInitiale() {
        LigneListe premiere = ligne(UUID.randomUUID(), 1);
        LigneListe milieu = ligne(UUID.randomUUID(), 2);
        LigneListe derniere = ligne(UUID.randomUUID(), 3);
        ListeCourses initiale = liste(List.of(premiere, milieu, derniere));

        ListeCourses resultat = initiale.supprimer(milieu.id());

        assertThat(resultat).isNotSameAs(initiale);
        assertThat(resultat.lignes()).containsExactly(premiere, derniere);
        assertThat(initiale.lignes()).containsExactly(premiere, milieu, derniere);
        assertThat(initiale.supprimer(premiere.id()).lignes()).containsExactly(milieu, derniere);
        assertThat(initiale.supprimer(derniere.id()).lignes()).containsExactly(premiere, milieu);
        assertThat(liste(List.of(milieu)).supprimer(milieu.id()).lignes()).isEmpty();
    }

    @Test
    void refuseDeSupprimerUnIdentifiantAbsentOuInconnuSansModifierLaListe() {
        LigneListe existante = ligne(UUID.randomUUID(), 1);
        ListeCourses initiale = liste(List.of(existante));
        UUID inconnu = UUID.fromString("6b0a7f19-11cb-4ad4-afd3-68e9ebd0e33c");

        assertThatThrownBy(() -> initiale.supprimer(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("L'identifiant à supprimer ne peut pas être null.");
        assertThatThrownBy(() -> initiale.supprimer(inconnu))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(inconnu.toString());
        assertThatThrownBy(() -> liste(List.of()).supprimer(inconnu))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(inconnu.toString());
        assertThat(initiale.lignes()).containsExactly(existante);
    }

    @Test
    void modifieUneLigneEnConservantSonIdentifiantSaPositionEtLaListeInitiale() {
        LigneListe avant = ligne(UUID.randomUUID(), 1);
        LigneListe aModifier = ligne(UUID.randomUUID(), 2);
        LigneListe apres = ligne(UUID.randomUUID(), 3);
        ContenuLigne nouveauContenu = new ContenuLigne(
                new ReferenceArticle(TypeReference.BESOIN_GENERIQUE, 4),
                new Quantite(new BigDecimal("3"), UniteQuantite.ARTICLE), "paquet");
        ListeCourses initiale = liste(List.of(avant, aModifier, apres));

        ListeCourses resultat = initiale.modifier(aModifier.id(), nouveauContenu);

        assertThat(resultat).isNotSameAs(initiale);
        assertThat(resultat.lignes()).hasSize(3);
        assertThat(resultat.lignes().get(0)).isEqualTo(avant);
        assertThat(resultat.lignes().get(1).id()).isEqualTo(aModifier.id());
        assertThat(resultat.lignes().get(1).contenu()).isEqualTo(nouveauContenu);
        assertThat(resultat.lignes().get(2)).isEqualTo(apres);
        assertThat(initiale.lignes()).containsExactly(avant, aModifier, apres);
        assertThat(aModifier.contenu().reference().id()).isEqualTo(2);
    }

    @Test
    void permetDeModifierUneLigneSansLaConfondreAvecElleMeme() {
        LigneListe existante = ligne(UUID.randomUUID(), 1);
        ContenuLigne contenuCompatible = new ContenuLigne(
                existante.contenu().reference(),
                new Quantite(new BigDecimal("2"), UniteQuantite.ARTICLE), null);
        ListeCourses initiale = liste(List.of(existante));

        ListeCourses resultat = initiale.modifier(existante.id(), contenuCompatible);

        assertThat(resultat.lignes()).hasSize(1);
        assertThat(resultat.lignes().get(0).id()).isEqualTo(existante.id());
        assertThat(resultat.lignes().get(0).contenu()).isEqualTo(contenuCompatible);
    }

    @Test
    void refuseModificationAvecIdentifiantOuContenuNulOuIdentifiantInconnu() {
        LigneListe existante = ligne(UUID.randomUUID(), 1);
        ListeCourses initiale = liste(List.of(existante));
        UUID inconnu = UUID.randomUUID();

        assertThatThrownBy(() -> initiale.modifier(null, existante.contenu()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("L'identifiant à modifier ne peut pas être null.");
        assertThatThrownBy(() -> initiale.modifier(existante.id(), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Le nouveau contenu ne peut pas être null.");
        assertThatThrownBy(() -> initiale.modifier(inconnu, existante.contenu()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(inconnu.toString());
        assertThat(initiale.lignes()).containsExactly(existante);
    }

    @Test
    void refuseUneModificationQuiRendDeuxLignesFusionnablesSansModifierLaListe() {
        ReferenceArticle reference = new ReferenceArticle(TypeReference.BESOIN_GENERIQUE, 1);
        LigneListe premiere = new LigneListe(UUID.randomUUID(), new ContenuLigne(
                reference, new Quantite(BigDecimal.ONE, UniteQuantite.ARTICLE), "petit"));
        LigneListe seconde = new LigneListe(UUID.randomUUID(), new ContenuLigne(
                reference, new Quantite(new BigDecimal("2"), UniteQuantite.ARTICLE), "commun"));
        ContenuLigne contenuEnConflit = new ContenuLigne(
                reference, new Quantite(new BigDecimal("5"), UniteQuantite.ARTICLE), "commun");
        ListeCourses initiale = liste(List.of(premiere, seconde));

        assertThatThrownBy(() -> initiale.modifier(premiere.id(), contenuEnConflit))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Modification impossible : une autre ligne est compatible avec ce contenu.");
        assertThat(initiale.lignes()).containsExactly(premiere, seconde);
    }

    @Test
    void fusionneApresConfirmationEnGardantIdentifiantPositionEtFormatExistant() {
        ReferenceArticle reference = new ReferenceArticle(TypeReference.BESOIN_GENERIQUE, 42);
        LigneListe avant = ligne(UUID.randomUUID(), 1);
        LigneListe existante = new LigneListe(UUID.randomUUID(), new ContenuLigne(
                reference, new Quantite(BigDecimal.ONE, UniteQuantite.KILOGRAMME),
                "  Crème   fraîche "));
        LigneListe apres = ligne(UUID.randomUUID(), 3);
        ContenuLigne aAjouter = new ContenuLigne(
                reference, new Quantite(new BigDecimal("500"), UniteQuantite.GRAMME),
                "creme fraîche");
        ListeCourses initiale = liste(List.of(avant, existante, apres));

        ListeCourses resultat =
                initiale.fusionnerApresConfirmation(existante.id(), aAjouter);

        assertThat(resultat).isNotSameAs(initiale);
        assertThat(resultat.lignes()).hasSize(3);
        assertThat(resultat.lignes().get(0)).isEqualTo(avant);
        LigneListe fusionnee = resultat.lignes().get(1);
        assertThat(fusionnee.id()).isEqualTo(existante.id());
        assertThat(fusionnee.contenu().formatSouhaite()).isEqualTo("  Crème   fraîche ");
        assertThat(fusionnee.contenu().quantite().value()).isEqualByComparingTo("1.5");
        assertThat(fusionnee.contenu().quantite().unite()).isEqualTo(UniteQuantite.KILOGRAMME);
        assertThat(resultat.lignes().get(2)).isEqualTo(apres);
        assertThat(initiale.lignes()).containsExactly(avant, existante, apres);
        assertThat(existante.contenu().quantite().value()).isEqualByComparingTo("1");
    }

    @Test
    void refuseUneFusionConfirmeeSiLaLigneEstInconnueOuNestPlusCandidate() {
        ReferenceArticle reference = new ReferenceArticle(TypeReference.BESOIN_GENERIQUE, 42);
        LigneListe existante = new LigneListe(UUID.randomUUID(), new ContenuLigne(
                reference, new Quantite(BigDecimal.ONE, UniteQuantite.KILOGRAMME), "creme fraîche"));
        ListeCourses liste = liste(List.of(existante));
        ContenuLigne candidate = new ContenuLigne(
                reference, new Quantite(BigDecimal.ONE, UniteQuantite.GRAMME), "Crème   fraîche");
        ContenuLigne formatNonEquivalent = new ContenuLigne(
                reference, new Quantite(BigDecimal.ONE, UniteQuantite.GRAMME), "sachet");

        assertThatThrownBy(() -> liste.fusionnerApresConfirmation(UUID.randomUUID(), candidate))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Aucune ligne");
        assertThatThrownBy(() -> liste.fusionnerApresConfirmation(
                existante.id(), formatNonEquivalent))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La ligne sélectionnée ne peut plus être fusionnée avec ce contenu.");
        assertThat(liste.lignes()).containsExactly(existante);
    }

    @Test
    void conserveLeProprietaireQuandLaListeEstAjouteeModifieeSupprimeeOuFusionnee() {
        LigneListe initiale = ligne(UUID.randomUUID(), 1);
        ListeCourses listeInitiale = liste(List.of(initiale));

        ListeCourses apresAjout = listeInitiale.ajouter(ligne(UUID.randomUUID(), 2));
        ResultatAjout resultatAjout = listeInitiale.preparerAjout(ligne(UUID.randomUUID(), 3));
        ListeCourses apresModification = listeInitiale.modifier(
                initiale.id(),
                new ContenuLigne(
                        new ReferenceArticle(TypeReference.BESOIN_GENERIQUE, 3),
                        new Quantite(BigDecimal.ONE, UniteQuantite.ARTICLE),
                        null));
        ListeCourses apresSuppression = listeInitiale.supprimer(initiale.id());

        ReferenceArticle reference = new ReferenceArticle(TypeReference.BESOIN_GENERIQUE, 42);
        LigneListe formatExistant = new LigneListe(UUID.randomUUID(), new ContenuLigne(
                reference, new Quantite(BigDecimal.ONE, UniteQuantite.KILOGRAMME),
                "crème fraîche"));
        ListeCourses listeAvecFormat = liste(List.of(formatExistant));
        ListeCourses apresFusionConfirmee = listeAvecFormat.fusionnerApresConfirmation(
                formatExistant.id(),
                new ContenuLigne(reference,
                        new Quantite(new BigDecimal("500"), UniteQuantite.GRAMME),
                        "CREME FRAICHE"));

        assertThat(apresAjout.proprietaireId()).isEqualTo(COMPTE_TEST);
        assertThat(resultatAjout).isInstanceOfSatisfying(
                ResultatAjout.AjoutSimple.class,
                ajout -> assertThat(ajout.liste().proprietaireId()).isEqualTo(COMPTE_TEST));
        assertThat(apresModification.proprietaireId()).isEqualTo(COMPTE_TEST);
        assertThat(apresSuppression.proprietaireId()).isEqualTo(COMPTE_TEST);
        assertThat(apresFusionConfirmee.proprietaireId()).isEqualTo(COMPTE_TEST);
    }

        @Test
        void conserveDeuxLignesSepareesQuandLaFusionCandidateNestPasConfirmee() {
            ReferenceArticle reference = new ReferenceArticle(TypeReference.BESOIN_GENERIQUE, 42);
            LigneListe ligneExistante = new LigneListe(
                    UUID.randomUUID(),
                    new ContenuLigne(
                            reference,
                            new Quantite(BigDecimal.ONE, UniteQuantite.ARTICLE),
                            "Crème fraîche"));
            ListeCourses liste = liste(List.of(ligneExistante));
            LigneListe nouvelleLigne = new LigneListe(
                    UUID.randomUUID(),
                    new ContenuLigne(
                            reference,
                            new Quantite(BigDecimal.ONE, UniteQuantite.ARTICLE),
                            "creme   fraîche"));

            List<LigneListe> candidates = liste.lignesAConfirmerAvec(nouvelleLigne.contenu());

            assertThat(candidates).containsExactly(ligneExistante);

            ListeCourses resultat = liste.ajouter(nouvelleLigne);

            assertThat(resultat.lignes()).containsExactly(ligneExistante, nouvelleLigne);
            assertThat(resultat.lignes()).hasSize(2);
            assertThat(resultat.lignes().get(0).id()).isEqualTo(ligneExistante.id());
            assertThat(resultat.lignes().get(0).contenu().quantite().value())
                    .isEqualByComparingTo("1");
            assertThat(resultat.lignes().get(1).id()).isEqualTo(nouvelleLigne.id());
            assertThat(resultat.lignes().get(1).contenu().quantite().value())
                    .isEqualByComparingTo("1");
            assertThat(liste.lignes()).containsExactly(ligneExistante);
         }

    @Test
    void prepareAjoutRetourneUnAjoutSimpleQuandAucuneLigneNeCorrespond() {
        LigneListe existante = ligne(UUID.randomUUID(), 1);
        LigneListe nouvelle = ligne(UUID.randomUUID(), 2);
        ListeCourses initiale = liste(List.of(existante));

        ResultatAjout resultat = initiale.preparerAjout(nouvelle);

        assertThat(resultat).isInstanceOfSatisfying(
                ResultatAjout.AjoutSimple.class,
                ajout -> assertThat(ajout.liste().lignes()).containsExactly(existante, nouvelle));
        assertThat(initiale.lignes()).containsExactly(existante);
    }

    @Test
    void prepareAjoutRetourneLaFusionAutomatiqueQuandUneLigneEstCompatible() {
        ReferenceArticle reference = new ReferenceArticle(TypeReference.BESOIN_GENERIQUE, 42);
        LigneListe existante = new LigneListe(UUID.randomUUID(), new ContenuLigne(
                reference, new Quantite(BigDecimal.ONE, UniteQuantite.KILOGRAMME), "sachet"));
        LigneListe nouvelle = new LigneListe(UUID.randomUUID(), new ContenuLigne(
                reference, new Quantite(new BigDecimal("500"), UniteQuantite.GRAMME), "sachet"));
        ListeCourses initiale = liste(List.of(existante));

        ResultatAjout resultat = initiale.preparerAjout(nouvelle);

        assertThat(resultat).isInstanceOfSatisfying(
                ResultatAjout.FusionAutomatique.class,
                fusion -> {
                    assertThat(fusion.idLigneConservee()).isEqualTo(existante.id());
                    assertThat(fusion.liste().lignes()).hasSize(1);
                    assertThat(fusion.liste().lignes().get(0).id()).isEqualTo(existante.id());
                    assertThat(fusion.liste().lignes().get(0).contenu().quantite().value())
                            .isEqualByComparingTo("1.5");
                });
        assertThat(initiale.lignes()).containsExactly(existante);
    }

    @Test
    void prepareAjoutDemandeConfirmationEtRetourneToutesLesLignesCandidates() {
        ReferenceArticle reference = new ReferenceArticle(TypeReference.BESOIN_GENERIQUE, 42);
        LigneListe premiere = new LigneListe(UUID.randomUUID(), new ContenuLigne(
                reference, new Quantite(BigDecimal.ONE, UniteQuantite.ARTICLE), "creme   fraîche"));
        LigneListe seconde = new LigneListe(UUID.randomUUID(), new ContenuLigne(
                reference, new Quantite(new BigDecimal("2"), UniteQuantite.ARTICLE), "CREME FRAICHE"));
        LigneListe nouvelle = new LigneListe(UUID.randomUUID(), new ContenuLigne(
                reference, new Quantite(BigDecimal.ONE, UniteQuantite.ARTICLE), "Crème fraîche"));
        ListeCourses initiale = liste(List.of(premiere, seconde));

        ResultatAjout resultat = initiale.preparerAjout(nouvelle);

        assertThat(resultat).isInstanceOfSatisfying(
                ResultatAjout.ConfirmationRequise.class,
                confirmation -> {
                    assertThat(confirmation.ligneAAjouter()).isEqualTo(nouvelle);
                    assertThat(confirmation.lignesCandidates()).containsExactly(premiere, seconde);
                });
        assertThat(initiale.lignes()).containsExactly(premiere, seconde);
    }

    @Test
    void prepareAjoutRetourneUnConflitPourPlusieursLignesFusionnablesAutomatiquement() {
        LigneListe premiere = ligne(UUID.randomUUID(), 42);
        LigneListe seconde = ligne(UUID.randomUUID(), 42);
        LigneListe nouvelle = ligne(UUID.randomUUID(), 42);
        ListeCourses initiale = liste(List.of(premiere, seconde));

        ResultatAjout resultat = initiale.preparerAjout(nouvelle);

        assertThat(resultat).isInstanceOfSatisfying(
                ResultatAjout.ConflitFusionAutomatique.class,
                conflit -> assertThat(conflit.lignesCandidates()).containsExactly(premiere, seconde));
        assertThat(initiale.lignes()).containsExactly(premiere, seconde);
    }

    @Test
    void parcoursConfirmationPermetDeChoisirUneCandidatePuisDeFusionner() {
        ReferenceArticle reference = new ReferenceArticle(TypeReference.BESOIN_GENERIQUE, 42);
        LigneListe premiere = new LigneListe(UUID.randomUUID(), new ContenuLigne(
                reference, new Quantite(BigDecimal.ONE, UniteQuantite.KILOGRAMME),
                "crème fraîche"));
        LigneListe choisie = new LigneListe(UUID.randomUUID(), new ContenuLigne(
                reference, new Quantite(new BigDecimal("2"), UniteQuantite.KILOGRAMME),
                "CREME FRAICHE"));
        LigneListe aAjouter = new LigneListe(UUID.randomUUID(), new ContenuLigne(
                reference, new Quantite(new BigDecimal("500"), UniteQuantite.GRAMME),
                "  Crème   fraîche "));
        ListeCourses initiale = liste(List.of(premiere, choisie));

        ResultatAjout resultat = initiale.preparerAjout(aAjouter);

        assertThat(resultat).isInstanceOfSatisfying(
                ResultatAjout.ConfirmationRequise.class,
                confirmation -> {
                    assertThat(confirmation.lignesCandidates()).containsExactly(premiere, choisie);
                    ListeCourses fusionnee = initiale.fusionnerApresConfirmation(
                            choisie.id(), confirmation.ligneAAjouter().contenu());

                    assertThat(fusionnee.lignes()).hasSize(2);
                    assertThat(fusionnee.lignes().get(0)).isEqualTo(premiere);
                    assertThat(fusionnee.lignes().get(1).id()).isEqualTo(choisie.id());
                    assertThat(fusionnee.lignes().get(1).contenu().quantite().value())
                            .isEqualByComparingTo("2.5");
                    assertThat(fusionnee.lignes().get(1).contenu().formatSouhaite())
                            .isEqualTo("CREME FRAICHE");
                });
        assertThat(initiale.lignes()).containsExactly(premiere, choisie);
    }

    @Test
    void parcoursConfirmationPermetDeRefuserEtDajouterLaLigneSeparément() {
        ReferenceArticle reference = new ReferenceArticle(TypeReference.BESOIN_GENERIQUE, 42);
        LigneListe existante = new LigneListe(UUID.randomUUID(), new ContenuLigne(
                reference, new Quantite(BigDecimal.ONE, UniteQuantite.ARTICLE),
                "Crème fraîche"));
        LigneListe aAjouter = new LigneListe(UUID.randomUUID(), new ContenuLigne(
                reference, new Quantite(BigDecimal.ONE, UniteQuantite.ARTICLE),
                "creme   fraîche"));
        ListeCourses initiale = liste(List.of(existante));

        ResultatAjout resultat = initiale.preparerAjout(aAjouter);

        assertThat(resultat).isInstanceOfSatisfying(
                ResultatAjout.ConfirmationRequise.class,
                confirmation -> {
                    assertThat(confirmation.lignesCandidates()).containsExactly(existante);
                    ListeCourses separee = initiale.ajouter(confirmation.ligneAAjouter());

                    assertThat(separee.lignes()).containsExactly(existante, aAjouter);
                    assertThat(separee.lignes().get(0).contenu().quantite().value())
                            .isEqualByComparingTo("1");
                    assertThat(separee.lignes().get(1).contenu().quantite().value())
                            .isEqualByComparingTo("1");
                });
        assertThat(initiale.lignes()).containsExactly(existante);
    }
}

package fr.miage.kadi.catalogue.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.miage.kadi.catalogue.application.exception.FusionLigneImpossibleException;
import fr.miage.kadi.catalogue.application.exception.LigneListeIntrouvableException;
import fr.miage.kadi.catalogue.application.exception.ReferenceListeIntrouvableException;
import fr.miage.kadi.catalogue.application.port.out.LigneListeVue;
import fr.miage.kadi.catalogue.application.port.out.ListePersonnelleRepository;
import fr.miage.kadi.catalogue.application.port.out.ReferencesListeRepository;
import fr.miage.kadi.catalogue.application.service.GererListeCoursesService.Decision;
import fr.miage.kadi.catalogue.application.service.GererListeCoursesService.DemandeAjoutLigne;
import fr.miage.kadi.catalogue.application.service.GererListeCoursesService.ResultatAjoutLigne.Statut;
import fr.miage.kadi.catalogue.domain.model.IdentifiantCompte;
import fr.miage.kadi.catalogue.domain.model.LigneListe;
import fr.miage.kadi.catalogue.domain.model.ListeCourses;
import fr.miage.kadi.catalogue.domain.model.NatureBesoin;
import fr.miage.kadi.catalogue.domain.model.Quantite;
import fr.miage.kadi.catalogue.domain.model.TypeReference;
import fr.miage.kadi.catalogue.domain.model.UniteQuantite;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

/** Service de la liste testé sans Spring ni base, avec des adaptateurs en mémoire. */
class GererListeCoursesServiceTest {
    private static final IdentifiantCompte INES = new IdentifiantCompte(1);
    private static final IdentifiantCompte ADAM = new IdentifiantCompte(2);
    private static final long FARINE_FRANCINE = 10;
    private static final long POMMES = 20;
    private static final long LAIT = 30;

    private final ListesEnMemoire listes = new ListesEnMemoire();
    private final ReferencesEnMemoire references = new ReferencesEnMemoire();
    private int compteur;
    private final GererListeCoursesService service = new GererListeCoursesService(listes, references,
            () -> new UUID(0, ++compteur));

    @Test
    void ajouteUnProduitPrecisALArticle() {
        var resultat = service.ajouter(INES, demande(TypeReference.PRODUIT, FARINE_FRANCINE, "2", UniteQuantite.ARTICLE));
        assertThat(resultat.statut()).isEqualTo(Statut.AJOUTEE);
        assertThat(listes.de(INES).lignes()).hasSize(1);
    }

    @Test
    void refuseUnProduitPrecisEnPoidsOuAvecUnFormatSouhaite() {
        assertThatThrownBy(() -> service.ajouter(INES,
                demande(TypeReference.PRODUIT, FARINE_FRANCINE, "500", UniteQuantite.GRAMME)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("unité de vente");
        assertThatThrownBy(() -> service.ajouter(INES, new DemandeAjoutLigne(TypeReference.PRODUIT, FARINE_FRANCINE,
                null, quantite("1", UniteQuantite.ARTICLE), "2 kg", null, null)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(listes.de(INES).lignes()).isEmpty();
    }

    @Test
    void verifieLesUnitesSelonLaNatureDuBesoin() {
        service.ajouter(INES, demande(TypeReference.BESOIN_GENERIQUE, POMMES, "1.5", UniteQuantite.KILOGRAMME));
        assertThatThrownBy(() -> service.ajouter(INES,
                demande(TypeReference.BESOIN_GENERIQUE, POMMES, "1", UniteQuantite.LITRE)))
                .isInstanceOf(IllegalArgumentException.class);
        service.ajouter(INES, demande(TypeReference.BESOIN_GENERIQUE, LAIT, "33", UniteQuantite.CENTILITRE));
        assertThat(listes.de(INES).lignes()).hasSize(2);
    }

    @Test
    void refuseUnTypeIncoherentAvecLeCatalogueOuUneReferenceInconnue() {
        assertThatThrownBy(() -> service.ajouter(INES, demande(TypeReference.PRODUIT, POMMES, "1", UniteQuantite.ARTICLE)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.ajouter(INES,
                demande(TypeReference.BESOIN_GENERIQUE, FARINE_FRANCINE, "1", UniteQuantite.ARTICLE)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.ajouter(INES, demande(TypeReference.PRODUIT, 404L, "1", UniteQuantite.ARTICLE)))
                .isInstanceOf(ReferenceListeIntrouvableException.class);
        assertThatThrownBy(() -> service.ajouter(INES, demande(TypeReference.PRODUIT, null, "1", UniteQuantite.ARTICLE)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void creeUnBesoinPersonnelUneSeuleFoisEtLeReserveASonAuteur() {
        var demande = new DemandeAjoutLigne(TypeReference.BESOIN_PERSONNALISE, null, "  Sauce maison  ",
                quantite("1", UniteQuantite.ARTICLE), null, null, null);
        service.ajouter(INES, demande);
        service.ajouter(INES, demande);
        assertThat(references.besoins).hasSize(1);
        assertThat(references.besoins.values().iterator().next().libelle()).isEqualTo("Sauce maison");
        assertThat(listes.de(INES).lignes()).hasSize(1);
        assertThat(listes.de(INES).lignes().get(0).contenu().quantite().value()).isEqualByComparingTo("2");

        long besoin = references.besoins.keySet().iterator().next();
        assertThatThrownBy(() -> service.ajouter(ADAM, new DemandeAjoutLigne(TypeReference.BESOIN_PERSONNALISE, besoin,
                null, quantite("1", UniteQuantite.ARTICLE), null, null, null)))
                .isInstanceOf(ReferenceListeIntrouvableException.class);
        assertThatThrownBy(() -> service.ajouter(INES, new DemandeAjoutLigne(TypeReference.BESOIN_PERSONNALISE, null,
                "   ", quantite("1", UniteQuantite.ARTICLE), null, null, null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void demandeConfirmationSansModifierLaListePuisRespecteLaDecision() {
        UUID premiere = service.ajouter(INES, avecFormat("Sachet de 1 kg", null, null)).ligneId();

        var confirmation = service.ajouter(INES, avecFormat("sachet de 1 KG", null, null));
        assertThat(confirmation.statut()).isEqualTo(Statut.CONFIRMATION_REQUISE);
        assertThat(confirmation.lignesCandidates()).containsExactly(premiere);
        assertThat(listes.de(INES).lignes()).hasSize(1);
        assertThat(listes.enregistrements).isEqualTo(1);

        assertThat(service.ajouter(INES, avecFormat("sachet de 1 KG", Decision.SEPARER, null)).statut())
                .isEqualTo(Statut.AJOUTEE);
        assertThat(listes.de(INES).lignes()).hasSize(2);
    }

    @Test
    void fusionneApresConfirmationEnGardantLeFormatDeLaPremiereLigne() {
        UUID premiere = service.ajouter(INES, avecFormat("Sachet de 1 kg", null, null)).ligneId();
        var fusion = service.ajouter(INES, avecFormat("sachet de 1 KG", Decision.FUSIONNER, premiere));
        assertThat(fusion.statut()).isEqualTo(Statut.FUSIONNEE);
        LigneListe ligne = listes.de(INES).lignes().get(0);
        assertThat(ligne.contenu().formatSouhaite()).isEqualTo("Sachet de 1 kg");
        assertThat(ligne.contenu().quantite().value()).isEqualByComparingTo("2");

        assertThatThrownBy(() -> service.ajouter(INES, avecFormat("sachet de 500 g", Decision.FUSIONNER, premiere)))
                .isInstanceOf(FusionLigneImpossibleException.class);
        assertThatThrownBy(() -> service.ajouter(INES, avecFormat("Sachet de 1 kg", Decision.FUSIONNER, new UUID(9, 9))))
                .isInstanceOf(LigneListeIntrouvableException.class);
    }

    @Test
    void modifieEtSupprimeUniquementDansLaListeDuProprietaire() {
        UUID ligne = service.ajouter(INES, demande(TypeReference.BESOIN_GENERIQUE, POMMES, "1", UniteQuantite.KILOGRAMME))
                .ligneId();
        assertThatThrownBy(() -> service.modifierQuantite(ADAM, ligne, new BigDecimal("3")))
                .isInstanceOf(LigneListeIntrouvableException.class);
        assertThatThrownBy(() -> service.supprimer(ADAM, ligne)).isInstanceOf(LigneListeIntrouvableException.class);

        service.modifierQuantite(INES, ligne, new BigDecimal("2.5"));
        assertThat(listes.de(INES).lignes().get(0).contenu().quantite())
                .isEqualTo(quantite("2.5", UniteQuantite.KILOGRAMME));
        assertThatThrownBy(() -> service.modifierQuantite(INES, ligne, BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);

        service.supprimer(INES, ligne);
        assertThat(listes.de(INES).lignes()).isEmpty();
    }

    @Test
    void refuseUnTexteTropLong() {
        String long256 = "a".repeat(256);
        assertThatThrownBy(() -> service.ajouter(INES, new DemandeAjoutLigne(TypeReference.BESOIN_PERSONNALISE, null,
                long256, quantite("1", UniteQuantite.ARTICLE), null, null, null)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.ajouter(INES, avecFormat(long256, null, null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static DemandeAjoutLigne demande(TypeReference type, Long id, String valeur, UniteQuantite unite) {
        return new DemandeAjoutLigne(type, id, null, quantite(valeur, unite), null, null, null);
    }

    private static DemandeAjoutLigne avecFormat(String format, Decision decision, UUID ligne) {
        return new DemandeAjoutLigne(TypeReference.BESOIN_GENERIQUE, POMMES, null, quantite("1", UniteQuantite.ARTICLE),
                format, decision, ligne);
    }

    private static Quantite quantite(String valeur, UniteQuantite unite) {
        return new Quantite(new BigDecimal(valeur), unite);
    }

    /** Une liste par compte, comme le dépôt PostgreSQL ; une opération qui échoue ne modifie rien. */
    private static final class ListesEnMemoire implements ListePersonnelleRepository {
        private final Map<IdentifiantCompte, ListeCourses> listes = new HashMap<>();
        private int enregistrements;

        ListeCourses de(IdentifiantCompte compte) {
            return listes.getOrDefault(compte, new ListeCourses(compte, List.of()));
        }

        @Override
        public List<LigneListeVue> consulter(IdentifiantCompte proprietaire) {
            return List.of();
        }

        @Override
        public <T> T modifier(IdentifiantCompte proprietaire, Function<ListeCourses, Modification<T>> operation) {
            Modification<T> modification = operation.apply(de(proprietaire));
            if (modification.liste() != null) {
                listes.put(proprietaire, modification.liste());
                enregistrements++;
            }
            return modification.resultat();
        }
    }

    private static final class ReferencesEnMemoire implements ReferencesListeRepository {
        private final Map<Long, ReferenceCatalogue> catalogue = Map.of(
                FARINE_FRANCINE, new ReferenceCatalogue(FARINE_FRANCINE, false, null),
                POMMES, new ReferenceCatalogue(POMMES, true, NatureBesoin.SOLIDE),
                LAIT, new ReferenceCatalogue(LAIT, true, NatureBesoin.LIQUIDE));
        private final Map<Long, Besoin> besoins = new HashMap<>();

        @Override
        public Optional<ReferenceCatalogue> trouverDansCatalogue(long produitId) {
            return Optional.ofNullable(catalogue.get(produitId));
        }

        @Override
        public boolean besoinPersonnelAppartientA(IdentifiantCompte proprietaire, long besoinId) {
            Besoin besoin = besoins.get(besoinId);
            return besoin != null && besoin.auteur().equals(proprietaire);
        }

        @Override
        public long enregistrerBesoinPersonnel(IdentifiantCompte proprietaire, String libelle) {
            List<Map.Entry<Long, Besoin>> existants = new ArrayList<>(besoins.entrySet());
            for (var existant : existants) {
                if (existant.getValue().auteur().equals(proprietaire)
                        && existant.getValue().libelle().equalsIgnoreCase(libelle)) {
                    return existant.getKey();
                }
            }
            long id = 100L + besoins.size();
            besoins.put(id, new Besoin(proprietaire, libelle));
            return id;
        }

        private record Besoin(IdentifiantCompte auteur, String libelle) {
        }
    }
}

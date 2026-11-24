package fr.miage.kadi.catalogue.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class ResultatAjoutTest {

    private static final IdentifiantCompte COMPTE_TEST = new IdentifiantCompte(101);

    private ListeCourses liste(List<LigneListe> lignes) {
        return new ListeCourses(COMPTE_TEST, lignes);
    }

    private LigneListe ligne(long referenceId) {
        return new LigneListe(
                UUID.randomUUID(),
                new ContenuLigne(
                        new ReferenceArticle(TypeReference.BESOIN_GENERIQUE, referenceId),
                        new Quantite(BigDecimal.ONE, UniteQuantite.ARTICLE),
                        null));
    }

    @Test
    void lesVariantesDeResultatConserventLeursDonnees() {
        ListeCourses liste = liste(List.of(ligne(1)));
        LigneListe aAjouter = ligne(2);
        LigneListe candidate = ligne(3);

        assertThat(new ResultatAjout.AjoutSimple(liste).liste()).isSameAs(liste);
        assertThat(new ResultatAjout.FusionAutomatique(liste, candidate.id()).idLigneConservee())
                .isEqualTo(candidate.id());
        assertThat(new ResultatAjout.ConfirmationRequise(aAjouter, List.of(candidate)))
                .satisfies(resultat -> {
                    assertThat(resultat.ligneAAjouter()).isEqualTo(aAjouter);
                    assertThat(resultat.lignesCandidates()).containsExactly(candidate);
                });
        assertThat(new ResultatAjout.ConflitFusionAutomatique(List.of(candidate, aAjouter))
                .lignesCandidates()).containsExactly(candidate, aAjouter);
    }

    @Test
    void copieEtProtegeLaListeDesCandidates() {
        LigneListe aAjouter = ligne(1);
        LigneListe candidate = ligne(2);
        List<LigneListe> candidates = new ArrayList<>(List.of(candidate));

        ResultatAjout.ConfirmationRequise resultat =
                new ResultatAjout.ConfirmationRequise(aAjouter, candidates);
        candidates.clear();

        assertThat(resultat.lignesCandidates()).containsExactly(candidate);
        assertThatThrownBy(() -> resultat.lignesCandidates().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void refuseLesDonneesInvalidesPourChaqueVariante() {
        LigneListe premiere = ligne(1);
        LigneListe seconde = ligne(2);

        assertThatThrownBy(() -> new ResultatAjout.AjoutSimple(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ResultatAjout.FusionAutomatique(null, premiere.id()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ResultatAjout.FusionAutomatique(
                liste(List.of()), null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ResultatAjout.ConfirmationRequise(null, List.of(premiere)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ResultatAjout.ConfirmationRequise(premiere, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ResultatAjout.ConfirmationRequise(
                premiere, java.util.Arrays.asList((LigneListe) null)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ResultatAjout.ConflitFusionAutomatique(List.of(premiere)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ResultatAjout.ConflitFusionAutomatique(
                java.util.Arrays.asList(premiere, null)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(new ResultatAjout.ConflitFusionAutomatique(List.of(premiere, seconde)))
                .isNotNull();
    }
}

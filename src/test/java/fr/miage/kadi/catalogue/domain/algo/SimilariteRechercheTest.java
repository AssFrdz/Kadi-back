package fr.miage.kadi.catalogue.domain.algo;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class SimilariteRechercheTest {
    @Test
    void normaliseLesAccentsEspacesEtConserveLesQuantites() {
        assertThat(SimilariteRecherche.normaliser(" PÂTES,  6 × 33 cl "))
                .isEqualTo("pates 6 33 cl");
    }

    @Test
    void classeLaSaisiePartielleEtTolereCertainesFautes() {
        assertThat(SimilariteRecherche.score("nut", "Nutella")).isEqualTo(1);
        assertThat(SimilariteRecherche.score("yaort", "Yaourt nature")).isGreaterThanOrEqualTo(0.3);
        assertThat(SimilariteRecherche.score("zzzzzz", "Yaourt nature")).isLessThan(0.3);
    }
}

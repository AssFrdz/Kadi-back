package fr.miage.kadi.catalogue.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class IdentifiantCompteTest {

    @Test
    void accepteEtConserveUnIdentifiantPositif() {
        IdentifiantCompte id = new IdentifiantCompte(42);

        assertThat(id.valeur()).isEqualTo(42);
    }

    @Test
    void refuseUnIdentifiantNulOuNegatif() {
        assertThatThrownBy(() -> new IdentifiantCompte(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("L'identifiant du compte doit être strictement positif.");
        assertThatThrownBy(() -> new IdentifiantCompte(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("L'identifiant du compte doit être strictement positif.");
    }
}

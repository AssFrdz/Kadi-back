package fr.miage.kadi.catalogue.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.miage.kadi.catalogue.application.exception.PropositionDejaModereeException;
import fr.miage.kadi.catalogue.application.port.out.PropositionsBesoinRepository;
import fr.miage.kadi.catalogue.domain.model.NatureBesoin;
import fr.miage.kadi.catalogue.domain.model.PropositionBesoin;
import fr.miage.kadi.catalogue.domain.model.StatutProposition;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Règles de modération, avec un faux adaptateur en mémoire. */
class ModererBesoinsServiceTest {

    private final FauxDepot depot = new FauxDepot();
    private final ModererBesoinsService service = new ModererBesoinsService(depot);

    @Test
    @DisplayName("REQ-039, REQ-042 : accepter avec un libellé corrigé publie ce libellé comme besoin générique")
    void publieLeLibelleCorrige() {
        depot.proposer(1, "  sauce  tomatte maison ");

        var validation = service.valider(1, "Sauce tomate maison", NatureBesoin.LIQUIDE, null);

        assertThat(validation.rattache()).isFalse();
        assertThat(depot.generiques).containsEntry(validation.besoinGeneriqueId(), "Sauce tomate maison");
        assertThat(depot.statut(1)).isEqualTo(StatutProposition.VALIDE);
    }

    @Test
    @DisplayName("REQ-043 : un besoin de même libellé existe déjà : la proposition y est rattachée, sans doublon")
    void rattacheAUnBesoinExistantSansDoublon() {
        depot.generiques.put(7L, "Pommes");
        depot.proposer(1, "pommes");

        var validation = service.valider(1, null, null, null);

        assertThat(validation.rattache()).isTrue();
        assertThat(validation.besoinGeneriqueId()).isEqualTo(7L);
        assertThat(depot.generiques).hasSize(1);
    }

    @Test
    @DisplayName("Créer un besoin exige sa nature, qui fixe les unités proposées")
    void exigeLaNaturePourCreerUnBesoin() {
        depot.proposer(1, "Kombucha");

        assertThatThrownBy(() -> service.valider(1, null, null, null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("nature");
        assertThat(depot.statut(1)).isEqualTo(StatutProposition.EN_ATTENTE);
    }

    @Test
    @DisplayName("REQ-041 : refuser laisse le besoin privé ; une proposition déjà traitée ne peut plus changer")
    void refuseUneSeuleFois() {
        depot.proposer(1, "Truc introuvable");

        service.refuser(1);

        assertThat(depot.statut(1)).isEqualTo(StatutProposition.REFUSE);
        assertThat(depot.generiques).isEmpty();
        assertThatThrownBy(() -> service.valider(1, null, NatureBesoin.SOLIDE, null))
                .isInstanceOf(PropositionDejaModereeException.class);
    }

    private static final class FauxDepot implements PropositionsBesoinRepository {
        private final Map<Long, PropositionBesoin> propositions = new HashMap<>();
        private final Map<Long, String> generiques = new HashMap<>();
        private long prochainId = 100;

        void proposer(long id, String libelle) {
            propositions.put(id, new PropositionBesoin(id, libelle, "Karim", StatutProposition.EN_ATTENTE, null, null));
        }

        StatutProposition statut(long id) {
            return propositions.get(id).statut();
        }

        @Override
        public List<PropositionBesoin> lister(StatutProposition statut) {
            return new ArrayList<>(propositions.values().stream().filter(p -> p.statut() == statut).toList());
        }

        @Override
        public Optional<PropositionBesoin> trouver(long id) {
            return Optional.ofNullable(propositions.get(id));
        }

        @Override
        public List<BesoinGenerique> listerBesoinsGeneriques() {
            return generiques.entrySet().stream()
                    .map(e -> new BesoinGenerique(e.getKey(), e.getValue(), NatureBesoin.SOLIDE)).toList();
        }

        @Override
        public Optional<BesoinGenerique> trouverBesoinGenerique(String libelle) {
            return listerBesoinsGeneriques().stream().filter(b -> b.libelle().equalsIgnoreCase(libelle)).findFirst();
        }

        @Override
        public Optional<BesoinGenerique> trouverBesoinGenerique(long id) {
            return listerBesoinsGeneriques().stream().filter(b -> b.id() == id).findFirst();
        }

        @Override
        public Optional<Long> validerEnCreant(long propositionId, String libelle, NatureBesoin nature) {
            long id = prochainId++;
            generiques.put(id, libelle);
            changer(propositionId, StatutProposition.VALIDE, libelle);
            return Optional.of(id);
        }

        @Override
        public boolean validerEnRattachant(long propositionId, long besoinGeneriqueId) {
            changer(propositionId, StatutProposition.VALIDE, generiques.get(besoinGeneriqueId));
            return true;
        }

        @Override
        public boolean refuser(long propositionId) {
            changer(propositionId, StatutProposition.REFUSE, null);
            return true;
        }

        private void changer(long id, StatutProposition statut, String libelleCommun) {
            PropositionBesoin p = propositions.get(id);
            propositions.put(id, new PropositionBesoin(id, p.libelle(), p.auteur(), statut, p.creeLe(), libelleCommun));
        }
    }
}

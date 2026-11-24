package fr.miage.kadi.catalogue.application.service;

import fr.miage.kadi.catalogue.application.exception.PropositionDejaModereeException;
import fr.miage.kadi.catalogue.application.exception.PropositionIntrouvableException;
import fr.miage.kadi.catalogue.application.exception.ReferenceListeIntrouvableException;
import fr.miage.kadi.catalogue.application.port.out.PropositionsBesoinRepository;
import fr.miage.kadi.catalogue.application.port.out.PropositionsBesoinRepository.BesoinGenerique;
import fr.miage.kadi.catalogue.domain.model.NatureBesoin;
import fr.miage.kadi.catalogue.domain.model.PropositionBesoin;
import fr.miage.kadi.catalogue.domain.model.StatutProposition;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Modération des besoins personnels (REQ-039 à REQ-043, REQ-047). Valider publie un besoin générique commun,
 * ou rattache la proposition à un besoin existant pour éviter un doublon ; refuser la laisse privée.
 * Dans les deux cas, la ligne de l'auteur garde sa saisie d'origine. Le contrôle du rôle administrateur
 * est fait par l'adaptateur web.
 */
public class ModererBesoinsService {

    private final PropositionsBesoinRepository propositions;

    public ModererBesoinsService(PropositionsBesoinRepository propositions) {
        this.propositions = Objects.requireNonNull(propositions);
    }

    public List<PropositionBesoin> lister(StatutProposition statut) {
        return propositions.lister(Objects.requireNonNull(statut, "Le statut est obligatoire."));
    }

    public List<BesoinGenerique> besoinsGeneriques() {
        return propositions.listerBesoinsGeneriques();
    }

    /**
     * @param correction       libellé corrigé, ou vide pour garder la saisie d'origine
     * @param nature           nature du nouveau besoin ; ignorée en cas de rattachement
     * @param besoinExistantId besoin générique auquel rattacher la proposition, ou {@code null}
     */
    public Validation valider(long id, String correction, NatureBesoin nature, Long besoinExistantId) {
        PropositionBesoin proposition = enAttente(id);
        String libelle = proposition.libellePublie(correction);
        Optional<BesoinGenerique> cible = besoinExistantId != null
                ? Optional.of(propositions.trouverBesoinGenerique(besoinExistantId)
                        .orElseThrow(() -> new ReferenceListeIntrouvableException(
                                "Le besoin générique " + besoinExistantId + " n'existe pas.")))
                : propositions.trouverBesoinGenerique(libelle);

        if (cible.isPresent()) {
            // Rattachement choisi, ou même libellé déjà présent : aucune entrée commune en double (REQ-043).
            confirmer(id, propositions.validerEnRattachant(id, cible.get().id()));
            return new Validation(cible.get().id(), cible.get().libelle(), true);
        }
        if (nature == null) {
            throw new IllegalArgumentException(
                    "Choisis la nature du besoin (SOLIDE, LIQUIDE ou UNITAIRE) pour le publier dans le catalogue.");
        }
        long cree = propositions.validerEnCreant(id, libelle, nature)
                .orElseThrow(() -> new PropositionDejaModereeException(id));
        return new Validation(cree, libelle, false);
    }

    public void refuser(long id) {
        enAttente(id);
        confirmer(id, propositions.refuser(id));
    }

    private PropositionBesoin enAttente(long id) {
        PropositionBesoin proposition = propositions.trouver(id)
                .orElseThrow(() -> new PropositionIntrouvableException(id));
        if (!proposition.estEnAttente()) {
            throw new PropositionDejaModereeException(id);
        }
        return proposition;
    }

    private static void confirmer(long id, boolean effectue) {
        if (!effectue) {
            // Traitée entre-temps par un autre administrateur.
            throw new PropositionDejaModereeException(id);
        }
    }

    public record Validation(long besoinGeneriqueId, String libelle, boolean rattache) {
    }
}

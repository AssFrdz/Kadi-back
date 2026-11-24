package fr.miage.kadi.catalogue.adapter.in.web;

import fr.miage.kadi.catalogue.application.exception.PropositionDejaModereeException;
import fr.miage.kadi.catalogue.application.exception.PropositionIntrouvableException;
import fr.miage.kadi.catalogue.application.exception.ReferenceListeIntrouvableException;
import fr.miage.kadi.catalogue.application.port.out.PropositionsBesoinRepository.BesoinGenerique;
import fr.miage.kadi.catalogue.application.service.ModererBesoinsService;
import fr.miage.kadi.catalogue.application.service.ModererBesoinsService.Validation;
import fr.miage.kadi.catalogue.domain.model.IdentifiantCompte;
import fr.miage.kadi.catalogue.domain.model.NatureBesoin;
import fr.miage.kadi.catalogue.domain.model.PropositionBesoin;
import fr.miage.kadi.catalogue.domain.model.StatutProposition;
import fr.miage.kadi.compte.application.service.UtilisateursSimulesService;
import fr.miage.kadi.compte.domain.model.UtilisateurSimule;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Modération des besoins personnels proposés au catalogue commun, réservée au profil administrateur
 * (403 pour un consommateur). Accepter publie un besoin générique ou le rattache à un besoin existant ;
 * refuser laisse le besoin privé. La liste de l'auteur garde toujours sa saisie d'origine.
 */
@RestController
@RequestMapping("/api/v1/admin/besoins")
@Tag(name = "Modération", description = "F1 : l'administrateur accepte ou refuse les besoins personnels proposés")
class ModerationBesoinsController {

    private final ModererBesoinsService moderation;
    private final UtilisateursSimulesService utilisateurs;

    ModerationBesoinsController(ModererBesoinsService moderation, UtilisateursSimulesService utilisateurs) {
        this.moderation = moderation;
        this.utilisateurs = utilisateurs;
    }

    @GetMapping
    @Operation(summary = "Lister les propositions", description = "statut : EN_ATTENTE (par défaut), VALIDE ou REFUSE.")
    List<PropositionResponse> lister(@UtilisateurCourant IdentifiantCompte utilisateur,
            @RequestParam(defaultValue = "EN_ATTENTE") String statut) {
        exigerAdministrateur(utilisateur);
        return moderation.lister(statutDe(statut)).stream().map(PropositionResponse::depuis).toList();
    }

    @GetMapping("/generiques")
    @Operation(summary = "Besoins génériques du catalogue", description = "Cibles possibles d'un rattachement.")
    List<BesoinGeneriqueResponse> besoinsGeneriques(@UtilisateurCourant IdentifiantCompte utilisateur) {
        exigerAdministrateur(utilisateur);
        return moderation.besoinsGeneriques().stream().map(BesoinGeneriqueResponse::depuis).toList();
    }

    @PostMapping("/{id}/validation")
    @Operation(summary = "Accepter une proposition",
            description = "libelle : correction facultative. nature (SOLIDE, LIQUIDE, UNITAIRE) : obligatoire pour "
                    + "créer un nouveau besoin. besoinExistantId : rattache la proposition à un besoin générique "
                    + "existant. Un besoin de même libellé est réutilisé automatiquement. 409 si déjà traitée.")
    ValidationResponse valider(@UtilisateurCourant IdentifiantCompte utilisateur, @PathVariable long id,
            @RequestBody(required = false) ValidationRequest requete) {
        exigerAdministrateur(utilisateur);
        ValidationRequest corps = requete == null ? new ValidationRequest(null, null, null) : requete;
        Validation validation = moderation.valider(id, corps.libelle(), natureDe(corps.nature()),
                corps.besoinExistantId());
        return new ValidationResponse(validation.besoinGeneriqueId(), validation.libelle(), validation.rattache());
    }

    @PostMapping("/{id}/refus")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Refuser une proposition", description = "Le besoin reste privé et dans la liste de son auteur.")
    void refuser(@UtilisateurCourant IdentifiantCompte utilisateur, @PathVariable long id) {
        exigerAdministrateur(utilisateur);
        moderation.refuser(id);
    }

    private void exigerAdministrateur(IdentifiantCompte utilisateur) {
        boolean administrateur = utilisateurs.trouver(utilisateur.valeur())
                .map(UtilisateurSimule::estAdministrateur).orElse(false);
        if (!administrateur) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "La modération est réservée à l'administrateur : choisis « Admin Kadi » dans « Utilisateur actuel ».");
        }
    }

    private static StatutProposition statutDe(String texte) {
        try {
            return StatutProposition.valueOf(texte.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException erreur) {
            throw new IllegalArgumentException("Le statut « " + texte + " » n'est pas reconnu : EN_ATTENTE, VALIDE ou REFUSE.");
        }
    }

    private static NatureBesoin natureDe(String texte) {
        if (texte == null || texte.isBlank()) {
            return null;
        }
        try {
            return NatureBesoin.valueOf(texte.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException erreur) {
            throw new IllegalArgumentException("La nature « " + texte + " » n'est pas reconnue : SOLIDE, LIQUIDE ou UNITAIRE.");
        }
    }

    @ExceptionHandler({IllegalArgumentException.class, HttpMessageNotReadableException.class})
    ProblemDetail requeteInvalide(Exception erreur) {
        String detail = erreur instanceof HttpMessageNotReadableException
                ? "Le corps de la requête n'est pas un JSON valide." : erreur.getMessage();
        return probleme(HttpStatus.BAD_REQUEST, "Demande invalide", detail);
    }

    @ExceptionHandler({PropositionIntrouvableException.class, ReferenceListeIntrouvableException.class})
    ProblemDetail introuvable(RuntimeException erreur) {
        return probleme(HttpStatus.NOT_FOUND, "Introuvable", erreur.getMessage());
    }

    @ExceptionHandler(PropositionDejaModereeException.class)
    ProblemDetail dejaTraitee(PropositionDejaModereeException erreur) {
        return probleme(HttpStatus.CONFLICT, "Déjà traitée", erreur.getMessage());
    }

    private static ProblemDetail probleme(HttpStatus statut, String titre, String detail) {
        ProblemDetail probleme = ProblemDetail.forStatusAndDetail(statut, detail);
        probleme.setTitle(titre);
        return probleme;
    }

    record PropositionResponse(long id, String libelle, String auteur, StatutProposition statut, Instant creeLe,
            String libelleCommun) {
        static PropositionResponse depuis(PropositionBesoin proposition) {
            return new PropositionResponse(proposition.id(), proposition.libelle(), proposition.auteur(),
                    proposition.statut(), proposition.creeLe(), proposition.libelleCommun());
        }
    }

    record BesoinGeneriqueResponse(long id, String libelle, NatureBesoin nature) {
        static BesoinGeneriqueResponse depuis(BesoinGenerique besoin) {
            return new BesoinGeneriqueResponse(besoin.id(), besoin.libelle(), besoin.nature());
        }
    }

    record ValidationRequest(String libelle, String nature, Long besoinExistantId) {
    }

    record ValidationResponse(long besoinGeneriqueId, String libelle, boolean rattache) {
    }
}

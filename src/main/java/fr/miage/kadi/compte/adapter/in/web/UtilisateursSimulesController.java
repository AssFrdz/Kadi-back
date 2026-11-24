package fr.miage.kadi.compte.adapter.in.web;

import fr.miage.kadi.compte.application.service.UtilisateursSimulesService;
import fr.miage.kadi.compte.domain.model.RoleCompte;
import fr.miage.kadi.compte.domain.model.UtilisateurSimule;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/utilisateurs")
@Tag(name = "Utilisateurs simulés", description = "Profils proposés dans la liste déroulante « Utilisateur actuel »")
class UtilisateursSimulesController {

    private final UtilisateursSimulesService utilisateurs;

    UtilisateursSimulesController(UtilisateursSimulesService utilisateurs) {
        this.utilisateurs = utilisateurs;
    }

    @GetMapping
    @Operation(summary = "Lister les utilisateurs simulés",
            description = "Kadi n'a pas de connexion : le front choisit l'un de ces profils et transmet son id "
                    + "dans l'en-tête X-Utilisateur-Id des requêtes qui concernent la liste de l'utilisateur.")
    List<UtilisateurSimuleResponse> lister() {
        return utilisateurs.lister().stream().map(UtilisateurSimuleResponse::depuis).toList();
    }

    record UtilisateurSimuleResponse(long id, String nomAffiche, RoleCompte role) {
        static UtilisateurSimuleResponse depuis(UtilisateurSimule utilisateur) {
            return new UtilisateurSimuleResponse(utilisateur.id(), utilisateur.nomAffiche(), utilisateur.role());
        }
    }
}

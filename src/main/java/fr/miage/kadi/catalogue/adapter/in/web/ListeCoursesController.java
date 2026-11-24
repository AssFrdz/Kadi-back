package fr.miage.kadi.catalogue.adapter.in.web;

import fr.miage.kadi.catalogue.application.port.out.LigneListeVue;
import fr.miage.kadi.catalogue.application.service.GererListeCoursesService;
import fr.miage.kadi.catalogue.application.service.GererListeCoursesService.Decision;
import fr.miage.kadi.catalogue.application.service.GererListeCoursesService.DemandeAjoutLigne;
import fr.miage.kadi.catalogue.application.service.GererListeCoursesService.ResultatAjoutLigne;
import fr.miage.kadi.catalogue.domain.model.IdentifiantCompte;
import fr.miage.kadi.catalogue.domain.model.Quantite;
import fr.miage.kadi.catalogue.domain.model.TypeReference;
import fr.miage.kadi.catalogue.domain.model.UniteQuantite;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Liste de courses personnelle (une par utilisateur simulé, transmis dans l'en-tête X-Utilisateur-Id). Une ligne d'un autre
 * utilisateur répond 404, comme une ligne inexistante.
 */
@RestController
@RequestMapping("/api/v1/liste")
@Tag(name = "Ma liste", description = "F1 : construire sa liste de courses")
class ListeCoursesController {
    static final BigDecimal QUANTITE_MAXIMALE = new BigDecimal("100000");

    private final GererListeCoursesService liste;

    ListeCoursesController(GererListeCoursesService liste) {
        this.liste = liste;
    }

    @GetMapping
    @Operation(summary = "Consulter ma liste", description = "Lignes dans l'ordre d'ajout ; liste vide au premier accès.")
    ListeResponse consulter(@UtilisateurCourant IdentifiantCompte proprietaire) {
        return reponse(proprietaire);
    }

    @PostMapping("/lignes")
    @Operation(summary = "Ajouter une ligne",
            description = "type : PRODUIT, BESOIN_GENERIQUE (referenceId du catalogue) ou BESOIN_PERSONNALISE "
                    + "(referenceId d'un besoin existant, ou libelle pour le créer en attente de validation). "
                    + "unite : ARTICLE, GRAMME, KILOGRAMME, LITRE ou CENTILITRE, parmi les unites proposées par la "
                    + "recherche. Statut AJOUTEE (201), FUSIONNEE (200) ou CONFIRMATION_REQUISE (200, rien n'est "
                    + "modifié) ; renvoyer alors la même demande avec decision FUSIONNER (et ligneAFusionner) ou SEPARER.")
    ResponseEntity<AjoutLigneResponse> ajouter(@UtilisateurCourant IdentifiantCompte proprietaire, @RequestBody AjoutLigneRequest requete) {
        ResultatAjoutLigne resultat = liste.ajouter(proprietaire, new DemandeAjoutLigne(
                valeur(TypeReference.class, requete.type(), "Le type de référence"),
                requete.referenceId(), requete.libelle(),
                new Quantite(quantite(requete.quantite()), valeur(UniteQuantite.class, requete.unite(), "L'unité")),
                requete.formatSouhaite(),
                requete.decision() == null ? null : valeur(Decision.class, requete.decision(), "La décision"),
                requete.ligneAFusionner()));
        HttpStatus statut = resultat.statut() == ResultatAjoutLigne.Statut.AJOUTEE ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(statut).body(new AjoutLigneResponse(resultat.statut().name(), resultat.ligneId(),
                resultat.lignesCandidates(), reponse(proprietaire).lignes()));
    }

    @PatchMapping("/lignes/{ligneId}")
    @Operation(summary = "Modifier la quantité d'une ligne", description = "L'unité de la ligne est conservée.")
    ListeResponse modifier(@UtilisateurCourant IdentifiantCompte proprietaire, @PathVariable UUID ligneId,
            @RequestBody ModifierQuantiteRequest requete) {
        liste.modifierQuantite(proprietaire, ligneId, quantite(requete.quantite()));
        return reponse(proprietaire);
    }

    @DeleteMapping("/lignes/{ligneId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Supprimer une ligne")
    void supprimer(@UtilisateurCourant IdentifiantCompte proprietaire, @PathVariable UUID ligneId) {
        liste.supprimer(proprietaire, ligneId);
    }

    private ListeResponse reponse(IdentifiantCompte proprietaire) {
        return new ListeResponse(liste.consulter(proprietaire).stream().map(LigneResponse::depuis).toList());
    }

    private static BigDecimal quantite(BigDecimal valeur) {
        if (valeur == null) {
            throw new IllegalArgumentException("La quantité est obligatoire.");
        }
        if (valeur.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("La quantité doit être strictement positive.");
        }
        if (valeur.stripTrailingZeros().scale() > 3) {
            throw new IllegalArgumentException("La quantité accepte au plus 3 décimales.");
        }
        if (valeur.compareTo(QUANTITE_MAXIMALE) > 0) {
            throw new IllegalArgumentException("La quantité ne peut pas dépasser " + QUANTITE_MAXIMALE + ".");
        }
        return valeur;
    }

    private static <E extends Enum<E>> E valeur(Class<E> type, String texte, String nom) {
        String acceptees = Arrays.stream(type.getEnumConstants()).map(Enum::name).collect(Collectors.joining(", "));
        if (texte == null || texte.isBlank()) {
            throw new IllegalArgumentException(nom + " est obligatoire (" + acceptees + ").");
        }
        try {
            return Enum.valueOf(type, texte.strip());
        } catch (IllegalArgumentException erreur) {
            throw new IllegalArgumentException(
                    nom + " « " + texte + " » n'est pas reconnue. Valeurs acceptées : " + acceptees + ".", erreur);
        }
    }

    record AjoutLigneRequest(String type, Long referenceId, String libelle, BigDecimal quantite, String unite,
            String formatSouhaite, String decision, UUID ligneAFusionner) {
    }

    record ModifierQuantiteRequest(BigDecimal quantite) {
    }

    record ListeResponse(List<LigneResponse> lignes) {
    }

    record AjoutLigneResponse(String statut, UUID ligneId, List<UUID> lignesCandidates, List<LigneResponse> lignes) {
    }

    record LigneResponse(UUID id, TypeReference type, long referenceId, String libelle, String marque,
            String formatProduit, BigDecimal quantite, UniteQuantite unite, String formatSouhaite, String statutBesoin,
            List<UniteQuantite> unitesAutorisees) {
        static LigneResponse depuis(LigneListeVue vue) {
            return new LigneResponse(vue.id(), vue.type(), vue.referenceId(), vue.libelle(), vue.marque(),
                    vue.formatProduit(), new BigDecimal(vue.quantite().stripTrailingZeros().toPlainString()),
                    vue.unite(), vue.formatSouhaite(), vue.statutBesoin(), vue.unitesAutorisees());
        }
    }
}

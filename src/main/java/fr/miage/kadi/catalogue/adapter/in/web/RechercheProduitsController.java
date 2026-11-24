package fr.miage.kadi.catalogue.adapter.in.web;

import fr.miage.kadi.catalogue.application.service.RechercherProduitsCompatiblesService;
import fr.miage.kadi.catalogue.domain.model.IdentifiantCompte;
import fr.miage.kadi.catalogue.domain.model.ProduitTrouve;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Recherche de produits commune à F1 et F2 : contrat convenu avec le module prix.
 * Remplace le contrôleur provisoire de F2 sur la même adresse ; les deux ne doivent jamais coexister.
 */
@RestController
@RequestMapping("/api/v1/produits")
class RechercheProduitsController {

    static final int LONGUEUR_MAXIMALE = 255;
    static final String PORTEE_LISTE = "liste";

    private final RechercherProduitsCompatiblesService recherche;

    RechercheProduitsController(RechercherProduitsCompatiblesService recherche) {
        this.recherche = recherche;
    }

    @GetMapping
    @Operation(summary = "Rechercher des produits du catalogue local",
            description = "Au plus 20 résultats, toujours 200. Moins de 2 caractères : liste vide. 6 à 14 chiffres "
                    + "seuls : code-barres exact. Sinon : mots dans n'importe quel ordre, sans tenir compte de la "
                    + "casse, des accents ni de l'espace dans les quantités (1l = 1 L), complétés par des résultats "
                    + "approchés (fautes de frappe) classés par ressemblance. Chaque élément contient id, libelle, "
                    + "codeBarres (obligatoires pour F2) et marque, quantite, confiance (SUR ou A_CONFIRMER), "
                    + "type (PRODUIT, BESOIN_GENERIQUE, BESOIN_PERSONNALISE) et unites. Sans portee : produits "
                    + "précis uniquement. portee=liste : ajoute les besoins génériques et les besoins personnels "
                    + "de l'utilisateur simulé (en-tête X-Utilisateur-Id obligatoire).")
    List<ProduitRechercheResponse> rechercher(
            @Parameter(description = "Texte saisi ou code-barres") @RequestParam(defaultValue = "") String recherche,
            @Parameter(description = "Absente (produits précis) ou « liste »") @RequestParam(required = false) String portee,
            @UtilisateurCourant(obligatoire = false) IdentifiantCompte proprietaire) {
        if (recherche.length() > LONGUEUR_MAXIMALE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La recherche ne peut pas dépasser " + LONGUEUR_MAXIMALE + " caractères.");
        }
        List<ProduitTrouve> resultats;
        if (portee == null || portee.isBlank()) {
            resultats = this.recherche.rechercherAvecConfiance(recherche);
        } else if (PORTEE_LISTE.equals(portee)) {
            resultats = this.recherche.rechercherPourListe(recherche, exiger(proprietaire));
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La portée « " + portee + " » n'est pas reconnue : utiliser « liste » ou ne pas la préciser.");
        }
        return resultats.stream().map(ProduitRechercheResponse::depuis).toList();
    }

    private static IdentifiantCompte exiger(IdentifiantCompte proprietaire) {
        if (proprietaire == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "portee=liste exige l'en-tête "
                    + UtilisateurCourant.EN_TETE + " : choisir un profil dans la liste « Utilisateur actuel ».");
        }
        return proprietaire;
    }
}

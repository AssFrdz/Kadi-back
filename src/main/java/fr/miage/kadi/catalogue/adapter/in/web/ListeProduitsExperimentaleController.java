package fr.miage.kadi.catalogue.adapter.in.web;

import fr.miage.kadi.catalogue.application.service.AjouterProduitListeService;
import fr.miage.kadi.catalogue.application.service.ConsulterListeService;
import fr.miage.kadi.catalogue.application.service.ModifierLigneListeService;
import fr.miage.kadi.catalogue.application.exception.LigneListeIntrouvableException;
import fr.miage.kadi.catalogue.application.exception.ProduitNonAjoutableException;
import fr.miage.kadi.catalogue.domain.model.IdentifiantCompte;
import fr.miage.kadi.catalogue.domain.model.Quantite;
import fr.miage.kadi.catalogue.domain.model.UniteQuantite;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@Profile("liste-experimentale")
@RequestMapping("/api/v1/liste-experimentale")
class ListeProduitsExperimentaleController {
    private final ConsulterListeService consulter;
    private final AjouterProduitListeService ajouter;
    private final ModifierLigneListeService modifier;

    ListeProduitsExperimentaleController(ConsulterListeService consulter, AjouterProduitListeService ajouter,
            ModifierLigneListeService modifier) {
        this.consulter = consulter;
        this.ajouter = ajouter;
        this.modifier = modifier;
    }

    @GetMapping
    List<LigneProduitResponse> consulter(@UtilisateurCourant IdentifiantCompte proprietaire) {
        return consulter.consulter(proprietaire).lignes().stream()
                .map(ligne -> new LigneProduitResponse(ligne.id(), ligne.contenu().reference().id(),
                        ligne.contenu().quantite().value()))
                .toList();
    }

    @PostMapping("/produits")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void ajouter(@UtilisateurCourant IdentifiantCompte proprietaire, @RequestBody AjoutProduitRequest requete) {
        if (requete.produitId() == null || requete.quantite() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Produit et quantité sont obligatoires.");
        }
        try {
            ajouter.ajouter(proprietaire, requete.produitId(),
                    new Quantite(requete.quantite(), UniteQuantite.ARTICLE));
        } catch (IllegalArgumentException | ProduitNonAjoutableException erreur) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, erreur.getMessage(), erreur);
        }
    }

    @PatchMapping("/produits/{ligneId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void modifierQuantite(@UtilisateurCourant IdentifiantCompte proprietaire, @PathVariable UUID ligneId,
            @RequestBody ModifierQuantiteRequest requete) {
        if (requete.quantite() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La quantité est obligatoire.");
        }
        try {
            modifier.modifierQuantite(proprietaire, ligneId,
                    new Quantite(requete.quantite(), UniteQuantite.ARTICLE));
        } catch (LigneListeIntrouvableException erreur) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, erreur.getMessage(), erreur);
        } catch (IllegalArgumentException | ProduitNonAjoutableException erreur) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, erreur.getMessage(), erreur);
        }
    }

    @DeleteMapping("/produits/{ligneId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void supprimer(@UtilisateurCourant IdentifiantCompte proprietaire, @PathVariable UUID ligneId) {
        try {
            modifier.supprimer(proprietaire, ligneId);
        } catch (LigneListeIntrouvableException erreur) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, erreur.getMessage(), erreur);
        }
    }

    record AjoutProduitRequest(Long produitId, BigDecimal quantite) {
    }

    record ModifierQuantiteRequest(BigDecimal quantite) {
    }

    record LigneProduitResponse(UUID id, long produitId, BigDecimal quantite) {
    }
}

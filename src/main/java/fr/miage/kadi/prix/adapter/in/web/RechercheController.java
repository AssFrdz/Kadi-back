package fr.miage.kadi.prix.adapter.in.web;

import fr.miage.kadi.prix.application.port.in.RechercherPourLaSaisie;
import fr.miage.kadi.prix.application.port.in.RechercherPourLaSaisie.MagasinTrouve;
import fr.miage.kadi.prix.application.port.in.RechercherPourLaSaisie.ProduitTrouve;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Recherche PROVISOIRE de produits et de magasins pour les écrans de F2.
 * La recherche du catalogue (F1) la remplacera derrière ces mêmes adresses.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Recherche (provisoire)", description = "Trouver un produit ou un magasin par son nom, en attendant la recherche du catalogue (F1)")
class RechercheController {

    private final RechercherPourLaSaisie rechercher;

    RechercheController(RechercherPourLaSaisie rechercher) {
        this.rechercher = rechercher;
    }

    @GetMapping("/produits")
    @Operation(summary = "Produits dont le nom contient tous les mots tapés, ou dont le code-barres est celui tapé (20 au plus)")
    List<ProduitTrouve> produits(@RequestParam(defaultValue = "") String recherche) {
        return rechercher.produits(recherche);
    }

    @GetMapping("/magasins")
    @Operation(summary = "Magasins dont le nom ou l'enseigne contient tous les mots tapés (20 au plus)")
    List<MagasinTrouve> magasins(@RequestParam(defaultValue = "") String recherche) {
        return rechercher.magasins(recherche);
    }
}

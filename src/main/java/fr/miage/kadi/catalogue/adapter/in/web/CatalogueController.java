package fr.miage.kadi.catalogue.adapter.in.web;

import fr.miage.kadi.catalogue.application.service.ConsulterCatalogueService;
import fr.miage.kadi.catalogue.application.service.ConsulterCatalogueService.PageCatalogue;
import fr.miage.kadi.catalogue.application.service.RechercherCatalogueService;
import fr.miage.kadi.catalogue.application.service.RechercherCatalogueService.PageRecherche;
import fr.miage.kadi.catalogue.domain.model.ProduitCatalogue;
import fr.miage.kadi.catalogue.domain.model.FiltresCatalogue;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/catalogue/produits")
class CatalogueController {

    private final ConsulterCatalogueService catalogue;
    private final RechercherCatalogueService recherche;

    CatalogueController(ConsulterCatalogueService catalogue, RechercherCatalogueService recherche) {
        this.catalogue = catalogue;
        this.recherche = recherche;
    }

    @GetMapping
    PageCatalogue consulter(@RequestParam(defaultValue = "0") long apres,
            @RequestParam(required = false) String marque,
            @RequestParam(required = false) String format) {
        if (apres < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'identifiant de pagination doit être positif ou nul.");
        }
        try {
            return catalogue.consulter(apres, new FiltresCatalogue(marque, format));
        } catch (IllegalArgumentException erreur) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, erreur.getMessage(), erreur);
        }
    }

    @GetMapping("/suggestions")
    List<ProduitCatalogue> suggerer(@RequestParam String q) {
        if (q.length() > 255) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La recherche ne peut pas dépasser 255 caractères.");
        }
        return recherche.rechercher(q);
    }

    @GetMapping("/recherche")
    PageRecherche rechercher(@RequestParam String q,
            @RequestParam(required = false) String apres,
            @RequestParam(defaultValue = "20") int taille,
            @RequestParam(required = false) String marque,
            @RequestParam(required = false) String format) {
        try {
            return recherche.rechercherPage(q, apres, taille, new FiltresCatalogue(marque, format));
        } catch (IllegalArgumentException erreur) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, erreur.getMessage(), erreur);
        }
    }
}

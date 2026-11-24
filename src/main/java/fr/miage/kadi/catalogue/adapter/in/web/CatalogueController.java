package fr.miage.kadi.catalogue.adapter.in.web;

import fr.miage.kadi.catalogue.application.service.ConsulterCatalogueService;
import fr.miage.kadi.catalogue.application.service.ConsulterCatalogueService.PageCatalogue;
import fr.miage.kadi.catalogue.application.service.ConsulterCatalogueService.PageCatalogueParNom;
import fr.miage.kadi.catalogue.application.service.RechercherCatalogueService;
import fr.miage.kadi.catalogue.application.service.RechercherCatalogueService.PageRecherche;
import fr.miage.kadi.catalogue.application.service.RechercherCatalogueService.MarqueCatalogue;
import fr.miage.kadi.catalogue.domain.model.ProduitCatalogue;
import fr.miage.kadi.catalogue.domain.model.FiltresCatalogue;
import fr.miage.kadi.catalogue.domain.model.NutriScore;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
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
    private final boolean nutriScoreActif;

    CatalogueController(ConsulterCatalogueService catalogue, RechercherCatalogueService recherche,
            @Value("${app.catalogue.nutri-score-actif:false}") boolean nutriScoreActif) {
        this.catalogue = catalogue;
        this.recherche = recherche;
        this.nutriScoreActif = nutriScoreActif;
    }

    @GetMapping("/par-nom")
    PageCatalogueParNom consulterParNom(@RequestParam(required = false) String apres,
            @RequestParam(required = false) String marque,
            @RequestParam(required = false) String format,
            @RequestParam(required = false) List<String> nutriScore) {
        try {
            return catalogue.consulterParNom(apres, filtres(marque, format, nutriScore));
        } catch (IllegalArgumentException erreur) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, erreur.getMessage(), erreur);
        }
    }

    @GetMapping
    PageCatalogue consulter(@RequestParam(defaultValue = "0") long apres,
            @RequestParam(required = false) String marque,
            @RequestParam(required = false) String format,
            @RequestParam(required = false) List<String> nutriScore) {
        if (apres < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'identifiant de pagination doit être positif ou nul.");
        }
        try {
            return catalogue.consulter(apres, filtres(marque, format, nutriScore));
        } catch (IllegalArgumentException erreur) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, erreur.getMessage(), erreur);
        }
    }

    @GetMapping("/suggestions")
    List<ProduitCatalogue> suggerer(@RequestParam String q,
            @RequestParam(required = false) String marque,
            @RequestParam(required = false) String format,
            @RequestParam(required = false) List<String> nutriScore) {
        if (q.length() > 255) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La recherche ne peut pas dépasser 255 caractères.");
        }
        try {
            return recherche.rechercherPage(q, null, 7, filtres(marque, format, nutriScore)).produits();
        } catch (IllegalArgumentException erreur) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, erreur.getMessage(), erreur);
        }
    }

    @GetMapping("/marques")
    List<MarqueCatalogue> marques(@RequestParam(defaultValue = "") String q,
            @RequestParam(required = false) String format,
            @RequestParam(required = false) List<String> nutriScore) {
        try {
            return recherche.compterMarques(q, format, filtres(null, format, nutriScore).nutriScores());
        } catch (IllegalArgumentException erreur) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, erreur.getMessage(), erreur);
        }
    }

    @GetMapping("/recherche")
    PageRecherche rechercher(@RequestParam String q,
            @RequestParam(required = false) String apres,
            @RequestParam(defaultValue = "20") int taille,
            @RequestParam(required = false) String marque,
            @RequestParam(required = false) String format,
            @RequestParam(required = false) List<String> nutriScore) {
        try {
            return recherche.rechercherPage(q, apres, taille, filtres(marque, format, nutriScore));
        } catch (IllegalArgumentException erreur) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, erreur.getMessage(), erreur);
        }
    }

    private FiltresCatalogue filtres(String marque, String format, List<String> valeurs) {
        Set<NutriScore> scores = EnumSet.noneOf(NutriScore.class);
        if (valeurs != null) {
            if (valeurs.isEmpty()) {
                throw new IllegalArgumentException("La sélection Nutri-Score est invalide.");
            }
            for (String valeur : valeurs) {
                try {
                    scores.add(NutriScore.valueOf(valeur));
                } catch (IllegalArgumentException erreur) {
                    throw new IllegalArgumentException("Le Nutri-Score doit être A, B, C, D ou E.", erreur);
                }
            }
        }
        if (!scores.isEmpty() && !nutriScoreActif) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Le filtre Nutri-Score n'est pas disponible.");
        }
        return new FiltresCatalogue(marque, format, scores);
    }
}

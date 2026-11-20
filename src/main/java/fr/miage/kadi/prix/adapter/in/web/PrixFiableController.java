package fr.miage.kadi.prix.adapter.in.web;

import fr.miage.kadi.prix.adapter.in.web.PrixFiableDto.ControleReponse;
import fr.miage.kadi.prix.adapter.in.web.PrixFiableDto.PrixDeReferenceReponse;
import fr.miage.kadi.prix.adapter.in.web.PrixFiableDto.ReleveEnregistreReponse;
import fr.miage.kadi.prix.adapter.in.web.PrixFiableDto.ReleveReponse;
import fr.miage.kadi.prix.application.port.in.ConsulterPrixDeReference;
import fr.miage.kadi.prix.application.port.in.DemandeDeReleve;
import fr.miage.kadi.prix.application.port.in.EnregistrerReleve;
import fr.miage.kadi.prix.application.port.in.ResultatEnregistrement;
import fr.miage.kadi.prix.domain.model.Montant;
import fr.miage.kadi.prix.domain.model.TypeDePreuve;
import fr.miage.kadi.prix.domain.model.UniteDePrix;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Les deux actions de F2 exposées en REST : consulter un prix fiable, saisir un prix contrôlé. */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Prix fiable", description = "F2 : prix de référence d'un produit dans un magasin, et saisie contrôlée d'un relevé")
class PrixFiableController {

    private final ConsulterPrixDeReference consulter;
    private final EnregistrerReleve enregistrer;

    PrixFiableController(ConsulterPrixDeReference consulter, EnregistrerReleve enregistrer) {
        this.consulter = consulter;
        this.enregistrer = enregistrer;
    }

    @GetMapping("/produits/{produitId}/magasins/{magasinId}/prix-de-reference")
    @Operation(summary = "Prix de référence d'un produit dans un magasin",
            description = "Médiane pondérée des relevés des 180 derniers jours, avec sa confiance et le détail des relevés retenus et écartés.")
    @ApiResponse(responseCode = "200", description = "Prix de référence calculé")
    @ApiResponse(responseCode = "404", description = "Produit ou magasin inconnu, ou aucun relevé exploitable")
    ResponseEntity<?> prixDeReference(@PathVariable long produitId, @PathVariable long magasinId) {
        return consulter.consulter(produitId, magasinId)
                .<ResponseEntity<?>>map(reference -> ResponseEntity.ok(PrixDeReferenceReponse.de(reference)))
                .orElseGet(() -> {
                    ProblemDetail probleme = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND,
                            "Aucun relevé exploitable pour ce produit dans ce magasin ni dans son enseigne");
                    probleme.setTitle("Pas encore de prix connu");
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(probleme);
                });
    }

    @PostMapping("/releves")
    @Operation(summary = "Saisir un prix relevé",
            description = "Le prix est contrôlé face à la référence existante. Un prix inhabituel n'est enregistré "
                    + "qu'après confirmation : renvoyer la même saisie avec confirmerMalgreAlerte = true.")
    @ApiResponse(responseCode = "201", description = "Relevé enregistré, avec le résultat du contrôle")
    @ApiResponse(responseCode = "400", description = "Saisie invalide (prix négatif, date future, contributeur vide…)")
    @ApiResponse(responseCode = "404", description = "Produit ou magasin inconnu")
    @ApiResponse(responseCode = "422", description = "Prix inhabituel non confirmé : rien n'est enregistré")
    ResponseEntity<?> saisir(@Valid @RequestBody SaisieReleveDto saisie) {
        ResultatEnregistrement resultat = enregistrer.enregistrer(new DemandeDeReleve(
                saisie.produitId(),
                saisie.magasinId(),
                new Montant(saisie.prix()),
                saisie.unite() == null ? UniteDePrix.PIECE : saisie.unite(),
                saisie.preuve() == null ? TypeDePreuve.SAISIE : saisie.preuve(),
                saisie.contributeur(),
                Boolean.TRUE.equals(saisie.enPromotion()),
                saisie.releveLe(),
                Boolean.TRUE.equals(saisie.confirmerMalgreAlerte())));

        ControleReponse controle = ControleReponse.de(resultat.controle());
        if (!resultat.estEnregistre()) {
            ProblemDetail probleme = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT,
                    "Ce prix est très éloigné des prix relevés pour ce produit ici. Vérifiez l'étiquette, "
                            + "ou renvoyez la saisie avec confirmerMalgreAlerte = true.");
            probleme.setTitle("Prix inhabituel");
            probleme.setProperty("controle", controle);
            return ResponseEntity.unprocessableContent().body(probleme);
        }
        ReleveReponse releve = ReleveReponse.de(resultat.releve());
        return ResponseEntity.created(URI.create("/api/v1/releves/" + releve.id()))
                .body(new ReleveEnregistreReponse(releve, controle));
    }
}

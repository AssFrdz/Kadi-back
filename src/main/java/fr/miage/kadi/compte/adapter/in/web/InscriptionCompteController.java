package fr.miage.kadi.compte.adapter.in.web;

import fr.miage.kadi.compte.application.service.InscrireCompteService;
import fr.miage.kadi.compte.domain.model.AdresseEmail;
import fr.miage.kadi.compte.domain.model.CompteUtilisateur;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/inscription")
public class InscriptionCompteController {

    private final InscrireCompteService inscrireCompteService;

    public InscriptionCompteController(InscrireCompteService inscrireCompteService) {
        this.inscrireCompteService = inscrireCompteService;
    }

    @PostMapping
    @Transactional
    public ResponseEntity<InscriptionCompteResponse> inscrire(
            @Valid @RequestBody InscriptionCompteRequest requete) {
        CompteUtilisateur compte = inscrireCompteService.inscrire(
                new AdresseEmail(requete.email()),
                requete.motDePasse());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new InscriptionCompteResponse(
                        compte.adresseEmail().valeur(),
                        compte.emailVerifie()));
    }
}

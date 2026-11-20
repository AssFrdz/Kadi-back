package fr.miage.kadi.compte.adapter.in.web;

import fr.miage.kadi.compte.application.service.VerificationEmailService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/verification-email/confirmation")
public class VerificationEmailController {

    private final VerificationEmailService verificationEmailService;

    public VerificationEmailController(VerificationEmailService verificationEmailService) {
        this.verificationEmailService = verificationEmailService;
    }

    @PostMapping
    public ResponseEntity<Void> confirmer(@Valid @RequestBody ConfirmerVerificationEmailRequest requete) {
        verificationEmailService.confirmer(requete.token());
        return ResponseEntity.noContent().build();
    }
}

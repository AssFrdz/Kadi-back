package fr.miage.kadi.compte.adapter.in.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ConfirmerVerificationEmailRequest(
        @NotBlank(message = "Le jeton de vérification est obligatoire.")
        @Pattern(regexp = "[A-Za-z0-9_-]{43}", message = "Le jeton de vérification n'est pas valide.")
        String token) {
}

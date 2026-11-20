package fr.miage.kadi.compte.adapter.in.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConnexionCompteRequest(
        @NotBlank(message = "L'adresse e-mail est obligatoire.")
        @Email(message = "L'adresse e-mail n'est pas valide.")
        @Size(max = 320, message = "L'adresse e-mail ne peut pas dépasser 320 caractères.")
        String email,
        @NotBlank(message = "Le mot de passe est obligatoire.")
        String motDePasse) {
}

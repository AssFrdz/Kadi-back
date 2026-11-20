package fr.miage.kadi.compte.adapter.in.web;

import fr.miage.kadi.compte.adapter.in.security.CompteAuthentifie;
import fr.miage.kadi.compte.adapter.in.security.CompteUtilisateurDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthentificationController {

    private final AuthenticationManager authenticationManager;
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;
    private final SecurityContextRepository securityContextRepository;

    public AuthentificationController(
            AuthenticationManager authenticationManager,
            SessionAuthenticationStrategy sessionAuthenticationStrategy,
            SecurityContextRepository securityContextRepository) {
        this.authenticationManager = authenticationManager;
        this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
        this.securityContextRepository = securityContextRepository;
    }

    @PostMapping("/connexion")
    public ResponseEntity<CompteSessionResponse> connecter(
            @Valid @RequestBody ConnexionCompteRequest requete,
            HttpServletRequest request,
            HttpServletResponse response) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(
                        requete.email(),
                        requete.motDePasse()));
        if (!(authentication.getPrincipal() instanceof CompteUtilisateurDetails compte)
                || !compte.emailVerifie()) {
            throw new BadCredentialsException("Adresse e-mail ou mot de passe incorrect.");
        }

        CompteAuthentifie principal = new CompteAuthentifie(
                compte.id(),
                compte.getUsername(),
                compte.role());
        Authentication sessionAuthentication = UsernamePasswordAuthenticationToken.authenticated(
                principal,
                null,
                principal.authorities());

        request.getSession(true);
        sessionAuthenticationStrategy.onAuthentication(sessionAuthentication, request, response);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(sessionAuthentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);

        return ResponseEntity.ok(CompteSessionResponse.depuis(principal));
    }

    @GetMapping("/session")
    public ResponseEntity<CompteSessionResponse> session(Authentication authentication) {
        if (authentication.getPrincipal() instanceof CompteAuthentifie compte) {
            return ResponseEntity.ok(CompteSessionResponse.depuis(compte));
        }
        throw new IllegalStateException("La session authentifiée ne contient pas de compte Kadi.");
    }

    @PostMapping("/deconnexion")
    public ResponseEntity<Void> deconnecter(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }
}

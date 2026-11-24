package fr.miage.kadi.catalogue.adapter.in.web;

import fr.miage.kadi.catalogue.domain.model.IdentifiantCompte;
import fr.miage.kadi.compte.application.service.UtilisateursSimulesService;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.server.ResponseStatusException;

/**
 * Lit l'utilisateur simulé dans l'en-tête {@value UtilisateurCourant#EN_TETE} et vérifie qu'il existe.
 * En-tête absent, mal formé ou id inconnu : 400 au format problem+json.
 */
public class UtilisateurCourantResolver implements HandlerMethodArgumentResolver {

    private final UtilisateursSimulesService utilisateurs;

    public UtilisateurCourantResolver(UtilisateursSimulesService utilisateurs) {
        this.utilisateurs = utilisateurs;
    }

    @Override
    public boolean supportsParameter(MethodParameter parametre) {
        return parametre.hasParameterAnnotation(UtilisateurCourant.class)
                && IdentifiantCompte.class.equals(parametre.getParameterType());
    }

    @Override
    public IdentifiantCompte resolveArgument(MethodParameter parametre, ModelAndViewContainer conteneur,
            NativeWebRequest requete, WebDataBinderFactory fabrique) {
        String valeur = requete.getHeader(UtilisateurCourant.EN_TETE);
        if (valeur == null || valeur.isBlank()) {
            if (!parametre.getParameterAnnotation(UtilisateurCourant.class).obligatoire()) {
                return null;
            }
            throw requeteInvalide("Aucun utilisateur sélectionné : l'en-tête " + UtilisateurCourant.EN_TETE
                    + " est obligatoire. Choisir un profil dans la liste « Utilisateur actuel ».");
        }
        long id;
        try {
            id = Long.parseLong(valeur.strip());
        } catch (NumberFormatException erreur) {
            throw requeteInvalide("L'en-tête " + UtilisateurCourant.EN_TETE + " doit contenir un identifiant numérique.");
        }
        return utilisateurs.trouver(id)
                .map(utilisateur -> new IdentifiantCompte(utilisateur.id()))
                .orElseThrow(() -> requeteInvalide("L'utilisateur " + id + " n'existe pas."));
    }

    private static ResponseStatusException requeteInvalide(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}

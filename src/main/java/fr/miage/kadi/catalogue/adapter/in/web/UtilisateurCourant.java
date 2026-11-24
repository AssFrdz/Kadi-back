package fr.miage.kadi.catalogue.adapter.in.web;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Injecte l'{@code IdentifiantCompte} de l'utilisateur simulé choisi dans le front, lu dans l'en-tête
 * {@value #EN_TETE}. Kadi n'a pas de connexion : cet identifiant n'est pas une preuve d'identité.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface UtilisateurCourant {

    String EN_TETE = "X-Utilisateur-Id";

    /** {@code false} : le paramètre vaut {@code null} si l'en-tête est absent. */
    boolean obligatoire() default true;
}

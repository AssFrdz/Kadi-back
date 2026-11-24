package fr.miage.kadi.catalogue.config;

import fr.miage.kadi.catalogue.adapter.in.web.UtilisateurCourant;
import fr.miage.kadi.catalogue.adapter.in.web.UtilisateurCourantResolver;
import fr.miage.kadi.compte.application.service.UtilisateursSimulesService;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import java.util.Arrays;
import java.util.List;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Branche l'utilisateur simulé sur Spring MVC et le documente comme en-tête dans OpenAPI. */
@Configuration
class UtilisateurCourantConfiguration implements WebMvcConfigurer {

    private final UtilisateursSimulesService utilisateurs;

    UtilisateurCourantConfiguration(UtilisateursSimulesService utilisateurs) {
        this.utilisateurs = utilisateurs;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolveurs) {
        resolveurs.add(new UtilisateurCourantResolver(utilisateurs));
    }

    @Bean
    OperationCustomizer enTeteUtilisateurCourant() {
        return (operation, methode) -> {
            Arrays.stream(methode.getMethodParameters())
                    .map(parametre -> parametre.getParameterAnnotation(UtilisateurCourant.class))
                    .filter(annotation -> annotation != null)
                    .findFirst()
                    .ifPresent(annotation -> operation.addParametersItem(new HeaderParameter()
                            .name(UtilisateurCourant.EN_TETE)
                            .required(annotation.obligatoire())
                            .description("Id de l'utilisateur simulé (GET /api/v1/utilisateurs)")
                            .schema(new IntegerSchema().format("int64"))));
            return operation;
        };
    }
}

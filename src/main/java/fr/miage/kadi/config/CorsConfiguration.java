package fr.miage.kadi.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Le front est servi depuis une autre origine : il faut l'autoriser explicitement à appeler l'API, y compris avec
 * l'en-tête X-Utilisateur-Id qui porte l'utilisateur simulé. Aucun cookie n'est échangé.
 */
@Configuration
class CorsConfiguration implements WebMvcConfigurer {

    private final String[] originesAutorisees;

    CorsConfiguration(@Value("${app.cors.origines-autorisees}") String[] originesAutorisees) {
        this.originesAutorisees = originesAutorisees;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(originesAutorisees)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("Content-Type", "Accept", "X-Utilisateur-Id")
                .allowCredentials(false);
    }
}

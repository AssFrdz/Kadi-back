package fr.miage.kadi.compte.adapter.out.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompteUtilisateurJpaRepository extends JpaRepository<CompteUtilisateurEntity, Long> {

    Optional<CompteUtilisateurEntity> findByEmailIgnoreCase(String email);
}

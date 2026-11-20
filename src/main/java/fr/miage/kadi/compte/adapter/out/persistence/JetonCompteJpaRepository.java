package fr.miage.kadi.compte.adapter.out.persistence;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

interface JetonCompteJpaRepository extends JpaRepository<JetonCompteEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select jeton from JetonCompteEntity jeton
            where jeton.jetonHash = :empreinte
              and jeton.type = :type
              and jeton.consommeLe is null
              and jeton.expireLe > :maintenant
            """)
    Optional<JetonCompteEntity> trouverJetonActif(
            @Param("empreinte") String empreinte,
            @Param("type") TypeJetonCompte type,
            @Param("maintenant") Instant maintenant);
}

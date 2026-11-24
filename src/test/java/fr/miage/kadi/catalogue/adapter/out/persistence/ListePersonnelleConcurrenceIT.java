package fr.miage.kadi.catalogue.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import fr.miage.kadi.catalogue.application.service.GererListeCoursesService;
import fr.miage.kadi.catalogue.application.service.GererListeCoursesService.DemandeAjoutLigne;
import fr.miage.kadi.catalogue.domain.model.IdentifiantCompte;
import fr.miage.kadi.catalogue.domain.model.Quantite;
import fr.miage.kadi.catalogue.domain.model.TypeReference;
import fr.miage.kadi.catalogue.domain.model.UniteQuantite;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Ajouts simultanés sur la même liste (double clic, deux onglets, deux appareils) : le verrou de la liste
 * les exécute l'un après l'autre, sans ligne en double ni quantité perdue. Sans transaction de test,
 * pour que chaque ajout ait réellement sa propre transaction.
 */
@SpringBootTest
class ListePersonnelleConcurrenceIT {
    private static final int AJOUTS = 12;

    @Autowired private GererListeCoursesService service;
    @Autowired private JdbcTemplate jdbc;

    private final String email = "concurrence-" + UUID.randomUUID() + "@example.test";
    private long compte;
    private long produit;
    private long autreCompte;

    @BeforeEach
    void preparer() {
        compte = jdbc.queryForObject("INSERT INTO compte_utilisateur(email, mot_de_passe_hash) VALUES (?, 'h') "
                + "RETURNING id", Long.class, email);
        autreCompte = jdbc.queryForObject("INSERT INTO compte_utilisateur(email, mot_de_passe_hash) VALUES (?, 'h') "
                + "RETURNING id", Long.class, "autre-" + email);
        produit = jdbc.queryForObject("INSERT INTO produit(libelle, type_reference, nature_besoin) "
                + "VALUES (?, 'GENERIQUE', 'SOLIDE') RETURNING id", Long.class, "Besoin concurrent " + UUID.randomUUID());
    }

    @AfterEach
    void nettoyer() {
        jdbc.update("DELETE FROM liste_courses WHERE compte_id IN (?, ?)", compte, autreCompte);
        jdbc.update("DELETE FROM compte_utilisateur WHERE id IN (?, ?)", compte, autreCompte);
        jdbc.update("DELETE FROM produit WHERE id = ?", produit);
    }

    @Test
    void desAjoutsSimultanesDuMemeBesoinSontTousComptesSurUneSeuleLigne() throws Exception {
        lancerEnMemeTemps(AJOUTS, index -> () -> {
            service.ajouter(new IdentifiantCompte(compte), demande("1", UniteQuantite.ARTICLE));
            return null;
        });

        assertThat(jdbc.queryForObject("SELECT count(*) FROM liste_courses WHERE compte_id = ?", Integer.class, compte))
                .isEqualTo(1);
        assertThat(jdbc.queryForList("""
                SELECT l.quantite FROM ligne_liste l JOIN liste_courses lc ON lc.id = l.liste_id WHERE lc.compte_id = ?
                """, BigDecimal.class, compte)).singleElement()
                .satisfies(quantite -> assertThat(quantite).isEqualByComparingTo(String.valueOf(AJOUTS)));
    }

    @Test
    void deuxComptesQuiAjoutentEnMemeTempsGardentChacunLeurListe() throws Exception {
        lancerEnMemeTemps(AJOUTS, index -> () -> {
            long proprietaire = index % 2 == 0 ? compte : autreCompte;
            service.ajouter(new IdentifiantCompte(proprietaire), demande("0.5", UniteQuantite.KILOGRAMME));
            return null;
        });

        for (long proprietaire : new long[] {compte, autreCompte}) {
            assertThat(jdbc.queryForObject("""
                    SELECT sum(l.quantite) FROM ligne_liste l JOIN liste_courses lc ON lc.id = l.liste_id
                    WHERE lc.compte_id = ?
                    """, BigDecimal.class, proprietaire)).isEqualByComparingTo(String.valueOf(AJOUTS / 4.0));
        }
    }

    private DemandeAjoutLigne demande(String valeur, UniteQuantite unite) {
        return new DemandeAjoutLigne(TypeReference.BESOIN_GENERIQUE, produit, null,
                new Quantite(new BigDecimal(valeur), unite), null, null, null);
    }

    private static void lancerEnMemeTemps(int nombre, java.util.function.IntFunction<Callable<Void>> taches)
            throws Exception {
        ExecutorService executeur = Executors.newFixedThreadPool(nombre);
        CountDownLatch depart = new CountDownLatch(1);
        try {
            List<Future<Void>> resultats = new ArrayList<>();
            for (int index = 0; index < nombre; index++) {
                Callable<Void> tache = taches.apply(index);
                resultats.add(executeur.submit(() -> {
                    depart.await();
                    return tache.call();
                }));
            }
            depart.countDown();
            for (Future<Void> resultat : resultats) {
                resultat.get(30, TimeUnit.SECONDS);
            }
        } finally {
            executeur.shutdownNow();
        }
    }
}

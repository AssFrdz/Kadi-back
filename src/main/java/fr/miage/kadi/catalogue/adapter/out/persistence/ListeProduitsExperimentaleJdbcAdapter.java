package fr.miage.kadi.catalogue.adapter.out.persistence;

import fr.miage.kadi.catalogue.application.port.out.AjoutProduitListeRepository;
import fr.miage.kadi.catalogue.application.port.out.ListesCoursesRepository;
import fr.miage.kadi.catalogue.application.port.out.ModifierLigneListeRepository;
import fr.miage.kadi.catalogue.application.exception.LigneListeIntrouvableException;
import fr.miage.kadi.catalogue.application.exception.ProduitNonAjoutableException;
import fr.miage.kadi.catalogue.application.service.VerifierVenteProduitService;
import fr.miage.kadi.catalogue.domain.model.ContenuLigne;
import fr.miage.kadi.catalogue.domain.model.IdentifiantCompte;
import fr.miage.kadi.catalogue.domain.model.LigneListe;
import fr.miage.kadi.catalogue.domain.model.ListeCourses;
import fr.miage.kadi.catalogue.domain.model.Quantite;
import fr.miage.kadi.catalogue.domain.model.ReferenceArticle;
import fr.miage.kadi.catalogue.domain.model.RegleVenteProduit;
import fr.miage.kadi.catalogue.domain.model.TypeReference;
import fr.miage.kadi.catalogue.domain.model.UniteQuantite;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Profile("liste-experimentale")
class ListeProduitsExperimentaleJdbcAdapter
        implements ListesCoursesRepository, AjoutProduitListeRepository, ModifierLigneListeRepository {
    private static final BigDecimal MAX_QUANTITE = BigDecimal.valueOf(Long.MAX_VALUE);
    private final JdbcTemplate jdbc;
    private final VerifierVenteProduitService verifierVente;

    ListeProduitsExperimentaleJdbcAdapter(JdbcTemplate jdbc, VerifierVenteProduitService verifierVente) {
        this.jdbc = jdbc;
        this.verifierVente = verifierVente;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ListeCourses> trouverPourProprietaire(IdentifiantCompte proprietaire) {
        List<LigneListe> lignes = jdbc.query("""
                SELECT id, produit_id, quantite
                FROM ligne_produit_proposition WHERE compte_id = ? ORDER BY id
                """, (rs, row) -> new LigneListe(rs.getObject("id", UUID.class),
                new ContenuLigne(new ReferenceArticle(TypeReference.PRODUIT, rs.getLong("produit_id")),
                        new Quantite(rs.getBigDecimal("quantite"), UniteQuantite.ARTICLE), null)),
                proprietaire.valeur());
        return Optional.of(new ListeCourses(proprietaire, lignes));
    }

    @Override
    @Transactional
    public void ajouter(IdentifiantCompte proprietaire, long produitId, Quantite quantite) {
        verifierProduit(produitId, quantite);
        jdbc.update("""
                INSERT INTO liste_personnelle_proposition(compte_id) VALUES (?)
                ON CONFLICT (compte_id) DO NOTHING
                """, proprietaire.valeur());
        List<BigDecimal> resultats = jdbc.query("""
                INSERT INTO ligne_produit_proposition(id, compte_id, produit_id, quantite)
                VALUES (?, ?, ?, ?)
                ON CONFLICT (compte_id, produit_id) DO UPDATE
                SET quantite = ligne_produit_proposition.quantite + EXCLUDED.quantite
                WHERE ligne_produit_proposition.quantite + EXCLUDED.quantite <= ?
                RETURNING quantite
                """, (rs, row) -> rs.getBigDecimal("quantite"),
                UUID.randomUUID(), proprietaire.valeur(), produitId, quantite.value(), MAX_QUANTITE);
        if (resultats.isEmpty()) {
            throw new ProduitNonAjoutableException("La quantité totale dépasse la limite autorisée.");
        }
    }

    @Override
    @Transactional
    public void modifierQuantite(IdentifiantCompte proprietaire, UUID ligneId, Quantite quantite) {
        List<Long> produits = jdbc.query("""
                SELECT produit_id FROM ligne_produit_proposition WHERE id = ? AND compte_id = ?
                """, (rs, row) -> rs.getLong("produit_id"), ligneId, proprietaire.valeur());
        if (produits.isEmpty()) {
            throw new LigneListeIntrouvableException();
        }
        long produitId = produits.get(0);
        verifierProduit(produitId, quantite);
        int lignes = jdbc.update("""
                UPDATE ligne_produit_proposition SET quantite = ?
                WHERE id = ? AND compte_id = ? AND produit_id = ?
                """, quantite.value(), ligneId, proprietaire.valeur(), produitId);
        if (lignes == 0) {
            throw new LigneListeIntrouvableException();
        }
    }

    @Override
    @Transactional
    public void supprimer(IdentifiantCompte proprietaire, UUID ligneId) {
        int lignes = jdbc.update("DELETE FROM ligne_produit_proposition WHERE id = ? AND compte_id = ?",
                ligneId, proprietaire.valeur());
        if (lignes == 0) {
            throw new LigneListeIntrouvableException();
        }
    }

    private void verifierProduit(long produitId, Quantite quantite) {
        List<Long> produits = jdbc.query("SELECT id FROM produit WHERE id = ? FOR SHARE",
                (rs, row) -> rs.getLong("id"), produitId);
        if (produits.isEmpty()) {
            throw new ProduitNonAjoutableException("Le produit n'existe pas.");
        }
        List<String> unites = jdbc.query("""
                SELECT unite FROM regle_vente_produit_proposition
                WHERE produit_id = ? FOR SHARE
                """, (rs, row) -> rs.getString("unite"), produitId);
        Optional<RegleVenteProduit> regle = unites.stream().findFirst()
                .map(unite -> new RegleVenteProduit(UniteQuantite.valueOf(unite), false));
        try {
            verifierVente.verifier(quantite, regle);
        } catch (IllegalArgumentException erreur) {
            throw new ProduitNonAjoutableException(erreur.getMessage(), erreur);
        }
        if (quantite.value().compareTo(MAX_QUANTITE) > 0) {
            throw new ProduitNonAjoutableException("La quantité dépasse la limite autorisée.");
        }
    }
}

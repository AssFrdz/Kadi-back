package fr.miage.kadi.catalogue.adapter.out.persistence;

import fr.miage.kadi.catalogue.application.port.out.PropositionsBesoinRepository;
import fr.miage.kadi.catalogue.domain.model.NatureBesoin;
import fr.miage.kadi.catalogue.domain.model.PropositionBesoin;
import fr.miage.kadi.catalogue.domain.model.StatutProposition;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
class PropositionsBesoinJdbcAdapter implements PropositionsBesoinRepository {

    private static final String SELECTION = """
            SELECT b.id, b.libelle, coalesce(c.nom_affiche, c.email) AS auteur, b.statut, b.cree_le,
                   p.libelle AS libelle_commun
            FROM besoin_personnalise b
            JOIN compte_utilisateur c ON c.id = b.compte_id
            LEFT JOIN produit p ON p.id = b.produit_id
            """;
    private static final String GENERIQUES =
            "SELECT id, libelle, nature_besoin FROM produit WHERE type_reference = 'GENERIQUE'";

    private final JdbcTemplate jdbc;

    PropositionsBesoinJdbcAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<PropositionBesoin> lister(StatutProposition statut) {
        return jdbc.query(SELECTION + " WHERE b.statut = ? ORDER BY b.cree_le, b.id",
                PropositionsBesoinJdbcAdapter::proposition, statut.name());
    }

    @Override
    public Optional<PropositionBesoin> trouver(long id) {
        return jdbc.query(SELECTION + " WHERE b.id = ?", PropositionsBesoinJdbcAdapter::proposition, id)
                .stream().findFirst();
    }

    @Override
    public List<BesoinGenerique> listerBesoinsGeneriques() {
        return jdbc.query(GENERIQUES + " ORDER BY lower(libelle)", PropositionsBesoinJdbcAdapter::generique);
    }

    @Override
    public Optional<BesoinGenerique> trouverBesoinGenerique(String libelle) {
        return jdbc.query(GENERIQUES + " AND lower(libelle) = lower(?)", PropositionsBesoinJdbcAdapter::generique,
                libelle).stream().findFirst();
    }

    @Override
    public Optional<BesoinGenerique> trouverBesoinGenerique(long id) {
        return jdbc.query(GENERIQUES + " AND id = ?", PropositionsBesoinJdbcAdapter::generique, id)
                .stream().findFirst();
    }

    @Override
    @Transactional
    public Optional<Long> validerEnCreant(long propositionId, String libelle, NatureBesoin nature) {
        if (!verrouillerEnAttente(propositionId)) {
            return Optional.empty();
        }
        Long cree = jdbc.queryForObject("""
                INSERT INTO produit(libelle, type_reference, nature_besoin)
                VALUES (?, 'GENERIQUE', ?) RETURNING id""", Long.class, libelle, nature.name());
        valider(propositionId, cree);
        return Optional.of(cree);
    }

    @Override
    @Transactional
    public boolean validerEnRattachant(long propositionId, long besoinGeneriqueId) {
        if (!verrouillerEnAttente(propositionId)) {
            return false;
        }
        valider(propositionId, besoinGeneriqueId);
        return true;
    }

    @Override
    public boolean refuser(long propositionId) {
        return jdbc.update("""
                UPDATE besoin_personnalise SET statut = 'REFUSE', modere_le = CURRENT_TIMESTAMP
                WHERE id = ? AND statut = 'EN_ATTENTE'""", propositionId) == 1;
    }

    /** Verrouille la proposition jusqu'à la fin de la transaction : deux administrateurs ne la traitent pas ensemble. */
    private boolean verrouillerEnAttente(long propositionId) {
        return !jdbc.queryForList("SELECT id FROM besoin_personnalise WHERE id = ? AND statut = 'EN_ATTENTE' FOR UPDATE",
                Long.class, propositionId).isEmpty();
    }

    private void valider(long propositionId, long besoinGeneriqueId) {
        jdbc.update("""
                UPDATE besoin_personnalise SET statut = 'VALIDE', produit_id = ?, modere_le = CURRENT_TIMESTAMP
                WHERE id = ?""", besoinGeneriqueId, propositionId);
    }

    private static PropositionBesoin proposition(ResultSet resultat, int numero) throws SQLException {
        Timestamp creeLe = resultat.getTimestamp("cree_le");
        return new PropositionBesoin(resultat.getLong("id"), resultat.getString("libelle"),
                resultat.getString("auteur"), StatutProposition.valueOf(resultat.getString("statut")),
                creeLe == null ? null : creeLe.toInstant(), resultat.getString("libelle_commun"));
    }

    private static BesoinGenerique generique(ResultSet resultat, int numero) throws SQLException {
        return new BesoinGenerique(resultat.getLong("id"), resultat.getString("libelle"),
                NatureBesoin.valueOf(resultat.getString("nature_besoin")));
    }
}

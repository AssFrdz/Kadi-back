package fr.miage.kadi.compte.adapter.out.persistence;

import fr.miage.kadi.compte.application.port.out.UtilisateursSimulesRepository;
import fr.miage.kadi.compte.domain.model.RoleCompte;
import fr.miage.kadi.compte.domain.model.UtilisateurSimule;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Seuls les comptes ayant un nom affiché (créés par la migration V4) sont des profils simulés. */
@Repository
class UtilisateursSimulesJdbcAdapter implements UtilisateursSimulesRepository {

    private static final String SELECTION =
            "SELECT id, nom_affiche, role FROM compte_utilisateur WHERE nom_affiche IS NOT NULL";

    private final JdbcTemplate jdbc;

    UtilisateursSimulesJdbcAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<UtilisateurSimule> lister() {
        return jdbc.query(SELECTION + " ORDER BY id", UtilisateursSimulesJdbcAdapter::convertir);
    }

    @Override
    public Optional<UtilisateurSimule> trouver(long id) {
        return jdbc.query(SELECTION + " AND id = ?", UtilisateursSimulesJdbcAdapter::convertir, id)
                .stream().findFirst();
    }

    private static UtilisateurSimule convertir(ResultSet ligne, int numero) throws SQLException {
        return new UtilisateurSimule(ligne.getLong("id"), ligne.getString("nom_affiche"),
                RoleCompte.valueOf(ligne.getString("role")));
    }
}

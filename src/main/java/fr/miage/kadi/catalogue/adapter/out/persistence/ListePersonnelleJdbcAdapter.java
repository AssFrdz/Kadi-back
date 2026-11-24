package fr.miage.kadi.catalogue.adapter.out.persistence;

import fr.miage.kadi.catalogue.application.port.out.LigneListeVue;
import fr.miage.kadi.catalogue.application.port.out.ListePersonnelleRepository;
import fr.miage.kadi.catalogue.application.port.out.ReferencesListeRepository;
import fr.miage.kadi.catalogue.domain.model.ContenuLigne;
import fr.miage.kadi.catalogue.domain.model.IdentifiantCompte;
import fr.miage.kadi.catalogue.domain.model.LigneListe;
import fr.miage.kadi.catalogue.domain.model.ListeCourses;
import fr.miage.kadi.catalogue.domain.model.NatureBesoin;
import fr.miage.kadi.catalogue.domain.model.Quantite;
import fr.miage.kadi.catalogue.domain.model.ReferenceArticle;
import fr.miage.kadi.catalogue.domain.model.TypeReference;
import fr.miage.kadi.catalogue.domain.model.UniteQuantite;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
class ListePersonnelleJdbcAdapter implements ListePersonnelleRepository, ReferencesListeRepository {

    private final JdbcTemplate jdbc;

    ListePersonnelleJdbcAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LigneListeVue> consulter(IdentifiantCompte proprietaire) {
        return jdbc.query("""
                SELECT l.id, l.type_reference, coalesce(l.produit_id, l.besoin_personnalise_id) AS reference_id,
                       l.libelle_origine, p.marque, p.format, p.nature_besoin, b.statut,
                       l.quantite, l.unite, l.format_souhaite
                FROM liste_courses lc
                JOIN ligne_liste l ON l.liste_id = lc.id
                LEFT JOIN produit p ON p.id = l.produit_id
                LEFT JOIN besoin_personnalise b ON b.id = l.besoin_personnalise_id
                WHERE lc.compte_id = ?
                ORDER BY l.ajoutee_le, l.id
                """, (resultat, ligne) -> {
            TypeReference type = TypeReference.valueOf(resultat.getString("type_reference"));
            String nature = resultat.getString("nature_besoin");
            boolean precis = type == TypeReference.PRODUIT;
            return new LigneListeVue(resultat.getObject("id", UUID.class), type, resultat.getLong("reference_id"),
                    resultat.getString("libelle_origine"),
                    precis ? resultat.getString("marque") : null,
                    precis ? resultat.getString("format") : null,
                    resultat.getBigDecimal("quantite"), UniteQuantite.valueOf(resultat.getString("unite")),
                    resultat.getString("format_souhaite"), resultat.getString("statut"),
                    unites(type, nature == null ? null : NatureBesoin.valueOf(nature)));
        }, proprietaire.valeur());
    }

    @Override
    @Transactional
    public <T> T modifier(IdentifiantCompte proprietaire, Function<ListeCourses, Modification<T>> operation) {
        jdbc.update("INSERT INTO liste_courses(compte_id) VALUES (?) ON CONFLICT (compte_id) DO NOTHING",
                proprietaire.valeur());
        long listeId = jdbc.queryForObject("SELECT id FROM liste_courses WHERE compte_id = ? FOR UPDATE",
                Long.class, proprietaire.valeur());
        List<LigneListe> lignes = jdbc.query("""
                SELECT id, type_reference, coalesce(produit_id, besoin_personnalise_id) AS reference_id,
                       quantite, unite, format_souhaite
                FROM ligne_liste WHERE liste_id = ? ORDER BY ajoutee_le, id
                """, (resultat, ligne) -> new LigneListe(resultat.getObject("id", UUID.class), new ContenuLigne(
                new ReferenceArticle(TypeReference.valueOf(resultat.getString("type_reference")),
                        resultat.getLong("reference_id")),
                new Quantite(resultat.getBigDecimal("quantite"), UniteQuantite.valueOf(resultat.getString("unite"))),
                resultat.getString("format_souhaite"))), listeId);
        ListeCourses avant = new ListeCourses(proprietaire, lignes);

        Modification<T> modification = operation.apply(avant);
        if (modification.liste() != null) {
            enregistrer(listeId, avant, modification.liste());
        }
        return modification.resultat();
    }

    private void enregistrer(long listeId, ListeCourses avant, ListeCourses apres) {
        if (!apres.proprietaireId().equals(avant.proprietaireId())) {
            throw new IllegalStateException("Une opération ne peut pas changer le propriétaire d'une liste.");
        }
        Map<UUID, ContenuLigne> anciennes = new HashMap<>();
        avant.lignes().forEach(ligne -> anciennes.put(ligne.id(), ligne.contenu()));
        for (LigneListe ligne : apres.lignes()) {
            ContenuLigne ancien = anciennes.remove(ligne.id());
            if (ancien == null) {
                inserer(listeId, ligne);
            } else if (!ancien.equals(ligne.contenu())) {
                jdbc.update("""
                        UPDATE ligne_liste SET quantite = ?, unite = ?, format_souhaite = ?
                        WHERE id = ? AND liste_id = ?
                        """, ligne.contenu().quantite().value(), ligne.contenu().quantite().unite().name(),
                        ligne.contenu().formatSouhaite(), ligne.id(), listeId);
            }
        }
        for (UUID supprimee : anciennes.keySet()) {
            jdbc.update("DELETE FROM ligne_liste WHERE id = ? AND liste_id = ?", supprimee, listeId);
        }
    }

    private void inserer(long listeId, LigneListe ligne) {
        ContenuLigne contenu = ligne.contenu();
        ReferenceArticle reference = contenu.reference();
        boolean personnel = reference.type() == TypeReference.BESOIN_PERSONNALISE;
        String libelle = personnel
                ? "(SELECT libelle FROM besoin_personnalise WHERE id = ?)"
                : "(SELECT libelle FROM produit WHERE id = ?)";
        jdbc.update("""
                INSERT INTO ligne_liste(id, liste_id, type_reference, produit_id, besoin_personnalise_id,
                                        libelle_origine, quantite, unite, format_souhaite, ajoutee_le)
                VALUES (?, ?, ?, ?, ?, %s, ?, ?, ?, clock_timestamp())
                """.formatted(libelle), ligne.id(), listeId, reference.type().name(),
                personnel ? null : reference.id(), personnel ? reference.id() : null, reference.id(),
                contenu.quantite().value(), contenu.quantite().unite().name(), contenu.formatSouhaite());
    }

    @Override
    public Optional<ReferenceCatalogue> trouverDansCatalogue(long produitId) {
        return jdbc.query("SELECT id, type_reference, nature_besoin FROM produit WHERE id = ?",
                (resultat, ligne) -> {
                    String nature = resultat.getString("nature_besoin");
                    return new ReferenceCatalogue(resultat.getLong("id"),
                            "GENERIQUE".equals(resultat.getString("type_reference")),
                            nature == null ? null : NatureBesoin.valueOf(nature));
                }, produitId).stream().findFirst();
    }

    @Override
    public boolean besoinPersonnelAppartientA(IdentifiantCompte proprietaire, long besoinId) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM besoin_personnalise WHERE id = ? AND compte_id = ?)",
                Boolean.class, besoinId, proprietaire.valeur()));
    }

    @Override
    @Transactional
    public long enregistrerBesoinPersonnel(IdentifiantCompte proprietaire, String libelle) {
        jdbc.update("""
                INSERT INTO besoin_personnalise(compte_id, libelle) VALUES (?, ?)
                ON CONFLICT (compte_id, lower(libelle)) DO NOTHING
                """, proprietaire.valeur(), libelle);
        return jdbc.queryForObject(
                "SELECT id FROM besoin_personnalise WHERE compte_id = ? AND lower(libelle) = lower(?)",
                Long.class, proprietaire.valeur(), libelle);
    }

    private static List<UniteQuantite> unites(TypeReference type, NatureBesoin nature) {
        return switch (type) {
            case PRODUIT -> List.of(UniteQuantite.ARTICLE);
            case BESOIN_GENERIQUE -> nature == null ? List.of(UniteQuantite.ARTICLE) : nature.unitesAutorisees();
            case BESOIN_PERSONNALISE -> List.of(UniteQuantite.values());
        };
    }
}

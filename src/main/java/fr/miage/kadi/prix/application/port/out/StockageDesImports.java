package fr.miage.kadi.prix.application.port.out;

import fr.miage.kadi.prix.application.port.in.ReleveAImporter;
import java.util.List;

/** Écriture en masse d'un import : chaque méthode crée ce qui manque et rend l'identifiant existant sinon. */
public interface StockageDesImports {

    long enseigne(String nom);

    long magasin(String referenceExterne, String nom, long enseigneId);

    long produit(String codeBarres, String libelle);

    /** Un relevé à écrire, avec les identifiants de son produit et de son magasin. */
    record ReleveAEcrire(long produitId, long magasinId, ReleveAImporter releve) {
    }

    /** @return le nombre de relevés réellement ajoutés (ceux déjà présents sont ignorés) */
    int ajouterReleves(List<ReleveAEcrire> releves);
}

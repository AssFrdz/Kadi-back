package fr.miage.kadi.prix.application.service;

import fr.miage.kadi.prix.application.port.in.BilanImport;
import fr.miage.kadi.prix.application.port.in.ImporterDesReleves;
import fr.miage.kadi.prix.application.port.in.ReleveAImporter;
import fr.miage.kadi.prix.application.port.out.StockageDesImports;
import fr.miage.kadi.prix.application.port.out.StockageDesImports.ReleveAEcrire;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Import en masse : retrouve ou crée enseignes, magasins et produits une seule fois chacun (mémorisés pendant l'import),
 * puis écrit les relevés par lots.
 */
public class ImportService implements ImporterDesReleves {

    static final int TAILLE_DU_LOT = 1_000;

    private final StockageDesImports stockage;

    public ImportService(StockageDesImports stockage) {
        this.stockage = stockage;
    }

    @Override
    public BilanImport importer(Iterable<ReleveAImporter> releves) {
        Map<String, Long> enseignes = new HashMap<>();
        Map<String, Long> magasins = new HashMap<>();
        Map<String, Long> produits = new HashMap<>();
        List<ReleveAEcrire> lot = new ArrayList<>(TAILLE_DU_LOT);
        int recus = 0;
        int ajoutes = 0;

        for (ReleveAImporter releve : releves) {
            recus++;
            long enseigneId = enseignes.computeIfAbsent(releve.enseigne(), stockage::enseigne);
            long magasinId = magasins.computeIfAbsent(releve.referenceMagasin(),
                    reference -> stockage.magasin(reference, releve.nomMagasin(), enseigneId));
            long produitId = produits.computeIfAbsent(releve.codeBarres(),
                    code -> stockage.produit(code, releve.libelleProduit()));
            lot.add(new ReleveAEcrire(produitId, magasinId, releve));
            if (lot.size() == TAILLE_DU_LOT) {
                ajoutes += stockage.ajouterReleves(lot);
                lot.clear();
            }
        }
        if (!lot.isEmpty()) {
            ajoutes += stockage.ajouterReleves(lot);
        }
        return new BilanImport(recus, ajoutes, recus - ajoutes);
    }
}

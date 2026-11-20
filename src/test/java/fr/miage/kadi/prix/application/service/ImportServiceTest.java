package fr.miage.kadi.prix.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import fr.miage.kadi.prix.application.port.in.BilanImport;
import fr.miage.kadi.prix.application.port.in.ReleveAImporter;
import fr.miage.kadi.prix.application.port.out.StockageDesImports;
import fr.miage.kadi.prix.domain.model.Montant;
import fr.miage.kadi.prix.domain.model.TypeDePreuve;
import fr.miage.kadi.prix.domain.model.UniteDePrix;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** L'import en masse avec un faux stockage : chaque enseigne, magasin et produit n'est demandé qu'une fois, les relevés vont par lots. */
class ImportServiceTest {

    private final FauxStockage stockage = new FauxStockage();
    private final ImportService service = new ImportService(stockage);

    @Test
    void chaque_enseigne_magasin_et_produit_n_est_cree_qu_une_fois() {
        service.importer(List.of(
                releve("1", "node/1", "E.Leclerc", "111"),
                releve("2", "node/1", "E.Leclerc", "222"),
                releve("3", "node/2", "E.Leclerc", "111")));

        assertThat(stockage.appelsEnseigne).containsExactly("E.Leclerc");
        assertThat(stockage.appelsMagasin).containsExactly("node/1", "node/2");
        assertThat(stockage.appelsProduit).containsExactly("111", "222");
    }

    @Test
    void les_releves_sont_ecrits_par_lots_de_1000() {
        List<ReleveAImporter> releves = new ArrayList<>();
        for (int i = 0; i < 2_500; i++) {
            releves.add(releve(String.valueOf(i), "node/1", "Lidl", "111"));
        }

        BilanImport bilan = service.importer(releves);

        assertThat(stockage.taillesDesLots).containsExactly(1_000, 1_000, 500);
        assertThat(bilan).isEqualTo(new BilanImport(2_500, 2_500, 0));
    }

    @Test
    void relancer_l_import_n_ajoute_rien_et_le_bilan_le_dit() {
        List<ReleveAImporter> releves = List.of(releve("1", "node/1", "Lidl", "111"), releve("2", "node/1", "Lidl", "222"));
        service.importer(releves);

        assertThat(service.importer(releves)).isEqualTo(new BilanImport(2, 0, 2));
    }

    private static ReleveAImporter releve(String reference, String magasin, String enseigne, String codeBarres) {
        return new ReleveAImporter("OPEN_PRICES", reference, codeBarres, "Produit " + codeBarres, magasin, "Magasin " + magasin,
                enseigne, Montant.de("1.25"), UniteDePrix.PIECE, Instant.parse("2026-11-01T11:00:00Z"), TypeDePreuve.ETIQUETTE,
                "openprices:abc", false);
    }

    /** Un faux stockage qui retient les appels et, comme la base, ignore un relevé déjà importé. */
    private static final class FauxStockage implements StockageDesImports {

        private final List<String> appelsEnseigne = new ArrayList<>();
        private final List<String> appelsMagasin = new ArrayList<>();
        private final List<String> appelsProduit = new ArrayList<>();
        private final List<Integer> taillesDesLots = new ArrayList<>();
        private final Map<String, Long> identifiants = new HashMap<>();
        private final Set<String> dejaImportes = new HashSet<>();

        @Override
        public long enseigne(String nom) {
            appelsEnseigne.add(nom);
            return identifiants.computeIfAbsent("e:" + nom, cle -> (long) identifiants.size() + 1);
        }

        @Override
        public long magasin(String referenceExterne, String nom, long enseigneId) {
            appelsMagasin.add(referenceExterne);
            return identifiants.computeIfAbsent("m:" + referenceExterne, cle -> (long) identifiants.size() + 1);
        }

        @Override
        public long produit(String codeBarres, String libelle) {
            appelsProduit.add(codeBarres);
            return identifiants.computeIfAbsent("p:" + codeBarres, cle -> (long) identifiants.size() + 1);
        }

        @Override
        public int ajouterReleves(List<ReleveAEcrire> releves) {
            taillesDesLots.add(releves.size());
            return (int) releves.stream()
                    .filter(r -> dejaImportes.add(r.releve().source() + ":" + r.releve().referenceExterne()))
                    .count();
        }
    }
}

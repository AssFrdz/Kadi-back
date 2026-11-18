package fr.miage.kadi.prix.adapter.in.importation;

import fr.miage.kadi.prix.adapter.in.importation.RegleImportOpenPrices.Lecture;
import fr.miage.kadi.prix.adapter.in.importation.RegleImportOpenPrices.Rejet;
import fr.miage.kadi.prix.application.port.in.BilanImport;
import fr.miage.kadi.prix.application.port.in.ImporterDesReleves;
import fr.miage.kadi.prix.application.port.in.ReleveAImporter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Lancé uniquement avec le profil « import » : lit le fichier extrait d'Open Prices, rejette les lignes inexploitables
 * avec leur motif, confie le reste au port d'import, puis affiche le bilan (outils/open-prices/README.md).
 */
@Component
@Profile("import")
class ImportOpenPricesLanceur implements ApplicationRunner {

    private static final Logger JOURNAL = LoggerFactory.getLogger(ImportOpenPricesLanceur.class);

    private final ImporterDesReleves importer;
    private final Path fichier;

    ImportOpenPricesLanceur(ImporterDesReleves importer, @Value("${kadi.import.open-prices.fichier}") Path fichier) {
        this.importer = importer;
        this.fichier = fichier;
    }

    @Override
    public void run(ApplicationArguments arguments) throws IOException {
        JOURNAL.info("Import Open Prices depuis {}", fichier.toAbsolutePath());
        List<ReleveAImporter> releves = new ArrayList<>();
        Map<Rejet, Integer> rejets = new EnumMap<>(Rejet.class);
        try (var lignes = Files.lines(fichier, StandardCharsets.UTF_8)) {
            // la première ligne est l'en-tête
            lignes.skip(1).forEach(ligne -> {
                Lecture lecture = RegleImportOpenPrices.lire(ligne);
                if (lecture.releve() != null) {
                    releves.add(lecture.releve());
                } else {
                    rejets.merge(lecture.rejet(), 1, Integer::sum);
                }
            });
        }
        BilanImport bilan = importer.importer(releves);
        JOURNAL.info("Import terminé : {} relevés reçus, {} ajoutés, {} déjà présents, lignes rejetées : {}",
                bilan.recus(), bilan.ajoutes(), bilan.dejaPresents(), rejets);
    }
}

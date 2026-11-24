package fr.miage.kadi.catalogue.adapter.in.cli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EvaluerExportOffTest {
    private static final String EN_TETE =
            "code\tproduct_name\tbrands\tquantity\tcountries_tags\tcategories_tags\tlast_modified_t\timage_url\n";

    @TempDir
    Path dossier;

    @Test
    void compteLesLignesEtLesRejetsSansInventerUnCreditImage() throws IOException {
        Path fichier = dossier.resolve("export.tsv");
        Files.writeString(fichier, EN_TETE
                + "000123\t\"Yaourt\tfictif\"\tMarque\t100 g\ten:france,en:belgium\ten:desserts\t123\thttps://exemple.test/image.jpg\n"
                + "123456\tProduit etranger\t\t\ten:belgium\t\t\t\n"
                + "ABC\tProduit invalide\t\t\ten:france\t\t\t\n"
                + "789\t\t\t\ten:france\t\t\t\n"
                + "456\tProduit valide\t\t\ten:france\t\t\t\n", StandardCharsets.UTF_8);

        var rapport = EvaluerExportOff.analyser(fichier, 10);
        assertThat(rapport.lues()).isEqualTo(5);
        assertThat(rapport.horsFrance()).isEqualTo(1);
        assertThat(rapport.codesInvalides()).isEqualTo(1);
        assertThat(rapport.nomsInvalides()).isEqualTo(1);
        assertThat(rapport.admissibles()).isEqualTo(2);
        assertThat(rapport.imagesSansCredit()).isEqualTo(1);
        assertThat(rapport.limiteAtteinte()).isFalse();
    }

    @Test
    void litUnGzipEtSignaleUneLectureIncomplète() throws IOException {
        Path fichier = dossier.resolve("export.tsv.gz");
        try (GZIPOutputStream gzip = new GZIPOutputStream(Files.newOutputStream(fichier))) {
            gzip.write((EN_TETE + "123\tUn\t\t\ten:france\t\t\t\n"
                    + "456\tDeux\t\t\ten:france\t\t\t\n").getBytes(StandardCharsets.UTF_8));
        }
        var rapport = EvaluerExportOff.analyser(fichier, 1);
        assertThat(rapport.lues()).isEqualTo(1);
        assertThat(rapport.admissibles()).isEqualTo(1);
        assertThat(rapport.limiteAtteinte()).isTrue();
    }

    @Test
    void refuseLesEntreesInvalidesAuLieuDeProduireUnRapportTrompeur() throws IOException {
        Path fichier = dossier.resolve("incomplet.tsv");
        Files.writeString(fichier, "code\tproduct_name\n123\tProduit\n", StandardCharsets.UTF_8);
        assertThatThrownBy(() -> EvaluerExportOff.analyser(fichier, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EvaluerExportOff.analyser(fichier, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Colonne obligatoire absente");

        Path tronque = dossier.resolve("tronque.tsv");
        Files.writeString(tronque, EN_TETE + "123\tProduit\n", StandardCharsets.UTF_8);
        assertThatThrownBy(() -> EvaluerExportOff.analyser(tronque, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Nombre de colonnes invalide");
    }
}

package fr.miage.kadi.catalogue.adapter.in.cli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Savepoint;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ImporterExportOffIT {
    private static final String EN_TETE =
            "code\tproduct_name\tbrands\tquantity\tcountries_tags\tcategories_tags\tlast_modified_t\timage_url\n";

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private MockMvc mockMvc;

    @TempDir
    Path dossier;

    @Test
    void lesChampsImportesSontRecherchablesEtVisiblesDansLApi() throws Exception {
        Path fichier = dossier.resolve("produit.tsv");
        Files.writeString(fichier, EN_TETE
                + "321123\tPates penne\tMarque fictive\t500 g\ten:france\ten:pasta\t100\t\n",
                StandardCharsets.UTF_8);
        ImporterExportOff.importer(DataSourceUtils.getConnection(dataSource), fichier.toString(), 10);
        mockMvc.perform(get("/api/v1/catalogue/produits/suggestions")
                        .with(user("test")).param("q", "marque fictive"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].libelle").value("Pates penne"))
                .andExpect(jsonPath("$[0].marque").value("Marque fictive"))
                .andExpect(jsonPath("$[0].format").value("500 g"));
    }
}

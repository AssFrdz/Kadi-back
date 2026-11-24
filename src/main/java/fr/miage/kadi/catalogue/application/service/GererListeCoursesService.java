package fr.miage.kadi.catalogue.application.service;

import fr.miage.kadi.catalogue.application.exception.FusionLigneImpossibleException;
import fr.miage.kadi.catalogue.application.exception.LigneListeIntrouvableException;
import fr.miage.kadi.catalogue.application.exception.ReferenceListeIntrouvableException;
import fr.miage.kadi.catalogue.application.port.out.LigneListeVue;
import fr.miage.kadi.catalogue.application.port.out.ListePersonnelleRepository;
import fr.miage.kadi.catalogue.application.port.out.ListePersonnelleRepository.Modification;
import fr.miage.kadi.catalogue.application.port.out.ReferencesListeRepository;
import fr.miage.kadi.catalogue.application.port.out.ReferencesListeRepository.ReferenceCatalogue;
import fr.miage.kadi.catalogue.domain.model.ContenuLigne;
import fr.miage.kadi.catalogue.domain.model.IdentifiantCompte;
import fr.miage.kadi.catalogue.domain.model.LigneListe;
import fr.miage.kadi.catalogue.domain.model.ListeCourses;
import fr.miage.kadi.catalogue.domain.model.Quantite;
import fr.miage.kadi.catalogue.domain.model.ReferenceArticle;
import fr.miage.kadi.catalogue.domain.model.RegleVenteProduit;
import fr.miage.kadi.catalogue.domain.model.ResultatAjout;
import fr.miage.kadi.catalogue.domain.model.TypeReference;
import fr.miage.kadi.catalogue.domain.model.UniteQuantite;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Cas d'utilisation de la liste personnelle (F1). Les règles d'ajout, de fusion, de modification et de suppression
 * sont celles du domaine ({@link ListeCourses}) ; ce service vérifie la référence choisie et ses unités,
 * puis délègue au dépôt l'enregistrement atomique limité au propriétaire.
 */
public class GererListeCoursesService {
    static final int LONGUEUR_MAXIMALE_TEXTE = 255;
    /** Un produit précis se vend à l'article, sans fraction (règle de conception A4). */
    private static final RegleVenteProduit VENTE_PRODUIT_PRECIS = new RegleVenteProduit(UniteQuantite.ARTICLE, false);

    private final ListePersonnelleRepository listes;
    private final ReferencesListeRepository references;
    private final Supplier<UUID> identifiants;

    public GererListeCoursesService(ListePersonnelleRepository listes, ReferencesListeRepository references) {
        this(listes, references, UUID::randomUUID);
    }

    GererListeCoursesService(ListePersonnelleRepository listes, ReferencesListeRepository references,
            Supplier<UUID> identifiants) {
        this.listes = Objects.requireNonNull(listes);
        this.references = Objects.requireNonNull(references);
        this.identifiants = Objects.requireNonNull(identifiants);
    }

    public List<LigneListeVue> consulter(IdentifiantCompte proprietaire) {
        return listes.consulter(Objects.requireNonNull(proprietaire, "Le propriétaire est obligatoire."));
    }

    public ResultatAjoutLigne ajouter(IdentifiantCompte proprietaire, DemandeAjoutLigne demande) {
        Objects.requireNonNull(proprietaire, "Le propriétaire est obligatoire.");
        Objects.requireNonNull(demande, "La demande est obligatoire.");
        String format = texteFacultatif(demande.formatSouhaite(), "Le format souhaité");
        ReferenceArticle reference = resoudre(proprietaire, demande, format);
        LigneListe nouvelle = new LigneListe(identifiants.get(),
                new ContenuLigne(reference, demande.quantite(), format));

        return listes.modifier(proprietaire, liste -> switch (demande.decision()) {
            case AUTOMATIQUE -> appliquer(liste.preparerAjout(nouvelle), nouvelle);
            case SEPARER -> ajouterSepare(liste, nouvelle);
            case FUSIONNER -> fusionner(liste, demande.ligneAFusionner(), nouvelle);
        });
    }

    public void modifierQuantite(IdentifiantCompte proprietaire, UUID ligneId, BigDecimal valeur) {
        Objects.requireNonNull(proprietaire, "Le propriétaire est obligatoire.");
        Objects.requireNonNull(ligneId, "La ligne est obligatoire.");
        Objects.requireNonNull(valeur, "La quantité est obligatoire.");
        listes.modifier(proprietaire, liste -> {
            ContenuLigne actuel = trouver(liste, ligneId).contenu();
            ContenuLigne modifie = new ContenuLigne(actuel.reference(),
                    new Quantite(valeur, actuel.quantite().unite()), actuel.formatSouhaite());
            try {
                return new Modification<>(liste.modifier(ligneId, modifie), null);
            } catch (IllegalStateException erreur) {
                throw new FusionLigneImpossibleException(erreur.getMessage(), erreur);
            }
        });
    }

    public void supprimer(IdentifiantCompte proprietaire, UUID ligneId) {
        Objects.requireNonNull(proprietaire, "Le propriétaire est obligatoire.");
        Objects.requireNonNull(ligneId, "La ligne est obligatoire.");
        listes.modifier(proprietaire, liste -> {
            trouver(liste, ligneId);
            return new Modification<>(liste.supprimer(ligneId), null);
        });
    }

    private ReferenceArticle resoudre(IdentifiantCompte proprietaire, DemandeAjoutLigne demande, String format) {
        Quantite quantite = Objects.requireNonNull(demande.quantite(), "La quantité est obligatoire.");
        TypeReference type = Objects.requireNonNull(demande.type(), "Le type de référence est obligatoire.");
        switch (type) {
            case PRODUIT -> {
                ReferenceCatalogue produit = catalogue(demande.referenceId());
                if (produit.generique()) {
                    throw new IllegalArgumentException("Cette référence est un besoin générique, pas un produit précis.");
                }
                if (format != null) {
                    throw new IllegalArgumentException(
                            "Le format souhaité ne s'applique qu'aux besoins : un produit précis a déjà son format.");
                }
                VENTE_PRODUIT_PRECIS.verifier(quantite);
                return new ReferenceArticle(type, produit.id());
            }
            case BESOIN_GENERIQUE -> {
                ReferenceCatalogue besoin = catalogue(demande.referenceId());
                if (!besoin.generique()) {
                    throw new IllegalArgumentException("Cette référence est un produit précis, pas un besoin générique.");
                }
                besoin.nature().verifier(quantite);
                return new ReferenceArticle(type, besoin.id());
            }
            case BESOIN_PERSONNALISE -> {
                if (demande.referenceId() != null) {
                    if (!references.besoinPersonnelAppartientA(proprietaire, demande.referenceId())) {
                        throw new ReferenceListeIntrouvableException("Ce besoin personnel n'existe pas.");
                    }
                    return new ReferenceArticle(type, demande.referenceId());
                }
                String libelle = texteFacultatif(demande.libellePersonnel(), "Le libellé du besoin");
                if (libelle == null) {
                    throw new IllegalArgumentException("Le libellé du besoin personnel est obligatoire.");
                }
                return new ReferenceArticle(type, references.enregistrerBesoinPersonnel(proprietaire, libelle));
            }
            default -> throw new IllegalArgumentException("Type de référence inconnu.");
        }
    }

    private ReferenceCatalogue catalogue(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("L'identifiant de la référence est obligatoire.");
        }
        return references.trouverDansCatalogue(id)
                .orElseThrow(() -> new ReferenceListeIntrouvableException("Cette référence n'existe pas dans le catalogue."));
    }

    private static Modification<ResultatAjoutLigne> appliquer(ResultatAjout resultat, LigneListe nouvelle) {
        return switch (resultat) {
            case ResultatAjout.AjoutSimple ajout ->
                    new Modification<>(ajout.liste(), ResultatAjoutLigne.ajoutee(nouvelle.id()));
            case ResultatAjout.FusionAutomatique fusion ->
                    new Modification<>(fusion.liste(), ResultatAjoutLigne.fusionnee(fusion.idLigneConservee()));
            case ResultatAjout.ConfirmationRequise confirmation -> new Modification<>(null,
                    ResultatAjoutLigne.confirmationRequise(confirmation.lignesCandidates().stream()
                            .map(LigneListe::id).toList()));
            case ResultatAjout.ConflitFusionAutomatique conflit -> throw new FusionLigneImpossibleException(
                    "Plusieurs lignes existantes correspondent à cet ajout ; modifie-les avant d'ajouter.");
        };
    }

    private static Modification<ResultatAjoutLigne> ajouterSepare(ListeCourses liste, LigneListe nouvelle) {
        try {
            ListeCourses resultat = liste.ajouter(nouvelle);
            boolean ajoutee = resultat.lignes().stream().anyMatch(ligne -> ligne.id().equals(nouvelle.id()));
            UUID conservee = ajoutee ? nouvelle.id() : liste.lignesFusionnablesAvec(nouvelle.contenu()).get(0).id();
            return new Modification<>(resultat,
                    ajoutee ? ResultatAjoutLigne.ajoutee(conservee) : ResultatAjoutLigne.fusionnee(conservee));
        } catch (IllegalStateException erreur) {
            throw new FusionLigneImpossibleException(erreur.getMessage(), erreur);
        }
    }

    private static Modification<ResultatAjoutLigne> fusionner(ListeCourses liste, UUID ligneExistante,
            LigneListe nouvelle) {
        if (ligneExistante == null) {
            throw new IllegalArgumentException("La ligne à fusionner est obligatoire.");
        }
        trouver(liste, ligneExistante);
        try {
            return new Modification<>(liste.fusionnerApresConfirmation(ligneExistante, nouvelle.contenu()),
                    ResultatAjoutLigne.fusionnee(ligneExistante));
        } catch (IllegalArgumentException erreur) {
            throw new FusionLigneImpossibleException("Ces lignes ne peuvent plus être fusionnées.", erreur);
        }
    }

    private static LigneListe trouver(ListeCourses liste, UUID ligneId) {
        return liste.lignes().stream().filter(ligne -> ligne.id().equals(ligneId)).findFirst()
                .orElseThrow(LigneListeIntrouvableException::new);
    }

    private static String texteFacultatif(String valeur, String nom) {
        if (valeur == null || valeur.isBlank()) {
            return null;
        }
        String texte = valeur.strip();
        if (texte.codePointCount(0, texte.length()) > LONGUEUR_MAXIMALE_TEXTE) {
            throw new IllegalArgumentException(nom + " ne peut pas dépasser " + LONGUEUR_MAXIMALE_TEXTE + " caractères.");
        }
        return texte;
    }

    /** Ce que l'utilisateur a choisi d'ajouter. Pour un besoin personnel nouveau, {@code referenceId} est nul. */
    public record DemandeAjoutLigne(TypeReference type, Long referenceId, String libellePersonnel, Quantite quantite,
            String formatSouhaite, Decision decision, UUID ligneAFusionner) {
        public DemandeAjoutLigne {
            decision = decision == null ? Decision.AUTOMATIQUE : decision;
        }
    }

    /**
     * AUTOMATIQUE : fusion seulement si la règle du domaine l'autorise sans question ;
     * FUSIONNER / SEPARER : réponse de l'utilisateur après une demande de confirmation.
     */
    public enum Decision {
        AUTOMATIQUE, FUSIONNER, SEPARER
    }

    public record ResultatAjoutLigne(Statut statut, UUID ligneId, List<UUID> lignesCandidates) {
        public enum Statut {
            AJOUTEE, FUSIONNEE, CONFIRMATION_REQUISE
        }

        static ResultatAjoutLigne ajoutee(UUID id) {
            return new ResultatAjoutLigne(Statut.AJOUTEE, id, List.of());
        }

        static ResultatAjoutLigne fusionnee(UUID id) {
            return new ResultatAjoutLigne(Statut.FUSIONNEE, id, List.of());
        }

        static ResultatAjoutLigne confirmationRequise(List<UUID> candidates) {
            return new ResultatAjoutLigne(Statut.CONFIRMATION_REQUISE, null, List.copyOf(candidates));
        }
    }
}

package fr.miage.kadi.prix.domain.algo;

import fr.miage.kadi.prix.domain.algo.MedianePonderee.Element;
import fr.miage.kadi.prix.domain.exception.DonneeInvalideException;
import fr.miage.kadi.prix.domain.model.Confiance;
import fr.miage.kadi.prix.domain.model.Montant;
import fr.miage.kadi.prix.domain.model.MotifExclusion;
import fr.miage.kadi.prix.domain.model.NiveauDeCalcul;
import fr.miage.kadi.prix.domain.model.PrixDeReference;
import fr.miage.kadi.prix.domain.model.ReleveDePrix;
import fr.miage.kadi.prix.domain.model.ReleveEcarte;
import fr.miage.kadi.prix.domain.model.UniteDePrix;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Le prix de référence d'un produit dans un magasin (fiche F2, section Algorithme, étapes 1 à 10).
 * « Maintenant » est un paramètre, jamais lu sur l'horloge : le calcul donne le même résultat à entrées égales.
 * Pour q relevés : O(q) en moyenne et O(q) en mémoire.
 */
public final class CalculPrixDeReference {

    /** Rend la MAD comparable à un écart-type pour des données normales. */
    static final double FACTEUR_MAD = 1.4826;

    private static final double MILLISECONDES_PAR_JOUR = 86_400_000.0;

    private final ParametresPrixFiable parametres;

    public CalculPrixDeReference(ParametresPrixFiable parametres) {
        if (parametres == null) {
            throw new DonneeInvalideException("Les paramètres du calcul sont obligatoires");
        }
        this.parametres = parametres;
    }

    /**
     * @param duMagasin   relevés du produit dans le magasin
     * @param deLEnseigne relevés du produit dans tous les magasins de l'enseigne, utilisés seulement en repli
     * @return vide si aucun relevé n'est exploitable, même au niveau de l'enseigne
     */
    public Optional<PrixDeReference> calculer(List<ReleveDePrix> duMagasin, List<ReleveDePrix> deLEnseigne, Instant maintenant) {
        return evaluer(duMagasin, deLEnseigne, maintenant).map(Evaluation::prix);
    }

    /** Le prix de référence et ce qu'il faut pour contrôler une saisie face à lui. */
    record Evaluation(PrixDeReference prix, double echelle, int contributeurs) {
    }

    Optional<Evaluation> evaluer(List<ReleveDePrix> duMagasin, List<ReleveDePrix> deLEnseigne, Instant maintenant) {
        if (maintenant == null) {
            throw new DonneeInvalideException("Le calcul a besoin de l'instant présent");
        }
        Optional<Evaluation> auMagasin = evaluerUnNiveau(listeOuVide(duMagasin), maintenant, NiveauDeCalcul.MAGASIN);
        if (auMagasin.isPresent()) {
            return auMagasin;
        }
        // Étape 10 : repli sur l'enseigne, avec une confiance réduite
        return evaluerUnNiveau(listeOuVide(deLEnseigne), maintenant, NiveauDeCalcul.ENSEIGNE);
    }

    /** Un relevé et son poids courant dans le calcul. */
    private record Pondere(ReleveDePrix releve, double poids) {
        BigDecimal valeur() {
            return releve.prix().valeur();
        }
    }

    private Optional<Evaluation> evaluerUnNiveau(List<ReleveDePrix> releves, Instant maintenant, NiveauDeCalcul niveau) {
        List<ReleveEcarte> ecartes = new ArrayList<>();

        // Entrée : promotions et relevés trop anciens exclus ; étape 3 : poids de base
        List<Pondere> candidats = new ArrayList<>();
        for (ReleveDePrix releve : releves) {
            double age = ageEnJours(releve, maintenant);
            if (releve.enPromotion()) {
                ecartes.add(new ReleveEcarte(releve, MotifExclusion.EN_PROMOTION));
            } else if (age > joursDe(parametres.fenetre())) {
                ecartes.add(new ReleveEcarte(releve, MotifExclusion.TROP_ANCIEN));
            } else {
                double fraicheur = Math.pow(2, -age / joursDe(parametres.demiViePoids()));
                candidats.add(new Pondere(releve, releve.preuve().poids() * fraicheur));
            }
        }
        if (candidats.isEmpty()) {
            return Optional.empty();
        }

        // Étape 1 : une seule unité
        UniteDePrix unite = uniteLaPlusLourde(candidats);
        List<Pondere> memeUnite = new ArrayList<>();
        for (Pondere candidat : candidats) {
            if (candidat.releve().unite() == unite) {
                memeUnite.add(candidat);
            } else {
                ecartes.add(new ReleveEcarte(candidat.releve(), MotifExclusion.UNITE_INCOMPATIBLE));
            }
        }

        candidats = retirerLesDoublons(memeUnite, ecartes);
        candidats = plafonnerParContributeur(candidats);

        // Étapes 5 à 7 : première médiane, dispersion robuste, valeurs aberrantes
        List<Pondere> retenus = candidats;
        if (nombreDeContributeurs(candidats) >= parametres.contributeursMinimum()) {
            BigDecimal premiereMediane = medianeDe(candidats);
            double echelle = echelle(candidats, premiereMediane);
            retenus = new ArrayList<>();
            for (Pondere candidat : candidats) {
                if (Math.abs(scoreZ(candidat.valeur(), premiereMediane, echelle)) > parametres.seuilAberrant()) {
                    ecartes.add(new ReleveEcarte(candidat.releve(), MotifExclusion.ABERRANT));
                } else {
                    retenus.add(candidat);
                }
            }
        }

        // Étape 8 : le prix de référence
        BigDecimal mediane = medianeDe(retenus);

        // Étape 9 : la confiance
        double score = fNombre(retenus) * fDispersion(retenus, mediane) * fFraicheur(retenus, maintenant);
        if (niveau == NiveauDeCalcul.ENSEIGNE) {
            score *= parametres.penaliteRepli();
        }

        PrixDeReference prix = new PrixDeReference(
                new Montant(mediane),
                unite,
                new Confiance(Math.min(1, Math.max(0, score))),
                niveau,
                retenus.stream().map(Pondere::releve).toList(),
                ecartes);
        return Optional.of(new Evaluation(prix, echelle(retenus, mediane), nombreDeContributeurs(retenus)));
    }

    /** L'unité qui porte le plus de poids ; une égalité est départagée par l'ordre de l'énumération. */
    private static UniteDePrix uniteLaPlusLourde(List<Pondere> candidats) {
        Map<UniteDePrix, Double> poidsParUnite = new EnumMap<>(UniteDePrix.class);
        for (Pondere candidat : candidats) {
            poidsParUnite.merge(candidat.releve().unite(), candidat.poids(), Double::sum);
        }
        UniteDePrix plusLourde = null;
        for (Map.Entry<UniteDePrix, Double> entree : poidsParUnite.entrySet()) {
            if (plusLourde == null || entree.getValue() > poidsParUnite.get(plusLourde) + MedianePonderee.TOLERANCE) {
                plusLourde = entree.getKey();
            }
        }
        return plusLourde;
    }

    /** Étape 2 : même contributeur, même jour, même montant ; on garde le plus récent (puis le plus petit identifiant). */
    private List<Pondere> retirerLesDoublons(List<Pondere> candidats, List<ReleveEcarte> ecartes) {
        List<Pondere> ordonnes = new ArrayList<>(candidats);
        ordonnes.sort(Comparator
                .comparing((Pondere p) -> p.releve().releveLe()).reversed()
                .thenComparing(p -> p.releve().id(), Comparator.nullsLast(Comparator.naturalOrder())));
        record Cle(String contributeur, LocalDate jour, Montant prix) {
        }
        Set<Cle> dejaVus = new HashSet<>();
        List<Pondere> uniques = new ArrayList<>();
        for (Pondere candidat : ordonnes) {
            ReleveDePrix releve = candidat.releve();
            Cle cle = new Cle(releve.contributeur(), LocalDate.ofInstant(releve.releveLe(), parametres.fuseau()), releve.prix());
            if (dejaVus.add(cle)) {
                uniques.add(candidat);
            } else {
                ecartes.add(new ReleveEcarte(releve, MotifExclusion.DOUBLON));
            }
        }
        return uniques;
    }

    /** Étape 4 : un contributeur pèse au plus autant que son relevé le plus lourd, quel que soit son nombre de saisies. */
    private static List<Pondere> plafonnerParContributeur(List<Pondere> candidats) {
        Map<String, Double> total = new HashMap<>();
        Map<String, Double> maximum = new HashMap<>();
        for (Pondere candidat : candidats) {
            String contributeur = candidat.releve().contributeur();
            total.merge(contributeur, candidat.poids(), Double::sum);
            maximum.merge(contributeur, candidat.poids(), Math::max);
        }
        List<Pondere> plafonnes = new ArrayList<>();
        for (Pondere candidat : candidats) {
            String contributeur = candidat.releve().contributeur();
            double facteur = maximum.get(contributeur) / total.get(contributeur);
            plafonnes.add(new Pondere(candidat.releve(), candidat.poids() * facteur));
        }
        return plafonnes;
    }

    /** Étape 6 : s = max(1,4826 × MAD ; plancher × m), pour qu'une MAD nulle ne rende pas tout écart infini. */
    private double echelle(List<Pondere> releves, BigDecimal mediane) {
        double brute = FACTEUR_MAD * mad(releves, mediane);
        return Math.max(brute, parametres.plancherEchelle() * mediane.doubleValue());
    }

    static double scoreZ(BigDecimal valeur, BigDecimal mediane, double echelle) {
        return valeur.subtract(mediane).doubleValue() / echelle;
    }

    /** f_n = n_eff / (n_eff + 2), n_eff = (Σ W)² / Σ W² sur le poids total W de chaque contributeur (Kish). */
    private static double fNombre(List<Pondere> retenus) {
        Map<String, Double> poidsParContributeur = new HashMap<>();
        for (Pondere retenu : retenus) {
            poidsParContributeur.merge(retenu.releve().contributeur(), retenu.poids(), Double::sum);
        }
        double somme = 0;
        double sommeDesCarres = 0;
        for (double poids : poidsParContributeur.values()) {
            somme += poids;
            sommeDesCarres += poids * poids;
        }
        double tailleEffective = somme * somme / sommeDesCarres;
        return tailleEffective / (tailleEffective + 2);
    }

    /** f_d = max(0 ; 1 − CV / dispersion maximale), CV = 1,4826 × MAD / m sur les relevés retenus. */
    private double fDispersion(List<Pondere> retenus, BigDecimal mediane) {
        double coefficientDeVariation = FACTEUR_MAD * mad(retenus, mediane) / mediane.doubleValue();
        return Math.max(0, 1 - coefficientDeVariation / parametres.dispersionMaximale());
    }

    /** f_a = 2^(−a_min / 30), a_min = âge en jours du relevé retenu le plus récent. */
    private double fFraicheur(List<Pondere> retenus, Instant maintenant) {
        double plusRecent = retenus.stream()
                .mapToDouble(retenu -> ageEnJours(retenu.releve(), maintenant))
                .min()
                .orElseThrow();
        return Math.pow(2, -plusRecent / joursDe(parametres.demiVieFraicheur()));
    }

    /** Écart absolu médian, pondéré par les mêmes poids que la médiane. */
    private static double mad(List<Pondere> releves, BigDecimal mediane) {
        List<Element<BigDecimal>> ecarts = releves.stream()
                .map(p -> new Element<>(p.valeur().subtract(mediane).abs(), p.poids()))
                .toList();
        return MedianePonderee.de(ecarts).doubleValue();
    }

    private static BigDecimal medianeDe(List<Pondere> releves) {
        return MedianePonderee.de(releves.stream().map(p -> new Element<>(p.valeur(), p.poids())).toList());
    }

    private static int nombreDeContributeurs(List<Pondere> releves) {
        return (int) releves.stream().map(p -> p.releve().contributeur()).distinct().count();
    }

    /** Âge en jours ; une date future (refusée par le service) compte comme un âge nul. */
    private static double ageEnJours(ReleveDePrix releve, Instant maintenant) {
        return Math.max(0, Duration.between(releve.releveLe(), maintenant).toMillis() / MILLISECONDES_PAR_JOUR);
    }

    private static double joursDe(Duration duree) {
        return duree.toMillis() / MILLISECONDES_PAR_JOUR;
    }

    private static <T> List<T> listeOuVide(List<T> liste) {
        return liste == null ? List.of() : liste;
    }
}

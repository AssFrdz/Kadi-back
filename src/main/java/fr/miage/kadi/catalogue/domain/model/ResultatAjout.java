package fr.miage.kadi.catalogue.domain.model;

import java.util.List;
import java.util.UUID;

public sealed interface ResultatAjout
        permits ResultatAjout.AjoutSimple,
                ResultatAjout.FusionAutomatique,
                ResultatAjout.ConfirmationRequise,
                ResultatAjout.ConflitFusionAutomatique {

    record AjoutSimple(ListeCourses liste) implements ResultatAjout {
        public AjoutSimple {
            if (liste == null) {
                throw new IllegalArgumentException("La liste résultante ne peut pas être null.");
            }
        }
    }

    record FusionAutomatique(ListeCourses liste, UUID idLigneConservee) implements ResultatAjout {
        public FusionAutomatique {
            if (liste == null) {
                throw new IllegalArgumentException("La liste résultante ne peut pas être null.");
            }
            if (idLigneConservee == null) {
                throw new IllegalArgumentException("L'identifiant de la ligne fusionnée ne peut pas être null.");
            }
        }
    }

    record ConfirmationRequise(
            LigneListe ligneAAjouter,
            List<LigneListe> lignesCandidates) implements ResultatAjout {
        public ConfirmationRequise {
            if (ligneAAjouter == null) {
                throw new IllegalArgumentException("La ligne à ajouter ne peut pas être null.");
            }
            if (lignesCandidates == null || lignesCandidates.isEmpty()) {
                throw new IllegalArgumentException("Les lignes candidates ne peuvent pas être nulles ou vides.");
            }
            if (lignesCandidates.stream().anyMatch(ligne -> ligne == null)) {
                throw new IllegalArgumentException("Une ligne candidate ne peut pas être null.");
            }
            lignesCandidates = List.copyOf(lignesCandidates);
        }
    }

    record ConflitFusionAutomatique(List<LigneListe> lignesCandidates) implements ResultatAjout {
        public ConflitFusionAutomatique {
            if (lignesCandidates == null || lignesCandidates.size() < 2) {
                throw new IllegalArgumentException(
                        "Un conflit automatique doit contenir au moins deux lignes candidates.");
            }
            if (lignesCandidates.stream().anyMatch(ligne -> ligne == null)) {
                throw new IllegalArgumentException("Une ligne candidate ne peut pas être null.");
            }
            lignesCandidates = List.copyOf(lignesCandidates);
        }
    }
}

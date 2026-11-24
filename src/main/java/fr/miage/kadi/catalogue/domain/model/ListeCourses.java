package fr.miage.kadi.catalogue.domain.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record ListeCourses(IdentifiantCompte proprietaireId, List<LigneListe> lignes) {

    public ListeCourses {
        if (proprietaireId == null) {
            throw new IllegalArgumentException("Le propriétaire de la liste ne peut pas être null.");
        }
        if (lignes == null) {
            throw new IllegalArgumentException("La liste des lignes ne peut pas être null.");
        }

        Set<UUID> ids = new HashSet<>();

        for (LigneListe ligne : lignes) {
            if (ligne == null) {
                throw new IllegalArgumentException("Une ligne de la liste ne peut pas être null.");
            }

            if (!ids.add(ligne.id())) {
                throw new IllegalArgumentException(
                    "Deux lignes ne peuvent pas avoir le même identifiant : " + ligne.id()
                );
            }
        }

        lignes = List.copyOf(lignes);
    }

    public List<LigneListe> lignesFusionnablesAvec(ContenuLigne contenu) {
            if (contenu == null) {
                throw new IllegalArgumentException("Le contenu recherché ne peut pas être null.");
            }

            List<LigneListe> resultat = new ArrayList<>();

            for (LigneListe ligne : lignes) {
                if (ligne.contenu().peutFusionnerAutomatiquementAvec(contenu)) {
                    resultat.add(ligne);
                }
            }

            return List.copyOf(resultat);
        }

    public List<LigneListe> lignesAConfirmerAvec(ContenuLigne contenu) {
        if (contenu == null) {
            throw new IllegalArgumentException("Le contenu recherché ne peut pas être null.");
        }

        List<LigneListe> resultat = new ArrayList<>();

        for (LigneListe ligne : lignes) {
            if (ligne.contenu().pourraitFusionnerApresNormalisationAvec(contenu)) {
                resultat.add(ligne);
            }
        }

        return List.copyOf(resultat);
    }

    public ListeCourses fusionnerApresConfirmation(
            UUID idLigneExistante,
            ContenuLigne contenuAAjouter) {
        if (idLigneExistante == null) {
            throw new IllegalArgumentException("L'identifiant de la ligne ne peut pas être null.");
        }

        if (contenuAAjouter == null) {
            throw new IllegalArgumentException("Le contenu à ajouter ne peut pas être null.");
        }

        List<LigneListe> resultat = new ArrayList<>(lignes);

        for (int i = 0; i < resultat.size(); i++) {
            LigneListe ligneExistante = resultat.get(i);

            if (!ligneExistante.id().equals(idLigneExistante)) {
                continue;
            }

            ContenuLigne contenuExistant = ligneExistante.contenu();
            if (!contenuExistant.pourraitFusionnerApresNormalisationAvec(contenuAAjouter)) {
                throw new IllegalArgumentException(
                        "La ligne sélectionnée ne peut plus être fusionnée avec ce contenu.");
            }

            Quantite quantiteFusionnee =
                    contenuExistant.quantite().ajouter(contenuAAjouter.quantite());
            ContenuLigne contenuFusionne = new ContenuLigne(
                    contenuExistant.reference(),
                    quantiteFusionnee,
                    contenuExistant.formatSouhaite());

            resultat.set(i, ligneExistante.avecContenu(contenuFusionne));
            return new ListeCourses(proprietaireId, resultat);
        }

        throw new IllegalArgumentException(
                "Aucune ligne ne possède l'identifiant : " + idLigneExistante);
    }

    public ResultatAjout preparerAjout(LigneListe nouvelleLigne) {
        if (nouvelleLigne == null) {
            throw new IllegalArgumentException("La ligne à ajouter ne peut pas être null.");
        }

        if (lignes.stream().anyMatch(ligne -> ligne.id().equals(nouvelleLigne.id()))) {
            throw new IllegalArgumentException(
                    "Une ligne avec l'identifiant " + nouvelleLigne.id() + " existe déjà.");
        }

        List<LigneListe> fusionnables =
                lignesFusionnablesAvec(nouvelleLigne.contenu());
        if (fusionnables.size() > 1) {
            return new ResultatAjout.ConflitFusionAutomatique(fusionnables);
        }
        if (fusionnables.size() == 1) {
            LigneListe ligneExistante = fusionnables.get(0);
            return new ResultatAjout.FusionAutomatique(
                    ajouter(nouvelleLigne), ligneExistante.id());
        }

        List<LigneListe> candidates =
                lignesAConfirmerAvec(nouvelleLigne.contenu());
        if (!candidates.isEmpty()) {
            return new ResultatAjout.ConfirmationRequise(nouvelleLigne, candidates);
        }

        return new ResultatAjout.AjoutSimple(ajouter(nouvelleLigne));
    }

        public ListeCourses ajouter(LigneListe nouvelleLigne) {
        if (nouvelleLigne == null) {
            throw new IllegalArgumentException("La ligne à ajouter ne peut pas être null.");
        }

        for (LigneListe ligne : lignes) {
            if (ligne.id().equals(nouvelleLigne.id())) {
                throw new IllegalArgumentException(
                    "Une ligne avec l'identifiant " + nouvelleLigne.id() + " existe déjà."
                );
            }
        }

        List<LigneListe> fusionnables =
                lignesFusionnablesAvec(nouvelleLigne.contenu());

        if (fusionnables.isEmpty()) {
            List<LigneListe> resultat = new ArrayList<>(lignes);
            resultat.add(nouvelleLigne);
            return new ListeCourses(proprietaireId, resultat);
        }

        if (fusionnables.size() == 1) {
            LigneListe ligneExistante = fusionnables.get(0);

            ContenuLigne contenuFusionne =
                    ligneExistante.contenu()
                                .fusionnerAutomatiquementAvec(nouvelleLigne.contenu());

            LigneListe ligneFusionnee =
                    ligneExistante.avecContenu(contenuFusionne);

            List<LigneListe> resultat = new ArrayList<>(lignes);
            int index = resultat.indexOf(ligneExistante);
            resultat.set(index, ligneFusionnee);

            return new ListeCourses(proprietaireId, resultat);
        }

        throw new IllegalStateException(
            "Impossible d'ajouter la ligne : plusieurs lignes existantes sont compatibles avec ce contenu."
        );
    }

    public ListeCourses supprimer(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("L'identifiant à supprimer ne peut pas être null.");
        }

        List<LigneListe> resultat = new ArrayList<>(lignes);

        Iterator<LigneListe> iterator = resultat.iterator();

        while (iterator.hasNext()) {
            LigneListe ligne = iterator.next();

            if (ligne.id().equals(id)) {
                iterator.remove();
                return new ListeCourses(proprietaireId, resultat);
            }
        }

        throw new IllegalArgumentException(
            "Aucune ligne ne possède l'identifiant : " + id
        );
    }

    
    public ListeCourses modifier(UUID id, ContenuLigne nouveauContenu) {
        if (id == null) {
            throw new IllegalArgumentException("L'identifiant à modifier ne peut pas être null.");
        }

        if (nouveauContenu == null) {
            throw new IllegalArgumentException("Le nouveau contenu ne peut pas être null.");
        }

        LigneListe ligneAModifier = null;

        for (LigneListe ligne : lignes) {
            if (ligne.id().equals(id)) {
                ligneAModifier = ligne;
                break;
            }
        }

        if (ligneAModifier == null) {
            throw new IllegalArgumentException(
                "Aucune ligne ne possède l'identifiant : " + id
            );
        }

        for (LigneListe ligne : lignes) {
            if (!ligne.id().equals(id)
                    && ligne.contenu().peutFusionnerAutomatiquementAvec(nouveauContenu)) {
                throw new IllegalStateException(
                    "Modification impossible : une autre ligne est compatible avec ce contenu."
                );
            }
        }

        List<LigneListe> resultat = new ArrayList<>(lignes);

        int index = resultat.indexOf(ligneAModifier);

        resultat.set(
            index,
            ligneAModifier.avecContenu(nouveauContenu)
        );

        return new ListeCourses(proprietaireId, resultat);
    }

    


} 
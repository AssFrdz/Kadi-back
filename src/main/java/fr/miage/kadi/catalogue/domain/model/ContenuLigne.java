package fr.miage.kadi.catalogue.domain.model;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Objects;

public record ContenuLigne(ReferenceArticle reference, Quantite quantite, String formatSouhaite) {

    public ContenuLigne{
        if(reference==null){
            throw new NullPointerException("Une référence ne peut pas être nulle");
        }

        if(quantite==null){
            throw new NullPointerException("Une quantité ne peut pas être nulle");
        }

    }

    public boolean aMemeReferenceEtFormatQue(ContenuLigne autre){
        if(autre==null){
            throw new NullPointerException("Le contenu d'une ligne ne peut pas être null");
        }

        if(reference.equals(autre.reference())){

            if(Objects.equals(autre.formatSouhaite, formatSouhaite)){
                return true;
            }

        }
        return false;
    }

    public boolean peutFusionnerAutomatiquementAvec(ContenuLigne autre){
        if(autre==null){
            throw new NullPointerException("Le contenu d'une ligne ne peut pas être null");
        }
        if(this.aMemeReferenceEtFormatQue(autre) && this.quantite.estCompatibleAvec(autre.quantite)){
            return true;
        }else{
            return false;
        }
    }

    public ContenuLigne fusionnerAutomatiquementAvec(ContenuLigne autre){
        if(autre==null) throw new NullPointerException("Le contenu d'une ligne ne peut pas être null");

        if(this.peutFusionnerAutomatiquementAvec(autre)!=true) throw new IllegalArgumentException("Les contenus ne peuvent pas être fusionnés automatiquement");
        
        Quantite qt = this.quantite.ajouter(autre.quantite);
        ContenuLigne cl = new ContenuLigne(this.reference, qt, this.formatSouhaite);

        return cl;

    }

    public boolean pourraitFusionnerApresNormalisationAvec(ContenuLigne autre) {
    if (autre == null
            || !reference.equals(autre.reference())
            || formatSouhaite == null
            || autre.formatSouhaite() == null
            || Objects.equals(formatSouhaite, autre.formatSouhaite())) {
        return false;
    }

    if (!quantite.estCompatibleAvec(autre.quantite())) {
        return false;
    }

    return normaliserFormat(formatSouhaite)
            .equals(normaliserFormat(autre.formatSouhaite()));
    }

    private static String normaliserFormat(String format) {
        return Normalizer.normalize(format, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .strip()
                .replaceAll("(?U)\\s+", " ");
    }

    
}

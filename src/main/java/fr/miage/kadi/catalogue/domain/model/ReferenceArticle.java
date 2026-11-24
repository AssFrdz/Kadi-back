package fr.miage.kadi.catalogue.domain.model;

public record ReferenceArticle(TypeReference type, long id) {
    public ReferenceArticle{
        if(type==null){
            throw new NullPointerException("Le type ne peut pas être null");
        }
        if(id<=0){
            throw new IllegalArgumentException("L'identifiant ne peut pas être inférieur ou égal à 0");
        }

    }


    
}

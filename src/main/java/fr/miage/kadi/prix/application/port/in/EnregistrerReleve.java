package fr.miage.kadi.prix.application.port.in;

/**
 * Contrôler un prix saisi face à la référence existante, puis l'enregistrer.
 * Un prix inhabituel n'est enregistré que si la personne a confirmé.
 */
public interface EnregistrerReleve {

    /**
     * @throws fr.miage.kadi.prix.domain.exception.ProduitInconnuException si le produit n'existe pas
     * @throws fr.miage.kadi.prix.domain.exception.MagasinInconnuException si le magasin n'existe pas
     * @throws fr.miage.kadi.prix.domain.exception.DonneeInvalideException si la saisie est absurde (date future, prix nul…)
     */
    ResultatEnregistrement enregistrer(DemandeDeReleve demande);
}

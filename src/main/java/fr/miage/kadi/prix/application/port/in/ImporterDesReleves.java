package fr.miage.kadi.prix.application.port.in;

/**
 * Charger en masse des relevés d'une source externe (Open Prices), en créant au passage
 * les enseignes, magasins et produits qui manquent. Relançable : un relevé déjà importé est ignoré.
 */
public interface ImporterDesReleves {

    BilanImport importer(Iterable<ReleveAImporter> releves);
}

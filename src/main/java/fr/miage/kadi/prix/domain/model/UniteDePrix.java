package fr.miage.kadi.prix.domain.model;

/** Ce à quoi un prix se rapporte : on ne compare jamais des kilos et des pièces. */
public enum UniteDePrix {
    PIECE,
    KILOGRAMME,
    LITRE
}

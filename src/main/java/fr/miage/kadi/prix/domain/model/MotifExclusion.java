package fr.miage.kadi.prix.domain.model;

/** Pourquoi un relevé ne participe pas au prix de référence : chaque exclusion est expliquée. */
public enum MotifExclusion {
    EN_PROMOTION,
    TROP_ANCIEN,
    UNITE_INCOMPATIBLE,
    DOUBLON,
    ABERRANT
}

package com.erp.domain;

public enum MovementType {
    /** Entrée de stock (achat, retour) */
    IN,
    /** Sortie de stock (vente, casse) */
    OUT,
    /** Ajustement d'inventaire : la quantité est signée (+/-) */
    ADJUSTMENT
}

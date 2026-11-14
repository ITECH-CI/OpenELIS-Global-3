package org.openelisglobal.dataexchange.order.valueholder;

/**
 * Catégorie d'une demande électronique : chaque écran ne montre que la sienne
 * (« Demandes reçues » : ROUTINE ; demandes d'étude charge virale : ETUDE_CV).
 */
public enum ElectronicOrderCategorie {
    ROUTINE, ETUDE_CV;

    /** Catégorie lue d'un code (ROUTINE, ETUDE_CV, etude-cv…), ou null. */
    public static ElectronicOrderCategorie fromCode(String code) {
        if (code == null) {
            return null;
        }
        String normalized = code.trim().toUpperCase().replace('-', '_');
        for (ElectronicOrderCategorie categorie : values()) {
            if (categorie.name().equals(normalized)) {
                return categorie;
            }
        }
        return null;
    }
}

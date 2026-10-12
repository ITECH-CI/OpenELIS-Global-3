package org.openelisglobal.organization.util;

import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;

/**
 * Site demandeur : uniquement les établissements du référentiel national (DHIS2
 * / MFL) quand la saisie libre est interdite (propriété
 * {@code restrictFreeTextRefSiteEntry}, activée par la migration du
 * référentiel). L'écran de saisie empêche déjà le texte libre ; ce contrôle
 * côté serveur refuse aussi toute création d'organisation à partir d'un nom
 * libre envoyé directement (API, ancien écran, saisie par lot).
 */
public final class SiteDemandeurReferentiel {

    private SiteDemandeurReferentiel() {
    }

    public static boolean saisieLibreInterdite() {
        return "true".equalsIgnoreCase(
                ConfigurationProperties.getInstance().getPropertyValue(Property.restrictFreeTextRefSiteEntry));
    }

    /**
     * À appeler avant de créer une organisation « site demandeur » à partir d'un
     * nom saisi : lève une erreur si la saisie libre est interdite.
     */
    public static void verifierCreationAutorisee(String nomSaisi) {
        if (saisieLibreInterdite()) {
            throw new LIMSRuntimeException("Le site demandeur « " + nomSaisi
                    + " » ne figure pas dans le référentiel des établissements : choisissez un"
                    + " établissement de la liste (ou demandez à l'administrateur de le réconcilier).");
        }
    }
}

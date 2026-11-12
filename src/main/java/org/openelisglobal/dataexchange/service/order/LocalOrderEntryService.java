package org.openelisglobal.dataexchange.service.order;

import java.util.ArrayList;
import java.util.List;

/**
 * « Nouvelle demande » : une demande saisie localement (accueil, salle de
 * prélèvement), reprise ensuite par le laboratoire dans « Demandes reçues ».
 * Rien n'est émis vers la plateforme avant la prise en charge.
 */
public interface LocalOrderEntryService {

    class LocalOrderForm {
        public String patientId;
        public String providerLastName;
        public String providerFirstName;
        public String requestingOrganizationId;
        public List<String> testIds = new ArrayList<>();
        public String venueNumber;
        public String priority;
    }

    /**
     * Crée la demande (une demande électronique par examen, origine SAISIE_LOCALE,
     * catégorie ROUTINE, à traiter) et renvoie son numéro.
     *
     * @throws IllegalArgumentException demande incomplète ou invalide
     */
    String create(LocalOrderForm form, String sysUserId);
}

package org.openelisglobal.dataexchange.service.order;

import java.util.List;
import java.util.Map;

/** Fenêtre « Demandes reçues » : demandes de routine, groupées par demande. */
public interface ReceivedOrdersService {

    /** Critères de la liste ; tout critère vide est ignoré. */
    class Criteria {
        public String etape; // une valeur de ReceivedOrderRow.Etape, ou TOUTES ; défaut A_TRAITER
        public String origine;
        public String etablissement;
        public String dateDebut; // jj/mm/aaaa
        public String dateFin;
        public String recherche; // numéro CMU, nom, numéro de demande
        public int page = 1;
        public int pageSize = 25;
    }

    class Page {
        public List<ReceivedOrderRow> rows;
        public int total;
        public int page;
        public int pageSize;
    }

    Page search(Criteria criteria);

    /** Nombre de demandes à traiter (compteur du menu). */
    int countToProcess();

    /** Renseignements cliniques et note du prescripteur, lus dans data. */
    Map<String, Object> detail(String electronicOrderId);
}

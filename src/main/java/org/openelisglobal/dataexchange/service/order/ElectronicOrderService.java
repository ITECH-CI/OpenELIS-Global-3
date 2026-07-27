package org.openelisglobal.dataexchange.service.order;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.common.services.StatusService.ExternalOrderStatus;
import org.openelisglobal.dataexchange.order.form.ElectronicOrderViewForm;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrder;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrder.SortOrder;

public interface ElectronicOrderService extends BaseObjectService<ElectronicOrder, String> {

    List<ElectronicOrder> getAllElectronicOrdersOrderedBy(ElectronicOrder.SortOrder order);

    List<ElectronicOrder> getElectronicOrdersByExternalId(String id);

    List<ElectronicOrder> getAllElectronicOrdersContainingValueOrderedBy(String searchValue, SortOrder sortOrder);

    List<ElectronicOrder> getAllElectronicOrdersContainingValuesOrderedBy(String accessionNumber,
            String patientLastName, String patientFirstName, String gender, SortOrder order);

    List<ElectronicOrder> getElectronicOrdersContainingValueExludedByOrderedBy(String searchValue,
            List<ExternalOrderStatus> excludedStatuses, SortOrder sortOrder);

    List<ElectronicOrder> getAllElectronicOrdersByDateAndStatus(Date startDate, Date endDate, String statusId,
            SortOrder sortOrder);

    int getCountOfAllElectronicOrdersByDateAndStatus(Date startDate, Date endDate, String statusId);

    List<ElectronicOrder> getAllElectronicOrdersByTimestampAndStatus(Timestamp startTimestamp, Timestamp endTimestamp,
            String statusId, SortOrder sortOrder);

    int getCountOfElectronicOrdersByTimestampAndStatus(Timestamp startTimestamp, Timestamp endTimestamp,
            String statusId);

    int getCountOfElectronicOrdersByStatusList(List<Integer> statusIds);

    List<ElectronicOrder> getAllElectronicOrdersByStatusList(List<Integer> statusIds, SortOrder sortOrder);

    List<ElectronicOrder> searchForElectronicOrders(ElectronicOrderViewForm form);

    List<ElectronicOrder> searchForStudyElectronicOrders(ElectronicOrderViewForm form);

    /**
     * Recherche combinée : code patient, dates et statut sont chacun des filtres
     * optionnels appliqués ensemble (ET logique), sur le même principe que
     * getAllElectronicOrdersByTimestampAndStatus. Contrairement à
     * searchForElectronicOrders/searchForStudyElectronicOrders (IDENTIFIER vs
     * DATE_STATUS mutuellement exclusifs, et dépendant du store FHIR local pour le
     * code patient), tout se résout ici directement en base, sans appel FHIR.
     */
    List<ElectronicOrder> searchStudyElectronicOrdersCombined(ElectronicOrderViewForm form);
}

/**
 * The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.mozilla.org/MPL/
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>The Original Code is OpenELIS code.
 *
 * <p>Copyright (C) ITECH-CI. All Rights Reserved.
 */
package org.openelisglobal.dataexchange.service.order;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.context.FhirContext;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.ExternalOrderStatus;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.dataexchange.fhir.FhirUtil;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrder;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrderCategorie;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrderOrigine;
import org.openelisglobal.dataexchange.service.order.ReceivedOrderRow.Etape;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.provider.service.ProviderService;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.OrderPriority;
import org.openelisglobal.sample.valueholder.Sample;

/** Fenêtre « Demandes reçues ». Patients fictifs (DEMO, CMU 9992611…). */
@RunWith(MockitoJUnitRunner.Silent.class)
public class ReceivedOrdersServiceTest {

    private static final FhirContext FHIR = FhirContext.forR4();
    private static final String ENTERED = "1";
    private static final String CANCELLED = "2";
    private static final String COMPLETED = "3";
    private static final String NON_CONFORMING = "4";

    @Mock
    private ElectronicOrderService electronicOrderService;
    @Mock
    private IStatusService statusService;
    @Mock
    private SampleService sampleService;
    @Mock
    private PatientService patientService;
    @Mock
    private OrganizationService organizationService;
    @Mock
    private ProviderService providerService;
    @Mock
    private FhirUtil fhirUtil;
    @Mock
    private FhirConfig fhirConfig;

    @InjectMocks
    private ReceivedOrdersServiceImpl service;

    private final List<ElectronicOrder> stored = new ArrayList<>();
    private final ElectronicOrderServiceImpl taskIdReader = new ElectronicOrderServiceImpl();

    @Before
    public void setUp() {
        when(fhirUtil.getFhirParser()).thenAnswer(invocation -> FHIR.newJsonParser());
        when(fhirConfig.getCmuFormat()).thenReturn("");
        when(statusService.getStatusID(ExternalOrderStatus.Entered)).thenReturn(ENTERED);
        when(statusService.getStatusID(ExternalOrderStatus.InProgress)).thenReturn("5");
        when(statusService.getStatusID(ExternalOrderStatus.Cancelled)).thenReturn(CANCELLED);
        when(statusService.getStatusID(ExternalOrderStatus.Completed)).thenReturn(COMPLETED);
        when(statusService.getStatusID(ExternalOrderStatus.NonConforming)).thenReturn(NON_CONFORMING);
        when(electronicOrderService.getTaskId(any()))
                .thenAnswer(invocation -> taskIdReader.getTaskId(invocation.getArgument(0)));
        when(electronicOrderService.getRoutineElectronicOrders(anyList(), any(), any())).thenAnswer(invocation -> {
            List<String> statusIds = invocation.getArgument(0);
            List<ElectronicOrder> out = new ArrayList<>();
            for (ElectronicOrder eOrder : stored) {
                if (statusIds.isEmpty() || statusIds.contains(eOrder.getStatusId())) {
                    out.add(eOrder);
                }
            }
            return out;
        });
    }

    private Patient patient(String lastName, String cmu) {
        Patient patient = new Patient();
        patient.setId(lastName);
        when(patientService.getLastName(patient)).thenReturn(lastName);
        when(patientService.getFirstName(patient)).thenReturn("Patient");
        when(patientService.getCMUNumber(patient)).thenReturn(cmu);
        return patient;
    }

    private ElectronicOrder order(String id, String taskId, String externalId, OrderPriority priority,
            String receivedAt, Patient patient, String testName) {
        ElectronicOrder eOrder = new ElectronicOrder();
        eOrder.setId(id);
        eOrder.setExternalId(externalId);
        eOrder.setStatusId(ENTERED);
        eOrder.setPriority(priority);
        eOrder.setOrderTimestamp(Timestamp.valueOf(receivedAt));
        eOrder.setPatient(patient);
        eOrder.setTestName(testName);
        eOrder.setCategorie(ElectronicOrderCategorie.ROUTINE);
        eOrder.setOrigine(ElectronicOrderOrigine.PLATEFORME);
        eOrder.setData("{\"resourceType\":\"Task\",\"id\":\"" + taskId + "\",\"status\":\"accepted\"}");
        stored.add(eOrder);
        return eOrder;
    }

    private ReceivedOrdersService.Page search(String etape, String recherche) {
        ReceivedOrdersService.Criteria criteria = new ReceivedOrdersService.Criteria();
        criteria.etape = etape;
        criteria.recherche = recherche;
        return service.search(criteria);
    }

    @Test
    public void examsOfTheSameTask_shouldBeOneRequest() {
        Patient demo = patient("DEMO", "9992611000001");
        order("1", "bdms-t1", "bdms-sr1", OrderPriority.ROUTINE, "2026-11-12 08:00:00", demo, "Charge virale");
        order("2", "bdms-t1", "bdms-sr2", OrderPriority.ROUTINE, "2026-11-12 08:00:00", demo, "CD4");

        ReceivedOrdersService.Page page = search(null, null);

        assertEquals(1, page.total);
        ReceivedOrderRow row = page.rows.get(0);
        assertEquals("bdms-t1", row.getRequestNumber());
        assertEquals(2, row.getExams().size());
        assertEquals(Etape.A_TRAITER, row.getEtape());
        assertEquals("9992611000001", row.getCmuNumber());
        assertFalse(row.isProvisionalIdentity());
        assertFalse(row.isReadOnly());
    }

    @Test
    public void sortedByPriorityThenAge() {
        Patient demo = patient("DEMO", "9992611000001");
        order("1", "bdms-old", "bdms-sr1", OrderPriority.ROUTINE, "2026-11-10 08:00:00", demo, "A");
        order("2", "bdms-new-urgent", "bdms-sr2", OrderPriority.STAT, "2026-11-12 08:00:00", demo, "B");
        order("3", "bdms-recent", "bdms-sr3", OrderPriority.ROUTINE, "2026-11-11 08:00:00", demo, "C");

        List<ReceivedOrderRow> rows = search(null, null).rows;

        assertEquals(Arrays.asList("bdms-new-urgent", "bdms-old", "bdms-recent"), Arrays.asList(
                rows.get(0).getRequestNumber(), rows.get(1).getRequestNumber(), rows.get(2).getRequestNumber()));
    }

    @Test
    public void patientWithoutCmu_shouldBeProvisionalIdentity() {
        order("1", "bdms-t1", "bdms-sr1", OrderPriority.ROUTINE, "2026-11-12 08:00:00", patient("DEMO", ""), "A");

        ReceivedOrderRow row = search(null, null).rows.get(0);

        assertTrue(row.isProvisionalIdentity());
        assertNull(row.getCmuNumber());
    }

    @Test
    public void searchByCmuNameOrRequestNumber() {
        order("1", "bdms-t1", "bdms-sr1", OrderPriority.ROUTINE, "2026-11-12 08:00:00",
                patient("DEMOUN", "9992611000001"), "A");
        order("2", "bdms-t2", "bdms-sr2", OrderPriority.ROUTINE, "2026-11-12 09:00:00",
                patient("DEMODEUX", "9992611000002"), "B");

        assertEquals(1, search(null, "999 2611 000 002").total);
        assertEquals("bdms-t2", search(null, "9992611000002").rows.get(0).getRequestNumber());
        assertEquals("bdms-t1", search(null, "demoun").rows.get(0).getRequestNumber());
        assertEquals("bdms-t2", search(null, "bdms-sr2").rows.get(0).getRequestNumber());
        assertEquals(0, search(null, "inconnu").total);
    }

    @Test
    public void takenInCharge_shouldBeReadOnlyWithLabNumber() {
        ElectronicOrder eOrder = order("1", "bdms-t1", "bdms-sr1", OrderPriority.ROUTINE, "2026-11-12 08:00:00",
                patient("DEMO", "9992611000001"), "A");
        eOrder.setStatusId(COMPLETED);
        Sample sample = mock(Sample.class);
        when(sample.getAccessionNumber()).thenReturn("DEMO26000001");
        when(sampleService.getSampleByReferringId("bdms-sr1")).thenReturn(sample);

        assertEquals(0, search(null, null).total);
        ReceivedOrderRow row = search("TOUTES", null).rows.get(0);
        assertEquals(Etape.PRISE_EN_CHARGE, row.getEtape());
        assertEquals("DEMO26000001", row.getLabNumber());
        assertTrue(row.isReadOnly());
    }

    @Test
    public void cancelledByRequester_shouldLeaveToProcessList() {
        ElectronicOrder eOrder = order("1", "bdms-t1", "bdms-sr1", OrderPriority.ROUTINE, "2026-11-12 08:00:00",
                patient("DEMO", "9992611000001"), "A");
        eOrder.setStatusId(CANCELLED);
        eOrder.setRequesterCancelledAt(Timestamp.valueOf("2026-11-12 09:00:00"));

        assertEquals(0, search(null, null).total);
        ReceivedOrderRow row = search("ANNULEE_DEMANDEUR", null).rows.get(0);
        assertEquals(Etape.ANNULEE_DEMANDEUR, row.getEtape());
        assertTrue(row.isCancelledByRequester());
    }

    @Test
    public void refusedRequest_shouldBeRefusee() {
        order("1", "bdms-t1", "bdms-sr1", OrderPriority.ROUTINE, "2026-11-12 08:00:00", patient("DEMO", ""), "A")
                .setStatusId(NON_CONFORMING);

        assertEquals(Etape.REFUSEE, search("TOUTES", null).rows.get(0).getEtape());
    }

    @Test
    public void pagination_andCounter() {
        Patient demo = patient("DEMO", "9992611000001");
        for (int i = 1; i <= 30; i++) {
            order(String.valueOf(i), "bdms-t" + i, "bdms-sr" + i, OrderPriority.ROUTINE,
                    String.format("2026-11-12 08:%02d:00", i), demo, "A");
        }
        ReceivedOrdersService.Criteria criteria = new ReceivedOrdersService.Criteria();
        criteria.page = 2;
        criteria.pageSize = 25;

        ReceivedOrdersService.Page page = service.search(criteria);

        assertEquals(30, page.total);
        assertEquals(5, page.rows.size());
        assertEquals(30, service.countToProcess());
    }
}

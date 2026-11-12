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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.context.FhirContext;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.hl7.fhir.r4.model.Task;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
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
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;

/** « Nouvelle demande » : demande saisie localement. Patients fictifs. */
@RunWith(MockitoJUnitRunner.Silent.class)
public class LocalOrderEntryServiceTest {

    private static final FhirContext FHIR = FhirContext.forR4();
    private static final String OE_SYSTEM = "http://openelis-global.org";

    @Mock
    private ElectronicOrderService electronicOrderService;
    @Mock
    private PatientService patientService;
    @Mock
    private TestService testService;
    @Mock
    private OrganizationService organizationService;
    @Mock
    private IStatusService statusService;
    @Mock
    private FhirUtil fhirUtil;
    @Mock
    private FhirConfig fhirConfig;

    @InjectMocks
    private LocalOrderEntryServiceImpl service;

    @Before
    public void setUp() {
        when(fhirUtil.getFhirParser()).thenAnswer(invocation -> FHIR.newJsonParser());
        when(fhirConfig.getOeFhirSystem()).thenReturn(OE_SYSTEM);
        when(fhirConfig.getVenueIdentifierSystem()).thenReturn(OE_SYSTEM + "/venue");
        when(statusService.getStatusID(ExternalOrderStatus.Entered)).thenReturn("1");
        Patient patient = new Patient();
        patient.setId("101");
        patient.setFhirUuid(UUID.fromString("0f1e2d3c-0000-4000-8000-000000000101"));
        when(patientService.get("101")).thenReturn(patient);
        when(testService.get("12")).thenReturn(test("12", "Glycémie", "Y"));
        when(testService.get("13")).thenReturn(test("13", "Créatinine", "Y"));
        when(testService.get("99")).thenReturn(test("99", "Ancien examen", "N"));
        Organization site = new Organization();
        site.setId("31");
        site.setOrganizationName("CSU DEMO");
        site.setFhirUuid(UUID.fromString("3f6c1a2e-1111-4c4c-9a9a-000000000031"));
        when(organizationService.get("31")).thenReturn(site);
    }

    private static Test test(String id, String name, String active) {
        Test test = new Test();
        test.setId(id);
        test.setName(name);
        test.setIsActive(active);
        return test;
    }

    private static LocalOrderEntryService.LocalOrderForm form(String... testIds) {
        LocalOrderEntryService.LocalOrderForm form = new LocalOrderEntryService.LocalOrderForm();
        form.patientId = "101";
        form.providerLastName = "DEMO";
        form.providerFirstName = "Prescripteur";
        form.requestingOrganizationId = "31";
        form.testIds = Arrays.asList(testIds);
        form.venueNumber = "V-DEMO-001";
        return form;
    }

    private List<ElectronicOrder> created(int count) {
        ArgumentCaptor<ElectronicOrder> orders = ArgumentCaptor.forClass(ElectronicOrder.class);
        verify(electronicOrderService, times(count)).insert(orders.capture());
        return orders.getAllValues();
    }

    @org.junit.Test
    public void create_shouldBeLocalRoutineToProcess_oneOrderPerExam() {
        String requestNumber = service.create(form("12", "13"), "1");

        assertTrue(requestNumber.startsWith(LocalOrderEntryServiceImpl.LOCAL_TASK_PREFIX));
        List<ElectronicOrder> orders = created(2);
        for (ElectronicOrder order : orders) {
            assertEquals(ElectronicOrderOrigine.SAISIE_LOCALE, order.getOrigine());
            assertEquals(ElectronicOrderCategorie.ROUTINE, order.getCategorie());
            assertEquals("1", order.getStatusId());
            assertEquals("CSU DEMO", order.getRequestingFacilityName());
            assertTrue(order.getExternalId().startsWith(requestNumber + "-"));
        }
        Task task = FHIR.newJsonParser().parseResource(Task.class, orders.get(1).getData());
        assertEquals(requestNumber, task.getIdElement().getIdPart());
        assertEquals(OE_SYSTEM + "/test_id", task.getFocus().getIdentifier().getSystem());
        assertEquals("13", task.getFocus().getIdentifier().getValue());
        assertEquals("V-DEMO-001", task.getEncounter().getIdentifier().getValue());
        assertEquals(OE_SYSTEM + "/venue", task.getEncounter().getIdentifier().getSystem());
        assertEquals("Organization/3f6c1a2e-1111-4c4c-9a9a-000000000031",
                task.getRestriction().getRecipientFirstRep().getReference());
        assertEquals("#prescripteur", task.getRequester().getReference());
    }

    @org.junit.Test
    public void create_withoutVenue_shouldHaveNoEncounter() {
        LocalOrderEntryService.LocalOrderForm form = form("12");
        form.venueNumber = " ";

        service.create(form, "1");

        Task task = FHIR.newJsonParser().parseResource(Task.class, created(1).get(0).getData());
        assertFalse(task.hasEncounter());
    }

    @org.junit.Test
    public void create_invalidRequests_shouldBeRefused() {
        expectRefused(form());
        LocalOrderEntryService.LocalOrderForm unknownPatient = form("12");
        unknownPatient.patientId = "999";
        expectRefused(unknownPatient);
        expectRefused(form("99"));
        verify(electronicOrderService, never()).insert(any());
    }

    private void expectRefused(LocalOrderEntryService.LocalOrderForm form) {
        try {
            service.create(form, "1");
            fail("demande refusée attendue");
        } catch (IllegalArgumentException expected) {
            // attendu
        }
    }
}

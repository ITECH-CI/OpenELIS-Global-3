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
package org.openelisglobal.dataexchange.fhir.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.HashSet;
import java.util.UUID;
import org.hl7.fhir.r4.model.Location;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.Specimen;
import org.hl7.fhir.r4.model.Task;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.services.TableIdService;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.dataexchange.fhir.service.FhirPersistanceServiceImpl.FhirOperations;
import org.openelisglobal.observationhistory.service.ObservationHistoryService;
import org.openelisglobal.observationhistory.service.ObservationHistoryServiceImpl.ObservationType;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;

/**
 * Exposition FHIR exploitable par la BDM (pilote) : site demandeur
 * (Organization à UUID, active, référencée par le ServiceRequest) et dates
 * réelles (demande, réception) au lieu de l'heure de la transformation.
 *
 * <p>
 * Tests unitaires (collaborateurs simulés) : dans le contexte Spring de test,
 * {@code FhirTransformService} est lui-même un mock.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class FhirBdmsPiloteTest {

    private static final String OE_SYSTEM = "http://openelis-global.org";
    private static final String REFERRING_ORG_TYPE_ID = "5";
    private static final String REFERRING_DEPARTMENT_TYPE_ID = "10";

    @Mock
    private FhirConfig fhirConfig;
    @Mock
    private OrganizationService organizationService;
    @Mock
    private SampleService sampleService;
    @Mock
    private AnalysisService analysisService;
    @Mock
    private SampleItemService sampleItemService;
    @Mock
    private ObservationHistoryService observationHistoryService;
    @Mock
    private org.openelisglobal.common.services.IStatusService statusService;
    @Mock
    private org.openelisglobal.dataexchange.service.order.ElectronicOrderService electronicOrderService;
    @Mock
    private org.openelisglobal.samplehuman.service.SampleHumanService sampleHumanService;
    @Mock
    private org.openelisglobal.note.service.NoteService noteService;
    @Mock
    private FhirPersistanceService fhirPersistanceService;

    @InjectMocks
    private FhirTransformServiceImpl fhirTransformService;

    private TableIdService previousTableIdService;

    private Sample sample;
    private Analysis analysis;
    private SampleItem sampleItem;

    @Before
    public void setUp() throws Exception {
        when(fhirConfig.getOeFhirSystem()).thenReturn(OE_SYSTEM);
        when(organizationService.get(any())).thenAnswer(invocation -> {
            Organization organization = new Organization();
            organization.setOrganizationTypes(new HashSet<>());
            return organization;
        });

        TableIdService tableIds = mock(TableIdService.class);
        tableIds.REFERRING_ORG_TYPE_ID = REFERRING_ORG_TYPE_ID;
        tableIds.REFERRING_ORG_DEPARTMENT_TYPE_ID = REFERRING_DEPARTMENT_TYPE_ID;
        previousTableIdService = swapTableIdService(tableIds);

        // mock : les setters de date de Sample passent par DateUtil (contexte Spring)
        sample = mock(Sample.class);
        when(sample.getId()).thenReturn("1");
        when(sample.getAccessionNumber()).thenReturn("CHRSP26000001");
        when(sample.getEnteredDate()).thenReturn(java.sql.Date.valueOf("2024-06-03"));
        when(sample.getReceivedTimestamp()).thenReturn(Timestamp.valueOf("2024-06-04 09:30:00"));

        sampleItem = new SampleItem();
        sampleItem.setId("11");
        sampleItem.setSample(sample);
        sampleItem.setSortOrder("1");
        sampleItem.setFhirUuid(UUID.randomUUID());
        sampleItem.setTypeOfSample(mock(TypeOfSample.class));
        when(sampleItemService.get("11")).thenReturn(sampleItem);

        analysis = new Analysis();
        analysis.setId("21");
        analysis.setFhirUuid(UUID.randomUUID());
        analysis.setStatusId("4");
        analysis.setSampleItem(sampleItem);
        when(analysisService.get("21")).thenReturn(analysis);
    }

    @After
    public void tearDown() throws Exception {
        swapTableIdService(previousTableIdService);
    }

    private static TableIdService swapTableIdService(TableIdService replacement) throws Exception {
        Field instance = TableIdService.class.getDeclaredField("INSTANCE");
        instance.setAccessible(true);
        TableIdService previous = (TableIdService) instance.get(null);
        instance.set(null, replacement);
        return previous;
    }

    private Organization organization(String isActive) {
        Organization organization = new Organization();
        organization.setId("31");
        organization.setOrganizationName("CSU Locodjro");
        organization.setIsActive(isActive);
        organization.setFhirUuid(UUID.randomUUID());
        organization.setOrganizationTypes(new HashSet<>());
        return organization;
    }

    @Test
    public void transformToFhirOrganization_activeOrganization_shouldBeActive() throws Exception {
        // valeur lue en base : une autre instance de chaîne que la constante "Y"
        Organization organization = organization(new String("Y"));

        org.hl7.fhir.r4.model.Organization fhirOrganization = fhirTransformService
                .transformToFhirOrganization(organization);

        assertTrue(fhirOrganization.getActive());
        assertEquals(organization.getFhirUuidAsString(), fhirOrganization.getIdElement().getIdPart());
    }

    @Test
    public void transformToFhirOrganization_inactiveOrganization_shouldBeInactive() throws Exception {
        assertFalse(fhirTransformService.transformToFhirOrganization(organization("N")).getActive());
    }

    @Test
    public void transformToFhirOrganization_withoutUuid_shouldGetUuidNotDatabaseId() throws Exception {
        Organization organization = organization("Y");
        organization.setFhirUuid(null);

        org.hl7.fhir.r4.model.Organization fhirOrganization = fhirTransformService
                .transformToFhirOrganization(organization);

        assertEquals(organization.getFhirUuidAsString(), fhirOrganization.getIdElement().getIdPart());
        assertFalse("31".equals(fhirOrganization.getIdElement().getIdPart()));
    }

    @Test
    public void transformToFhirOrganization_defaultShortName_shouldNotBeAnIdentifier() throws Exception {
        String shortNameSystem = OE_SYSTEM + "/org_shortName";
        Organization organization = organization("Y");

        organization.setShortName("\"\"");
        assertTrue(fhirTransformService.transformToFhirOrganization(organization).getIdentifier().stream()
                .noneMatch(identifier -> shortNameSystem.equals(identifier.getSystem())));
        organization.setShortName(" ");
        assertTrue(fhirTransformService.transformToFhirOrganization(organization).getIdentifier().stream()
                .noneMatch(identifier -> shortNameSystem.equals(identifier.getSystem())));
        organization.setShortName("CSUL");
        assertTrue(fhirTransformService.transformToFhirOrganization(organization).getIdentifier().stream().anyMatch(
                identifier -> shortNameSystem.equals(identifier.getSystem()) && "CSUL".equals(identifier.getValue())));
    }

    @Test
    public void transformToServiceRequest_sampleWithRequesterSite_shouldReferenceSiteLocation() {
        Organization site = organization("Y");
        when(sampleService.getOrganizationRequester(sample, REFERRING_ORG_TYPE_ID)).thenReturn(site);

        ServiceRequest serviceRequest = fhirTransformService.transformToServiceRequest("21");

        // Reference(Location) : le HAPI refuse une Organization sur ce champ
        // (HAPI-0931)
        assertEquals(1, serviceRequest.getLocationReference().size());
        assertEquals("Location/" + fhirTransformService.locationIdFor(site),
                serviceRequest.getLocationReferenceFirstRep().getReference());
    }

    @Test
    public void transformToFhirLocation_shouldBeManagedBySiteOrganization() {
        Organization site = organization(new String("Y"));

        Location location = fhirTransformService.transformToFhirLocation(site);

        assertEquals(fhirTransformService.locationIdFor(site), location.getIdElement().getIdPart());
        assertEquals("Organization/" + site.getFhirUuidAsString(), location.getManagingOrganization().getReference());
        assertEquals("CSU Locodjro", location.getName());
        assertEquals(Location.LocationStatus.ACTIVE, location.getStatus());
        assertFalse(site.getFhirUuidAsString().equals(location.getIdElement().getIdPart()));
        // id stable d'une transformation à l'autre
        assertEquals(location.getIdElement().getIdPart(),
                fhirTransformService.transformToFhirLocation(site).getIdElement().getIdPart());
    }

    @Test
    public void transformToFhirLocation_inactiveSite_shouldBeInactive() {
        assertEquals(Location.LocationStatus.INACTIVE,
                fhirTransformService.transformToFhirLocation(organization("N")).getStatus());
    }

    @Test
    public void transformToTask_sampleWithRequesterSite_shouldHaveOrganizationRequester() {
        Organization site = organization("Y");
        when(sampleService.get("1")).thenReturn(sample);
        when(sample.getStatusId()).thenReturn("1");
        when(sampleService.getOrganizationRequester(sample, REFERRING_ORG_TYPE_ID)).thenReturn(site);

        Task task = fhirTransformService.transformToTask("1");

        assertEquals("Organization/" + site.getFhirUuidAsString(), task.getRequester().getReference());
    }

    @Test
    public void transformToServiceRequest_requestDateEntered_shouldBeAuthoredOn() {
        FhirTransformServiceImpl service = spy(fhirTransformService);
        when(observationHistoryService.getValueForSample(ObservationType.REQUEST_DATE, "1")).thenReturn("01/06/2024");
        doReturn(java.sql.Date.valueOf("2024-06-01")).when(service).parseDisplayDate("01/06/2024");

        ServiceRequest serviceRequest = service.transformToServiceRequest("21");

        assertEquals("2024-06-01", serviceRequest.getAuthoredOnElement().getValueAsString());
        assertEquals(TemporalPrecisionEnum.DAY, serviceRequest.getAuthoredOnElement().getPrecision());
    }

    @Test
    public void transformToServiceRequest_enteredYesterday_shouldBeEnteredDateNotToday() {
        ServiceRequest serviceRequest = fhirTransformService.transformToServiceRequest("21");

        assertEquals("2024-06-03", serviceRequest.getAuthoredOnElement().getValueAsString());
    }

    @Test
    public void transformToServiceRequest_unreadableRequestDate_shouldFallBackToEnteredDate() {
        FhirTransformServiceImpl service = spy(fhirTransformService);
        when(observationHistoryService.getValueForSample(eq(ObservationType.REQUEST_DATE), any()))
                .thenReturn("pas une date");
        doThrow(new IllegalArgumentException()).when(service).parseDisplayDate("pas une date");

        assertEquals("2024-06-03", service.transformToServiceRequest("21").getAuthoredOnElement().getValueAsString());
    }

    @Test
    public void transformToServiceRequest_twice_shouldKeepSameAuthoredOn() {
        String first = fhirTransformService.transformToServiceRequest("21").getAuthoredOnElement().getValueAsString();

        assertEquals(first,
                fhirTransformService.transformToServiceRequest("21").getAuthoredOnElement().getValueAsString());
    }

    @Test
    public void transformToSpecimen_receivedTimestamp_shouldBeReceivedTime() {
        Specimen specimen = fhirTransformService.transformToSpecimen("11");

        assertEquals(Timestamp.valueOf("2024-06-04 09:30:00").getTime(), specimen.getReceivedTime().getTime());
    }

    @Test
    public void transformToSpecimen_noReceivedTimestamp_shouldHaveNoReceivedTime() {
        when(sample.getReceivedTimestamp()).thenReturn(null);

        Specimen specimen = fhirTransformService.transformToSpecimen("11");

        assertNull(specimen.getReceivedTime());
        assertFalse(specimen.hasReceivedTime());
    }

    @Test
    public void requestDateOf_noDateAtAll_shouldBeAbsentNotNow() {
        when(sample.getEnteredDate()).thenReturn(null);

        assertNull(fhirTransformService.requestDateOf(sample));
    }

    @Test
    public void transformPersistOrganizations_shouldWriteOrganizationAndItsLocationInOneTransaction() throws Exception {
        Organization site = organization("Y");
        when(organizationService.get("31")).thenReturn(site);
        ArgumentCaptor<FhirOperations> operations = ArgumentCaptor.forClass(FhirOperations.class);

        fhirTransformService.transformPersistOrganizations(Arrays.asList("31"));

        verify(fhirPersistanceService).createUpdateFhirResourcesInFhirStore(operations.capture());
        assertEquals(
                new HashSet<>(Arrays.asList("Organization/" + site.getFhirUuidAsString(),
                        "Location/" + fhirTransformService.locationIdFor(site))),
                operations.getValue().updateResources.keySet());
    }

    @Test
    public void transformPersistOrganization_adminScreen_shouldAlsoWriteLocation() throws Exception {
        Organization site = organization("Y");
        ArgumentCaptor<FhirOperations> operations = ArgumentCaptor.forClass(FhirOperations.class);

        fhirTransformService.transformPersistOrganization(site);

        verify(fhirPersistanceService).createUpdateFhirResourcesInFhirStore(operations.capture());
        assertEquals(2, operations.getValue().updateResources.size());
    }
}

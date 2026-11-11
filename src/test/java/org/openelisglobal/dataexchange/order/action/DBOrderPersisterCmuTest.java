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
package org.openelisglobal.dataexchange.order.action;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrder;
import org.openelisglobal.dataexchange.service.order.ElectronicOrderService;
import org.openelisglobal.patient.service.PatientCmuLookupService;
import org.openelisglobal.patient.service.PatientContactService;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.patientidentity.service.PatientIdentityService;
import org.openelisglobal.patientidentity.valueholder.PatientIdentity;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.sample.valueholder.OrderPriority;

/**
 * Patient d'une demande électronique : un patient existant qui porte le même
 * numéro CMU est repris (aucun doublon, aucune fusion) ; sinon le patient créé
 * reçoit son numéro dans le type d'identité "CMU". Patients fictifs.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class DBOrderPersisterCmuTest {

    private static final String DEMO_CMU = "9992611000002";
    private static final String CMU_TYPE_ID = "77";

    @Mock
    private ElectronicOrderService eOrderService;
    @Mock
    private PatientIdentityService identityService;
    @Mock
    private PersonService personService;
    @Mock
    private PatientService patientService;
    @Mock
    private PatientContactService patientContactService;
    @Mock
    private org.openelisglobal.address.service.PersonAddressService personAddressService;
    @Mock
    private PatientCmuLookupService patientCmuLookupService;

    @InjectMocks
    private DBOrderPersister persister;

    @Before
    public void setUp() throws Exception {
        Field field = DBOrderPersister.class.getDeclaredField("IDENTITY_CMU_ID");
        field.setAccessible(true);
        field.set(persister, CMU_TYPE_ID);
        Field guid = DBOrderPersister.class.getDeclaredField("IDENTITY_GUID_ID");
        guid.setAccessible(true);
        guid.set(persister, "1");
    }

    private static MessagePatient incoming() {
        MessagePatient messagePatient = new MessagePatient();
        messagePatient.setLastName("DEMO");
        messagePatient.setFirstName("Patient");
        messagePatient.setGender("M");
        messagePatient.setCmuNumber(DEMO_CMU);
        messagePatient.setNationalId(DEMO_CMU);
        return messagePatient;
    }

    private static ElectronicOrder order() {
        ElectronicOrder eOrder = new ElectronicOrder();
        eOrder.setExternalId("DEMO-DEMANDE-2");
        eOrder.setPriority(OrderPriority.ROUTINE);
        return eOrder;
    }

    @Test
    public void persist_existingPatientWithSameCmu_shouldBeReused_withoutDuplicate() {
        Patient existing = new Patient();
        existing.setId("42");
        existing.setNationalId(DEMO_CMU);
        Person existingPerson = new Person();
        existingPerson.setLastName("DEMO");
        existing.setPerson(existingPerson);
        when(patientCmuLookupService.findPatientIdsByCmu(DEMO_CMU)).thenReturn(Arrays.asList("42"));
        when(patientService.get("42")).thenReturn(existing);
        when(patientService.getIdentityList(existing)).thenReturn(new ArrayList<>());

        ElectronicOrder eOrder = order();
        persister.persist(incoming(), eOrder);

        assertSame(existing, eOrder.getPatient());
        verify(eOrderService).insert(eOrder);
        verify(patientService, never()).insert(any(Patient.class));
        verify(personService, never()).insert(any(Person.class));
        verify(personService, never()).update(any(Person.class));
        // le numéro CMU manquant est ajouté au patient repris
        ArgumentCaptor<PatientIdentity> identity = ArgumentCaptor.forClass(PatientIdentity.class);
        verify(identityService).insert(identity.capture());
        assertEquals(CMU_TYPE_ID, identity.getValue().getIdentityTypeId());
        assertEquals(DEMO_CMU, identity.getValue().getIdentityData());
        assertEquals("42", identity.getValue().getPatientId());
    }

    @Test
    public void persist_severalPatientsWithSameCmu_shouldReuseOldest_withoutMerge() {
        Patient oldest = new Patient();
        oldest.setId("40");
        oldest.setNationalId(DEMO_CMU);
        when(patientCmuLookupService.findPatientIdsByCmu(DEMO_CMU)).thenReturn(Arrays.asList("40", "43"));
        when(patientService.get("40")).thenReturn(oldest);
        PatientIdentity cmu = new PatientIdentity();
        cmu.setIdentityTypeId(CMU_TYPE_ID);
        cmu.setIdentityData(DEMO_CMU);
        when(patientService.getIdentityList(oldest)).thenReturn(new ArrayList<>(Arrays.asList(cmu)));

        ElectronicOrder eOrder = order();
        persister.persist(incoming(), eOrder);

        assertSame(oldest, eOrder.getPatient());
        verify(patientService, never()).insert(any(Patient.class));
        verify(identityService, never()).insert(any(PatientIdentity.class));
        verify(patientService, never()).update(any(Patient.class));
    }

    @Test
    public void persist_unknownCmu_shouldCreatePatientWithCmuIdentity() {
        when(patientCmuLookupService.findPatientIdsByCmu(DEMO_CMU)).thenReturn(Collections.emptyList());

        ElectronicOrder eOrder = order();
        persister.persist(incoming(), eOrder);

        verify(patientService, times(1)).insert(any(Patient.class));
        ArgumentCaptor<PatientIdentity> identities = ArgumentCaptor.forClass(PatientIdentity.class);
        verify(identityService, times(2)).insert(identities.capture());
        List<PatientIdentity> cmuIdentities = new ArrayList<>();
        for (PatientIdentity identity : identities.getAllValues()) {
            if (CMU_TYPE_ID.equals(identity.getIdentityTypeId())) {
                cmuIdentities.add(identity);
            }
        }
        assertEquals(1, cmuIdentities.size());
        assertEquals(DEMO_CMU, cmuIdentities.get(0).getIdentityData());
        assertEquals(DEMO_CMU, eOrder.getPatient().getNationalId());
    }
}

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
package org.openelisglobal.patient.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.patientidentity.service.PatientIdentityService;
import org.openelisglobal.patientidentity.valueholder.PatientIdentity;

/**
 * Repli de getCMUNumber sur le nationalId : seulement pour une valeur conforme.
 * Numéros fictifs.
 */
public class PatientCmuNumberTest {

    private static final String CMU_TYPE_ID = "77";

    private PatientServiceImpl patientService;
    private PatientIdentityService identityService;
    private Object previousCmuType;

    @Before
    public void setUp() throws Exception {
        patientService = mock(PatientServiceImpl.class, Mockito.CALLS_REAL_METHODS);
        identityService = mock(PatientIdentityService.class);
        setField(patientService, "patientIdentityService", identityService);
        setField(patientService, "cmuFormat", "");
        Field cmuType = PatientServiceImpl.class.getDeclaredField("PATIENT_CMU_IDENTITY");
        cmuType.setAccessible(true);
        previousCmuType = cmuType.get(null);
        cmuType.set(null, CMU_TYPE_ID);
    }

    @After
    public void tearDown() throws Exception {
        Field cmuType = PatientServiceImpl.class.getDeclaredField("PATIENT_CMU_IDENTITY");
        cmuType.setAccessible(true);
        cmuType.set(null, previousCmuType);
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = PatientServiceImpl.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static Patient patient(String nationalId) {
        Patient patient = new Patient();
        patient.setId("101");
        patient.setNationalId(nationalId);
        return patient;
    }

    @Test
    public void getCMUNumber_emptyCmuType_conformingNationalId_shouldBeCmuNumber() {
        when(identityService.getPatitentIdentityForPatientAndType(any(), any())).thenReturn(null);
        assertEquals("9992611000003", patientService.getCMUNumber(patient("999 2611 000 003")));
    }

    @Test
    public void getCMUNumber_emptyCmuType_otherNationalId_shouldBeEmpty() {
        when(identityService.getPatitentIdentityForPatientAndType(any(), any())).thenReturn(null);
        assertEquals("", patientService.getCMUNumber(patient("CNI-DEMO-1")));
    }

    @Test
    public void getCMUNumber_cmuType_shouldWin() {
        PatientIdentity identity = new PatientIdentity();
        identity.setIdentityData("9992611000004");
        when(identityService.getPatitentIdentityForPatientAndType("101", CMU_TYPE_ID)).thenReturn(identity);
        assertEquals("9992611000004", patientService.getCMUNumber(patient("9992611000003")));
    }
}

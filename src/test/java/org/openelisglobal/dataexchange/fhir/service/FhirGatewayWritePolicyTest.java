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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.dataexchange.fhir.dao.FhirGatewayAccessLogDAO;
import org.openelisglobal.dataexchange.fhir.dao.FhirGatewayClientDAO;
import org.openelisglobal.dataexchange.fhir.dao.FhirGatewayTokenDAO;
import org.openelisglobal.dataexchange.fhir.valueholder.FhirGatewayAccessLog;
import org.openelisglobal.dataexchange.fhir.valueholder.FhirGatewayClient;
import org.openelisglobal.dataexchange.fhir.valueholder.FhirGatewayToken;

/**
 * Écriture par la passerelle : PUT /fhir/&lt;Type&gt;/&lt;id&gt; seulement,
 * pour un client autorisé, un type autorisé et un identifiant préfixé ; tout le
 * reste en écriture est refusé (403) et journalisé.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class FhirGatewayWritePolicyTest {

    private static final String TOKEN = "jeton-de-test";

    @Mock
    private FhirGatewayTokenDAO baseObjectDAO;
    @Mock
    private FhirGatewayClientDAO clientDAO;
    @Mock
    private FhirGatewayAccessLogDAO accessLogDAO;

    @InjectMocks
    private FhirGatewayTokenServiceImpl service;

    private FhirGatewayClient client;

    @Before
    public void setUp() {
        client = new FhirGatewayClient();
        client.setId("7");
        client.setName("Plateforme DEMO");
        client.setIsActive("Y");
        client.setAllowedResources("Patient,Specimen,ServiceRequest,Task,DiagnosticReport,Observation");
        FhirGatewayToken token = new FhirGatewayToken();
        token.setClientId("7");
        when(baseObjectDAO.findActiveByTokenHash(anyString())).thenReturn(token);
        when(clientDAO.get("7")).thenReturn(Optional.of(client));
    }

    private void allowWrite(String prefix) {
        client.setAllowWrite("Y");
        client.setWriteIdPrefix(prefix);
    }

    private int access(String method, String uri) {
        return service.authorizeAccess(TOKEN, method, uri);
    }

    @Test
    public void clientWithoutWrite_putShouldBeRefused() {
        assertEquals(403, access("PUT", "/fhir/Task/bdms-1"));
        assertEquals(200, access("GET", "/fhir/Task/bdms-1"));
    }

    @Test
    public void clientWithWrite_putWithPrefixShouldBeAccepted() {
        allowWrite("bdms-");
        assertEquals(200, access("PUT", "/fhir/Task/bdms-1"));
        assertEquals(200, access("PUT", "/fhir/Patient/bdms-p1"));
        assertEquals(200, access("PUT", "/fhir/Specimen/bdms-s1"));
        assertEquals(200, access("PUT", "/fhir/ServiceRequest/bdms-sr1"));
    }

    @Test
    public void clientWithWrite_putOutsidePrefixShouldBeRefused() {
        allowWrite("bdms-");
        assertEquals(403, access("PUT", "/fhir/Task/autre-1"));
        assertEquals(403, access("PUT", "/fhir/Task/bdms-"));
    }

    @Test
    public void clientWithWrite_transactionPostDeletePatchShouldBeRefused() {
        allowWrite("bdms-");
        assertEquals(403, access("POST", "/fhir/"));
        assertEquals(403, access("POST", "/fhir"));
        assertEquals(403, access("POST", "/fhir/Task"));
        assertEquals(403, access("DELETE", "/fhir/Task/bdms-1"));
        assertEquals(403, access("PATCH", "/fhir/Task/bdms-1"));
    }

    @Test
    public void clientWithWrite_putOfUnauthorizedTypeShouldBeRefused() {
        allowWrite("bdms-");
        assertEquals(403, access("PUT", "/fhir/Organization/bdms-1"));
        assertEquals(403, access("PUT", "/fhir/task/bdms-1"));
    }

    @Test
    public void clientWithWrite_emptyTypeListShouldNotAllowWrites() {
        allowWrite("bdms-");
        client.setAllowedResources(null);
        assertEquals(403, access("PUT", "/fhir/Task/bdms-1"));
        assertEquals(200, access("GET", "/fhir/Task/bdms-1"));
    }

    @Test
    public void clientWithWrite_otherUriFormsShouldBeRefused() {
        allowWrite("bdms-");
        assertEquals(403, access("PUT", "/fhir/Task?identifier=bdms-1"));
        assertEquals(403, access("PUT", "/fhir/Task/bdms-1?_format=json"));
        assertEquals(403, access("PUT", "/fhir/Task/bdms-1/_history/2"));
        assertEquals(403, access("PUT", "/fhir/Task/bdms-1/../../Patient/x"));
        assertEquals(403, access("PUT", "/fhir/Task/bdms-%2e%2e"));
        assertEquals(403, access("PUT", "//fhir/Task/bdms-1"));
        assertEquals(403, access("PUT", "/fhir-in/Task/bdms-1"));
        assertEquals(403, access("PUT", "/fhir/Task/bdms-1;x=y"));
    }

    @Test
    public void clientWithWriteButNoPrefix_shouldBeRefused() {
        allowWrite(null);
        assertEquals(403, access("PUT", "/fhir/Task/bdms-1"));
    }

    @Test
    public void writes_shouldBeInAccessLog() {
        allowWrite("bdms-");
        access("PUT", "/fhir/Task/bdms-1");
        access("PUT", "/fhir/Task/autre-1");
        ArgumentCaptor<FhirGatewayAccessLog> logs = ArgumentCaptor.forClass(FhirGatewayAccessLog.class);
        verify(accessLogDAO, org.mockito.Mockito.times(2)).insert(logs.capture());
        assertEquals("PUT", logs.getAllValues().get(0).getMethod());
        assertEquals("Task", logs.getAllValues().get(0).getResourceType());
        assertEquals(Integer.valueOf(200), logs.getAllValues().get(0).getStatus());
        assertEquals(Integer.valueOf(403), logs.getAllValues().get(1).getStatus());
        assertEquals("7", logs.getAllValues().get(1).getClientId());
    }

    @Test
    public void writePolicy_requiresPrefix_andOnlyOneWriter() {
        try {
            service.updateClientWritePolicy("7", true, "");
            fail("préfixe obligatoire");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("préfixe"));
        }
        try {
            service.updateClientWritePolicy("7", true, "bdms/");
            fail("préfixe invalide");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("préfixe"));
        }

        FhirGatewayClient other = new FhirGatewayClient();
        other.setId("8");
        other.setName("Autre DEMO");
        other.setAllowWrite("Y");
        when(clientDAO.getAll()).thenReturn(Arrays.asList(client, other));
        try {
            service.updateClientWritePolicy("7", true, "bdms-");
            fail("un seul client peut écrire");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("un seul client"));
        }
        verify(clientDAO, never()).update(any());

        other.setAllowWrite("N");
        service.updateClientWritePolicy("7", true, " bdms- ");
        assertEquals("Y", client.getAllowWrite());
        assertEquals("bdms-", client.getWriteIdPrefix());
        verify(clientDAO).update(client);

        service.updateClientWritePolicy("7", false, "bdms-");
        assertEquals("N", client.getAllowWrite());
    }
}

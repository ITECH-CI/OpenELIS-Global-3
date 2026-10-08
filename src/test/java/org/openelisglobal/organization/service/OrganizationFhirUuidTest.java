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
package org.openelisglobal.organization.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.organization.dao.OrganizationDAO;
import org.openelisglobal.organization.valueholder.Organization;

/**
 * Toute organisation reçoit un fhir_uuid au point de passage commun des
 * créations (saisie d'échantillon, commande, modification, administration…), et
 * ne change jamais d'UUID.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class OrganizationFhirUuidTest {

    @Mock
    private OrganizationDAO baseObjectDAO;

    @InjectMocks
    private OrganizationServiceImpl organizationService = new OrganizationServiceImpl();

    @Before
    public void setUp() {
        when(baseObjectDAO.duplicateOrganizationExists(any())).thenReturn(false);
        when(baseObjectDAO.insert(any())).thenReturn("31");
        when(baseObjectDAO.update(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private Organization site() {
        Organization organization = new Organization();
        organization.setOrganizationName("Centre de santé créé à la saisie");
        organization.setIsActive("Y");
        return organization;
    }

    @Test
    public void insert_organizationCreatedAtSampleEntry_shouldGetFhirUuid() {
        Organization organization = site();

        organizationService.insert(organization);

        assertNotNull(organization.getFhirUuid());
    }

    @Test
    public void insert_organizationWithUuid_shouldKeepIt() {
        Organization organization = site();
        UUID uuid = UUID.randomUUID();
        organization.setFhirUuid(uuid);

        organizationService.insert(organization);

        assertEquals(uuid, organization.getFhirUuid());
    }

    @Test
    public void update_detachedOrganizationWithoutUuid_shouldKeepUuidInDatabase() {
        UUID uuid = UUID.randomUUID();
        Organization stored = site();
        stored.setId("31");
        stored.setFhirUuid(uuid);
        when(baseObjectDAO.get("31")).thenReturn(Optional.of(stored));
        Organization detached = site();
        detached.setId("31");

        organizationService.update(detached);

        assertEquals(uuid, detached.getFhirUuid());
    }

    @Test
    public void update_legacyOrganizationWithoutUuid_shouldGetOne() {
        Organization legacy = site();
        legacy.setId("31");
        when(baseObjectDAO.get("31")).thenReturn(Optional.of(legacy));

        organizationService.update(legacy);

        assertNotNull(legacy.getFhirUuid());
    }
}

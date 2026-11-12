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
package org.openelisglobal.dataexchange.order.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.dataexchange.service.order.LocalOrderEntryService;
import org.openelisglobal.login.dao.UserModuleService;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.systemusermodule.service.PermissionModuleService;
import org.openelisglobal.systemusermodule.valueholder.PermissionModule;
import org.openelisglobal.userrole.service.UserRoleService;

/** « Nouvelle demande » : réglage et droit « Enregistrer des demandes ». */
@RunWith(MockitoJUnitRunner.Silent.class)
public class LocalOrderEntryRestControllerTest {

    @Mock
    private LocalOrderEntryService localOrderEntryService;
    @Mock
    private FhirConfig fhirConfig;
    @Mock
    private UserModuleService userModuleService;
    @Mock
    private UserRoleService userRoleService;
    @Mock
    private PermissionModuleService<PermissionModule> permissionModuleService;

    @InjectMocks
    private LocalOrderEntryRestController controller;

    private HttpServletRequest request;

    @Before
    public void setUp() {
        request = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);
        UserSessionData usd = mock(UserSessionData.class);
        when(usd.getSystemUserId()).thenReturn(7);
        when(session.getAttribute(IActionConstants.USER_SESSION_DATA)).thenReturn(usd);
        when(request.getSession()).thenReturn(session);
        when(userRoleService.getRoleIdsForUser("7")).thenReturn(Arrays.asList("3"));
    }

    private void userHasRight(boolean right) {
        when(permissionModuleService.getAllPermittedPagesFromAgentId(3)).thenReturn(right
                ? new HashSet<>(Arrays.asList(LocalOrderEntryRestController.MODULE))
                : Collections.emptySet());
    }

    @Test
    public void settingFalse_buttonAbsent_andCreationRefused() {
        when(fhirConfig.isEorderLocalEntryEnabled()).thenReturn(false);
        userHasRight(true);

        assertFalse(controller.settings(request).get("enabled"));
        assertFalse(controller.settings(request).get("permitted"));
        assertEquals(404,
                controller.create(request, new LocalOrderEntryService.LocalOrderForm()).getStatusCode().value());
        verify(localOrderEntryService, never()).create(any(), anyString());
    }

    @Test
    public void userWithoutRight_shouldBeRefused() {
        when(fhirConfig.isEorderLocalEntryEnabled()).thenReturn(true);
        userHasRight(false);

        assertFalse(controller.settings(request).get("permitted"));
        assertEquals(403,
                controller.create(request, new LocalOrderEntryService.LocalOrderForm()).getStatusCode().value());
        verify(localOrderEntryService, never()).create(any(), anyString());
    }

    @Test
    public void userWithRight_shouldCreate() {
        when(fhirConfig.isEorderLocalEntryEnabled()).thenReturn(true);
        userHasRight(true);
        when(localOrderEntryService.create(any(), anyString())).thenReturn("local-demo");

        assertTrue(controller.settings(request).get("permitted"));
        assertEquals(200,
                controller.create(request, new LocalOrderEntryService.LocalOrderForm()).getStatusCode().value());
    }

    @Test
    public void invalidRequest_shouldBe400WithMessage() {
        when(fhirConfig.isEorderLocalEntryEnabled()).thenReturn(true);
        userHasRight(true);
        when(localOrderEntryService.create(any(), anyString()))
                .thenThrow(new IllegalArgumentException("aucun examen demandé"));

        assertEquals("aucun examen demandé",
                controller.create(request, new LocalOrderEntryService.LocalOrderForm()).getBody().get("error"));
    }
}

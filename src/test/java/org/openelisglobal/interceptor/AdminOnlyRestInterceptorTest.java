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
package org.openelisglobal.interceptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.login.dao.UserModuleService;
import org.springframework.util.AntPathMatcher;

/**
 * Administration de la passerelle FHIR et du référentiel : administrateur
 * seulement ; le contrôle d'un jeton de passerelle (/auth) reste ouvert.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class AdminOnlyRestInterceptorTest {

    @Mock
    private UserModuleService userModuleService;

    @InjectMocks
    private AdminOnlyRestInterceptor interceptor;

    @Test
    public void administrator_shouldPass() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(userModuleService.isUserAdmin(request)).thenReturn(true);

        assertTrue(interceptor.preHandle(request, mock(HttpServletResponse.class), null));
    }

    @Test
    public void otherUser_shouldGet403() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));
        when(userModuleService.isUserAdmin(request)).thenReturn(false);

        assertFalse(interceptor.preHandle(request, response, null));
        Mockito.verify(response).setStatus(403);
    }

    private static boolean covered(String path) {
        AntPathMatcher matcher = new AntPathMatcher();
        boolean included = Arrays.stream(AdminOnlyRestInterceptor.ADMIN_ONLY_PATHS)
                .anyMatch(p -> matcher.match(p, path));
        boolean excluded = Arrays.stream(AdminOnlyRestInterceptor.EXCLUDED_PATHS).anyMatch(p -> matcher.match(p, path));
        return included && !excluded;
    }

    @Test
    public void protectedPaths() {
        assertTrue(covered("/rest/fhir-gateway/clients"));
        assertTrue(covered("/rest/fhir-gateway/clients/7/write-policy"));
        assertTrue(covered("/rest/fhir-gateway/clients/7/tokens"));
        assertTrue(covered("/rest/fhir-gateway/tokens/3/revoke"));
        assertTrue(covered("/rest/fhir-gateway/access-log"));
        assertTrue(covered("/rest/organization-referentiel/fusion"));
        assertFalse(covered("/rest/fhir-gateway/auth"));
        assertFalse(covered("/rest/ReceivedOrders"));
        assertEquals(2, AdminOnlyRestInterceptor.ADMIN_ONLY_PATHS.length);
    }
}

package org.openelisglobal.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.openelisglobal.login.dao.UserModuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Administration réservée à l'administrateur : l'intercepteur de modules laisse
 * passer tout utilisateur connecté sur une URL /rest non déclarée pour un
 * module. Appliqué à l'administration de la passerelle FHIR (clients, jetons,
 * politiques d'accès et d'écriture, journal) et à la réconciliation du
 * référentiel des établissements. 403 sinon.
 */
@Component
public class AdminOnlyRestInterceptor implements HandlerInterceptor {

    /** Chemins réservés (le contrôle d'un jeton de passerelle reste ouvert). */
    public static final String[] ADMIN_ONLY_PATHS = { "/rest/fhir-gateway/**", "/rest/organization-referentiel/**" };
    public static final String[] EXCLUDED_PATHS = { "/rest/fhir-gateway/auth" };

    @Autowired
    private UserModuleService userModuleService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        if (userModuleService.isUserAdmin(request)) {
            return true;
        }
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{ \"status\": 403, \"message\": \"Administrateur requis\" }");
        response.getWriter().flush();
        return false;
    }
}

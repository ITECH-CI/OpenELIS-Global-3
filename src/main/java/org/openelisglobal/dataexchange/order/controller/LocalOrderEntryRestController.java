package org.openelisglobal.dataexchange.order.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.dataexchange.service.order.LocalOrderEntryService;
import org.openelisglobal.login.dao.UserModuleService;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.systemusermodule.service.PermissionModuleService;
import org.openelisglobal.systemusermodule.valueholder.PermissionModule;
import org.openelisglobal.userrole.service.UserRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * « Nouvelle demande ». /rest/LocalOrderEntry est déclarée (/LocalOrderEntry)
 * pour le module LocalOrderEntry (droit « Enregistrer des demandes ») :
 * l'intercepteur de modules refuse tout autre utilisateur que ses titulaires et
 * l'administrateur (401). /rest/LocalOrderEntrySettings dit à l'écran s'il doit
 * montrer le bouton.
 */
@RestController
public class LocalOrderEntryRestController {

    public static final String MODULE = "LocalOrderEntry";

    @Autowired
    private LocalOrderEntryService localOrderEntryService;
    @Autowired
    private FhirConfig fhirConfig;
    @Autowired
    private UserModuleService userModuleService;
    @Autowired
    private UserRoleService userRoleService;
    @Autowired
    private PermissionModuleService<PermissionModule> permissionModuleService;

    @GetMapping("/rest/LocalOrderEntrySettings")
    public Map<String, Boolean> settings(HttpServletRequest request) {
        Map<String, Boolean> settings = new HashMap<>();
        settings.put("enabled", fhirConfig.isEorderLocalEntryEnabled());
        settings.put("permitted", fhirConfig.isEorderLocalEntryEnabled() && isPermitted(request));
        return settings;
    }

    @PostMapping("/rest/LocalOrderEntry")
    public ResponseEntity<Map<String, String>> create(HttpServletRequest request,
            @RequestBody LocalOrderEntryService.LocalOrderForm form) {
        Map<String, String> body = new HashMap<>();
        if (!fhirConfig.isEorderLocalEntryEnabled()) {
            body.put("error", "saisie locale des demandes désactivée");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
        }
        // Défense en profondeur : l'intercepteur a déjà refusé un utilisateur sans le
        // droit.
        if (!isPermitted(request)) {
            body.put("error", "droit « Enregistrer des demandes » requis");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
        }
        try {
            body.put("requestNumber", localOrderEntryService.create(form, sysUserId(request)));
            return ResponseEntity.ok(body);
        } catch (IllegalArgumentException e) {
            body.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(body);
        }
    }

    private boolean isPermitted(HttpServletRequest request) {
        if (userModuleService.isUserAdmin(request)) {
            return true;
        }
        String userId = sysUserId(request);
        if (userId == null) {
            return false;
        }
        for (String roleId : userRoleService.getRoleIdsForUser(userId)) {
            if (permissionModuleService.getAllPermittedPagesFromAgentId(Integer.parseInt(roleId)).contains(MODULE)) {
                return true;
            }
        }
        return false;
    }

    private static String sysUserId(HttpServletRequest request) {
        UserSessionData usd = (UserSessionData) request.getSession().getAttribute(IActionConstants.USER_SESSION_DATA);
        return usd == null ? null : String.valueOf(usd.getSystemUserId());
    }
}

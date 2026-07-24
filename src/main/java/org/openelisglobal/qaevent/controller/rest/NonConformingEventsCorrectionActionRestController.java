package org.openelisglobal.qaevent.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.patient.action.bean.PatientSearch;
import org.openelisglobal.qaevent.form.NonConformingEventForm;
import org.openelisglobal.qaevent.service.NCEventService;
import org.openelisglobal.qaevent.valueholder.NcEvent;
import org.openelisglobal.qaevent.worker.NonConformingEventWorker;
import org.openelisglobal.spring.util.SpringContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/rest")
public class NonConformingEventsCorrectionActionRestController extends BaseRestController {

    private NCEventService ncEventService = SpringContext.getBean(NCEventService.class);

    @Autowired
    private NonConformingEventWorker nonConformingEventWorker;

    @GetMapping(value = "/nonconformingcorrectiveaction")
    public ResponseEntity<?> getNCECorrectionActions(@RequestParam(required = false) String labNumber,
            @RequestParam(required = false) String nceNumber, @RequestParam(required = false) String status) {
        NonConformingEventForm nceForm = new NonConformingEventForm();

        Map<String, Object> searchParameters = new HashMap<>();
        List<NcEvent> searchResults = new ArrayList<>();

        searchParameters.put("status", status);

        if (!"".equalsIgnoreCase(labNumber)) {
            searchParameters.put("labOrderNumber", labNumber);
        } else if (!"".equalsIgnoreCase(nceNumber)) {
            searchParameters.put("nceNumber", nceNumber);
        }

        searchResults = ncEventService.getAllMatching(searchParameters);

        nceForm.setnceEventsSearchResults(searchResults);
        nceForm.setReportingUnits(
                DisplayListService.getInstance().getList(DisplayListService.ListType.TEST_SECTION_ACTIVE));

        return ResponseEntity.ok().body(nceForm);
    }

    @GetMapping(value = "/NCECorrectiveAction")
    public ResponseEntity<?> getNCECorrectiveActionForm(@RequestParam(required = true) String nceNumber,
            HttpServletRequest request)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        NonConformingEventForm form = new NonConformingEventForm();

        form.setCurrentUserId(getSysUserId(request));

        form.setPatientSearch(new PatientSearch());

        if (!GenericValidator.isBlankOrNull(nceNumber)) {
            nonConformingEventWorker.initFormForCorrectiveAction(nceNumber, form);
        }

        return ResponseEntity.ok().body(form);
    }

    @PostMapping(value = "/NCECorrectiveAction")
    public ResponseEntity<?> updateNCECorretiveActionForm(@RequestBody NonConformingEventForm form,
            HttpServletRequest request) {
        // L'utilisateur courant est déterminé côté serveur (session) et NON depuis le
        // corps de requête : le front ne le renvoie pas, et le worker s'en sert pour
        // l'audit des lignes d'action corrective (sys_user_id).
        form.setCurrentUserId(getSysUserId(request));
        try {
            boolean updated = nonConformingEventWorker.updateCorrectiveAction(form);
            return ResponseEntity.ok().body(Map.of("success", updated));
        } catch (RuntimeException e) {
            LogEvent.logError(this.getClass().getSimpleName(), "updateNCECorretiveActionForm", e.toString());
            return ResponseEntity.ok().body(Map.of("success", false));
        }
    }

    /**
     * Clôture (résolution) d'un événement de non-conformité : passe le statut à
     * {@code Completed}, enregistre l'action corrective finale et la signature.
     * Sans cet endpoint REST, la résolution n'était atteignable que par un
     * contrôleur MVC mort et un NCE restait bloqué en {@code CAPA} indéfiniment.
     */
    @PostMapping(value = "/ResolveNonConformingEvent")
    public ResponseEntity<?> resolveNonConformingEvent(@RequestBody NonConformingEventForm form,
            HttpServletRequest request) {
        // Idem : currentUserId depuis la session (résolution → signature =
        // nom de l'utilisateur courant, via systemUserService.getUserById).
        form.setCurrentUserId(getSysUserId(request));
        try {
            boolean resolved = nonConformingEventWorker.resolveNCEvent(form);
            return ResponseEntity.ok().body(Map.of("success", resolved));
        } catch (RuntimeException e) {
            LogEvent.logError(this.getClass().getSimpleName(), "resolveNonConformingEvent", e.toString());
            return ResponseEntity.ok().body(Map.of("success", false));
        }
    }
}

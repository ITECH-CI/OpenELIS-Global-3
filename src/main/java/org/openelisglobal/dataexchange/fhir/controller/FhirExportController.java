package org.openelisglobal.dataexchange.fhir.controller;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.hl7.fhir.r4.model.ResourceType;
import org.itech.fhir.dataexport.api.service.DataExportService;
import org.itech.fhir.dataexport.core.model.DataExportTask;
import org.itech.fhir.dataexport.core.service.DataExportTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Pilotage et supervision du <b>push FHIR distant</b> (mini-HIE, sens sortant).
 *
 * <p>
 * OpenELIS pousse périodiquement les ressources FHIR nouvelles/modifiées du
 * store local vers un serveur distant (« subscriber » / Consolidated Server),
 * via le module {@code dataexport} : un {@link DataExportTask} (endpoint +
 * ressources + headers d'auth + intervalle) est créé au démarrage à partir de
 * la propriété {@code org.openelisglobal.fhir.subscriber}, puis un scheduler
 * exporte le delta ({@code lastUpdated}) en Bundle transaction.
 *
 * <p>
 * Ce contrôleur permet de <b>déclencher</b> un export à la demande (POST) et de
 * <b>superviser</b> l'état de chaque cible (GET /status : dernier essai,
 * dernier succès), le module natif n'exposant pas cette visibilité.
 */
@RestController
@RequestMapping("/dataexport/fhir")
public class FhirExportController {

    @Autowired
    private DataExportService dataExportService;
    @Autowired
    private DataExportTaskService dataExportTaskService;

    @PostMapping
    public void runAllDataExportTasks() throws InterruptedException, ExecutionException, TimeoutException {
        for (DataExportTask dataExportTask : dataExportTaskService.getDAO().findAll()) {
            dataExportService.exportNewDataFromLocalToRemote(dataExportTask);
        }
    }

    /**
     * État des cibles de push distant : pour chaque {@link DataExportTask}, son
     * endpoint, les types de ressources poussés, l'intervalle, et l'horodatage du
     * dernier essai / dernier succès. Liste vide = aucun subscriber configuré (push
     * distant inactif).
     */
    @GetMapping("/status")
    public List<Map<String, Object>> exportStatus() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (DataExportTask task : dataExportTaskService.getDAO().findAll()) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", task.getId());
            row.put("endpoint", task.getEndpoint());
            row.put("intervalMinutes", task.getMaxDataExportInterval());
            List<ResourceType> resources = task.getFhirResources();
            row.put("resources", resources == null ? List.of()
                    : resources.stream().map(ResourceType::name).collect(Collectors.toList()));
            Instant lastAttempt = safeInstant(() -> dataExportTaskService.getLatestInstantForDataExportTask(task));
            Instant lastSuccess = safeInstant(
                    () -> dataExportTaskService.getLatestSuccessInstantForDataExportTask(task));
            row.put("lastAttempt", lastAttempt == null ? null : lastAttempt.toString());
            row.put("lastSuccess", lastSuccess == null ? null : lastSuccess.toString());
            out.add(row);
        }
        return out;
    }

    // Les accès "dernier instant" peuvent lever si aucun essai n'existe encore ; on
    // tolère (cible jamais exécutée => null) sans faire échouer la supervision.
    private Instant safeInstant(Supplier<Instant> supplier) {
        try {
            return supplier.get();
        } catch (RuntimeException e) {
            return null;
        }
    }
}

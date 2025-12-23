package org.openelisglobal.nonconformity.controller.rest;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.nonconformity.form.NonConformityForm;
import org.openelisglobal.nonconformity.service.NonConformityService;
import org.openelisglobal.nonconformity.valueholder.NonConformity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
public class NonConformityRestController extends org.openelisglobal.common.rest.BaseRestController {

    @Autowired
    private NonConformityService nonConformityService;

    @GetMapping(value = "/rest/nonconformities", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getAllNonConformities() {
        try {
            List<NonConformity> nonConformities = nonConformityService.getAllNonConformities();
            List<Map<String, Object>> response = new ArrayList<>();

            for (NonConformity nc : nonConformities) {
                Map<String, Object> ncMap = convertToMap(nc);
                response.add(ncMap);
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving non-conformities: " + e.getMessage());
        }
    }

    @GetMapping(value = "/rest/nonconformity", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getNonConformityByNumber(@RequestParam String ncNumber) {
        try {
            NonConformity nc = nonConformityService.getNonConformityByNumber(ncNumber);
            if (nc == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Non-conformity not found with number: " + ncNumber);
            }
            return ResponseEntity.ok(convertToMap(nc));
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving non-conformity: " + e.getMessage());
        }
    }

    @GetMapping(value = "/rest/nonconformities/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> searchNonConformities(
            @RequestParam(required = false) String siteProvenance,
            @RequestParam(required = false) String sampleType,
            @RequestParam(required = false) String rejectionReason,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String status) {
        try {
            Date sqlStartDate = null;
            Date sqlEndDate = null;

            if (startDate != null && !startDate.isEmpty()) {
                sqlStartDate = parseDate(startDate);
            }
            if (endDate != null && !endDate.isEmpty()) {
                sqlEndDate = parseDate(endDate);
            }

            List<NonConformity> nonConformities = nonConformityService.searchNonConformities(
                    siteProvenance, sampleType, rejectionReason, sqlStartDate, sqlEndDate, status);

            List<Map<String, Object>> response = new ArrayList<>();
            for (NonConformity nc : nonConformities) {
                response.add(convertToMap(nc));
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error searching non-conformities: " + e.getMessage());
        }
    }

    @PostMapping(value = "/rest/nonconformity", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> saveNonConformity(@RequestBody NonConformityForm form,
                                               HttpServletRequest request) {
        try {
            System.out.println("ENTER Saving Non-Conformity with data:");
            NonConformity nc;
            boolean isNew = form.getId() == null || form.getId().isEmpty();

            if (isNew) {
                nc = new NonConformity();
                String ncNumber = nonConformityService.generateNextNcNumber();
                nc.setNcNumber(ncNumber);
                nc.setStatus("NEW");
            } else {
                System.out.println("UPDATE ELSE: ");
                nc = nonConformityService.get(form.getId());
                //nc = nonConformityService.getNonConformityByNumber(form.getNcNumber());
                System.out.println("Updating Non-Conformity with ID: " + form.getId());
                if (nc == null) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body("Non-conformity not found with id: " + form.getId());
                }
            }

            // Update fields
            System.out.println("Setting report date: " + form.getReportDate());
            if (form.getReportDate() != null && !form.getReportDate().isEmpty()) {
                nc.setReportDate(parseDate(form.getReportDate()));
            }
            nc.setSiteProvenance(form.getSiteProvenance());
            nc.setSampleType(form.getSampleType());
            nc.setRejectionReason(form.getRejectionReason());
            nc.setComment(form.getComment());
            nc.setReporterName(form.getReporterName());
            nc.setLabNumber(form.getLabNumber());
            nc.setCorrectiveAction(form.getCorrectiveAction());
            if (form.getStatus() != null && !form.getStatus().isEmpty()) {
                nc.setStatus(form.getStatus());
            }

            String currentUserId = getSysUserId(request);
            if (isNew) {
                nc.setCreatedBy(currentUserId);
                nc.setCreatedDate(new Date(System.currentTimeMillis()));
                nonConformityService.insert(nc);
            } else {
                nc.setLastUpdatedBy(currentUserId);
                nc.setLastUpdated(new Date(System.currentTimeMillis()));
                nonConformityService.update(nc);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", isNew ? "Non-conformity created successfully"
                                          : "Non-conformity updated successfully");
            response.put("ncNumber", nc.getNcNumber());
            response.put("id", nc.getId());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            LogEvent.logError(e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error saving non-conformity: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping(value = "/rest/nonconformities/export", produces = "text/csv")
    public ResponseEntity<?> exportNonConformities(
            @RequestParam(required = false) String siteProvenance,
            @RequestParam(required = false) String sampleType,
            @RequestParam(required = false) String rejectionReason,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String status) {
        try {
            Date sqlStartDate = null;
            Date sqlEndDate = null;

            if (startDate != null && !startDate.isEmpty()) {
                sqlStartDate = parseDate(startDate);
            }
            if (endDate != null && !endDate.isEmpty()) {
                sqlEndDate = parseDate(endDate);
            }

            List<NonConformity> nonConformities = nonConformityService.getAllNonConformities();

            StringBuilder csv = new StringBuilder();
            // CSV Header
            csv.append("Numéro NC,Date de Signalement,Site de Provenance,Type d'Échantillon,")
               .append("Raison du Rejet,Commentaire,Rapporteur,Numéro Laboratoire,")
               .append("Action Corrective,Statut\n");

            // CSV Data
            for (NonConformity nc : nonConformities) {
                csv.append(escapeCsv(nc.getNcNumber())).append(",");
                csv.append(escapeCsv(nc.getReportDate() != null ? nc.getReportDate().toString() : "")).append(",");
                csv.append(escapeCsv(nc.getSiteProvenance())).append(",");
                csv.append(escapeCsv(nc.getSampleType())).append(",");
                csv.append(escapeCsv(nc.getRejectionReason())).append(",");
                csv.append(escapeCsv(nc.getComment())).append(",");
                csv.append(escapeCsv(nc.getReporterName())).append(",");
                csv.append(escapeCsv(nc.getLabNumber())).append(",");
                csv.append(escapeCsv(nc.getCorrectiveAction())).append(",");
                csv.append(escapeCsv(nc.getStatus())).append("\n");
            }

            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=non_conformities.csv")
                    .body(csv.toString());
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error exporting non-conformities: " + e.getMessage());
        }
    }

    private Map<String, Object> convertToMap(NonConformity nc) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", nc.getId());
        map.put("ncNumber", nc.getNcNumber());
        map.put("reportDate", nc.getReportDate() != null ? nc.getReportDate().toString() : null);
        map.put("siteProvenance", nc.getSiteProvenance());
        map.put("sampleType", nc.getSampleType());
        map.put("rejectionReason", nc.getRejectionReason());
        map.put("comment", nc.getComment());
        map.put("reporterName", nc.getReporterName());
        map.put("labNumber", nc.getLabNumber());
        map.put("correctiveAction", nc.getCorrectiveAction());
        map.put("status", nc.getStatus());
        map.put("createdDate", nc.getCreatedDate() != null ? nc.getCreatedDate().toString() : null);
        map.put("lastUpdated", nc.getLastUpdated() != null ? nc.getLastUpdated().toString() : null);
        return map;
    }

    private Date parseDate(String dateStr) throws ParseException {
        SimpleDateFormat[] formats = {
            new SimpleDateFormat("yyyy-MM-dd"),
            new SimpleDateFormat("dd/MM/yyyy"),
            new SimpleDateFormat("MM/dd/yyyy")
        };

        for (SimpleDateFormat format : formats) {
            try {
                java.util.Date parsed = format.parse(dateStr);
                return new Date(parsed.getTime());
            } catch (ParseException e) {
                // Try next format
            }
        }
        throw new ParseException("Unable to parse date: " + dateStr, 0);
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

}

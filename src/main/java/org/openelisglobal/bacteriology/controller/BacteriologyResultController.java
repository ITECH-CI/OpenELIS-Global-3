package org.openelisglobal.bacteriology.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.openelisglobal.bacteriology.action.bean.AntibiogramResultBean;
import org.openelisglobal.bacteriology.action.bean.AntibioticDTO;
import org.openelisglobal.bacteriology.action.bean.BacteriologyOrganismBean;
import org.openelisglobal.bacteriology.action.bean.BacteriologyResultForm;
import org.openelisglobal.bacteriology.action.bean.BacteriologyValidationForm;
import org.openelisglobal.bacteriology.service.BacteriologyAntibiogramService;
import org.openelisglobal.bacteriology.service.BacteriologyOrganismService;
import org.openelisglobal.bacteriology.service.BacteriologyResultGroupService;
import org.openelisglobal.bacteriology.service.BacteriologyWorkflowService;
import org.openelisglobal.bacteriology.service.BacteriologyWorkflowService.BacteriologyResultData;
import org.openelisglobal.bacteriology.service.BacteriologyWorkflowService.OrganismWithAntibiogram;
import org.openelisglobal.bacteriology.valueholder.BacteriologyAntibiogram;
import org.openelisglobal.bacteriology.valueholder.BacteriologyOrganism;
import org.openelisglobal.bacteriology.valueholder.BacteriologyResultGroup;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.typeoftestresult.service.TypeOfTestResultServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/bacteriology")
public class BacteriologyResultController extends BaseController {

    @Autowired
    private BacteriologyWorkflowService workflowService;

    @Autowired
    private BacteriologyResultGroupService resultGroupService;

    @Autowired
    private BacteriologyOrganismService organismService;

    @Autowired
    private BacteriologyAntibiogramService antibiogramService;

    @Autowired
    private org.openelisglobal.result.service.ResultService resultService;

    @Autowired
    private org.openelisglobal.analysis.service.AnalysisService analysisService;

    @Autowired
    private org.openelisglobal.testresult.service.TestResultService testResultService;

    @Autowired
    private org.openelisglobal.dictionary.service.DictionaryService dictionaryService;

    @Autowired
    private org.openelisglobal.test.service.TestService testService;

    @Autowired
    private org.openelisglobal.bacteriology.service.BacteriologyFloraService bacteriologyFloraService;

    @Autowired
    private org.openelisglobal.observationhistory.service.ObservationHistoryService observationHistoryService;

    @Autowired
    private org.openelisglobal.sample.service.SampleService sampleService;

    @Autowired
    private org.openelisglobal.unitofmeasure.service.UnitOfMeasureService unitOfMeasureService;

    /**
     * Get all antibiotics from dictionary
     */
    @GetMapping(value = "/antibiotics", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<AntibioticDTO>> getAllAntibiotics() {
        try {
            List<Dictionary> antibiotics = antibiogramService.getAllAntibioticsSorted();
            List<AntibioticDTO> dtoList = antibiotics.stream()
                    .map(dict -> new AntibioticDTO(Integer.parseInt(dict.getId()), dict.getDictEntry(),
                            dict.getLocalAbbreviation(), dict.getSortOrder()))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(dtoList);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get all organism names from dictionary
     */
    @GetMapping(value = "/organisms", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<AntibioticDTO>> getAllOrganisms() {
        try {
            List<Dictionary> organisms = organismService.getAllOrganismNamesSorted();
            List<AntibioticDTO> dtoList = organisms.stream()
                    .map(dict -> new AntibioticDTO(Integer.parseInt(dict.getId()), dict.getDictEntry(),
                            dict.getLocalAbbreviation(), dict.getSortOrder()))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(dtoList);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get bacteriology results for an analysis
     */
    @GetMapping(value = "/results/{analysisId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional(readOnly = true)
    public ResponseEntity<BacteriologyResultData> getBacteriologyResults(
            @PathVariable("analysisId") Integer analysisId,
            @org.springframework.web.bind.annotation.RequestParam(name = "includeFinalized",
                    required = false, defaultValue = "false") boolean includeFinalized) {
        try {
            // Get bacteriology-specific data (organisms, antibiograms)
            BacteriologyResultData resultData = workflowService.getBacteriologyResults(analysisId);

            // Load test results (macroscopy, microscopy, culture) from result table.
            // includeFinalized=true is required by the result-entry / modification pages so
            // already-validated tests remain visible and editable (lab-number / accession
            // search). For the validation page (default), Finalized tests are hidden.
            loadTestResults(analysisId, resultData, includeFinalized);

            // Load biologist's interpretation note (SAMPLE_INTERPRETATION observation),
            // so the validation UI can pre-fill the existing note on mount.
            org.openelisglobal.analysis.valueholder.Analysis analysisForInterp = analysisService
                    .get(String.valueOf(analysisId));
            if (analysisForInterp != null && analysisForInterp.getSampleItem() != null
                    && analysisForInterp.getSampleItem().getSample() != null) {
                String sampleIdForInterp = analysisForInterp.getSampleItem().getSample().getId();
                String existingInterp = observationHistoryService.getValueForSample(
                        org.openelisglobal.observationhistory.service.ObservationHistoryServiceImpl.ObservationType.SAMPLE_INTERPRETATION,
                        sampleIdForInterp);
                resultData.setSampleInterpretation(existingInterp);
            }

            return ResponseEntity.ok(resultData);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Load test results from result table and add to BacteriologyResultData Loads
     * ALL test results for the accessionNumber (not just one analysisId)
     */
    private void loadTestResults(Integer analysisId, BacteriologyResultData resultData, boolean includeFinalized) {
        try {
            org.openelisglobal.analysis.valueholder.Analysis primaryAnalysis = analysisService
                    .get(String.valueOf(analysisId));

            if (primaryAnalysis == null) {
                return;
            }

            // Get ALL analyses for this sample item (all tests: macroscopy, microscopy,
            // culture, etc.)
            List<org.openelisglobal.analysis.valueholder.Analysis> allAnalyses = analysisService
                    .getAnalysesBySampleItem(primaryAnalysis.getSampleItem());

            // Filter out analyses that are already validated (Finalized) unless the caller
            // asked to include them (modification by lab number / accession needs them).
            if (!includeFinalized) {
                IStatusService statusService = SpringContext.getBean(IStatusService.class);
                String finalizedStatusId = statusService
                        .getStatusID(org.openelisglobal.common.services.StatusService.AnalysisStatus.Finalized);

                allAnalyses = allAnalyses.stream().filter(analysis -> {
                    String statusId = analysis.getStatusId();
                    return statusId != null && !statusId.equals(finalizedStatusId);
                }).collect(java.util.stream.Collectors.toList());
            }

            // Collect results from ALL analyses for this sample
            List<org.openelisglobal.result.valueholder.Result> results = new ArrayList<>();
            for (org.openelisglobal.analysis.valueholder.Analysis analysis : allAnalyses) {
                List<org.openelisglobal.result.valueholder.Result> analysisResults = resultService
                        .getResultsByAnalysis(analysis);
                if (analysisResults != null) {
                    results.addAll(analysisResults);
                }
            }

            // Use Maps to store unique results (testId -> test result bean)
            // This automatically deduplicates - if same testId appears multiple times,
            // latest value wins
            java.util.Map<String, BacteriologyWorkflowService.BacteriologyTestResultBean> macroscopyMap = new java.util.LinkedHashMap<>();
            java.util.Map<String, BacteriologyWorkflowService.BacteriologyTestResultBean> microscopyMap = new java.util.LinkedHashMap<>();
            java.util.Map<String, BacteriologyWorkflowService.BacteriologyTestResultBean> cultureMap = new java.util.LinkedHashMap<>();

            // Also build simple Maps (testId -> value) for result entry form
            java.util.Map<String, String> macroscopyResultsMap = new java.util.HashMap<>();
            java.util.Map<String, String> microscopyResultsMap = new java.util.HashMap<>();
            java.util.Map<String, String> cultureResultsMap = new java.util.HashMap<>();

            // Per-result UoM overrides for microscopy (testId -> uom_id). Lets
            // the frontend restore the unit picker selection on reload.
            java.util.Map<String, String> microscopyUomsMap = new java.util.HashMap<>();

            for (org.openelisglobal.result.valueholder.Result result : results) {
                org.openelisglobal.testresult.valueholder.TestResult testResult = result.getTestResult();
                if (testResult == null || testResult.getTest() == null) {
                    continue;
                }

                org.openelisglobal.test.valueholder.Test test = testResult.getTest();
                String testId = test.getId();
                String testName = test.getName();
                String testDescription = test.getDescription();
                String value = result.getValue();

                if (value == null || value.trim().isEmpty()) {
                    continue;
                }

                // Create test result bean with full details
                BacteriologyWorkflowService.BacteriologyTestResultBean testResultBean = new BacteriologyWorkflowService.BacteriologyTestResultBean();
                testResultBean.setAnalysisId(String.valueOf(analysisId));
                testResultBean.setTestId(testId);
                testResultBean.setTestName(testName);
                testResultBean.setTestDescription(testDescription);
                testResultBean.setValue(value);
                testResultBean.setResultType(result.getResultType());
                // Propagate parent/child + flora flags so the frontend can group
                // child tests under their flora-count or conditional parent and
                // avoid rendering them as primary cells.
                testResultBean.setParentTestId(test.getParentTestId());
                testResultBean.setParentTriggerValue(test.getParentTriggerValue());
                testResultBean.setIsFloraCountTest(Boolean.TRUE.equals(test.getIsFloraCountTest()));

                // Get unit of measure if available. Prefer the per-result
                // override (result.uom_id) when set; otherwise fall back to
                // the test's default UoM exposed by resultService.getUOM().
                String overrideUomId = result.getUomId();
                if (overrideUomId != null && !overrideUomId.trim().isEmpty()) {
                    testResultBean.setUomId(overrideUomId);
                    String overrideName = result.getUomName();
                    if (overrideName != null && !overrideName.trim().isEmpty()) {
                        testResultBean.setUnitOfMeasure(overrideName);
                    } else {
                        // Fallback to default if the FK row vanished
                        String uom = resultService.getUOM(result);
                        if (uom != null && !uom.trim().isEmpty()) {
                            testResultBean.setUnitOfMeasure(uom);
                        }
                    }
                } else {
                    String uom = resultService.getUOM(result);
                    if (uom != null && !uom.trim().isEmpty()) {
                        testResultBean.setUnitOfMeasure(uom);
                    }
                }

                // Resolve dictionary value if this is a dictionary result
                String displayValue = value;
                if (TypeOfTestResultServiceImpl.ResultType.DICTIONARY.matches(result.getResultType())) {
                    try {
                        Dictionary dictionary = dictionaryService.getDataForId(value);
                        if (dictionary != null) {
                            displayValue = dictionary.getLocalizedName();
                        }
                    } catch (Exception e) {
                        LogEvent.logWarn("BacteriologyResultController", "loadTestResults",
                                "Could not resolve dictionary value " + value + ": " + e.getMessage());
                    }
                }
                testResultBean.setDisplayValue(displayValue);

                // Categorize by test name - store in Maps (automatically deduplicates)
                if (testName != null) {
                    String lowerName = testName.toLowerCase();
                    if (lowerName.contains("macroscopie")) {
                        macroscopyMap.put(testId, testResultBean);
                        macroscopyResultsMap.put(testId, value);
                    } else if (lowerName.contains("microscopie")) {
                        microscopyMap.put(testId, testResultBean);
                        microscopyResultsMap.put(testId, value);
                        if (testResultBean.getUomId() != null) {
                            microscopyUomsMap.put(testId, testResultBean.getUomId());
                        }
                    } else if (lowerName.contains("culture") || test.getIsCultureTest()) {
                        cultureMap.put(testId, testResultBean);
                        cultureResultsMap.put(testId, value);
                    }
                }
            }

            // Build culture results from bacteriology_result_group table.
            // IMPORTANT: iterate over ALL analyses of the sample item, NOT only those
            // kept after the Finalized filter above. The validation page needs the full
            // list of cultures even when they are already validated, because it uses
            // cultureResults as the lookup table to match each organism to its parent
            // culture (organismGroup.testId == cultureResult.testId). Filtering out
            // Finalized cultures would orphan all already-validated organisms.
            List<org.openelisglobal.analysis.valueholder.Analysis> allAnalysesForCultures = analysisService
                    .getAnalysesBySampleItem(primaryAnalysis.getSampleItem());
            for (org.openelisglobal.analysis.valueholder.Analysis analysis : allAnalysesForCultures) {
                Integer currentAnalysisId = Integer.parseInt(analysis.getId());
                List<BacteriologyResultGroup> cultureGroups = resultGroupService
                        .getGroupsByAnalysisAndType(currentAnalysisId, "CULTURE");

                for (BacteriologyResultGroup cultureGroup : cultureGroups) {
                    Integer testId = cultureGroup.getTestId();
                    if (testId == null) {
                        continue;
                    }

                    // Skip if already processed (deduplicate by testId)
                    if (cultureMap.containsKey(String.valueOf(testId))) {
                        continue;
                    }

                    // Get test details
                    org.openelisglobal.test.valueholder.Test test = testService.get(String.valueOf(testId));
                    if (test == null) {
                        continue;
                    }

                    // Create test result bean for culture
                    BacteriologyWorkflowService.BacteriologyTestResultBean cultureResultBean = new BacteriologyWorkflowService.BacteriologyTestResultBean();
                    cultureResultBean.setAnalysisId(String.valueOf(currentAnalysisId));
                    cultureResultBean.setTestId(String.valueOf(testId));
                    cultureResultBean.setTestName(test.getName());
                    cultureResultBean.setTestDescription(test.getDescription());

                    // Culture result value is determined by whether organisms exist
                    boolean hasOrganisms = resultData.getOrganisms() != null
                            && resultData.getOrganisms().stream().anyMatch(org -> org.getOrganismGroup() != null
                                    && testId.equals(org.getOrganismGroup().getTestId()));

                    String cultureValue = hasOrganisms ? "Positive" : "Negative";
                    cultureResultBean.setValue(cultureValue);
                    cultureResultBean.setDisplayValue(cultureValue);
                    cultureResultBean.setResultType("A"); // Alphanumeric

                    // Add to culture map (deduplicate by testId)
                    cultureMap.put(String.valueOf(testId), cultureResultBean);
                    cultureResultsMap.put(String.valueOf(testId), cultureValue);
                }
            }

            // Inject flora-count tests: their value is NOT stored in the `result` table
            // but in `bacteriology_flora` (count + per-flora details). They are microscopy
            // tests by nature (e.g. "Nombre de flore"), so they show up in microscopyMap.
            for (org.openelisglobal.analysis.valueholder.Analysis analysis : allAnalyses) {
                org.openelisglobal.test.valueholder.Test test = analysis.getTest();
                if (test == null || !Boolean.TRUE.equals(test.getIsFloraCountTest())) {
                    continue;
                }
                String testId = test.getId();
                if (microscopyMap.containsKey(testId)) {
                    // Already populated (shouldn't happen since `result` table is empty for
                    // these tests, but be defensive).
                    continue;
                }
                Integer analysisIdInt;
                try {
                    analysisIdInt = Integer.valueOf(analysis.getId());
                } catch (NumberFormatException nfe) {
                    continue;
                }
                org.openelisglobal.bacteriology.valueholder.BacteriologyFlora flora = bacteriologyFloraService
                        .getByAnalysisIdAndTestId(analysisIdInt, Integer.valueOf(testId));
                String floraCount = flora != null ? flora.getFloraCount() : null;
                if (floraCount == null || floraCount.trim().isEmpty()) {
                    continue;
                }

                // Build a human-readable summary including per-flora details (Gram type,
                // grouping mode, "Capsulé"/"Non Capsulé") so the validation page shows
                // every characteristic that was saved. Newlines are used between the
                // count and each flora line; the frontend renders this cell with
                // white-space: pre-line so they appear as real line breaks.
                String displayValue = floraCount;
                if (flora.getDetails() != null && !flora.getDetails().isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(floraCount);
                    List<org.openelisglobal.bacteriology.valueholder.BacteriologyFloraDetail> sorted = new java.util.ArrayList<>(
                            flora.getDetails());
                    sorted.sort(java.util.Comparator.comparing(
                            d -> d.getFloraNumber() != null ? d.getFloraNumber() : Integer.MAX_VALUE));
                    for (org.openelisglobal.bacteriology.valueholder.BacteriologyFloraDetail detail : sorted) {
                        sb.append("\nFlore ");
                        sb.append(detail.getFloraNumber() != null ? detail.getFloraNumber() : "?");
                        sb.append(" : ");
                        String gram = resolveDictLabel(detail.getGramTypeDictId());
                        String grouping = resolveDictLabel(detail.getGroupingModeDictId());
                        String other = resolveDictLabel(detail.getOtherCharacteristicDictId());
                        List<String> parts = new java.util.ArrayList<>();
                        if (gram != null) parts.add(gram);
                        if (grouping != null) parts.add(grouping);
                        if (other != null) parts.add(other);
                        sb.append(parts.isEmpty() ? "-" : String.join(", ", parts));
                    }
                    displayValue = sb.toString();
                }

                BacteriologyWorkflowService.BacteriologyTestResultBean floraBean = new BacteriologyWorkflowService.BacteriologyTestResultBean();
                floraBean.setAnalysisId(String.valueOf(analysis.getId()));
                floraBean.setTestId(testId);
                floraBean.setTestName(test.getName());
                floraBean.setTestDescription(test.getDescription());
                floraBean.setValue(floraCount);
                floraBean.setDisplayValue(displayValue);
                floraBean.setResultType("A");

                microscopyMap.put(testId, floraBean);
                microscopyResultsMap.put(testId, floraCount);
            }

            // Convert Maps to Lists for validation display
            resultData.setMacroscopyResults(new java.util.ArrayList<>(macroscopyMap.values()));
            resultData.setMicroscopyResults(new java.util.ArrayList<>(microscopyMap.values()));
            resultData.setCultureResults(new java.util.ArrayList<>(cultureMap.values()));

            // Set simple Maps for result entry form
            resultData.setMacroscopyResultsMap(macroscopyResultsMap);
            resultData.setMicroscopyResultsMap(microscopyResultsMap);
            resultData.setCultureResultsMap(cultureResultsMap);
            resultData.setMicroscopyUomsMap(microscopyUomsMap);

        } catch (Exception e) {
            LogEvent.logError("BacteriologyResultController", "loadTestResults",
                    "Error loading test results: " + e.getMessage());
            LogEvent.logError(e);
        }
    }

    /**
     * Resolve a dictionary id to its localized label, returning null when the id
     * is missing or the entry can't be found (so callers can skip the segment).
     */
    private String resolveDictLabel(Integer dictId) {
        if (dictId == null) {
            return null;
        }
        try {
            Dictionary dict = dictionaryService.getDataForId(String.valueOf(dictId));
            if (dict == null) {
                return null;
            }
            String label = dict.getLocalizedName();
            if (label == null || label.trim().isEmpty()) {
                label = dict.getDictEntry();
            }
            return (label == null || label.trim().isEmpty()) ? null : label;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Check if bacteriology results exist for an analysis
     */
    @GetMapping(value = "/results/{analysisId}/exists", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Boolean> hasBacteriologyResults(@PathVariable("analysisId") Integer analysisId) {
        try {
            boolean exists = workflowService.hasBacteriologyResults(analysisId);
            return ResponseEntity.ok(exists);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Save complete bacteriology results
     */
    @PostMapping(value = "/results", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> saveBacteriologyResults(@Valid @RequestBody BacteriologyResultForm form,
            BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body("Validation errors: " + bindingResult.getAllErrors());
        }

        try {
            // Save test results (macroscopy, microscopy, culture) to result table
            // Returns map of testId -> analysisId for correct linking
            Map<String, Integer> testIdToAnalysisIdMap = saveTestResults(form);

            // Convert form to BacteriologyResultData with correct analysis IDs
            BacteriologyResultData resultData = convertFormToResultData(form, testIdToAnalysisIdMap);

            // Save bacteriology-specific data (organisms, antibiograms)
            workflowService.saveBacteriologyResults(form.getAnalysisId(), resultData, form.getSysUserId());

            // Update ALL affected analyses to TechnicalAcceptance so they all appear in
            // validation
            // This includes the primary analysis AND all analyses for tests that had
            // results saved
            java.util.Set<Integer> affectedAnalysisIds = new java.util.HashSet<>(testIdToAnalysisIdMap.values());
            affectedAnalysisIds.add(form.getAnalysisId()); // Ensure primary analysis is included
            for (Integer analysisId : affectedAnalysisIds) {
                updateAnalysisStatusToTechnicalAcceptance(analysisId, form.getSysUserId());
            }

            return ResponseEntity.ok("Bacteriology results saved successfully");
        } catch (IllegalArgumentException e) {
            LogEvent.logError(e);
            return ResponseEntity.badRequest().body("Validation error: " + e.getMessage());
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to save bacteriology results: " + e.getMessage());
        }
    }

    /**
     * Update analysis status to TechnicalAcceptance after results are saved
     */
    private void updateAnalysisStatusToTechnicalAcceptance(Integer analysisId, String sysUserId) {
        try {
            org.openelisglobal.analysis.valueholder.Analysis analysis = analysisService.get(String.valueOf(analysisId));

            if (analysis == null) {
                LogEvent.logError("BacteriologyResultController", "updateAnalysisStatusToTechnicalAcceptance",
                        "Analysis not found: " + analysisId);
                return;
            }

            String currentStatusId = analysis.getStatusId();

            String technicalAcceptanceStatusId = SpringContext.getBean(IStatusService.class)
                    .getStatusID(org.openelisglobal.common.services.StatusService.AnalysisStatus.TechnicalAcceptance);

            // Already in TechnicalAcceptance → skip the write, nothing to do.
            if (technicalAcceptanceStatusId.equals(currentStatusId)) {
                return;
            }

            // Any other status (NotStarted, Finalized, BiologistRejected, ...) must regress
            // to TechnicalAcceptance when results are (re-)saved on this endpoint, so the
            // modified analysis reappears in the biological-validation queue.
            analysis.setStatusId(technicalAcceptanceStatusId);
            analysis.setSysUserId(sysUserId);
            analysisService.update(analysis);

        } catch (Exception e) {
            LogEvent.logError("BacteriologyResultController", "updateAnalysisStatusToTechnicalAcceptance",
                    "Error updating analysis status for analysis " + analysisId + ": " + e.getMessage());
            LogEvent.logError(e);
            e.printStackTrace();
            // Don't throw - status update failure shouldn't prevent result save
        }
    }

    /**
     * Save test results (macroscopy, microscopy, culture) to result table Returns
     * a map of testId to analysisId for correct linking of organisms to culture tests
     */
    private Map<String, Integer> saveTestResults(BacteriologyResultForm form) {
        Map<String, Integer> testIdToAnalysisIdMap = new HashMap<>();

        // Get the primary analysis to extract accession number
        org.openelisglobal.analysis.valueholder.Analysis primaryAnalysis = analysisService
                .get(String.valueOf(form.getAnalysisId()));

        if (primaryAnalysis == null) {
            throw new IllegalArgumentException("Analysis not found: " + form.getAnalysisId());
        }

        // Get accession number from sample
        String accessionNumber = primaryAnalysis.getSampleItem().getSample().getAccessionNumber();

        // Save macroscopy results
        if (form.getMacroscopyResults() != null && !form.getMacroscopyResults().isEmpty()) {
            for (Map.Entry<String, String> entry : form.getMacroscopyResults().entrySet()) {
                String testId = entry.getKey();
                String value = entry.getValue();

                // Skip empty values
                if (value == null || value.trim().isEmpty()) {
                    continue;
                }

                // Find or create the analysis for this specific test
                org.openelisglobal.analysis.valueholder.Analysis analysis = findOrCreateAnalysisForTest(
                        primaryAnalysis.getSampleItem(), testId, form.getSysUserId());
                if (analysis != null) {
                    testIdToAnalysisIdMap.put(testId, Integer.parseInt(analysis.getId()));
                    saveOrUpdateResult(analysis, testId, value, null, form.getSysUserId());
                }
            }
        }

        // Save microscopy results
        if (form.getMicroscopyResults() != null && !form.getMicroscopyResults().isEmpty()) {
            Map<String, String> microscopyUoms = form.getMicroscopyUoms() != null
                    ? form.getMicroscopyUoms()
                    : new HashMap<>();
            for (Map.Entry<String, String> entry : form.getMicroscopyResults().entrySet()) {
                String testId = entry.getKey();
                String value = entry.getValue();

                // Skip empty values, but keep "0" which is a valid numeric value
                if (value == null || value.trim().isEmpty()) {
                    continue;
                }

                // Find or create the analysis for this specific test
                org.openelisglobal.analysis.valueholder.Analysis analysis = findOrCreateAnalysisForTest(
                        primaryAnalysis.getSampleItem(), testId, form.getSysUserId());
                if (analysis != null) {
                    testIdToAnalysisIdMap.put(testId, Integer.parseInt(analysis.getId()));
                    String uomId = microscopyUoms.get(testId);
                    saveOrUpdateResult(analysis, testId, value, uomId, form.getSysUserId());
                }
            }
        }

        // Save culture results from new cultures format
        if (form.getCultures() != null && !form.getCultures().isEmpty()) {
            for (Map.Entry<String, BacteriologyResultForm.CultureData> entry : form.getCultures().entrySet()) {
                String testId = entry.getKey();
                BacteriologyResultForm.CultureData cultureData = entry.getValue();
                String cultureResult = cultureData.getCultureResult();

                // Skip "default" key
                if ("default".equals(testId)) {
                    continue;
                }

                // Skip empty values
                if (cultureResult == null || cultureResult.trim().isEmpty()) {
                    continue;
                }

                // Find or create the analysis for this specific test (culture tests may not exist yet)
                org.openelisglobal.analysis.valueholder.Analysis analysis = findOrCreateAnalysisForTest(
                        primaryAnalysis.getSampleItem(), testId, form.getSysUserId());
                if (analysis != null) {
                    testIdToAnalysisIdMap.put(testId, Integer.parseInt(analysis.getId()));
                    saveOrUpdateResult(analysis, testId, cultureResult, null, form.getSysUserId());
                }
            }
        }
        // Fallback: save from old cultureResult field if cultures is empty
        else if (form.getCultureResult() != null && !form.getCultureResult().trim().isEmpty()) {
            // This is for backward compatibility - we don't have the testId here
            // The culture test would need to be looked up from the analysis
            LogEvent.logWarn("BacteriologyResultController", "saveBacteriologyResults",
                    "Using deprecated cultureResult field. Please use cultures map instead.");
        }

        return testIdToAnalysisIdMap;
    }

    /**
     * Find the analysis for a specific test within an accession number
     */
    private org.openelisglobal.analysis.valueholder.Analysis findAnalysisForTest(String accessionNumber,
            String testId) {
        List<org.openelisglobal.analysis.valueholder.Analysis> analyses = analysisService
                .getAnalysisByAccessionAndTestId(accessionNumber, testId);

        if (analyses == null || analyses.isEmpty()) {
            LogEvent.logWarn("BacteriologyResultController", "findAnalysisForTest",
                    "No analysis found for accessionNumber=" + accessionNumber + ", testId=" + testId);
            return null;
        }

        // Return the first (should be only one for a given accessionNumber + testId
        // combination)
        return analyses.get(0);
    }

    /**
     * Find or create the analysis for a specific test This is needed for
     * conditional child tests that may not have an analysis created yet
     */
    private org.openelisglobal.analysis.valueholder.Analysis findOrCreateAnalysisForTest(
            org.openelisglobal.sampleitem.valueholder.SampleItem sampleItem, String testId, String sysUserId) {

        String accessionNumber = sampleItem.getSample().getAccessionNumber();

        // Try to find existing analysis
        List<org.openelisglobal.analysis.valueholder.Analysis> analyses = analysisService
                .getAnalysisByAccessionAndTestId(accessionNumber, testId);

        if (analyses != null && !analyses.isEmpty()) {
            return analyses.get(0);
        }

        // Analysis doesn't exist - create it (this happens for conditional child tests)
        try {
            org.openelisglobal.analysis.valueholder.Analysis newAnalysis = new org.openelisglobal.analysis.valueholder.Analysis();

            // Get the test
            org.openelisglobal.test.valueholder.Test test = testService.get(testId);
            if (test == null) {
                LogEvent.logError("BacteriologyResultController", "findOrCreateAnalysisForTest",
                        "Test not found: " + testId);
                return null;
            }

            // Set analysis properties
            newAnalysis.setTest(test);
            newAnalysis.setSampleItem(sampleItem);
            newAnalysis.setRevision("0");
            newAnalysis.setIsReportable(test.getIsReportable());
            newAnalysis.setAnalysisType("MANUAL"); // Manual entry for bacteriology
            newAnalysis.setStartedDate(new java.sql.Date(System.currentTimeMillis()));
            newAnalysis.setTestSection(test.getTestSection()); // Set the test section from the test
            newAnalysis.setSysUserId(sysUserId);

            // Set status to NotStarted initially
            String notStartedStatusId = SpringContext.getBean(IStatusService.class)
                    .getStatusID(org.openelisglobal.common.services.StatusService.AnalysisStatus.NotStarted);
            newAnalysis.setStatusId(notStartedStatusId);

            // Save the new analysis
            analysisService.insert(newAnalysis);

            return newAnalysis;
        } catch (Exception e) {
            LogEvent.logError("BacteriologyResultController", "findOrCreateAnalysisForTest",
                    "Failed to create analysis for test " + testId + ": " + e.getMessage());
            LogEvent.logError(e);
            return null;
        }
    }

    /**
     * Save or update a single result
     */
    private void saveOrUpdateResult(org.openelisglobal.analysis.valueholder.Analysis analysis, String testId,
            String value, String uomId, String sysUserId) {
        try {
            // Resolve the optional per-result UoM override once (or null if none/invalid).
            org.openelisglobal.unitofmeasure.valueholder.UnitOfMeasure overrideUom = null;
            if (uomId != null && !uomId.trim().isEmpty()) {
                try {
                    overrideUom = unitOfMeasureService.get(uomId.trim());
                } catch (Exception ex) {
                    LogEvent.logWarn("BacteriologyResultController", "saveOrUpdateResult",
                            "Unknown uomId=" + uomId + " for testId=" + testId + " — ignoring override");
                }
            }

            // Find existing result for this analysis
            List<org.openelisglobal.result.valueholder.Result> existingResults = resultService
                    .getResultsByAnalysis(analysis);

            // Find ALL existing results for this test (there may be duplicates from
            // previous bug)
            List<org.openelisglobal.result.valueholder.Result> resultsForThisTest = new ArrayList<>();
            for (org.openelisglobal.result.valueholder.Result r : existingResults) {
                org.openelisglobal.testresult.valueholder.TestResult tr = r.getTestResult();
                if (tr != null && tr.getTest() != null && testId.equals(tr.getTest().getId())) {
                    resultsForThisTest.add(r);
                }
            }

            // Find available test results for this test
            List<org.openelisglobal.testresult.valueholder.TestResult> availableTestResults = testResultService
                    .getActiveTestResultsByTest(testId);

            if (resultsForThisTest.isEmpty()) {
                // No existing result - create new one
                org.openelisglobal.result.valueholder.Result newResult = new org.openelisglobal.result.valueholder.Result();
                newResult.setAnalysis(analysis);
                newResult.setSysUserId(sysUserId);
                newResult.setValue(value);

                // Determine result type and test result based on availableTestResults
                org.openelisglobal.testresult.valueholder.TestResult matchedTestResult = null;
                String resultType = "A"; // Default to alphanumeric/text

                if (!availableTestResults.isEmpty()) {
                    // Check the result type from TestResult
                    org.openelisglobal.testresult.valueholder.TestResult firstTr = availableTestResults.get(0);
                    String trResultType = firstTr.getTestResultType();

                    if ("D".equals(trResultType) || "M".equals(trResultType)) {
                        // Dictionary or Multiselect - find matching test result by value
                        resultType = trResultType;
                        for (org.openelisglobal.testresult.valueholder.TestResult tr : availableTestResults) {
                            if (value.equals(tr.getValue())) {
                                matchedTestResult = tr;
                                break;
                            }
                        }
                    } else if ("N".equals(trResultType)) {
                        // Numeric - use first test result, value is numeric
                        resultType = "N";
                        matchedTestResult = firstTr;
                    } else {
                        // Text or other - use first test result
                        resultType = trResultType;
                        matchedTestResult = firstTr;
                    }
                }

                newResult.setResultType(resultType);
                if (matchedTestResult != null) {
                    newResult.setTestResult(matchedTestResult);
                }

                // Optional per-result UoM override (null = use test default)
                newResult.setUom(overrideUom);

                resultService.insert(newResult);
            } else if (resultsForThisTest.size() == 1) {
                // Single existing result - update it
                org.openelisglobal.result.valueholder.Result existingResult = resultsForThisTest.get(0);
                existingResult.setValue(value);
                existingResult.setSysUserId(sysUserId);

                // Update result type and test result based on availableTestResults
                if (!availableTestResults.isEmpty()) {
                    org.openelisglobal.testresult.valueholder.TestResult firstTr = availableTestResults.get(0);
                    String trResultType = firstTr.getTestResultType();

                    existingResult.setResultType(trResultType);

                    if ("D".equals(trResultType) || "M".equals(trResultType)) {
                        // Dictionary or Multiselect - find matching test result by value
                        for (org.openelisglobal.testresult.valueholder.TestResult tr : availableTestResults) {
                            if (value.equals(tr.getValue())) {
                                existingResult.setTestResult(tr);
                                break;
                            }
                        }
                    } else {
                        // Numeric, text, or other - use first test result
                        existingResult.setTestResult(firstTr);
                    }
                }

                // Only touch the per-result UoM when the caller passed a uomId:
                // a non-empty value sets/replaces the override, an empty value
                // explicitly clears it; null means "don't touch" (e.g. saves
                // coming from a UI that doesn't expose the picker).
                if (uomId != null) {
                    existingResult.setUom(overrideUom);
                }

                resultService.update(existingResult);
            } else {
                // MULTIPLE existing results (duplicates from previous bug) - clean up!
                LogEvent.logWarn("BacteriologyResultController", "saveOrUpdateResult",
                        "Found " + resultsForThisTest.size() + " duplicate results for test " + testId
                                + ". Keeping first, deleting others.");

                // Keep the first one, delete the rest
                org.openelisglobal.result.valueholder.Result resultToKeep = resultsForThisTest.get(0);
                resultToKeep.setValue(value);
                resultToKeep.setSysUserId(sysUserId);

                // Update result type and test result based on availableTestResults
                if (!availableTestResults.isEmpty()) {
                    org.openelisglobal.testresult.valueholder.TestResult firstTr = availableTestResults.get(0);
                    String trResultType = firstTr.getTestResultType();

                    resultToKeep.setResultType(trResultType);

                    if ("D".equals(trResultType) || "M".equals(trResultType)) {
                        // Dictionary or Multiselect - find matching test result by value
                        for (org.openelisglobal.testresult.valueholder.TestResult tr : availableTestResults) {
                            if (value.equals(tr.getValue())) {
                                resultToKeep.setTestResult(tr);
                                break;
                            }
                        }
                    } else {
                        // Numeric, text, or other - use first test result
                        resultToKeep.setTestResult(firstTr);
                    }
                }

                if (uomId != null) {
                    resultToKeep.setUom(overrideUom);
                }

                resultService.update(resultToKeep);

                // Delete duplicates
                for (int i = 1; i < resultsForThisTest.size(); i++) {
                    org.openelisglobal.result.valueholder.Result duplicate = resultsForThisTest.get(i);
                    resultService.delete(duplicate);
                }
            }
        } catch (Exception e) {
            LogEvent.logError("BacteriologyResultController", "saveOrUpdateResult",
                    "Error saving result for test " + testId + ": " + e.getMessage());
            LogEvent.logError(e);
        }
    }

    /**
     * Create a new organism group
     */
    @PostMapping(value = "/organism/group", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BacteriologyResultGroup> createOrganismGroup(
            @RequestParam("cultureGroupId") Integer cultureGroupId,
            @RequestParam("organismNumber") Integer organismNumber, @RequestParam("analysisId") Integer analysisId,
            @RequestParam("sysUserId") String sysUserId) {
        try {
            BacteriologyResultGroup organismGroup = workflowService.createOrganismGroup(cultureGroupId, organismNumber,
                    analysisId, sysUserId);
            return ResponseEntity.ok(organismGroup);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Save organism identification
     */
    @PostMapping(value = "/organism", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> saveOrganismIdentification(@Valid @RequestBody BacteriologyOrganismBean organismBean,
            BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body("Validation errors: " + bindingResult.getAllErrors());
        }

        try {
            // Convert bean to entity
            BacteriologyOrganism organism = convertBeanToOrganism(organismBean);

            // Save organism
            workflowService.saveOrganismIdentification(organismBean.getOrganismGroupId(), organism, "SYSTEM");

            return ResponseEntity.ok("Organism identification saved successfully");
        } catch (IllegalArgumentException e) {
            LogEvent.logError(e);
            return ResponseEntity.badRequest().body("Validation error: " + e.getMessage());
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to save organism identification: " + e.getMessage());
        }
    }

    /**
     * Save antibiogram results for an organism
     */
    @PostMapping(value = "/antibiogram/{organismId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> saveAntibiogramResults(@PathVariable("organismId") Integer organismId,
            @RequestBody List<@Valid AntibiogramResultBean> antibiogramBeans,
            @RequestParam("sysUserId") String sysUserId) {
        try {
            // Convert beans to entities
            List<BacteriologyAntibiogram> antibiograms = antibiogramBeans.stream().map(this::convertBeanToAntibiogram)
                    .collect(Collectors.toList());

            // Save antibiograms
            workflowService.saveAntibiogramResults(organismId, antibiograms, sysUserId);

            return ResponseEntity.ok("Antibiogram results saved successfully");
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to save antibiogram results: " + e.getMessage());
        }
    }

    /**
     * Delete an organism and its antibiograms
     */
    @DeleteMapping(value = "/organism/{organismGroupId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> deleteOrganism(@PathVariable("organismGroupId") Integer organismGroupId,
            @RequestParam("sysUserId") String sysUserId) {
        try {
            workflowService.deleteOrganism(organismGroupId, sysUserId);
            return ResponseEntity.ok("Organism deleted successfully");
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to delete organism: " + e.getMessage());
        }
    }

    /**
     * Clear all bacteriology results for an analysis
     */
    @DeleteMapping(value = "/results/{analysisId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> clearBacteriologyResults(@PathVariable("analysisId") Integer analysisId,
            @RequestParam("sysUserId") String sysUserId) {
        try {
            workflowService.clearBacteriologyResults(analysisId, sysUserId);
            return ResponseEntity.ok("Bacteriology results cleared successfully");
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to clear bacteriology results: " + e.getMessage());
        }
    }

    /**
     * Validate bacteriology results - mark analysis as finalized or rejected
     */
    @PostMapping(value = "/validate", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ResponseEntity<String> validateBacteriologyResults(@RequestBody BacteriologyValidationForm form,
            HttpServletRequest request) {
        try {
            // Get sysUserId from session
            String sysUserId = getSysUserId(request);
            if (sysUserId == null) {
                sysUserId = "1"; // Fallback to default
            }

            Integer analysisId = form.getAnalysisId();

            if (analysisId == null) {
                return ResponseEntity.badRequest().body("Analysis ID is required");
            }

            // Get the primary analysis to access the sample and accession number
            org.openelisglobal.analysis.valueholder.Analysis primaryAnalysis = analysisService
                    .get(String.valueOf(analysisId));

            if (primaryAnalysis == null) {
                return ResponseEntity.badRequest().body("Analysis not found: " + analysisId);
            }

            String accessionNumber = primaryAnalysis.getSampleItem().getSample().getAccessionNumber();

            // Get status IDs
            IStatusService statusService = SpringContext.getBean(IStatusService.class);
            String finalizedStatusId = statusService
                    .getStatusID(org.openelisglobal.common.services.StatusService.AnalysisStatus.Finalized);
            String notStartedStatusId = statusService
                    .getStatusID(org.openelisglobal.common.services.StatusService.AnalysisStatus.NotStarted);

            // Determine if the analysis is being accepted or rejected
            BacteriologyValidationForm.ValidatedItems validated = form.getValidated();
            BacteriologyValidationForm.RejectedItems rejected = form.getRejected();

            // Collect all testIds that need validation or rejection
            List<String> allValidatedTestIds = new ArrayList<>();
            List<String> allRejectedTestIds = new ArrayList<>();

            if (validated != null) {
                if (validated.getMacroscopy() != null) {
                    allValidatedTestIds.addAll(validated.getMacroscopy());
                }
                if (validated.getMicroscopy() != null) {
                    allValidatedTestIds.addAll(validated.getMicroscopy());
                }
                if (validated.getCulture() != null) {
                    allValidatedTestIds.addAll(validated.getCulture());
                }
            }

            if (rejected != null) {
                if (rejected.getMacroscopy() != null) {
                    allRejectedTestIds.addAll(rejected.getMacroscopy());
                }
                if (rejected.getMicroscopy() != null) {
                    allRejectedTestIds.addAll(rejected.getMicroscopy());
                }
                if (rejected.getCulture() != null) {
                    allRejectedTestIds.addAll(rejected.getCulture());
                }
            }

            if (allValidatedTestIds.isEmpty() && allRejectedTestIds.isEmpty()) {
                return ResponseEntity.badRequest().body("No items selected for validation or rejection");
            }

            int validatedCount = 0;
            int rejectedCount = 0;

            // Process validated tests - set status to Finalized
            for (String testId : allValidatedTestIds) {
                List<org.openelisglobal.analysis.valueholder.Analysis> analyses = analysisService
                        .getAnalysisByAccessionAndTestId(accessionNumber, testId);

                if (analyses != null) {
                    for (org.openelisglobal.analysis.valueholder.Analysis analysis : analyses) {
                        analysis.setStatusId(finalizedStatusId);
                        analysis.setReleasedDate(new java.sql.Date(System.currentTimeMillis()));
                        analysis.setSysUserId(sysUserId);
                        analysisService.update(analysis);
                        validatedCount++;
                    }
                }
            }

            // Process rejected tests - set status to NotStarted to force re-entry
            for (String testId : allRejectedTestIds) {
                List<org.openelisglobal.analysis.valueholder.Analysis> analyses = analysisService
                        .getAnalysisByAccessionAndTestId(accessionNumber, testId);

                if (analyses != null) {
                    for (org.openelisglobal.analysis.valueholder.Analysis analysis : analyses) {
                        analysis.setStatusId(notStartedStatusId);
                        analysis.setSysUserId(sysUserId);
                        analysisService.update(analysis);
                        rejectedCount++;
                    }
                }
            }

            // Persist the biologist's interpretation note (if any) as a SAMPLE_INTERPRETATION
            // observation history attached to the sample. The bacterio report's
            // "Remarques générales du laboratoire" cell reads from this same source.
            String interpretationNote = form.getSampleInterpretation();
            if (interpretationNote != null && primaryAnalysis.getSampleItem() != null
                    && primaryAnalysis.getSampleItem().getSample() != null) {
                String sampleId = primaryAnalysis.getSampleItem().getSample().getId();
                String patientId = sampleService.getPatient(primaryAnalysis.getSampleItem().getSample()) != null
                        ? sampleService.getPatient(primaryAnalysis.getSampleItem().getSample()).getId()
                        : null;
                String typeId = observationHistoryService.getObservationTypeIdForType(
                        org.openelisglobal.observationhistory.service.ObservationHistoryServiceImpl.ObservationType.SAMPLE_INTERPRETATION);
                if (sampleId != null && typeId != null) {
                    List<org.openelisglobal.observationhistory.valueholder.ObservationHistory> existing = observationHistoryService
                            .getAll(null, primaryAnalysis.getSampleItem().getSample(), typeId);
                    String value = interpretationNote.trim();
                    if (existing != null && !existing.isEmpty()) {
                        org.openelisglobal.observationhistory.valueholder.ObservationHistory obs = existing.get(0);
                        obs.setValue(value);
                        obs.setValueType(
                                org.openelisglobal.observationhistory.valueholder.ObservationHistory.ValueType.LITERAL
                                        .getCode());
                        obs.setSysUserId(sysUserId);
                        observationHistoryService.update(obs);
                    } else if (!value.isEmpty()) {
                        org.openelisglobal.observationhistory.valueholder.ObservationHistory obs = new org.openelisglobal.observationhistory.valueholder.ObservationHistory();
                        obs.setSampleId(sampleId);
                        obs.setPatientId(patientId);
                        obs.setObservationHistoryTypeId(typeId);
                        obs.setValue(value);
                        obs.setValueType(
                                org.openelisglobal.observationhistory.valueholder.ObservationHistory.ValueType.LITERAL
                                        .getCode());
                        obs.setSysUserId(sysUserId);
                        observationHistoryService.insert(obs);
                    }
                }
            }

            return ResponseEntity.ok("Bacteriology results validated successfully: " + validatedCount + " validated, "
                    + rejectedCount + " rejected");
        } catch (Exception e) {
            LogEvent.logError("BacteriologyResultController", "validateBacteriologyResults",
                    "Error validating bacteriology results: " + e.getMessage());
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to validate bacteriology results: " + e.getMessage());
        }
    }

    // Helper methods for conversion

    private BacteriologyResultData convertFormToResultData(BacteriologyResultForm form,
            Map<String, Integer> testIdToAnalysisIdMap) {
        BacteriologyResultData resultData = new BacteriologyResultData();
        Timestamp now = new Timestamp(System.currentTimeMillis());

        // Create macroscopy groups if macroscopy results are provided
        if (form.getMacroscopyResults() != null && !form.getMacroscopyResults().isEmpty()) {
            List<BacteriologyResultGroup> macroscopyGroups = new ArrayList<>();
            BacteriologyResultGroup macroscopyGroup = new BacteriologyResultGroup();
            macroscopyGroup.setAnalysisId(form.getAnalysisId());
            macroscopyGroup.setGroupType("MACROSCOPY");
            macroscopyGroup.setDisplayOrder(1);
            macroscopyGroup.setIsActive(true);
            macroscopyGroup.setCreatedDate(now);
            macroscopyGroups.add(macroscopyGroup);
            resultData.setMacroscopyGroups(macroscopyGroups);

            // Note: The actual test result values in macroscopyResults map (testId ->
            // dictionaryId)
            // should be saved to the standard result table via the Result service.
            // This is handled separately from the bacteriology workflow.
        }

        // Create microscopy groups if microscopy results are provided
        if (form.getMicroscopyResults() != null && !form.getMicroscopyResults().isEmpty()) {
            List<BacteriologyResultGroup> microscopyGroups = new ArrayList<>();
            BacteriologyResultGroup microscopyGroup = new BacteriologyResultGroup();
            microscopyGroup.setAnalysisId(form.getAnalysisId());
            microscopyGroup.setGroupType("MICROSCOPY");
            microscopyGroup.setDisplayOrder(1);
            microscopyGroup.setIsActive(true);
            microscopyGroup.setCreatedDate(now);
            microscopyGroups.add(microscopyGroup);
            resultData.setMicroscopyGroups(microscopyGroups);

            // Note: The actual test result values in microscopyResults map (testId ->
            // dictionaryId)
            // should be saved to the standard result table via the Result service.
            // This is handled separately from the bacteriology workflow.
        }

        // Process cultures map (new architecture)
        List<OrganismWithAntibiogram> allOrganisms = new ArrayList<>();
        Map<String, Integer> cultureGroupIdsByTestId = new HashMap<>(); // testId -> cultureGroupId

        if (form.getCultures() != null && !form.getCultures().isEmpty()) {
            // Create a culture group for each test
            for (Map.Entry<String, BacteriologyResultForm.CultureData> entry : form.getCultures().entrySet()) {
                String testId = entry.getKey();
                BacteriologyResultForm.CultureData cultureData = entry.getValue();

                // Skip "default" key (backward compatibility)
                if ("default".equals(testId)) {
                    continue;
                }

                // Get the CORRECT analysisId for this culture test
                Integer cultureAnalysisId = testIdToAnalysisIdMap.get(testId);
                if (cultureAnalysisId == null) {
                    // Fallback to primary analysis if mapping not found
                    cultureAnalysisId = form.getAnalysisId();
                    LogEvent.logWarn("BacteriologyResultController", "convertFormToResultData",
                            "No analysisId found for testId: " + testId + ", using primary analysis");
                }

                // Create a culture group for this specific test
                BacteriologyResultGroup cultureGroup = new BacteriologyResultGroup();
                cultureGroup.setAnalysisId(cultureAnalysisId); // ✓ CORRECT - Use the culture's analysisId
                cultureGroup.setGroupType("CULTURE");
                cultureGroup.setTestId(Integer.parseInt(testId)); // Link to specific test
                cultureGroup.setDisplayOrder(1);
                cultureGroup.setIsActive(true);
                cultureGroup.setCreatedDate(now);

                // We'll save this later in the workflow service, but store it for now
                // Track this culture group by testId for organism linking
                cultureGroupIdsByTestId.put(testId, -1); // Placeholder, will be set after save

                // Convert organisms for this culture
                if (cultureData.getOrganisms() != null) {
                    for (BacteriologyOrganismBean organismBean : cultureData.getOrganisms()) {
                        OrganismWithAntibiogram organismData = new OrganismWithAntibiogram();

                        // Create organism group
                        BacteriologyResultGroup organismGroup = new BacteriologyResultGroup();
                        organismGroup.setAnalysisId(cultureAnalysisId); // ✓ CORRECT - Use the culture's analysisId
                        organismGroup.setGroupType("ORGANISM");
                        organismGroup.setGroupNumber(organismBean.getOrganismNumber());
                        organismGroup.setDisplayOrder(organismBean.getOrganismNumber());
                        organismGroup.setTestId(Integer.parseInt(testId)); // Link organism to test
                        organismGroup.setIsActive(true);
                        organismData.setOrganismGroup(organismGroup);

                        // Set organism
                        BacteriologyOrganism organism = convertBeanToOrganism(organismBean);
                        organismData.setOrganism(organism);

                        // Set antibiograms
                        List<BacteriologyAntibiogram> antibiograms = organismBean.getAntibiograms().stream()
                                .map(this::convertBeanToAntibiogram).collect(Collectors.toList());
                        organismData.setAntibiograms(antibiograms);

                        allOrganisms.add(organismData);
                    }
                }
            }
        } else {
            // Backward compatibility: use old fields if cultures not provided
            // Create culture group if culture result is provided
            if (form.getCultureResult() != null && !form.getCultureResult().isEmpty()) {
                BacteriologyResultGroup cultureGroup = new BacteriologyResultGroup();
                cultureGroup.setAnalysisId(form.getAnalysisId());
                cultureGroup.setGroupType("CULTURE");
                cultureGroup.setDisplayOrder(1);
                cultureGroup.setIsActive(true);
                cultureGroup.setCreatedDate(now);
                resultData.setCultureGroup(cultureGroup);
            }

            // Convert organism beans to workflow data (old way)
            allOrganisms = form.getOrganisms().stream().map(organismBean -> {
                OrganismWithAntibiogram organismData = new OrganismWithAntibiogram();

                // Create organism group
                BacteriologyResultGroup organismGroup = new BacteriologyResultGroup();
                organismGroup.setAnalysisId(form.getAnalysisId());
                organismGroup.setGroupType("ORGANISM");
                organismGroup.setGroupNumber(organismBean.getOrganismNumber());
                organismGroup.setDisplayOrder(organismBean.getOrganismNumber());
                organismGroup.setIsActive(true);
                organismData.setOrganismGroup(organismGroup);

                // Set organism
                BacteriologyOrganism organism = convertBeanToOrganism(organismBean);
                organismData.setOrganism(organism);

                // Set antibiograms
                List<BacteriologyAntibiogram> antibiograms = organismBean.getAntibiograms().stream()
                        .map(this::convertBeanToAntibiogram).collect(Collectors.toList());
                organismData.setAntibiograms(antibiograms);

                return organismData;
            }).collect(Collectors.toList());
        }

        resultData.setOrganisms(allOrganisms);

        return resultData;
    }

    private BacteriologyOrganism convertBeanToOrganism(BacteriologyOrganismBean bean) {
        BacteriologyOrganism organism = new BacteriologyOrganism();
        organism.setId(bean.getId());
        organism.setOrganismNumber(bean.getOrganismNumber());
        organism.setOrganismType(bean.getOrganismType());
        organism.setOrganismNameDictId(bean.getOrganismNameDictId());
        organism.setOrganismNameText(bean.getOrganismNameText());
        organism.setGramType(bean.getGramType());
        organism.setGroupingMode(bean.getGroupingMode());
        organism.setCapsulePresence(bean.getCapsulePresence());
        organism.setOtherCharacteristics(bean.getOtherCharacteristics());
        organism.setIsActive(true);
        return organism;
    }

    private BacteriologyAntibiogram convertBeanToAntibiogram(AntibiogramResultBean bean) {
        BacteriologyAntibiogram antibiogram = new BacteriologyAntibiogram();
        antibiogram.setId(bean.getId());
        antibiogram.setAntibioticDictId(bean.getAntibioticDictId());
        antibiogram.setResult(bean.getResult());
        antibiogram.setDiameterMm(bean.getDiameterMm());
        antibiogram.setMicValue(bean.getMicValue());
        antibiogram.setInterpretationComment(bean.getInterpretationComment());
        antibiogram.setIsActive(true);
        return antibiogram;
    }

    @Override
    protected String findLocalForward(String forward) {
        return forward;
    }

    @Override
    protected String getPageTitleKey() {
        return "bacteriology.result.title";
    }

    @Override
    protected String getPageSubtitleKey() {
        return "bacteriology.result.subtitle";
    }
}

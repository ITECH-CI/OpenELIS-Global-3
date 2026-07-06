package org.openelisglobal.testconfiguration.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.service.TestServiceImpl;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Configure parent/child test relationships (e.g. show 'Densité parasitaire'
 * only when 'Goutte Epaisse' result is 'Positif').
 *
 * The mapping is stored on the test table: - parent_trigger_value on the PARENT
 * test (dictionary id of the triggering result) - parent_test_id on the CHILD
 * test (id of the parent test)
 */
@RestController
@RequestMapping("/rest/conditional-test")
public class ConditionalTestConfigRestController extends BaseRestController {

    @Autowired
    private TestService testService;

    @Autowired
    private TestResultService testResultService;

    @Autowired
    private DictionaryService dictionaryService;

    /**
     * List all active tests (id + localized name). Used to populate parent/child
     * pickers on the configuration page.
     */
    @GetMapping(value = "/tests", produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional(readOnly = true)
    public ResponseEntity<List<Map<String, String>>> listTests() {
        try {
            List<Test> tests = testService.getAllActiveTests(false);
            List<Map<String, String>> out = new ArrayList<>(tests.size());
            for (Test t : tests) {
                Map<String, String> row = new HashMap<>();
                row.put("id", t.getId());
                row.put("name", TestServiceImpl.getUserLocalizedTestName(t));
                row.put("parentTestId", t.getParentTestId());
                row.put("parentTriggerValue", t.getParentTriggerValue());
                out.add(row);
            }
            return ResponseEntity.ok(out);
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Return the available trigger values for a given parent test. For dictionary
     * tests, returns the list of {id, label} from active test_result rows. For
     * other types, returns an empty list (free-text trigger not yet supported).
     */
    @GetMapping(value = "/trigger-values/{testId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional(readOnly = true)
    public ResponseEntity<List<Map<String, String>>> getTriggerValues(@PathVariable("testId") String testId) {
        try {
            List<TestResult> trs = testResultService.getActiveTestResultsByTest(testId);
            List<Map<String, String>> out = new ArrayList<>(trs.size());
            for (TestResult tr : trs) {
                if (!"D".equals(tr.getTestResultType()) && !"M".equals(tr.getTestResultType())) {
                    continue;
                }
                Dictionary d = dictionaryService.getDataForId(tr.getValue());
                if (d == null) {
                    continue;
                }
                Map<String, String> row = new HashMap<>();
                row.put("id", d.getId());
                row.put("label", d.getDictEntry() != null && !d.getDictEntry().isBlank() ? d.getDictEntry()
                        : d.getLocalizedName());
                out.add(row);
            }
            return ResponseEntity.ok(out);
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Persist a parent/trigger/child mapping. - sets parent_trigger_value on the
     * parent test - sets parent_test_id on the child test
     */
    @PostMapping(value = "/mapping", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ResponseEntity<Map<String, String>> saveMapping(HttpServletRequest request,
            @RequestBody Map<String, String> body) {
        try {
            String parentTestId = body.get("parentTestId");
            String childTestId = body.get("childTestId");
            String triggerValue = body.get("triggerValue");

            if (parentTestId == null || parentTestId.isBlank() || childTestId == null || childTestId.isBlank()
                    || triggerValue == null || triggerValue.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "parentTestId, childTestId and triggerValue are required"));
            }
            if (parentTestId.equals(childTestId)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Parent and child must be different tests"));
            }

            Test parent = testService.get(parentTestId);
            Test child = testService.get(childTestId);
            if (parent == null || child == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Test not found"));
            }

            String userId = getSysUserId(request);
            parent.setParentTriggerValue(triggerValue);
            parent.setSysUserId(userId);
            testService.update(parent);

            child.setParentTestId(parentTestId);
            child.setSysUserId(userId);
            testService.update(child);

            return ResponseEntity.ok(Map.of("status", "ok"));
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", String.valueOf(e.getMessage())));
        }
    }

    /**
     * Remove the mapping for a given child test (clear parent_test_id). Optionally
     * clears parent_trigger_value on the former parent if no other child references
     * it.
     */
    @PostMapping(value = "/mapping/clear", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ResponseEntity<Map<String, String>> clearMapping(HttpServletRequest request,
            @RequestBody Map<String, String> body) {
        try {
            String childTestId = body.get("childTestId");
            if (childTestId == null || childTestId.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "childTestId is required"));
            }
            Test child = testService.get(childTestId);
            if (child == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Child test not found"));
            }
            String userId = getSysUserId(request);
            String formerParentId = child.getParentTestId();
            child.setParentTestId(null);
            child.setSysUserId(userId);
            testService.update(child);

            if (formerParentId != null && !formerParentId.isBlank()) {
                List<Test> allTests = testService.getAllActiveTests(false);
                boolean stillReferenced = allTests.stream()
                        .anyMatch(t -> formerParentId.equals(t.getParentTestId()) && !t.getId().equals(childTestId));
                if (!stillReferenced) {
                    Test parent = testService.get(formerParentId);
                    if (parent != null) {
                        parent.setParentTriggerValue(null);
                        parent.setSysUserId(userId);
                        testService.update(parent);
                    }
                }
            }

            return ResponseEntity.ok(Map.of("status", "ok"));
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", String.valueOf(e.getMessage())));
        }
    }
}

package org.openelisglobal.testconfiguration.controller.rest;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.method.service.MethodService;
import org.openelisglobal.method.valueholder.Method;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.service.TestServiceImpl;
import org.openelisglobal.test.valueholder.Test;
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
 * Generic many-to-many mapping between tests and methods, persisted in
 * clinlims.test_method (id, test_id, method_id, is_active).
 */
@RestController
@RequestMapping("/rest/method-test-map")
public class MethodTestMapRestController extends BaseRestController {

    @Autowired
    private TestService testService;

    @Autowired
    private MethodService methodService;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * List all active methods (id + localized name) for picker UI.
     */
    @GetMapping(value = "/methods", produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional(readOnly = true)
    public ResponseEntity<List<Map<String, String>>> listMethods() {
        try {
            List<Method> methods = methodService.getAll();
            List<Map<String, String>> out = new ArrayList<>(methods.size());
            for (Method m : methods) {
                Map<String, String> row = new HashMap<>();
                row.put("id", m.getId());
                String name = m.getLocalization() != null
                        ? m.getLocalization().getLocalizedValue(java.util.Locale.getDefault())
                        : m.getMethodName();
                row.put("name", name != null && !name.isBlank() ? name : m.getMethodName());
                row.put("isActive", m.getIsActive());
                out.add(row);
            }
            return ResponseEntity.ok(out);
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * List all active tests (id + localized name) for picker UI.
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
                out.add(row);
            }
            return ResponseEntity.ok(out);
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Methods currently assigned to a given test.
     */
    /**
     * Full mapping testId -> [methodId, ...]. Used by result entry pages to filter
     * the method dropdown per test without N round-trips.
     */
    @GetMapping(value = "/all", produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, List<String>>> allMappings() {
        try {
            @SuppressWarnings("unchecked")
            List<Object[]> rows = entityManager
                    .createNativeQuery("SELECT test_id, method_id FROM clinlims.test_method WHERE is_active = 'Y'")
                    .getResultList();
            Map<String, List<String>> out = new HashMap<>();
            for (Object[] row : rows) {
                String testId = String.valueOf(row[0]);
                String methodId = String.valueOf(row[1]);
                out.computeIfAbsent(testId, k -> new ArrayList<>()).add(methodId);
            }
            return ResponseEntity.ok(out);
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/methods-for-test/{testId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional(readOnly = true)
    public ResponseEntity<List<String>> methodsForTest(@PathVariable("testId") String testId) {
        try {
            @SuppressWarnings("unchecked")
            List<Object> rows = entityManager
                    .createNativeQuery(
                            "SELECT method_id FROM clinlims.test_method WHERE test_id = :tid AND is_active = 'Y'")
                    .setParameter("tid", Integer.parseInt(testId)).getResultList();
            List<String> ids = new ArrayList<>(rows.size());
            for (Object o : rows) {
                ids.add(String.valueOf(o));
            }
            return ResponseEntity.ok(ids);
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Tests currently assigned to a given method.
     */
    @GetMapping(value = "/tests-for-method/{methodId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional(readOnly = true)
    public ResponseEntity<List<String>> testsForMethod(@PathVariable("methodId") String methodId) {
        try {
            @SuppressWarnings("unchecked")
            List<Object> rows = entityManager
                    .createNativeQuery(
                            "SELECT test_id FROM clinlims.test_method WHERE method_id = :mid AND is_active = 'Y'")
                    .setParameter("mid", Integer.parseInt(methodId)).getResultList();
            List<String> ids = new ArrayList<>(rows.size());
            for (Object o : rows) {
                ids.add(String.valueOf(o));
            }
            return ResponseEntity.ok(ids);
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Replace the set of methods assigned to a given test. Body: {testId,
     * methodIds: [...]}.
     */
    @PostMapping(value = "/save-for-test", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ResponseEntity<Map<String, String>> saveForTest(HttpServletRequest request,
            @RequestBody Map<String, Object> body) {
        try {
            String testIdStr = String.valueOf(body.get("testId"));
            Object raw = body.get("methodIds");
            if (testIdStr == null || testIdStr.isBlank() || "null".equals(testIdStr) || !(raw instanceof List)) {
                return ResponseEntity.badRequest().body(Map.of("error", "testId and methodIds (array) are required"));
            }
            int testId = Integer.parseInt(testIdStr);
            List<Integer> methodIds = parseIds(raw);

            replaceAssignments("test_id", "method_id", testId, methodIds);
            // Activate the methods that were just assigned (so they appear in
            // pickers and result entry forms).
            activateMethods(methodIds);
            return ResponseEntity.ok(Map.of("status", "ok"));
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", String.valueOf(e.getMessage())));
        }
    }

    /**
     * Replace the set of tests assigned to a given method. Body: {methodId,
     * testIds: [...]}.
     */
    @PostMapping(value = "/save-for-method", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ResponseEntity<Map<String, String>> saveForMethod(HttpServletRequest request,
            @RequestBody Map<String, Object> body) {
        try {
            String methodIdStr = String.valueOf(body.get("methodId"));
            Object raw = body.get("testIds");
            if (methodIdStr == null || methodIdStr.isBlank() || "null".equals(methodIdStr) || !(raw instanceof List)) {
                return ResponseEntity.badRequest().body(Map.of("error", "methodId and testIds (array) are required"));
            }
            int methodId = Integer.parseInt(methodIdStr);
            List<Integer> testIds = parseIds(raw);

            replaceAssignments("method_id", "test_id", methodId, testIds);
            // Activate the method itself if it was inactive
            activateMethods(List.of(methodId));
            return ResponseEntity.ok(Map.of("status", "ok"));
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", String.valueOf(e.getMessage())));
        }
    }

    private List<Integer> parseIds(Object raw) {
        @SuppressWarnings("unchecked")
        List<Object> rawList = (List<Object>) raw;
        List<Integer> out = new ArrayList<>(rawList.size());
        for (Object o : rawList) {
            if (o == null)
                continue;
            out.add(Integer.parseInt(String.valueOf(o)));
        }
        return out;
    }

    /**
     * Activate (is_active='Y') the given method ids if they're currently inactive.
     */
    private void activateMethods(List<Integer> methodIds) {
        if (methodIds == null || methodIds.isEmpty()) {
            return;
        }
        entityManager
                .createNativeQuery("UPDATE clinlims.method SET is_active = 'Y' "
                        + "WHERE id IN (:ids) AND (is_active IS NULL OR is_active <> 'Y')")
                .setParameter("ids", methodIds).executeUpdate();
    }

    /**
     * Deactivate any active assignment for the given pivot ID that's not in the new
     * set, then insert (or reactivate) each desired pair.
     *
     * @param pivotCol the side held constant (e.g. "test_id")
     * @param otherCol the side being replaced (e.g. "method_id")
     */
    private void replaceAssignments(String pivotCol, String otherCol, int pivotId, List<Integer> otherIds) {
        // Deactivate rows not in the new set
        if (otherIds.isEmpty()) {
            entityManager
                    .createNativeQuery("UPDATE clinlims.test_method SET is_active = 'N', lastupdated = now() "
                            + "WHERE " + pivotCol + " = :pid AND is_active = 'Y'")
                    .setParameter("pid", pivotId).executeUpdate();
        } else {
            entityManager
                    .createNativeQuery(
                            "UPDATE clinlims.test_method SET is_active = 'N', lastupdated = now() " + "WHERE "
                                    + pivotCol + " = :pid AND " + otherCol + " NOT IN (:oids) " + "AND is_active = 'Y'")
                    .setParameter("pid", pivotId).setParameter("oids", otherIds).executeUpdate();
        }

        for (Integer otherId : otherIds) {
            int updated = entityManager
                    .createNativeQuery("UPDATE clinlims.test_method SET is_active = 'Y', lastupdated = now() "
                            + "WHERE " + pivotCol + " = :pid AND " + otherCol + " = :oid")
                    .setParameter("pid", pivotId).setParameter("oid", otherId).executeUpdate();
            if (updated == 0) {
                entityManager
                        .createNativeQuery("INSERT INTO clinlims.test_method (id, " + pivotCol + ", " + otherCol
                                + ", is_active, lastupdated) "
                                + "VALUES (nextval('clinlims.test_method_seq'), :pid, :oid, 'Y', now())")
                        .setParameter("pid", pivotId).setParameter("oid", otherId).executeUpdate();
            }
        }
    }
}

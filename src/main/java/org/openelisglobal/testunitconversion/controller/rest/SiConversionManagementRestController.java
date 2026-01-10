package org.openelisglobal.testunitconversion.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testunitconversion.dao.TestAliasDAO;
import org.openelisglobal.testunitconversion.dao.TestDerivedDependencyDAO;
import org.openelisglobal.testunitconversion.dao.TestDerivedFormulaDAO;
import org.openelisglobal.testunitconversion.dao.TestUnitConversionDAO;
import org.openelisglobal.testunitconversion.valueholder.TestAlias;
import org.openelisglobal.testunitconversion.valueholder.TestDerivedDependency;
import org.openelisglobal.testunitconversion.valueholder.TestDerivedFormula;
import org.openelisglobal.testunitconversion.valueholder.TestUnitConversion;
import org.openelisglobal.unitofmeasure.service.UnitOfMeasureService;
import org.openelisglobal.unitofmeasure.valueholder.UnitOfMeasure;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing SI unit conversions. Provides CRUD operations
 * for: - Simple unit conversions (TestUnitConversion) - Derived formulas
 * (TestDerivedFormula) - Test aliases (TestAlias)
 */
@RestController
@RequestMapping("/rest/si-conversion-management")
public class SiConversionManagementRestController extends BaseRestController {

    @Autowired
    private TestUnitConversionDAO testUnitConversionDAO;

    @Autowired
    private TestDerivedFormulaDAO testDerivedFormulaDAO;

    @Autowired
    private TestDerivedDependencyDAO testDerivedDependencyDAO;

    @Autowired
    private TestAliasDAO testAliasDAO;

    @Autowired
    private TestService testService;

    @Autowired
    private UnitOfMeasureService unitOfMeasureService;

    /**
     * Get initialization data for the management page. Returns all tests, UOMs, and
     * existing conversions.
     */
    @GetMapping
    public ResponseEntity<SiConversionManagementForm> getInitialData(HttpServletRequest request) {
        try {
            SiConversionManagementForm form = new SiConversionManagementForm();

            // Get all tests
            List<Test> allTests = testService.getAllTests(false);
            form.setTests(allTests.stream().map(t -> new IdValuePair(t.getId(), t.getDescription()))
                    .collect(Collectors.toList()));

            // Get all UOMs
            List<UnitOfMeasure> allUoms = unitOfMeasureService.getAll();
            form.setUnitOfMeasures(allUoms.stream().map(u -> new IdValuePair(u.getId(), u.getUnitOfMeasureName()))
                    .collect(Collectors.toList()));

            // Get all simple conversions
            List<TestUnitConversion> conversions = testUnitConversionDAO.getAll();
            form.setSimpleConversions(
                    conversions.stream().map(this::toSimpleConversionDTO).collect(Collectors.toList()));

            // Get all derived formulas
            List<TestDerivedFormula> formulas = testDerivedFormulaDAO.getAll();
            form.setDerivedFormulas(formulas.stream().map(this::toDerivedFormulaDTO).collect(Collectors.toList()));

            // Get all test aliases
            List<TestAlias> aliases = testAliasDAO.getAll();
            form.setTestAliases(aliases.stream().map(this::toTestAliasDTO).collect(Collectors.toList()));

            return ResponseEntity.ok(form);
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "getInitialData",
                    "Error getting initial data: " + e.getMessage());
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Save or update a simple unit conversion.
     */
    @PostMapping("/simple-conversion")
    public ResponseEntity<ResponseMessage> saveSimpleConversion(@RequestBody SimpleConversionRequest request,
            HttpServletRequest httpRequest) {
        try {
            String userId = getSysUserId(httpRequest);

            TestUnitConversion conversion;
            boolean isNew = request.getId() == null || request.getId().isEmpty();

            if (isNew) {
                conversion = new TestUnitConversion();
                conversion.setSysUserId(userId);
            } else {
                // Reload the entity to get a managed instance
                conversion = testUnitConversionDAO.get(request.getId())
                        .orElseThrow(() -> new RuntimeException("Conversion not found"));
            }

            // Validate and set properties
            Test test = testService.get(request.getTestId());
            if (test == null) {
                return ResponseEntity.badRequest().body(new ResponseMessage("Invalid test ID", false));
            }
            conversion.setTest(test);

            if (request.getFromUomId() != null && !request.getFromUomId().isEmpty()) {
                UnitOfMeasure fromUom = unitOfMeasureService.get(request.getFromUomId());
                conversion.setFromUom(fromUom);
            }

            if (request.getToUomId() != null) {
                UnitOfMeasure toUom = unitOfMeasureService.get(request.getToUomId());
                conversion.setToUom(toUom);
            }

            conversion.setFactor(request.getFactor());
            conversion.setOffsetValue(request.getOffsetValue());
            conversion.setDecimals(request.getDecimals());
            conversion.setActive(request.getActive() != null ? request.getActive() : true);
            conversion.setSysUserId(userId);

            if (isNew) {
                testUnitConversionDAO.insert(conversion);
            } else {
                testUnitConversionDAO.update(conversion);
            }

            return ResponseEntity.ok(new ResponseMessage(
                    isNew ? "Conversion created successfully" : "Conversion updated successfully", true));
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "saveSimpleConversion",
                    "Error saving conversion: " + e.getMessage());
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ResponseMessage("Error saving conversion: " + e.getMessage(), false));
        }
    }

    /**
     * Delete (deactivate) a simple conversion.
     */
    @DeleteMapping("/simple-conversion/{id}")
    public ResponseEntity<ResponseMessage> deleteSimpleConversion(@PathVariable String id) {
        try {
            Optional<TestUnitConversion> opt = testUnitConversionDAO.get(id);
            if (!opt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ResponseMessage("Conversion not found", false));
            }

            TestUnitConversion conversion = opt.get();
            conversion.setActive(false);
            conversion.setLastupdated(new Timestamp(System.currentTimeMillis()));
            testUnitConversionDAO.update(conversion);

            return ResponseEntity.ok(new ResponseMessage("Conversion deactivated successfully", true));
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "deleteSimpleConversion",
                    "Error deleting conversion: " + e.getMessage());
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ResponseMessage("Error deleting conversion: " + e.getMessage(), false));
        }
    }

    /**
     * Save or update a derived formula.
     */
    @PostMapping("/derived-formula")
    public ResponseEntity<ResponseMessage> saveDerivedFormula(@RequestBody DerivedFormulaRequest request,
            HttpServletRequest httpRequest) {
        try {
            String userId = getSysUserId(httpRequest);

            TestDerivedFormula formula;
            boolean isNew = request.getId() == null || request.getId().isEmpty();

            if (isNew) {
                formula = new TestDerivedFormula();
                formula.setSysUserId(userId);
            } else {
                // Reload the entity to get a managed instance
                formula = testDerivedFormulaDAO.get(request.getId())
                        .orElseThrow(() -> new RuntimeException("Formula not found"));
            }

            // Validate and set properties
            Test test = testService.get(request.getTestId());
            if (test == null) {
                return ResponseEntity.badRequest().body(new ResponseMessage("Invalid test ID", false));
            }
            formula.setTest(test);

            formula.setExpression(request.getExpression());

            if (request.getFromUomId() != null && !request.getFromUomId().isEmpty()) {
                UnitOfMeasure fromUom = unitOfMeasureService.get(request.getFromUomId());
                formula.setFromUom(fromUom);
            }

            if (request.getToUomSiId() != null) {
                UnitOfMeasure toUomSi = unitOfMeasureService.get(request.getToUomSiId());
                formula.setToUomSi(toUomSi);
            }

            formula.setDecimals(request.getDecimals());
            formula.setActive(request.getActive() != null ? request.getActive() : true);
            formula.setSysUserId(userId);

            if (isNew) {
                testDerivedFormulaDAO.insert(formula);
            } else {
                testDerivedFormulaDAO.update(formula);
            }

            // Update dependencies
            if (request.getDependencies() != null) {
                // Delete existing dependencies
                if (!isNew) {
                    List<TestDerivedDependency> existingDeps = testDerivedDependencyDAO
                            .findByFormulaId(formula.getId());
                    for (TestDerivedDependency dep : existingDeps) {
                        testDerivedDependencyDAO.delete(dep);
                    }
                }

                // Create new dependencies
                for (DependencyDTO depDTO : request.getDependencies()) {
                    TestDerivedDependency dependency = new TestDerivedDependency();
                    dependency.setDerivedFormula(formula);

                    Test sourceTest = testService.get(depDTO.getSourceTestId());
                    if (sourceTest == null) {
                        return ResponseEntity.badRequest().body(
                                new ResponseMessage("Invalid source test ID: " + depDTO.getSourceTestId(), false));
                    }
                    dependency.setSourceTest(sourceTest);
                    dependency.setSysUserId(userId);

                    testDerivedDependencyDAO.insert(dependency);
                }
            }

            return ResponseEntity.ok(
                    new ResponseMessage(isNew ? "Formula created successfully" : "Formula updated successfully", true));
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "saveDerivedFormula",
                    "Error saving formula: " + e.getMessage());
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ResponseMessage("Error saving formula: " + e.getMessage(), false));
        }
    }

    /**
     * Delete (deactivate) a derived formula.
     */
    @DeleteMapping("/derived-formula/{id}")
    public ResponseEntity<ResponseMessage> deleteDerivedFormula(@PathVariable String id) {
        try {
            Optional<TestDerivedFormula> opt = testDerivedFormulaDAO.get(id);
            if (!opt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ResponseMessage("Formula not found", false));
            }

            TestDerivedFormula formula = opt.get();
            formula.setActive(false);
            formula.setLastupdated(new Timestamp(System.currentTimeMillis()));
            testDerivedFormulaDAO.update(formula);

            return ResponseEntity.ok(new ResponseMessage("Formula deactivated successfully", true));
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "deleteDerivedFormula",
                    "Error deleting formula: " + e.getMessage());
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ResponseMessage("Error deleting formula: " + e.getMessage(), false));
        }
    }

    /**
     * Save or update a test alias.
     */
    @PostMapping("/test-alias")
    public ResponseEntity<ResponseMessage> saveTestAlias(@RequestBody TestAliasRequest request,
            HttpServletRequest httpRequest) {
        try {
            String userId = getSysUserId(httpRequest);

            TestAlias alias;
            boolean isNew = request.getId() == null || request.getId().isEmpty();

            if (isNew) {
                alias = new TestAlias();
                alias.setSysUserId(userId);
            } else {
                // Reload the entity to get a managed instance
                alias = testAliasDAO.get(request.getId())
                        .orElseThrow(() -> new RuntimeException("Test alias not found"));
            }

            // Validate and set properties
            Test test = testService.get(request.getTestId());
            if (test == null) {
                return ResponseEntity.badRequest().body(new ResponseMessage("Invalid test ID", false));
            }
            alias.setTest(test);
            alias.setAlias(request.getAlias());
            alias.setSysUserId(userId);

            if (isNew) {
                testAliasDAO.insert(alias);
            } else {
                testAliasDAO.update(alias);
            }

            return ResponseEntity.ok(new ResponseMessage(
                    isNew ? "Test alias created successfully" : "Test alias updated successfully", true));
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "saveTestAlias",
                    "Error saving test alias: " + e.getMessage());
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ResponseMessage("Error saving test alias: " + e.getMessage(), false));
        }
    }

    /**
     * Delete a test alias.
     */
    @DeleteMapping("/test-alias/{id}")
    public ResponseEntity<ResponseMessage> deleteTestAlias(@PathVariable String id) {
        try {
            Optional<TestAlias> opt = testAliasDAO.get(id);
            if (!opt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ResponseMessage("Test alias not found", false));
            }

            TestAlias alias = opt.get();
            testAliasDAO.delete(alias);

            return ResponseEntity.ok(new ResponseMessage("Test alias deleted successfully", true));
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "deleteTestAlias",
                    "Error deleting test alias: " + e.getMessage());
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ResponseMessage("Error deleting test alias: " + e.getMessage(), false));
        }
    }

    /**
     * Get dependencies for a specific formula.
     */
    @GetMapping("/derived-formula/{id}/dependencies")
    public ResponseEntity<List<DependencyDTO>> getFormulaDependencies(@PathVariable String id) {
        try {
            List<TestDerivedDependency> dependencies = testDerivedDependencyDAO.findByFormulaId(id);

            List<DependencyDTO> dtos = dependencies.stream().map(this::toDependencyDTO).collect(Collectors.toList());

            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "getFormulaDependencies",
                    "Error getting dependencies: " + e.getMessage());
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // DTO conversion methods

    private SimpleConversionDTO toSimpleConversionDTO(TestUnitConversion conversion) {
        SimpleConversionDTO dto = new SimpleConversionDTO();
        dto.setId(conversion.getId());
        dto.setTestId(conversion.getTest() != null ? conversion.getTest().getId() : null);
        dto.setTestName(conversion.getTest() != null ? conversion.getTest().getDescription() : null);
        dto.setFromUomId(conversion.getFromUom() != null ? conversion.getFromUom().getId() : null);
        dto.setFromUomName(conversion.getFromUom() != null ? conversion.getFromUom().getUnitOfMeasureName() : null);
        dto.setToUomId(conversion.getToUom() != null ? conversion.getToUom().getId() : null);
        dto.setToUomName(conversion.getToUom() != null ? conversion.getToUom().getUnitOfMeasureName() : null);
        dto.setFactor(conversion.getFactor());
        dto.setOffsetValue(conversion.getOffsetValue());
        dto.setDecimals(conversion.getDecimals());
        dto.setActive(conversion.getActive());
        return dto;
    }

    private DerivedFormulaDTO toDerivedFormulaDTO(TestDerivedFormula formula) {
        DerivedFormulaDTO dto = new DerivedFormulaDTO();
        dto.setId(formula.getId());
        dto.setTestId(formula.getTest() != null ? formula.getTest().getId() : null);
        dto.setTestName(formula.getTest() != null ? formula.getTest().getDescription() : null);
        dto.setExpression(formula.getExpression());
        dto.setFromUomId(formula.getFromUom() != null ? formula.getFromUom().getId() : null);
        dto.setFromUomName(formula.getFromUom() != null ? formula.getFromUom().getUnitOfMeasureName() : null);
        dto.setToUomSiId(formula.getToUomSi() != null ? formula.getToUomSi().getId() : null);
        dto.setToUomSiName(formula.getToUomSi() != null ? formula.getToUomSi().getUnitOfMeasureName() : null);
        dto.setDecimals(formula.getDecimals());
        dto.setActive(formula.getActive());

        // Get dependencies
        List<TestDerivedDependency> dependencies = testDerivedDependencyDAO.findByFormulaId(formula.getId());
        dto.setDependencies(dependencies.stream().map(this::toDependencyDTO).collect(Collectors.toList()));

        return dto;
    }

    private TestAliasDTO toTestAliasDTO(TestAlias alias) {
        TestAliasDTO dto = new TestAliasDTO();
        dto.setId(alias.getId());
        dto.setTestId(alias.getTest() != null ? alias.getTest().getId() : null);
        dto.setTestName(alias.getTest() != null ? alias.getTest().getDescription() : null);
        dto.setAlias(alias.getAlias());
        dto.setActive(true); // TestAlias doesn't have active field, always true if exists
        return dto;
    }

    private DependencyDTO toDependencyDTO(TestDerivedDependency dependency) {
        DependencyDTO dto = new DependencyDTO();
        dto.setId(dependency.getId());
        dto.setSourceTestId(dependency.getSourceTest() != null ? dependency.getSourceTest().getId() : null);
        dto.setSourceTestName(dependency.getSourceTest() != null ? dependency.getSourceTest().getDescription() : null);
        // TestAlias is looked up separately through the test_alias table
        dto.setTestAlias("");
        return dto;
    }

    // DTOs and Request/Response classes

    public static class SiConversionManagementForm {
        private List<IdValuePair> tests;
        private List<IdValuePair> unitOfMeasures;
        private List<SimpleConversionDTO> simpleConversions;
        private List<DerivedFormulaDTO> derivedFormulas;
        private List<TestAliasDTO> testAliases;

        public List<IdValuePair> getTests() {
            return tests;
        }

        public void setTests(List<IdValuePair> tests) {
            this.tests = tests;
        }

        public List<IdValuePair> getUnitOfMeasures() {
            return unitOfMeasures;
        }

        public void setUnitOfMeasures(List<IdValuePair> unitOfMeasures) {
            this.unitOfMeasures = unitOfMeasures;
        }

        public List<SimpleConversionDTO> getSimpleConversions() {
            return simpleConversions;
        }

        public void setSimpleConversions(List<SimpleConversionDTO> simpleConversions) {
            this.simpleConversions = simpleConversions;
        }

        public List<DerivedFormulaDTO> getDerivedFormulas() {
            return derivedFormulas;
        }

        public void setDerivedFormulas(List<DerivedFormulaDTO> derivedFormulas) {
            this.derivedFormulas = derivedFormulas;
        }

        public List<TestAliasDTO> getTestAliases() {
            return testAliases;
        }

        public void setTestAliases(List<TestAliasDTO> testAliases) {
            this.testAliases = testAliases;
        }
    }

    public static class SimpleConversionDTO {
        private String id;
        private String testId;
        private String testName;
        private String fromUomId;
        private String fromUomName;
        private String toUomId;
        private String toUomName;
        private BigDecimal factor;
        private BigDecimal offsetValue;
        private Integer decimals;
        private Boolean active;

        // Getters and setters
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getTestId() {
            return testId;
        }

        public void setTestId(String testId) {
            this.testId = testId;
        }

        public String getTestName() {
            return testName;
        }

        public void setTestName(String testName) {
            this.testName = testName;
        }

        public String getFromUomId() {
            return fromUomId;
        }

        public void setFromUomId(String fromUomId) {
            this.fromUomId = fromUomId;
        }

        public String getFromUomName() {
            return fromUomName;
        }

        public void setFromUomName(String fromUomName) {
            this.fromUomName = fromUomName;
        }

        public String getToUomId() {
            return toUomId;
        }

        public void setToUomId(String toUomId) {
            this.toUomId = toUomId;
        }

        public String getToUomName() {
            return toUomName;
        }

        public void setToUomName(String toUomName) {
            this.toUomName = toUomName;
        }

        public BigDecimal getFactor() {
            return factor;
        }

        public void setFactor(BigDecimal factor) {
            this.factor = factor;
        }

        public BigDecimal getOffsetValue() {
            return offsetValue;
        }

        public void setOffsetValue(BigDecimal offsetValue) {
            this.offsetValue = offsetValue;
        }

        public Integer getDecimals() {
            return decimals;
        }

        public void setDecimals(Integer decimals) {
            this.decimals = decimals;
        }

        public Boolean getActive() {
            return active;
        }

        public void setActive(Boolean active) {
            this.active = active;
        }
    }

    public static class DerivedFormulaDTO {
        private String id;
        private String testId;
        private String testName;
        private String expression;
        private String fromUomId;
        private String fromUomName;
        private String toUomSiId;
        private String toUomSiName;
        private Integer decimals;
        private Boolean active;
        private List<DependencyDTO> dependencies;

        // Getters and setters
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getTestId() {
            return testId;
        }

        public void setTestId(String testId) {
            this.testId = testId;
        }

        public String getTestName() {
            return testName;
        }

        public void setTestName(String testName) {
            this.testName = testName;
        }

        public String getExpression() {
            return expression;
        }

        public void setExpression(String expression) {
            this.expression = expression;
        }

        public String getFromUomId() {
            return fromUomId;
        }

        public void setFromUomId(String fromUomId) {
            this.fromUomId = fromUomId;
        }

        public String getFromUomName() {
            return fromUomName;
        }

        public void setFromUomName(String fromUomName) {
            this.fromUomName = fromUomName;
        }

        public String getToUomSiId() {
            return toUomSiId;
        }

        public void setToUomSiId(String toUomSiId) {
            this.toUomSiId = toUomSiId;
        }

        public String getToUomSiName() {
            return toUomSiName;
        }

        public void setToUomSiName(String toUomSiName) {
            this.toUomSiName = toUomSiName;
        }

        public Integer getDecimals() {
            return decimals;
        }

        public void setDecimals(Integer decimals) {
            this.decimals = decimals;
        }

        public Boolean getActive() {
            return active;
        }

        public void setActive(Boolean active) {
            this.active = active;
        }

        public List<DependencyDTO> getDependencies() {
            return dependencies;
        }

        public void setDependencies(List<DependencyDTO> dependencies) {
            this.dependencies = dependencies;
        }
    }

    public static class TestAliasDTO {
        private String id;
        private String testId;
        private String testName;
        private String alias;
        private Boolean active;

        // Getters and setters
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getTestId() {
            return testId;
        }

        public void setTestId(String testId) {
            this.testId = testId;
        }

        public String getTestName() {
            return testName;
        }

        public void setTestName(String testName) {
            this.testName = testName;
        }

        public String getAlias() {
            return alias;
        }

        public void setAlias(String alias) {
            this.alias = alias;
        }

        public Boolean getActive() {
            return active;
        }

        public void setActive(Boolean active) {
            this.active = active;
        }
    }

    public static class DependencyDTO {
        private String id;
        private String sourceTestId;
        private String sourceTestName;
        private String testAlias;

        // Getters and setters
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getSourceTestId() {
            return sourceTestId;
        }

        public void setSourceTestId(String sourceTestId) {
            this.sourceTestId = sourceTestId;
        }

        public String getSourceTestName() {
            return sourceTestName;
        }

        public void setSourceTestName(String sourceTestName) {
            this.sourceTestName = sourceTestName;
        }

        public String getTestAlias() {
            return testAlias;
        }

        public void setTestAlias(String testAlias) {
            this.testAlias = testAlias;
        }
    }

    public static class SimpleConversionRequest {
        private String id;
        private String testId;
        private String fromUomId;
        private String toUomId;
        private BigDecimal factor;
        private BigDecimal offsetValue;
        private Integer decimals;
        private Boolean active;

        // Getters and setters
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getTestId() {
            return testId;
        }

        public void setTestId(String testId) {
            this.testId = testId;
        }

        public String getFromUomId() {
            return fromUomId;
        }

        public void setFromUomId(String fromUomId) {
            this.fromUomId = fromUomId;
        }

        public String getToUomId() {
            return toUomId;
        }

        public void setToUomId(String toUomId) {
            this.toUomId = toUomId;
        }

        public BigDecimal getFactor() {
            return factor;
        }

        public void setFactor(BigDecimal factor) {
            this.factor = factor;
        }

        public BigDecimal getOffsetValue() {
            return offsetValue;
        }

        public void setOffsetValue(BigDecimal offsetValue) {
            this.offsetValue = offsetValue;
        }

        public Integer getDecimals() {
            return decimals;
        }

        public void setDecimals(Integer decimals) {
            this.decimals = decimals;
        }

        public Boolean getActive() {
            return active;
        }

        public void setActive(Boolean active) {
            this.active = active;
        }
    }

    public static class DerivedFormulaRequest {
        private String id;
        private String testId;
        private String expression;
        private String fromUomId;
        private String toUomSiId;
        private Integer decimals;
        private Boolean active;
        private List<DependencyDTO> dependencies;

        // Getters and setters
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getTestId() {
            return testId;
        }

        public void setTestId(String testId) {
            this.testId = testId;
        }

        public String getExpression() {
            return expression;
        }

        public void setExpression(String expression) {
            this.expression = expression;
        }

        public String getFromUomId() {
            return fromUomId;
        }

        public void setFromUomId(String fromUomId) {
            this.fromUomId = fromUomId;
        }

        public String getToUomSiId() {
            return toUomSiId;
        }

        public void setToUomSiId(String toUomSiId) {
            this.toUomSiId = toUomSiId;
        }

        public Integer getDecimals() {
            return decimals;
        }

        public void setDecimals(Integer decimals) {
            this.decimals = decimals;
        }

        public Boolean getActive() {
            return active;
        }

        public void setActive(Boolean active) {
            this.active = active;
        }

        public List<DependencyDTO> getDependencies() {
            return dependencies;
        }

        public void setDependencies(List<DependencyDTO> dependencies) {
            this.dependencies = dependencies;
        }
    }

    public static class TestAliasRequest {
        private String id;
        private String testId;
        private String alias;
        private Boolean active;

        // Getters and setters
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getTestId() {
            return testId;
        }

        public void setTestId(String testId) {
            this.testId = testId;
        }

        public String getAlias() {
            return alias;
        }

        public void setAlias(String alias) {
            this.alias = alias;
        }

        public Boolean getActive() {
            return active;
        }

        public void setActive(Boolean active) {
            this.active = active;
        }
    }

    public static class ResponseMessage {
        private String message;
        private boolean success;

        public ResponseMessage(String message, boolean success) {
            this.message = message;
            this.success = success;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }
    }
}

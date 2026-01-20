package org.openelisglobal.testunitconversion.controller.rest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testunitconversion.dao.TestDerivedFormulaDAO;
import org.openelisglobal.testunitconversion.dao.TestUnitConversionDAO;
import org.openelisglobal.testunitconversion.valueholder.TestDerivedFormula;
import org.openelisglobal.testunitconversion.valueholder.TestUnitConversion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for SI unit conversion operations. Provides endpoints for: -
 * Retrieving available conversion rules - Previewing conversions - Getting
 * test-specific conversion information
 */
@RestController
@RequestMapping("/rest/si-conversion")
public class SiConversionRestController extends BaseRestController {

    @Autowired
    private TestUnitConversionDAO testUnitConversionDAO;

    @Autowired
    private TestDerivedFormulaDAO testDerivedFormulaDAO;

    @Autowired
    private TestService testService;

    /**
     * Get all active conversion rules for a specific test.
     *
     * @param testId The test ID
     * @return List of conversion rules
     */
    @GetMapping("/test/{testId}/conversions")
    public ResponseEntity<List<ConversionRuleDTO>> getConversionsForTest(@PathVariable String testId) {
        try {
            List<TestUnitConversion> conversions = testUnitConversionDAO.findByTestId(testId);

            List<ConversionRuleDTO> dtos = conversions.stream().filter(TestUnitConversion::getActive)
                    .map(this::toConversionRuleDTO).collect(Collectors.toList());

            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get derived formula for a specific test.
     *
     * @param testId The test ID
     * @return Derived formula information or 404 if not found
     */
    @GetMapping("/test/{testId}/formula")
    public ResponseEntity<DerivedFormulaDTO> getFormulaForTest(@PathVariable String testId) {
        try {
            TestDerivedFormula formula = testDerivedFormulaDAO.findByTestId(testId);

            if (formula == null || !formula.getActive()) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(toDerivedFormulaDTO(formula));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Preview a conversion without saving.
     *
     * @param request The conversion preview request
     * @return Preview result with converted value
     */
    @PostMapping("/preview")
    public ResponseEntity<ConversionPreviewDTO> previewConversion(@RequestBody ConversionPreviewRequest request) {
        try {
            if (request.getValue() == null || request.getTestId() == null) {
                return ResponseEntity.badRequest().build();
            }

            List<TestUnitConversion> conversions = testUnitConversionDAO.findByTestId(request.getTestId());

            if (conversions.isEmpty()) {
                return ResponseEntity.ok(new ConversionPreviewDTO(request.getValue(), null, null, false,
                        "No conversion rule found for this test"));
            }

            // Use the first active conversion
            TestUnitConversion conversion = conversions.stream().filter(TestUnitConversion::getActive).findFirst()
                    .orElse(null);

            if (conversion == null) {
                return ResponseEntity.ok(new ConversionPreviewDTO(request.getValue(), null, null, false,
                        "No active conversion rule found"));
            }

            // Perform conversion
            BigDecimal value = new BigDecimal(request.getValue());
            BigDecimal siValue = conversion.convertToSi(value);

            String siUom = conversion.getToUom() != null ? conversion.getToUom().getUnitOfMeasureName() : null;

            return ResponseEntity
                    .ok(new ConversionPreviewDTO(request.getValue(), siValue.toPlainString(), siUom, true, null));
        } catch (NumberFormatException e) {
            return ResponseEntity
                    .ok(new ConversionPreviewDTO(request.getValue(), null, null, false, "Invalid numeric value"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get all tests that have SI conversion rules.
     *
     * @return List of test IDs with conversion capabilities
     */
    @GetMapping("/tests/with-conversions")
    public ResponseEntity<List<TestConversionInfoDTO>> getTestsWithConversions() {
        try {
            List<TestUnitConversion> allConversions = testUnitConversionDAO.findAllActive();
            List<TestDerivedFormula> allFormulas = testDerivedFormulaDAO.findAllActive();

            List<TestConversionInfoDTO> infos = new ArrayList<>();

            // Add tests with simple conversions
            allConversions.stream().filter(TestUnitConversion::getActive)
                    .collect(Collectors.groupingBy(c -> c.getTest().getId())).forEach((testId, conversions) -> {
                        Test test = conversions.get(0).getTest();
                        infos.add(
                                new TestConversionInfoDTO(testId, test.getDescription(), "simple", conversions.size()));
                    });

            // Add tests with derived formulas
            allFormulas.stream().filter(TestDerivedFormula::getActive).forEach(formula -> {
                Test test = formula.getTest();
                infos.add(new TestConversionInfoDTO(test.getId(), test.getDescription(), "derived", 1));
            });

            return ResponseEntity.ok(infos);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // DTOs

    private ConversionRuleDTO toConversionRuleDTO(TestUnitConversion conversion) {
        ConversionRuleDTO dto = new ConversionRuleDTO();
        dto.setId(conversion.getId());
        dto.setFromUom(conversion.getFromUom() != null ? conversion.getFromUom().getUnitOfMeasureName() : null);
        dto.setToUom(conversion.getToUom() != null ? conversion.getToUom().getUnitOfMeasureName() : null);
        dto.setFactor(conversion.getFactor());
        dto.setOffsetValue(conversion.getOffsetValue());
        dto.setDecimals(conversion.getDecimals());
        dto.setActive(conversion.getActive());
        return dto;
    }

    private DerivedFormulaDTO toDerivedFormulaDTO(TestDerivedFormula formula) {
        DerivedFormulaDTO dto = new DerivedFormulaDTO();
        dto.setId(formula.getId());
        dto.setExpression(formula.getExpression());
        dto.setFromUom(formula.getFromUom() != null ? formula.getFromUom().getUnitOfMeasureName() : null);
        dto.setToUomSi(formula.getToUomSi() != null ? formula.getToUomSi().getUnitOfMeasureName() : null);
        dto.setDecimals(formula.getDecimals());
        dto.setActive(formula.getActive());
        return dto;
    }

    // DTO Classes

    public static class ConversionRuleDTO {
        private String id;
        private String fromUom;
        private String toUom;
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

        public String getFromUom() {
            return fromUom;
        }

        public void setFromUom(String fromUom) {
            this.fromUom = fromUom;
        }

        public String getToUom() {
            return toUom;
        }

        public void setToUom(String toUom) {
            this.toUom = toUom;
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
        private String expression;
        private String fromUom;
        private String toUomSi;
        private Integer decimals;
        private Boolean active;

        // Getters and setters
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getExpression() {
            return expression;
        }

        public void setExpression(String expression) {
            this.expression = expression;
        }

        public String getFromUom() {
            return fromUom;
        }

        public void setFromUom(String fromUom) {
            this.fromUom = fromUom;
        }

        public String getToUomSi() {
            return toUomSi;
        }

        public void setToUomSi(String toUomSi) {
            this.toUomSi = toUomSi;
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

    public static class ConversionPreviewRequest {
        private String testId;
        private String value;

        // Getters and setters
        public String getTestId() {
            return testId;
        }

        public void setTestId(String testId) {
            this.testId = testId;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    public static class ConversionPreviewDTO {
        private String originalValue;
        private String siValue;
        private String siUom;
        private boolean hasConversion;
        private String message;

        public ConversionPreviewDTO(String originalValue, String siValue, String siUom, boolean hasConversion,
                String message) {
            this.originalValue = originalValue;
            this.siValue = siValue;
            this.siUom = siUom;
            this.hasConversion = hasConversion;
            this.message = message;
        }

        // Getters and setters
        public String getOriginalValue() {
            return originalValue;
        }

        public void setOriginalValue(String originalValue) {
            this.originalValue = originalValue;
        }

        public String getSiValue() {
            return siValue;
        }

        public void setSiValue(String siValue) {
            this.siValue = siValue;
        }

        public String getSiUom() {
            return siUom;
        }

        public void setSiUom(String siUom) {
            this.siUom = siUom;
        }

        public boolean isHasConversion() {
            return hasConversion;
        }

        public void setHasConversion(boolean hasConversion) {
            this.hasConversion = hasConversion;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    public static class TestConversionInfoDTO {
        private String testId;
        private String testName;
        private String conversionType;
        private int ruleCount;

        public TestConversionInfoDTO(String testId, String testName, String conversionType, int ruleCount) {
            this.testId = testId;
            this.testName = testName;
            this.conversionType = conversionType;
            this.ruleCount = ruleCount;
        }

        // Getters and setters
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

        public String getConversionType() {
            return conversionType;
        }

        public void setConversionType(String conversionType) {
            this.conversionType = conversionType;
        }

        public int getRuleCount() {
            return ruleCount;
        }

        public void setRuleCount(int ruleCount) {
            this.ruleCount = ruleCount;
        }
    }
}

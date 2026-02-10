/**
 * The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.mozilla.org/MPL/
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>The Original Code is OpenELIS code.
 *
 * <p>Copyright (C) ITECH-CI. All Rights Reserved.
 */
package org.openelisglobal.bacteriology.validator;

import java.util.List;
import org.openelisglobal.bacteriology.controller.rest.dto.FloraDataDTO;
import org.openelisglobal.bacteriology.controller.rest.dto.FloraDetailDTO;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

/**
 * BacteriologyResultValidator - Validates bacteriology result data
 */
@Component
public class BacteriologyResultValidator {

    /**
     * Validate flora data
     *
     * @param floraData The flora data to validate
     * @param testName  The test name (for error messages)
     * @param errors    Spring Errors object to accumulate validation errors
     */
    public void validateFloraData(FloraDataDTO floraData, String testName, Errors errors) {
        if (floraData == null) {
            errors.rejectValue("floraData", "error.floraData.required", "Flora data is required for test: " + testName);
            return;
        }

        // Validate count
        Integer count = floraData.getCount();
        if (count == null) {
            errors.rejectValue("floraData.count", "error.floraData.count.required",
                    "Flora count is required for test: " + testName);
        } else if (count < 0) {
            errors.rejectValue("floraData.count", "error.floraData.count.negative",
                    "Flora count must be non-negative for test: " + testName);
        } else if (count > 3) {
            errors.rejectValue("floraData.count", "error.floraData.count.exceeded",
                    "Flora count cannot exceed 3 for test: " + testName);
        }

        // Validate details match count
        List<FloraDetailDTO> details = floraData.getDetails();
        if (count != null && count > 0) {
            if (details == null || details.isEmpty()) {
                errors.rejectValue("floraData.details", "error.floraData.details.required",
                        "Flora details are required when count > 0 for test: " + testName);
            } else if (details.size() != count) {
                errors.rejectValue("floraData.details", "error.floraData.details.mismatch",
                        String.format("Number of flora details (%d) must match count (%d) for test: %s", details.size(),
                                count, testName));
            } else {
                // Validate each detail
                for (int i = 0; i < details.size(); i++) {
                    validateFloraDetail(details.get(i), i + 1, testName, errors);
                }
            }
        } else if (details != null && !details.isEmpty()) {
            errors.rejectValue("floraData.details", "error.floraData.details.shouldBeEmpty",
                    "Flora details should be empty when count is 0 for test: " + testName);
        }
    }

    /**
     * Validate individual flora detail
     *
     * @param detail      The flora detail to validate
     * @param floraNumber The flora number (for error messages)
     * @param testName    The test name (for error messages)
     * @param errors      Spring Errors object to accumulate validation errors
     */
    private void validateFloraDetail(FloraDetailDTO detail, int floraNumber, String testName, Errors errors) {
        String fieldPrefix = String.format("floraData.details[%d]", floraNumber - 1);

        if (detail == null) {
            errors.rejectValue(fieldPrefix, "error.floraDetail.null",
                    String.format("Flora detail #%d is null for test: %s", floraNumber, testName));
            return;
        }

        // Validate flora number
        if (detail.getFloraNumber() == null) {
            errors.rejectValue(fieldPrefix + ".floraNumber", "error.floraDetail.floraNumber.required",
                    String.format("Flora number is required for flora #%d in test: %s", floraNumber, testName));
        } else if (detail.getFloraNumber() != floraNumber) {
            errors.rejectValue(fieldPrefix + ".floraNumber", "error.floraDetail.floraNumber.mismatch",
                    String.format("Flora number mismatch: expected %d but got %d for test: %s", floraNumber,
                            detail.getFloraNumber(), testName));
        }

        // Validate gram type (required)
        if (detail.getGramTypeDictId() == null) {
            errors.rejectValue(fieldPrefix + ".gramTypeDictId", "error.floraDetail.gramType.required",
                    String.format("Gram type is required for flora #%d in test: %s", floraNumber, testName));
        } else if (detail.getGramTypeDictId() <= 0) {
            errors.rejectValue(fieldPrefix + ".gramTypeDictId", "error.floraDetail.gramType.invalid",
                    String.format("Invalid gram type ID for flora #%d in test: %s", floraNumber, testName));
        }

        // Validate grouping mode (required)
        if (detail.getGroupingModeDictId() == null) {
            errors.rejectValue(fieldPrefix + ".groupingModeDictId", "error.floraDetail.groupingMode.required",
                    String.format("Grouping mode is required for flora #%d in test: %s", floraNumber, testName));
        } else if (detail.getGroupingModeDictId() <= 0) {
            errors.rejectValue(fieldPrefix + ".groupingModeDictId", "error.floraDetail.groupingMode.invalid",
                    String.format("Invalid grouping mode ID for flora #%d in test: %s", floraNumber, testName));
        }

        // Validate capsulated (should never be null due to default, but check anyway)
        if (detail.getCapsulated() == null) {
            errors.rejectValue(fieldPrefix + ".capsulated", "error.floraDetail.capsulated.required",
                    String.format("Capsulated field is required for flora #%d in test: %s", floraNumber, testName));
        }
    }

    /**
     * Validate conditional test result Ensures child tests are only filled when
     * parent trigger condition is met
     *
     * @param parentValue        The parent test result value
     * @param parentTriggerValue The trigger value that enables child tests
     * @param childValue         The child test result value
     * @param parentTestName     The parent test name (for error messages)
     * @param childTestName      The child test name (for error messages)
     * @param errors             Spring Errors object to accumulate validation
     *                           errors
     */
    public void validateConditionalTestResult(String parentValue, String parentTriggerValue, String childValue,
            String parentTestName, String childTestName, Errors errors) {
        // If parent value doesn't match trigger, child must be empty
        if (parentValue == null || !parentValue.equals(parentTriggerValue)) {
            if (childValue != null && !childValue.trim().isEmpty()) {
                errors.rejectValue("conditionalTest." + childTestName, "error.conditionalTest.invalidChild",
                        String.format("Child test '%s' cannot have a value when parent test '%s' is not '%s'",
                                childTestName, parentTestName, parentTriggerValue));
            }
        }
    }

    /**
     * Validate specialized culture result
     *
     * @param cultureType   The culture type (NORMAL, NEISSERIA_GONORRHOEAE, etc.)
     * @param cultureResult The culture result value
     * @param testName      The test name (for error messages)
     * @param errors        Spring Errors object to accumulate validation errors
     */
    public void validateSpecializedCultureResult(String cultureType, String cultureResult, String testName,
            Errors errors) {
        if (cultureType == null || cultureType.trim().isEmpty()) {
            errors.rejectValue("cultureType", "error.cultureType.required",
                    "Culture type is required for specialized culture test: " + testName);
            return;
        }

        // Validate result based on culture type
        switch (cultureType.toUpperCase()) {
        case "NORMAL":
            if (cultureResult != null && !cultureResult.trim().isEmpty()) {
                if (!cultureResult.equalsIgnoreCase("negative") && !cultureResult.equalsIgnoreCase("identification")) {
                    errors.rejectValue("cultureResult", "error.cultureResult.invalidForNormal", String.format(
                            "Invalid culture result '%s' for NORMAL culture. Valid values: negative, identification",
                            cultureResult));
                }
            }
            break;

        case "NEISSERIA_GONORRHOEAE":
            if (cultureResult != null && !cultureResult.trim().isEmpty()) {
                if (!cultureResult.equalsIgnoreCase("negative")
                        && !cultureResult.equalsIgnoreCase("neisseria_gonorrhoeae")) {
                    errors.rejectValue("cultureResult", "error.cultureResult.invalidForNeisseria", String.format(
                            "Invalid culture result '%s' for NEISSERIA_GONORRHOEAE culture. Valid values: negative, neisseria_gonorrhoeae",
                            cultureResult));
                }
            }
            break;

        default:
            errors.rejectValue("cultureType", "error.cultureType.unknown",
                    String.format("Unknown culture type '%s' for test: %s", cultureType, testName));
            break;
        }
    }

    /**
     * Validate that required fields are present for flora count test
     *
     * @param isFloraCountTest Whether this is a flora count test
     * @param floraData        The flora data (can be null)
     * @param testName         The test name (for error messages)
     * @param errors           Spring Errors object to accumulate validation errors
     */
    public void validateFloraCountTest(Boolean isFloraCountTest, FloraDataDTO floraData, String testName,
            Errors errors) {
        if (Boolean.TRUE.equals(isFloraCountTest)) {
            if (floraData == null) {
                errors.rejectValue("floraData", "error.floraData.requiredForFloraCountTest",
                        "Flora data is required for flora count test: " + testName);
            } else {
                validateFloraData(floraData, testName, errors);
            }
        } else {
            // If not a flora count test, flora data should be null
            if (floraData != null) {
                errors.rejectValue("floraData", "error.floraData.shouldNotBeProvided",
                        "Flora data should not be provided for non-flora-count test: " + testName);
            }
        }
    }
}

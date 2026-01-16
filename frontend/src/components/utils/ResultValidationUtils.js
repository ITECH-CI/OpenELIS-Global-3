/**
 * Utility functions for validating numeric results and applying conditional formatting
 */

/**
 * Validates the format of a numeric result value
 * @param {string|number} value - The result value to validate
 * @param {object} row - The result row containing range information
 * @returns {object} Validation object with isInvalid, isBlank, isNaN, and newValue properties
 */
export const validateNumberFormat = (value, row) => {
  // Ignore < or > from the analyser on validation
  var greaterThanOrLessThan = "";
  if (("" + value).startsWith("<") || ("" + value).startsWith(">")) {
    greaterThanOrLessThan = value.charAt(0);
  }
  var actualValue = ("" + value).replace(/[<>]/g, "");

  let validation = { isInvalid: false };

  if (!actualValue) {
    return { ...validation, isInvalid: true, isBlank: true };
  }

  if (actualValue.trim() === ".") {
    validation = {
      ...validation,
      newValue: greaterThanOrLessThan + "0.0",
    };
  }

  if (isNaN(actualValue)) {
    return { ...validation, isInvalid: true, isNaN: true };
  }

  if (!isNaN(row.significantDigits)) {
    const valueStr = actualValue.toString();
    if (valueStr.includes(".")) {
      const decimalPlaces = valueStr.split(".")[1].length;
      if (decimalPlaces > row.significantDigits) {
        actualValue = parseFloat(actualValue).toFixed(row.significantDigits);
      }
    }
    validation = {
      ...validation,
      newValue: greaterThanOrLessThan + actualValue,
    };
  }

  return validation;
};

/**
 * Validates numeric results against normal and valid ranges
 * Returns validation object with flags for different range violations
 * @param {string|number} value - The result value to validate
 * @param {object} row - The result row containing range information
 * @returns {object} Validation object with properties:
 *   - isInvalid: true if format is invalid
 *   - outsideNormal: true if outside normal range but within valid range
 *   - isCritical: true if within critical range
 *   - isBlank: true if value is empty
 *   - isNaN: true if value is not a number
 *   - outsideValid: true if outside valid/abnormal range
 *   - newValue: formatted value
 */
export const validateNumericResults = (value, row) => {
  // Ignore < or > from the analyser on validation
  var greaterThanOrLessThan = "";
  if (("" + value).startsWith("<") || ("" + value).startsWith(">")) {
    greaterThanOrLessThan = value.charAt(0);
  }
  var actualValue = ("" + value).replace(/[<>]/g, "");

  let validation = {
    isInvalid: false,
    outsideNormal: false,
    isCritical: false,
    isBlank: false,
    isNaN: false,
    outsideValid: false,
    newValue: value,
  };

  // Validate number format first
  validation = { ...validation, ...validateNumberFormat(value, row) };

  // If not a valid number, return early
  if (validation.isNaN) {
    return { ...validation };
  }

  // Check if in critical range
  if (
    row.lowerCritical != row.higherCritical &&
    actualValue > row.lowerCritical &&
    actualValue < row.higherCritical
  ) {
    return { ...validation, isCritical: true };
  }

  // Check if outside valid/abnormal range (RED background)
  if (
    row.lowerAbnormalRange != row.upperAbnormalRange &&
    (actualValue < row.lowerAbnormalRange ||
      actualValue > row.upperAbnormalRange)
  ) {
    return { ...validation, isInvalid: true, outsideValid: true };
  }

  // Check if outside normal range (YELLOW background)
  if (
    row.lowerNormalRange != row.upperNormalRange &&
    (actualValue < row.lowerNormalRange || actualValue > row.upperNormalRange)
  ) {
    return { ...validation, outsideNormal: true };
  }

  // Within normal range
  return { ...validation, outsideNormal: false };
};

/**
 * Gets the style object for conditional formatting based on validation results
 * @param {object} validation - Validation object from validateNumericResults
 * @returns {object} Style object with background and borderColor properties
 */
export const getResultStyle = (validation) => {
  if (!validation) {
    return {};
  }

  return {
    borderColor: validation.isCritical
      ? "orange"
      : validation.isInvalid
        ? "red"
        : "",
    background: validation.outsideValid
      ? "#ffa0a0" // Light red for out of valid range
      : validation.outsideNormal
        ? "#ffffa0" // Light yellow for out of normal range
        : "var(--cds-field)",
  };
};

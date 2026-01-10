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
 * <p>Copyright (C) The Minnesota Department of Health. All Rights Reserved.
 */
package org.openelisglobal.testunitconversion.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testunitconversion.dao.TestAliasDAO;
import org.openelisglobal.testunitconversion.valueholder.TestDerivedFormula;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service implementation for evaluating derived formulas. Supports basic
 * arithmetic operations: +, -, *, /, parentheses, and test aliases.
 */
@Service
public class FormulaEvaluationServiceImpl implements FormulaEvaluationService {

    @Autowired
    private TestAliasDAO testAliasDAO;

    // Cache: alias -> Test
    private Map<String, Test> aliasCache = new HashMap<>();

    @Override
    public synchronized BigDecimal evaluateFormula(TestDerivedFormula formula, Map<Test, Result> sourceResults) {
        if (formula == null || formula.getExpression() == null || formula.getExpression().trim().isEmpty()) {
            return null;
        }

        try {
            String expression = formula.getExpression();

            // Step 1: Resolve aliases to test IDs and replace with SI values
            expression = resolveAliasesAndSubstituteValues(expression, sourceResults);

            // Step 2: Evaluate the mathematical expression
            BigDecimal result = evaluateExpression(expression);

            // Step 3: Apply rounding if decimals is specified
            if (result != null && formula.getDecimals() != null && formula.getDecimals() >= 0) {
                result = result.setScale(formula.getDecimals(), RoundingMode.HALF_UP);
            }

            return result;

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "evaluateFormula",
                    "Error evaluating formula '" + formula.getExpression() + "': " + e.getMessage());
            LogEvent.logError(e);
            return null;
        }
    }

    @Override
    public synchronized void clearCache() {
        aliasCache.clear();
    }

    /**
     * Resolve test aliases in the expression and substitute with actual SI values.
     *
     * @param expression    The formula expression
     * @param sourceResults Map of test to result
     * @return Expression with aliases replaced by numeric values
     */
    private String resolveAliasesAndSubstituteValues(String expression, Map<Test, Result> sourceResults)
            throws Exception {
        // Find all potential alias tokens (letters, numbers, underscores)
        Pattern tokenPattern = Pattern.compile("[a-zA-Z][a-zA-Z0-9_]*");
        Matcher matcher = tokenPattern.matcher(expression);

        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String token = matcher.group();

            // Try to resolve the token as an alias
            Test test = resolveAlias(token);
            if (test != null) {
                // Find the result for this test
                Result testResult = sourceResults.get(test);
                if (testResult == null) {
                    throw new Exception("No result found for test alias: " + token);
                }

                // Use SI value if available, otherwise use traditional value
                String value = testResult.getValueSi();
                if (value == null || value.trim().isEmpty()) {
                    value = testResult.getValue();
                }

                if (value == null || value.trim().isEmpty()) {
                    throw new Exception("No value (traditional or SI) found for test alias: " + token);
                }

                // Clean the value (remove non-numeric characters except decimal point and
                // minus)
                value = value.trim().replaceAll("[^0-9.-]", "");

                matcher.appendReplacement(result, value);
            } else {
                // Not an alias, keep as is
                matcher.appendReplacement(result, token);
            }
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * Resolve a test alias to the actual Test object.
     *
     * @param alias The alias to resolve
     * @return The Test object, or null if not found
     */
    private synchronized Test resolveAlias(String alias) {
        Test test = aliasCache.get(alias);
        if (test != null) {
            return test;
        }

        // Load from database
        test = testAliasDAO.findTestByAlias(alias);
        if (test != null) {
            aliasCache.put(alias, test);
        }

        return test;
    }

    /**
     * Evaluate a mathematical expression. Supports +, -, *, /, parentheses, and
     * numeric values.
     *
     * @param expression The expression to evaluate
     * @return The result
     */
    private BigDecimal evaluateExpression(String expression) throws Exception {
        // Remove all whitespace
        expression = expression.replaceAll("\\s+", "");

        // Replace 'x' or 'X' with '*' for multiplication
        expression = expression.replaceAll("[xX]", "*");

        return parseExpression(expression);
    }

    /**
     * Parse and evaluate an expression (handles + and -).
     */
    private BigDecimal parseExpression(String expr) throws Exception {
        BigDecimal result = parseTerm(expr);
        return result;
    }

    /**
     * Simple expression parser using recursive descent. This is a basic
     * implementation. For production, consider using a library like exp4j or
     * mXparser.
     */
    private BigDecimal parseTerm(String expr) throws Exception {
        try {
            // Try to parse the entire expression as a number first
            return new BigDecimal(expr);
        } catch (NumberFormatException e) {
            // Not a simple number, need to parse the expression
        }

        // Find the last + or - not inside parentheses
        int depth = 0;
        for (int i = expr.length() - 1; i >= 0; i--) {
            char c = expr.charAt(i);
            if (c == ')') {
                depth++;
            } else if (c == '(') {
                depth--;
            } else if (depth == 0 && (c == '+' || c == '-')) {
                if (i == 0) {
                    // Unary minus/plus
                    continue;
                }
                String left = expr.substring(0, i);
                String right = expr.substring(i + 1);
                BigDecimal leftVal = parseTerm(left);
                BigDecimal rightVal = parseTerm(right);
                return c == '+' ? leftVal.add(rightVal) : leftVal.subtract(rightVal);
            }
        }

        // Find the last * or / not inside parentheses
        depth = 0;
        for (int i = expr.length() - 1; i >= 0; i--) {
            char c = expr.charAt(i);
            if (c == ')') {
                depth++;
            } else if (c == '(') {
                depth--;
            } else if (depth == 0 && (c == '*' || c == '/')) {
                String left = expr.substring(0, i);
                String right = expr.substring(i + 1);
                BigDecimal leftVal = parseTerm(left);
                BigDecimal rightVal = parseTerm(right);
                if (c == '/') {
                    if (rightVal.compareTo(BigDecimal.ZERO) == 0) {
                        throw new ArithmeticException("Division by zero");
                    }
                    return leftVal.divide(rightVal, 10, RoundingMode.HALF_UP);
                } else {
                    return leftVal.multiply(rightVal);
                }
            }
        }

        // Handle parentheses
        if (expr.startsWith("(") && expr.endsWith(")")) {
            return parseTerm(expr.substring(1, expr.length() - 1));
        }

        // Handle unary minus
        if (expr.startsWith("-")) {
            return parseTerm(expr.substring(1)).negate();
        }

        throw new Exception("Invalid expression: " + expr);
    }
}

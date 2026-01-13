import React, { useMemo } from "react";
import { Grid, Column } from "@carbon/react";
import BacteriologyResultField from "./common/BacteriologyResultField";

/**
 * ConditionalTestGroup - Manages parent-child test relationships
 *
 * When a parent test has a specific value (trigger value), this component
 * displays the child tests. Otherwise, child tests are hidden and their
 * values are cleared.
 *
 * @param {string} accessionNumber - Used for generating unique IDs
 * @param {object} parentTest - Parent test object with testId, name, resultValue, etc.
 * @param {array} childTests - Array of child test objects
 * @param {function} onResultChange - Callback when any result changes
 * @param {boolean} disabled - Whether fields are disabled
 */
const ConditionalTestGroup = ({
  accessionNumber,
  parentTest,
  childTests = [],
  onResultChange,
  disabled = false,
}) => {
  // Determine if child tests should be shown based on parent's result
  const shouldShowChildren = useMemo(() => {
    if (!parentTest || !parentTest.parentTriggerValue) {
      return false;
    }
    return parentTest.resultValue === parentTest.parentTriggerValue;
  }, [parentTest]);

  // Handle parent result change
  const handleParentChange = (testId, value) => {
    onResultChange(testId, value);

    // If parent value is not the trigger value, clear all child results
    if (value !== parentTest.parentTriggerValue) {
      childTests.forEach((childTest) => {
        onResultChange(childTest.testId, "");
      });
    }
  };

  // Clean test name by removing sample type prefix
  const cleanTestName = (testName) => {
    if (!testName) return "";
    // Remove prefixes like "COL-", "MUQUEUSE-", "VULVE-", "URETHRAL-", "OTHER-"
    return testName.replace(/^(COL|MUQUEUSE|VULVE|URETHRAL|OTHER)-/, "");
  };

  // Generate unique ID for form elements
  const generateId = (testId, suffix = "") => {
    const prefix = accessionNumber
      ? accessionNumber.replace(/[^a-zA-Z0-9]/g, "_")
      : "conditional";
    return `${prefix}_${testId}${suffix ? "_" + suffix : ""}`;
  };

  if (!parentTest) {
    return null;
  }

  return (
    <div className="conditional-test-group">
      {/* Parent Test */}
      <Grid fullWidth>
        <Column lg={8} md={4} sm={4} style={{ marginBottom: "1.5rem" }}>
          <BacteriologyResultField
            id={generateId(parentTest.testId, "parent")}
            label={cleanTestName(parentTest.testName)}
            testId={parentTest.testId}
            resultType={parentTest.resultType}
            resultValue={parentTest.resultValue || ""}
            resultOptions={parentTest.dictionaryResults || []}
            onResultChange={handleParentChange}
            disabled={disabled}
          />
        </Column>
      </Grid>

      {/* Child Tests - Only shown when parent has trigger value */}
      {shouldShowChildren && childTests.length > 0 && (
        <div
          className="conditional-children"
          style={{ marginLeft: "2rem", marginTop: "1rem" }}
        >
          <Grid fullWidth>
            {childTests.map((childTest) => (
              <Column
                key={childTest.testId}
                lg={8}
                md={4}
                sm={4}
                style={{ marginBottom: "1.5rem" }}
              >
                <BacteriologyResultField
                  id={generateId(childTest.testId, "child")}
                  label={cleanTestName(childTest.testName)}
                  testId={childTest.testId}
                  resultType={childTest.resultType}
                  resultValue={childTest.resultValue || ""}
                  resultOptions={childTest.dictionaryResults || []}
                  onResultChange={onResultChange}
                  disabled={disabled}
                />
              </Column>
            ))}
          </Grid>
        </div>
      )}
    </div>
  );
};

export default ConditionalTestGroup;

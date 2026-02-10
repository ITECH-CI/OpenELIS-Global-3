import { useMemo } from "react";
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
  // Only depends on the specific values we need, not the entire parentTest object
  const shouldShowChildren = useMemo(() => {
    if (!parentTest || !parentTest.parentTriggerValue) {
      return false;
    }

    const show = parentTest.resultValue === parentTest.parentTriggerValue;
    return show;
  }, [
    parentTest?.resultValue,
    parentTest?.parentTriggerValue,
    parentTest?.testName,
  ]);

  // Handle parent result change
  const handleParentChange = (testId, value) => {
    // Simply delegate to parent - don't clear child values here
    // The parent component (MacroscopySection) will handle clearing if needed
    onResultChange(testId, value);
  };

  // Clean test name by removing sample type prefix
  const cleanTestName = (testName) => {
    if (!testName) return "";
    // Remove prefixes like "COL-", "MUQUEUSE-", "VULVE-", "URETHRAL-", "OTHER-"
    return testName.replace(/^(COL|MUQUEUSE|VULVE|URETHRAL|OTHER)-/, "");
  };

  // Generate unique ID for form elements
  // Include parent test ID to ensure uniqueness across different conditional groups
  const generateId = (testId, suffix = "") => {
    const prefix = accessionNumber
      ? accessionNumber.replace(/[^a-zA-Z0-9]/g, "_")
      : "conditional";
    const parentId = parentTest?.testId || "noparent";
    return `${prefix}_p${parentId}_${testId}${suffix ? "_" + suffix : ""}`;
  };

  if (!parentTest) {
    return null;
  }

  return (
    <div className="conditional-test-group">
      {/* Parent Test */}
      <div className="bacteriology-regular-tests">
        <Grid fullWidth>
          <Column lg={8} md={4} sm={4} style={{ marginBottom: "1.5rem" }}>
            <BacteriologyResultField
              id={generateId(parentTest.testId, "parent")}
              label={cleanTestName(parentTest.testName)}
              type={
                parentTest.resultType === "D" || parentTest.resultType === "M"
                  ? "select"
                  : parentTest.resultType === "N"
                    ? "number"
                    : "text"
              }
              value={parentTest.resultValue ?? ""}
              options={parentTest.dictionaryResults || []}
              onChange={(value) => handleParentChange(parentTest.testId, value)}
              disabled={disabled}
            />
          </Column>
        </Grid>
      </div>

      {/* Child Tests - Only shown when parent has trigger value */}
      {shouldShowChildren && childTests.length > 0 && (
        <div
          className="conditional-children"
          style={{ marginLeft: "2rem", marginTop: "1rem" }}
        >
          <Grid fullWidth>
            {childTests.map((childTest) => {
              const cleanedLabel = cleanTestName(childTest.testName);
              const uniqueKey = `${childTest.analysisId}-${childTest.testId}`;

              return (
                <Column
                  key={uniqueKey}
                  lg={8}
                  md={4}
                  sm={4}
                  style={{ marginBottom: "1.5rem" }}
                >
                  <BacteriologyResultField
                    id={generateId(childTest.testId, "child")}
                    label={cleanedLabel || childTest.testName}
                    type={
                      childTest.resultType === "D" ||
                      childTest.resultType === "M"
                        ? "select"
                        : childTest.resultType === "N"
                          ? "number"
                          : "text"
                    }
                    value={childTest.resultValue ?? ""}
                    options={childTest.dictionaryResults || []}
                    onChange={(value) =>
                      onResultChange(childTest.testId, value)
                    }
                    disabled={disabled}
                  />
                </Column>
              );
            })}
          </Grid>
        </div>
      )}
    </div>
  );
};

export default ConditionalTestGroup;

import React from "react";
import { Grid, Column, Section } from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";

/**
 * Header component displaying patient and sample information for bacteriology results
 */
const ResultHeader = ({ testResult, totalTests }) => {
  const intl = useIntl();

  if (!testResult) {
    return null;
  }

  // Translate test section name if known
  const getTestSectionDisplay = (testSectionName) => {
    if (!testSectionName) return "N/A";

    // Map of known test section names to translation keys
    const testSectionTranslations = {
      "Routine Bacteriology": "testsection.routine.bacteriology",
      Bacteriology: "testsection.bacteriology",
      Hematology: "testsection.hematology",
      Biochemistry: "testsection.biochemistry",
      Immunology: "testsection.immunology",
      Serology: "testsection.serology",
      Virology: "testsection.virology",
      Parasitology: "testsection.parasitology",
    };

    const translationKey = testSectionTranslations[testSectionName];
    if (translationKey) {
      const translated = intl.formatMessage({ id: translationKey });
      // If translation exists (not same as key), use it; otherwise fallback to original
      return translated !== translationKey ? translated : testSectionName;
    }

    return testSectionName;
  };

  const formatDate = (dateString) => {
    if (!dateString) return "";

    // If already in dd/MM/yyyy format, return as is
    if (
      typeof dateString === "string" &&
      dateString.match(/^\d{2}\/\d{2}\/\d{4}$/)
    ) {
      return dateString;
    }

    try {
      // Try parsing as ISO date (yyyy-MM-dd) or timestamp
      const date = new Date(dateString);
      if (isNaN(date.getTime())) {
        return dateString; // Return original if parsing fails
      }

      return date.toLocaleDateString("fr-FR", {
        year: "numeric",
        month: "2-digit",
        day: "2-digit",
      });
    } catch (e) {
      return dateString;
    }
  };

  return (
    <Section>
      <div
        style={{
          backgroundColor: "#f4f4f4",
          padding: "1rem",
          marginBottom: "1.5rem",
          borderRadius: "4px",
          border: "1px solid #e0e0e0",
        }}
      >
        {totalTests && (
          <div
            style={{
              marginBottom: "0.75rem",
              paddingBottom: "0.75rem",
              borderBottom: "1px solid #e0e0e0",
            }}
          >
            <strong style={{ color: "#0f62fe" }}>
              {totalTests} test{totalTests > 1 ? "s" : ""} à compléter
            </strong>
          </div>
        )}
        <Grid>
          <Column lg={4} md={4} sm={4}>
            <div style={{ marginBottom: "0.5rem" }}>
              <strong>
                <FormattedMessage id="sample.label.labnumber" />:
              </strong>{" "}
              {testResult.accessionNumber || "N/A"}
            </div>
            <div style={{ marginBottom: "0.5rem" }}>
              <strong>
                <FormattedMessage id="sample.receivedDate" />:
              </strong>{" "}
              {formatDate(testResult.receivedDate)}
            </div>
          </Column>

          <Column lg={4} md={4} sm={4}>
            <div style={{ marginBottom: "0.5rem" }}>
              <strong>
                <FormattedMessage id="patient.label" />:
              </strong>{" "}
              {testResult.patientInfo || "N/A"}
            </div>
            <div style={{ marginBottom: "0.5rem" }}>
              <strong>
                <FormattedMessage id="sample.type" />:
              </strong>{" "}
              {testResult.sampleType || "N/A"}
            </div>
          </Column>

          <Column lg={4} md={4} sm={4}>
            <div style={{ marginBottom: "0.5rem" }}>
              <strong>
                <FormattedMessage id="column.name.priority" />:
              </strong>{" "}
              <span
                style={{
                  fontWeight: "bold",
                  color: testResult.priority === "Urgent" ? "#da1e28" : "#000",
                }}
              >
                {testResult.priority || "Normal"}
              </span>
            </div>
            <div style={{ marginBottom: "0.5rem" }}>
              <strong>
                <FormattedMessage id="sample.entry.project.testSection" />:
              </strong>{" "}
              {getTestSectionDisplay(testResult.testSectionName)}
            </div>
          </Column>
        </Grid>
      </div>
    </Section>
  );
};

export default ResultHeader;

import React from "react";
import { Grid, Column, Section } from "@carbon/react";
import { FormattedMessage } from "react-intl";

/**
 * Header component displaying patient and sample information for bacteriology results
 */
const ResultHeader = ({ testResult }) => {
  if (!testResult) {
    return null;
  }

  const formatDate = (dateString) => {
    if (!dateString) return "";
    try {
      const date = new Date(dateString);
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
              {testResult.testSectionName || "N/A"}
            </div>
          </Column>
        </Grid>
      </div>
    </Section>
  );
};

export default ResultHeader;

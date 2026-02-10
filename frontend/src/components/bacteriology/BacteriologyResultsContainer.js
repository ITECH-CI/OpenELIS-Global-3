import React, { useMemo, useState } from "react";
import { Stack, Accordion, AccordionItem, Pagination } from "@carbon/react";
import { FormattedMessage } from "react-intl";
import BacteriologyResultEntry from "./BacteriologyResultEntry";

/**
 * Container component that groups test results by accession number (lab number)
 * and renders a separate BacteriologyResultEntry for each lab number
 */
const BacteriologyResultsContainer = ({
  testResults = [],
  sysUserId,
  onSave,
  disabled = false,
  showAllResults = false, // Flag from backend: true = show all (patient/accession search), false = filter finalized
}) => {
  const [currentPage, setCurrentPage] = useState(1);
  const itemsPerPage = 5; // 5 ordonnances par page

  // Deduplicate test results (same analysisId + testId) and optionally filter out finalized tests
  const deduplicatedTests = useMemo(() => {
    const seen = new Map();
    const deduplicated = [];

    // Status IDs - only filter out tests that are biologically validated (Finalized or Released)
    // Tests in TechnicalAcceptance should remain visible and modifiable until biological validation
    // unless user has specific "result modifier" role
    const FINALIZED_STATUS_ID = "9"; // Biologically validated - should NOT appear in results page
    const RELEASED_STATUS_ID = "10"; // Released - should NOT appear in results page

    const excludedStatusIds = [FINALIZED_STATUS_ID, RELEASED_STATUS_ID];

    testResults.forEach((test) => {
      const key = `${test.analysisId}-${test.testId}`;

      // Skip if already seen
      if (seen.has(key)) {
        return;
      }

      // For patient/accession searches (showAllResults=true): show ALL results, including finalized
      // For other searches (showAllResults=false): filter out finalized/released
      if (
        !showAllResults &&
        test.analysisStatusId &&
        excludedStatusIds.includes(String(test.analysisStatusId))
      ) {
        return;
      }

      seen.set(key, true);
      deduplicated.push(test);
    });

    return deduplicated;
  }, [testResults, showAllResults]);

  // Group test results by accessionNumber (lab number)
  const groupedResults = useMemo(() => {
    const groups = {};

    deduplicatedTests.forEach((test) => {
      const accessionNumber = test.accessionNumber || "UNKNOWN";
      if (!groups[accessionNumber]) {
        groups[accessionNumber] = [];
      }
      groups[accessionNumber].push(test);
    });

    return groups;
  }, [deduplicatedTests]);

  // Get array of accessionNumbers sorted alphabetically
  const accessionNumbers = useMemo(() => {
    return Object.keys(groupedResults).sort((a, b) => a.localeCompare(b));
  }, [groupedResults]);

  // Pagination
  const totalItems = accessionNumbers.length;
  const totalPages = Math.ceil(totalItems / itemsPerPage);
  const startIndex = (currentPage - 1) * itemsPerPage;
  const endIndex = startIndex + itemsPerPage;
  const paginatedAccessionNumbers = accessionNumbers.slice(
    startIndex,
    endIndex,
  );

  if (accessionNumbers.length === 0) {
    return (
      <div style={{ padding: "2rem", textAlign: "center" }}>
        No bacteriology results found.
      </div>
    );
  }

  return (
    <div>
      {/* Titre unique pour toute la page avec compteurs */}
      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          marginBottom: "1.5rem",
        }}
      >
        <h2 style={{ margin: 0 }}>
          <FormattedMessage id="bacteriology.title" />
        </h2>
        <div
          style={{ fontSize: "0.875rem", color: "#525252", fontWeight: "500" }}
        >
          {accessionNumbers.length} échantillon
          {accessionNumbers.length > 1 ? "s" : ""} |{" "}
          {(() => {
            const totalTests = Object.values(groupedResults).reduce(
              (sum, tests) => sum + tests.length,
              0,
            );
            return `${totalTests} test${totalTests > 1 ? "s" : ""}`;
          })()}
        </div>
      </div>

      {/* Accordéon pour chaque numéro de labo */}
      <Accordion>
        {paginatedAccessionNumbers.map((accessionNumber, index) => {
          const testsForAccession = groupedResults[accessionNumber];
          const primaryAnalysisId = testsForAccession[0]?.analysisId;
          const firstTest = testsForAccession[0];

          // Titre de l'accordéon avec infos clés
          const accordionTitle = `${accessionNumber} - ${firstTest?.patientInfo || "N/A"} - ${firstTest?.sampleType || "N/A"}`;

          return (
            <div key={accessionNumber} style={{ marginBottom: "1rem" }}>
              <AccordionItem
                title={
                  <div
                    style={{
                      display: "flex",
                      justifyContent: "space-between",
                      alignItems: "center",
                      width: "100%",
                      paddingRight: "1rem",
                    }}
                  >
                    <span>{accordionTitle}</span>
                    <span
                      style={{
                        fontSize: "0.875rem",
                        color: "#525252",
                        fontWeight: "500",
                      }}
                    >
                      {testsForAccession.length} test
                      {testsForAccession.length > 1 ? "s" : ""}
                    </span>
                  </div>
                }
                open={index === 0} // Premier élément ouvert par défaut
              >
                <div
                  style={{
                    padding: "1.5rem",
                    backgroundColor: "#fafafa",
                    border: "1px solid #e0e0e0",
                    borderTop: "none",
                  }}
                >
                  <BacteriologyResultEntry
                    analysisId={primaryAnalysisId}
                    accessionNumber={accessionNumber}
                    testResults={testsForAccession}
                    sysUserId={sysUserId}
                    onSave={onSave}
                    disabled={disabled}
                  />
                </div>
              </AccordionItem>
            </div>
          );
        })}
      </Accordion>

      {/* Pagination si plus de 5 ordonnances */}
      {totalPages > 1 && (
        <div
          style={{
            marginTop: "2rem",
            display: "flex",
            justifyContent: "center",
          }}
        >
          <Pagination
            page={currentPage}
            totalItems={totalItems}
            pageSize={itemsPerPage}
            pageSizes={[5, 10, 20]}
            onChange={({ page, pageSize }) => {
              setCurrentPage(page);
            }}
          />
        </div>
      )}
    </div>
  );
};

export default BacteriologyResultsContainer;

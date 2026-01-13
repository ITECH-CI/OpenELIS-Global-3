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
}) => {
  const [currentPage, setCurrentPage] = useState(1);
  const itemsPerPage = 5; // 5 ordonnances par page

  // Group test results by accessionNumber (lab number)
  const groupedResults = useMemo(() => {
    const groups = {};

    testResults.forEach((test) => {
      const accessionNumber = test.accessionNumber || "UNKNOWN";
      if (!groups[accessionNumber]) {
        groups[accessionNumber] = [];
      }
      groups[accessionNumber].push(test);
    });

    return groups;
  }, [testResults]);

  // Get array of accessionNumbers sorted alphabetically
  const accessionNumbers = useMemo(() => {
    return Object.keys(groupedResults).sort((a, b) => a.localeCompare(b));
  }, [groupedResults]);

  // Pagination
  const totalItems = accessionNumbers.length;
  const totalPages = Math.ceil(totalItems / itemsPerPage);
  const startIndex = (currentPage - 1) * itemsPerPage;
  const endIndex = startIndex + itemsPerPage;
  const paginatedAccessionNumbers = accessionNumbers.slice(startIndex, endIndex);

  if (accessionNumbers.length === 0) {
    return (
      <div style={{ padding: "2rem", textAlign: "center" }}>
        No bacteriology results found.
      </div>
    );
  }

  return (
    <div>
      {/* Titre unique pour toute la page */}
      <h2 style={{ marginBottom: "1.5rem" }}>
        <FormattedMessage id="bacteriology.title" />
      </h2>

      {/* Accordéon pour chaque numéro de labo */}
      <Accordion>
        {paginatedAccessionNumbers.map((accessionNumber, index) => {
          const testsForAccession = groupedResults[accessionNumber];
          const primaryAnalysisId = testsForAccession[0]?.analysisId;
          const firstTest = testsForAccession[0];

          // Titre de l'accordéon avec infos clés
          const accordionTitle = `${accessionNumber} - ${firstTest?.patientInfo || "N/A"} - ${firstTest?.sampleType || "N/A"}`;

          return (
            <AccordionItem
              key={accessionNumber}
              title={accordionTitle}
              open={index === 0} // Premier élément ouvert par défaut
            >
              <div style={{ padding: "1rem" }}>
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
          );
        })}
      </Accordion>

      {/* Pagination si plus de 5 ordonnances */}
      {totalPages > 1 && (
        <div style={{ marginTop: "2rem", display: "flex", justifyContent: "center" }}>
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

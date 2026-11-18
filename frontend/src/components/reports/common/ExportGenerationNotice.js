import React from "react";
import { InlineLoading, InlineNotification } from "@carbon/react";
import { useIntl } from "react-intl";

/**
 * Retour de la génération d'un export : en cours, fichier produit (nom et
 * nombre de lignes), ou échec avec sa cause.
 */
const ExportGenerationNotice = ({ generation }) => {
  const intl = useIntl();

  if (generation.status === "running") {
    return (
      <InlineLoading
        description={intl.formatMessage({ id: "report.export.generating" })}
      />
    );
  }
  if (generation.status === "done") {
    return (
      <InlineNotification
        kind="success"
        lowContrast
        hideCloseButton
        title={intl.formatMessage(
          { id: "report.export.done" },
          { fileName: generation.fileName, rowCount: generation.rowCount },
        )}
      />
    );
  }
  if (generation.status === "failed") {
    const noData = /no data returned/i.test(generation.detail || "");
    return (
      <InlineNotification
        kind={noData ? "info" : "error"}
        lowContrast
        hideCloseButton
        title={intl.formatMessage({
          id: noData ? "report.export.nodata" : "report.export.failed",
        })}
        subtitle={noData ? undefined : generation.detail}
      />
    );
  }
  return null;
};

export default ExportGenerationNotice;

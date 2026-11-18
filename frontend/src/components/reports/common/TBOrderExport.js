import React, { useState } from "react";
import {
  Form,
  FormLabel,
  Grid,
  Column,
  Section,
  Button,
  Loading,
} from "@carbon/react";
import ExportGenerationNotice from "./ExportGenerationNotice";
import { FormattedMessage, useIntl } from "react-intl";
import "../../Style.css";
import CustomDatePicker from "../../common/CustomDatePicker";
import config from "../../../config.json";
import { encodeDate } from "../../utils/Utils";
import { downloadReportFile } from "../../utils/ReportDownload";

/**
 * Export CSV par période (commandes TB par défaut ; réutilisé pour les
 * demandes électroniques via les props report / titleKey / descriptionKey).
 */
const TBOrderExport = ({
  report = "TBOrderExport",
  titleKey = "report.tb.export.title",
  descriptionKey = "report.tb.export.description",
}) => {
  const intl = useIntl();
  const [loading, setLoading] = useState(false);
  const [generation, setGeneration] = useState({ status: "idle" });

  const [reportFormValues, setReportFormValues] = useState({
    startDate: null,
    endDate: null,
    error: null,
  });

  const handleDatePickerChangeDate = (datePicker, date) => {
    let updatedDate = encodeDate(date);
    let obj = null;
    switch (datePicker) {
      case "startDate":
        obj = {
          ...reportFormValues,
          startDate: updatedDate,
        };
        break;
      case "endDate":
        obj = {
          ...reportFormValues,
          endDate: updatedDate,
        };
        break;
      default:
    }
    setReportFormValues(obj);
  };

  const handleSubmit = () => {
    // Validate required fields
    if (!reportFormValues.startDate || !reportFormValues.endDate) {
      setReportFormValues({
        ...reportFormValues,
        error: intl.formatMessage({
          id: "error.dateRange.start",
          defaultMessage: "Please select Start and end date.",
        }),
      });
      return;
    }

    setReportFormValues({
      ...reportFormValues,
      error: "",
    });

    setLoading(true);

    // Build URL for TB Order Export (CSV)
    const baseUrl = `${config.serverBaseUrl}/ReportPrint`;
    const params = `report=${report}&type=patient&upperDateRange=${reportFormValues.endDate}&lowerDateRange=${reportFormValues.startDate}`;
    const url = `${baseUrl}?${params}`;

    setGeneration({ status: "running" });
    downloadReportFile(url, `${report}.csv`)
      .then(({ fileName, rowCount }) =>
        setGeneration({ status: "done", fileName, rowCount }),
      )
      .catch((error) =>
        setGeneration({ status: "failed", detail: error.message }),
      )
      .finally(() => setLoading(false));
  };

  return (
    <>
      {loading && <Loading description="Loading..." />}
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <Section>
            <Section>
              <h2>
                <FormattedMessage id={titleKey} />
              </h2>
              <p>
                <FormattedMessage id={descriptionKey} />
              </p>
            </Section>
            <Form>
              <Grid fullWidth={true}>
                {/* Date Range */}
                <Column lg={4} md={4} sm={4}>
                  <FormLabel htmlFor="startDate">
                    <FormattedMessage id="label.button.startDate" />
                  </FormLabel>
                  <CustomDatePicker
                    id="startDate"
                    value={reportFormValues.startDate}
                    onChange={(date) =>
                      handleDatePickerChangeDate("startDate", date)
                    }
                  />
                </Column>

                <Column lg={4} md={4} sm={4}>
                  <FormLabel htmlFor="endDate">
                    <FormattedMessage id="label.button.endDate" />
                  </FormLabel>
                  <CustomDatePicker
                    id="endDate"
                    value={reportFormValues.endDate}
                    onChange={(date) =>
                      handleDatePickerChangeDate("endDate", date)
                    }
                  />
                </Column>

                {/* Error Message */}
                {reportFormValues.error && (
                  <Column lg={16} md={8} sm={4}>
                    <div style={{ color: "red", marginTop: "1rem" }}>
                      {reportFormValues.error}
                    </div>
                  </Column>
                )}

                {/* Submit Button */}
                <Column lg={16} md={8} sm={4}>
                  <Button
                    onClick={handleSubmit}
                    style={{ marginTop: "2rem" }}
                    type="button"
                    disabled={generation.status === "running"}
                  >
                    <FormattedMessage id="label.button.generateFile" />
                  </Button>
                  <ExportGenerationNotice generation={generation} />
                </Column>
              </Grid>
            </Form>
          </Section>
        </Column>
      </Grid>
    </>
  );
};

export default TBOrderExport;

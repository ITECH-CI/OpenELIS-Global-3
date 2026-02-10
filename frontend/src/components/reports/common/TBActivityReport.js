import {
  Button,
  Column,
  Form,
  FormLabel,
  Grid,
  Loading,
  Section,
  Select,
  SelectItem,
} from "@carbon/react";
import { useEffect, useState } from "react";
import { FormattedMessage, useIntl } from "react-intl";
import config from "../../../config.json";
import "../../Style.css";
import CustomDatePicker from "../../common/CustomDatePicker";
import { AlertDialog } from "../../common/CustomNotification";
import { encodeDate, getFromOpenElisServer } from "../../utils/Utils";

/**
 * TB Activity Report Form
 * Allows selection of report type (GeneXpert MTB or Microscopy) and date range
 */
const TBActivityReport = () => {
  const intl = useIntl();
  const [loading, setLoading] = useState(false);
  const [notificationVisible, setNotificationVisible] = useState(false);
  const [reportTypes, setReportTypes] = useState([]);

  const [reportFormValues, setReportFormValues] = useState({
    startDate: null,
    endDate: null,
    reportType: "", // GENEXPERT_MTB_REPORT or MICROSCOPY_REPORT
    error: null,
  });

  useEffect(() => {
    // Fetch TB activity report types from backend
    getFromOpenElisServer("/rest/displayList/TB_ACTIVITY_REPORT", (data) => {
      setReportTypes(data || []);
      setLoading(false);
    });
  }, []);

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

  const handleReportTypeChange = (e) => {
    setReportFormValues({
      ...reportFormValues,
      reportType: e.target.value,
    });
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

    if (!reportFormValues.reportType) {
      setReportFormValues({
        ...reportFormValues,
        error: intl.formatMessage({
          id: "error.tb.reporttype",
          defaultMessage: "Please select a report type.",
        }),
      });
      return;
    }

    setReportFormValues({
      ...reportFormValues,
      error: "",
    });

    setLoading(true);

    // Build URL for TB Activity Report
    const baseUrl = `${config.serverBaseUrl}/ReportPrint`;
    const params = `report=TBOrderReport&type=indicator&selectList.selection=${reportFormValues.reportType}&upperDateRange=${reportFormValues.endDate}&lowerDateRange=${reportFormValues.startDate}`;
    const url = `${baseUrl}?${params}`;

    window.open(url, "_blank");
    setLoading(false);
    setNotificationVisible(true);
  };

  return (
    <>
      {notificationVisible === true ? (
        <AlertDialog
          title={intl.formatMessage({ id: "notification.title" })}
          message={intl.formatMessage({ id: "notification.report.generated" })}
          open={notificationVisible}
          onClose={() => {
            setNotificationVisible(false);
          }}
        />
      ) : null}
      {loading && <Loading description="Loading..." />}
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <Section>
            <Section>
              <h2>
                <FormattedMessage id="report.tb.activity.title" />
              </h2>
              <p>
                <FormattedMessage id="report.tb.activity.description" />
              </p>
            </Section>
            <Form>
              <Grid fullWidth={true}>
                {/* Report Type Selection */}
                <Column lg={16} md={8} sm={4}>
                  <FormLabel htmlFor="reportType">
                    <FormattedMessage id="report.tb.activity.reporttype" />
                  </FormLabel>
                  <Select
                    id="reportType"
                    labelText=""
                    value={reportFormValues.reportType}
                    onChange={handleReportTypeChange}
                  >
                    <SelectItem
                      text={intl.formatMessage({
                        id: "report.select.placeholder",
                      })}
                      value=""
                    />
                    {reportTypes.map((reportType, index) => (
                      <SelectItem
                        key={index}
                        text={reportType.value}
                        value={reportType.id}
                      />
                    ))}
                  </Select>
                </Column>

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
                  >
                    <FormattedMessage id="label.button.generateReport" />
                  </Button>
                </Column>
              </Grid>
            </Form>
          </Section>
        </Column>
      </Grid>
    </>
  );
};

export default TBActivityReport;

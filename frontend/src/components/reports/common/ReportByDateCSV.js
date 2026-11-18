import React, { useContext, useEffect, useState } from "react";
import {
  Form,
  FormLabel,
  Grid,
  Column,
  Section,
  Button,
  Select,
  SelectItem,
  Loading,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import "../../Style.css";
import { AlertDialog } from "../../common/CustomNotification";
import CustomDatePicker, {
  isFutureDisplayDate,
} from "../../common/CustomDatePicker";
import { ConfigurationContext } from "../../layout/Layout";
import config from "../../../config.json";
import { encodeDate, getFromOpenElisServer } from "../../utils/Utils";

const ReportByDateCSV = (props) => {
  const intl = useIntl();
  const { configurationProperties } = useContext(ConfigurationContext);
  const dayFirst = configurationProperties.DEFAULT_DATE_LOCALE == "fr-FR";
  const [futureDates, setFutureDates] = useState({
    startDate: false,
    endDate: false,
  });
  const [loading, setLoading] = useState(false);
  const [notificationVisible, setNotificationVisible] = useState(false);
  const [statusOptions, setStatusOptions] = useState([]);

  const [reportFormValues, setReportFormValues] = useState(() => {
    if (props.report === "CIStudyExport") {
      return {
        startDate: null,
        endDate: null,
        error: null,
        studyType: null,
        dateType: null,
      };
    } else {
      return {
        startDate: null,
        endDate: null,
        error: null,
        studyType: null,
      };
    }
  });

  const handleDatePickerChangeDate = (datePicker, date) => {
    setFutureDates((previous) => ({
      ...previous,
      [datePicker]: isFutureDisplayDate(date, dayFirst),
    }));
    let updatedDate = encodeDate(date);
    let obj = null;
    switch (datePicker) {
      case "startDate":
        obj = {
          ...reportFormValues,
          startDate: updatedDate,
          error: "",
        };
        break;
      case "endDate":
        obj = {
          ...reportFormValues,
          endDate: updatedDate,
          error: "",
        };
        break;
      default:
    }
    setReportFormValues(obj);
  };

  const handleSubmit = () => {
    if (futureDates.startDate || futureDates.endDate) {
      setReportFormValues({
        ...reportFormValues,
        error: intl.formatMessage({
          id: futureDates.endDate
            ? "error.date.future.end"
            : "error.date.future.start",
        }),
      });
      return;
    }
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

    if (!reportFormValues.studyType) {
      setReportFormValues({
        ...reportFormValues,
        error: intl.formatMessage({
          id: "error.report.csv.study",
          defaultMessage: "Please select study type.",
        }),
      });
      return;
    }
    if (props.report === "CIStudyExport" && !reportFormValues.dateType) {
      setReportFormValues({
        ...reportFormValues,
        error: intl.formatMessage({
          id: "error.report.csv.dateType",
          defaultMessage: "Please select date type.",
        }),
      });
      return;
    }

    setReportFormValues({
      ...reportFormValues,
      error: "",
    });

    setLoading(true);

    const baseParams = `report=${props.report}&type=patient`;
    const baseUrl = `${config.serverBaseUrl}/ReportPrint`;
    const additionalParams =
      props.report === "CIStudyExport"
        ? `projectCode=${reportFormValues.studyType}&dateType=${reportFormValues.dateType}`
        : `vlStudyType=${reportFormValues.studyType}`;
    const url = `${baseUrl}?${baseParams}&upperDateRange=${reportFormValues.endDate}&lowerDateRange=${reportFormValues.startDate}&${additionalParams}`;

    window.open(url, "_blank");
    setLoading(false);
    setNotificationVisible(true);
  };

  useEffect(() => {
    if (props.report === "CIStudyExport") {
      getFromOpenElisServer("/rest/projects", (data) => {
        setStatusOptions(data);
      });
    } else if (props.report === "EIDTrends") {
      getFromOpenElisServer("/rest/eidprojects", (data) => {
        setStatusOptions(data);
      });
    } else {
      getFromOpenElisServer("/rest/trendsprojects", (data) => {
        setStatusOptions(data);
      });
    }
  }, [props]);

  // Libellés fixes renvoyés par le serveur (leur valeur pilote le filtrage de
  // l'export) : traduits à l'affichage seulement.
  const studyTypeLabelKeys = {
    "Unsuppressed VL": "report.vl.unsuppressed",
    "Suppressed VL": "report.vl.suppressed",
  };
  const studyTypeLabel = (value) =>
    studyTypeLabelKeys[value]
      ? intl.formatMessage({ id: studyTypeLabelKeys[value] })
      : value;

  const dateOptions = [
    {
      text: "Order Date",
      value: "ORDER_DATE",
    },
    {
      text: "Result Date",
      value: "RESULT_DATE",
    },
    {
      text: "Print Date",
      value: "PRINT_DATE",
    },
  ];

  return (
    <>
      <Grid>
        <Column lg={16} md={8} sm={4}>
          <FormLabel>
            <Section>
              <Section>
                <h1>
                  <FormattedMessage id={props.id} />
                </h1>
              </Section>
            </Section>
          </FormLabel>
        </Column>
      </Grid>
      {notificationVisible && <AlertDialog />}
      {loading && <Loading />}
      <Grid fullWidth={true}>
        <Column lg={16} md={6} sm={4}>
          <Form>
            <Grid fullWidth={true}>
              <Column lg={4} md={4} sm={4}>
                <CustomDatePicker
                  key="startDate"
                  id={"startDate"}
                  labelText={intl.formatMessage({
                    id: "eorder.date.start",
                    defaultMessage: "Start Date",
                  })}
                  disallowFutureDate={true}
                  futureDateErrorText={intl.formatMessage({
                    id: "error.date.future.start",
                  })}
                  autofillDate={true}
                  value={reportFormValues.startDate}
                  onChange={(date) =>
                    handleDatePickerChangeDate("startDate", date)
                  }
                />
              </Column>
              <Column lg={4} md={4} sm={4}>
                <CustomDatePicker
                  key="endDate"
                  id={"endDate"}
                  labelText={intl.formatMessage({
                    id: "eorder.date.end",
                    defaultMessage: "End Date",
                  })}
                  disallowFutureDate={true}
                  futureDateErrorText={intl.formatMessage({
                    id: "error.date.future.end",
                  })}
                  autofillDate={true}
                  value={reportFormValues.endDate}
                  onChange={(date) =>
                    handleDatePickerChangeDate("endDate", date)
                  }
                />
              </Column>
              <Column lg={16}>
                {" "}
                <br />
              </Column>
              <Column lg={8} md={4} sm={4}>
                <Select
                  id="studyType"
                  labelText={intl.formatMessage({
                    id: "report.select.studttype",
                  })}
                  value={reportFormValues.studyType}
                  onChange={(e) => {
                    setReportFormValues({
                      ...reportFormValues,
                      studyType: e.target.value,
                    });
                  }}
                >
                  <SelectItem
                    value=""
                    text={intl.formatMessage({
                      id: "report.select.studttype",
                    })}
                  />

                  {statusOptions.map((statusOption) => (
                    <SelectItem
                      key={statusOption.id}
                      value={statusOption.id}
                      text={studyTypeLabel(statusOption.value)}
                    />
                  ))}
                </Select>
              </Column>
              <Column lg={16}>
                {" "}
                <br />
              </Column>
              <Column lg={8} md={4} sm={4}>
                {props.report === "CIStudyExport" ? (
                  <Select
                    id="dateType"
                    labelText={intl.formatMessage({
                      id: "report.label.site.dateType",
                    })}
                    value={reportFormValues.dateType}
                    onChange={(e) => {
                      setReportFormValues({
                        ...reportFormValues,
                        dateType: e.target.value,
                      });
                    }}
                  >
                    <SelectItem value="" text="Select Date Type" />
                    {dateOptions.map((dateOption) => (
                      <SelectItem
                        key={dateOption.value}
                        value={dateOption.value}
                        text={dateOption.text}
                      />
                    ))}
                  </Select>
                ) : (
                  <div></div>
                )}
              </Column>
            </Grid>
            <br />
            <Section>
              <br />
              {reportFormValues.error !== "" && (
                <div style={{ color: "#c62828", margin: 4 }}>
                  {reportFormValues.error}
                </div>
              )}

              <Button
                data-cy="printableVersion"
                type="button"
                onClick={handleSubmit}
              >
                <FormattedMessage id="label.button.generateFile" />
              </Button>
            </Section>
          </Form>
        </Column>
      </Grid>
    </>
  );
};

export default ReportByDateCSV;

import React, { useContext, useState, useEffect } from "react";
import { AlertDialog } from "../../common/CustomNotification";
import { NotificationContext } from "../../layout/Layout";
import { Loading } from "@carbon/react";
import { injectIntl, useIntl } from "react-intl";
import PatientStatusReport from "../common/PatientStatusReport";
import ReportByDate from "../common/ReportByDate";
import PageBreadCrumb from "../../common/PageBreadCrumb";

export const BacteriologyReports = (props) => {
  const { type, report } = props;

  return (
    <>
      {type === "patient" && report === "BacterioPatientReport" && (
        <PatientStatusReport
          report={"BacterioPatientReport"}
          id={"sidenav.label.bacterio.patientreport"}
        />
      )}

      {type === "export" && report === "bacteriologyCSV" && (
        <ReportByDate
          report={"bacteriologyCSV"}
          id={"sidenav.label.bacterio.csvexport"}
        />
      )}
    </>
  );
};

const BacteriologyIndex = () => {
  const intl = useIntl();
  const { setNotificationVisible, addNotification, notificationVisible } =
    useContext(NotificationContext);

  const [type, setType] = useState("");
  const [report, setReport] = useState("");
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const paramType = params.get("type");
    const paramReport = params.get("report");
    setType(paramType);
    setReport(paramReport);

    if (paramType && paramReport) {
      setIsLoading(false);
    } else {
      window.location.href = "/BacteriologyReports";
    }
  }, []);

  return (
    <>
      <br />
      <PageBreadCrumb
        breadcrumbs={[
          { label: "home.label", link: "/" },
          {
            label: "banner.menu.microbiology_classic",
            link: "/BacteriologyReports",
          },
        ]}
      />
      <div className="orderLegendBody">
        {notificationVisible === true && <AlertDialog />}
        {isLoading && <Loading />}
        {!isLoading && <BacteriologyReports type={type} report={report} />}
      </div>
    </>
  );
};

export default injectIntl(BacteriologyIndex);

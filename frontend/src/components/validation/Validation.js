import { Copy } from "@carbon/icons-react";
import {
  Button,
  Checkbox,
  Column,
  Form,
  Grid,
  Pagination,
  TextArea,
} from "@carbon/react";
import { Field, Formik } from "formik";
import { useContext, useEffect, useRef, useState } from "react";
import DataTable from "react-data-table-component";
import { FormattedMessage, useIntl } from "react-intl";
import config from "../../config.json";
import { NotificationKinds } from "../common/CustomNotification";
import SiValueDisplay from "../common/SiValueDisplay";
import { priorities } from "../data/orderOptions";
import ValidationSearchFormValues from "../formModel/innitialValues/ValidationSearchFormValues";
import { ConfigurationContext, NotificationContext } from "../layout/Layout";
import { validateNumericResults } from "../utils/ResultValidationUtils";
import {
  convertAlphaNumLabNumForDisplay,
  postToOpenElisServer,
} from "../utils/Utils";
import BacteriologyValidation from "../bacteriology/BacteriologyValidation";
import "../Style.css";

// Each test's accessionNumber carries a per-test suffix (e.g. "LY24001731-1",
// "LY24001731-2", ...), so grouping "same sample" rows must compare the LabNo
// (the part before the dash), not the raw accessionNumber.
const getLabNo = (accessionNumber) =>
  accessionNumber ? accessionNumber.split("-")[0] : null;

const Validation = (props) => {
  const componentMounted = useRef(false);

  const { setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const { configurationProperties } = useContext(ConfigurationContext);

  const intl = useIntl();

  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(100);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [, forceUpdate] = useState({});
  const [validationState, setValidationState] = useState({});
  const [bacteriologyPage, setBacteriologyPage] = useState(1);
  const [bacteriologyPageSize, setBacteriologyPageSize] = useState(5);
  // Only one sample's interpretation panel can be open at a time. A single
  // scalar (rather than a per-LabNo map) avoids any risk of the
  // expand/collapse state for one sample leaking onto another. Grouped by
  // LabNo (accessionNumber without its per-test suffix) since that's the
  // reliable "same sample" key - sampleId turned out not to be trustworthy.
  const [expandedLabNo, setExpandedLabNo] = useState(null);

  useEffect(() => {
    componentMounted.current = true;
    return () => {
      componentMounted.current = false;
    };
  }, [props.results]);

  // Validate numeric results for conditional formatting
  useEffect(() => {
    if (props.results?.resultList) {
      let newValidationState = {};
      props.results.resultList.forEach((row) => {
        if (row.resultType === "N" && row.result) {
          const validation = validateNumericResults(row.result, row);

          // Add CSS classes based on validation
          const classes = [];
          if (validation.outsideValid) {
            classes.push("result-outside-valid");
          } else if (validation.outsideNormal) {
            classes.push("result-outside-normal");
          }

          if (validation.isCritical) {
            classes.push("result-critical");
          } else if (validation.isInvalid) {
            classes.push("result-invalid");
          }

          validation.className = classes.join(" ");
          newValidationState[row.id] = validation;
        }
      });
      setValidationState(newValidationState);
    }
  }, [props.results]);

  const columns = [
    {
      id: "priority",
      name: intl.formatMessage({ id: "column.name.priority" }),
      cell: (row, index, column, id) => {
        return renderCell(row, index, column, id);
      },
      sortable: true,
      selector: (row) => row.priority,
      width: "5rem",
    },
    {
      id: "sampleInfo",
      name: intl.formatMessage({ id: "column.name.sampleInfo" }),
      cell: (row, index, column, id) => {
        return renderCell(row, index, column, id);
      },
      selector: (row) => row.accessionNumber,
      sortable: true,
      width: "16rem",
    },
    {
      id: "testName",
      name: intl.formatMessage({ id: "column.name.testName" }),
      selector: (row) => row.testName,
      cell: (row, index, column, id) => {
        return renderCell(row, index, column, id);
      },
      sortable: true,
      width: "15rem",
    },
    {
      id: "normalRange",
      name: intl.formatMessage({ id: "column.name.normalRange" }),
      selector: (row) => row.normalRange,
      sortable: true,
      width: "8rem",
    },
    {
      id: "result",
      name: intl.formatMessage({ id: "column.name.result" }),
      cell: (row, index, column, id) => {
        return renderCell(row, index, column, id);
      },
      width: "8rem",
    },
    {
      id: "save",
      name: intl.formatMessage({ id: "column.name.save" }),
      cell: (row, index, column, id) => {
        return renderCell(row, index, column, id);
      },
      width: "8rem",
    },
    {
      id: "retest",
      name: intl.formatMessage({ id: "column.name.retest" }),
      cell: (row, index, column, id) => {
        return renderCell(row, index, column, id);
      },
      width: "8rem",
    },
    {
      id: "notes",
      name: intl.formatMessage({ id: "column.name.notes" }),
      cell: (row, index, column, id) => {
        return renderCell(row, index, column, id);
      },
      width: "15rem",
    },
    {
      id: "pastNotes",
      name: intl.formatMessage({ id: "column.name.pastNotes" }),
      cell: (row, index, column, id) => {
        return renderCell(row, index, column, id);
      },
      width: "28rem",
    },
  ];

  const handleSave = (values) => {
    if (isSubmitting) {
      return;
    }
    setIsSubmitting(true);
    postToOpenElisServer(
      "/rest/AccessionValidation",
      JSON.stringify(props.results),
      handleResponse,
    );
  };
  const handleResponse = (status) => {
    let message = intl.formatMessage({ id: "validation.save.error" });
    let kind = NotificationKinds.error;
    setIsSubmitting(false);
    if (status == 200) {
      message = intl.formatMessage({ id: "validation.save.success" });
      kind = NotificationKinds.success;
      window.location.href = "/validation" + props.params;
    }
    addNotification({
      kind: kind,
      title: intl.formatMessage({ id: "notification.title" }),
      message: message,
    });
    setNotificationVisible(true);
  };

  const handlePageChange = (pageInfo) => {
    if (page != pageInfo.page) {
      setPage(pageInfo.page);
    }
    if (pageSize != pageInfo.pageSize) {
      setPageSize(pageInfo.pageSize);
    }
  };

  const handleChange = (e, rowId) => {
    const { name, id, value } = e.target;
    let form = props.results;
    var jp = require("jsonpath");
    jp.value(form, name, value);
  };

  const handleDatePickerChange = (date, rowId) => {
    console.debug("handleDatePickerChange:" + date);
    const d = new Date(date).toLocaleDateString("fr-FR");
    var form = props.results;
    var jp = require("jsonpath");
    jp.value(form, "resultList[" + rowId + "].sentDate_", d);
  };
  const handleCheckBox = (e, rowId) => {
    const { name, id, checked } = e.target;
    let form = props.results;
    var jp = require("jsonpath");
    jp.value(form, name, checked);
  };

  const handleAutomatedCheck = (checked, name) => {
    let form = props.results;
    var jp = require("jsonpath");
    jp.value(form, name, checked);
  };

  const handleInterpretationChange = (e, labNo) => {
    const { value } = e.target;
    const limitedValue = value.slice(0, 199);
    let form = props.results;
    var jp = require("jsonpath");

    // Update interpretation for all results sharing the same LabNo, since a
    // sample's tests can span several rows (each with its own accessionNumber
    // suffix).
    if (form.resultList) {
      form.resultList.forEach((result, index) => {
        if (getLabNo(result.accessionNumber) === labNo) {
          jp.value(
            form,
            `resultList[${index}].sampleInterpretation`,
            limitedValue,
          );
        }
      });
      // Force re-render to update the UI
      forceUpdate({});
    }
  };

  // Helper function to detect if a result is bacteriology
  const isBacteriologyResult = (result) => {
    if (!result) return false;
    const testName = result.testName || "";
    // The result payload exposes the section as testSectionName; keep testSection
    // as a fallback for any caller that still sets the older field.
    const testSectionName = result.testSectionName || result.testSection || "";

    // Log for debugging
    console.log("[Validation] Checking result:", {
      accessionNumber: result.accessionNumber,
      testName: testName,
      testSection: testSectionName,
      testId: result.testId,
    });

    // A result belongs to the bacteriology workflow when either its test section
    // is a bacteriology section, or its name carries a macro/micro/culture
    // keyword. Chemistry tests (Glucose, Protéine) belong to bacteriology too but
    // their names ("Glucose", "Protéine") are also common standalone biochemistry
    // tests — so they only count as bacteriology when the SECTION is bacteriology,
    // never on the name alone (otherwise a routine serum Glucose would be pulled
    // out of the standard validation grid).
    const lowerSection = testSectionName.toLowerCase();
    const lowerTestName = testName.toLowerCase();
    const isBacterioSection =
      lowerSection.includes("bacteriology") ||
      lowerSection.includes("bactériologie") ||
      lowerSection.includes("routine bacteriology");
    const isBacterio =
      isBacterioSection ||
      lowerTestName.includes("macroscopie") ||
      lowerTestName.includes("microscopie") ||
      lowerTestName.includes("culture");

    console.log("[Validation] Is bacteriology?", isBacterio);
    return isBacterio;
  };

  // Group results by sample and bacteriology status
  const groupResultsBySample = () => {
    if (!props.results?.resultList) {
      return { bacteriology: [], standard: [] };
    }

    const bacteriologySamples = new Map(); // accessionNumber -> { analysisId, results }
    const standardResults = [];

    props.results.resultList.forEach((result, index) => {
      // A stable, guaranteed-unique row key: the backend doesn't always
      // populate a per-row id, and sampleId repeats across a sample's
      // multiple tests, so neither is safe to use as the DataTable keyField.
      result.__rowKey = `row-${index}`;
      if (isBacteriologyResult(result)) {
        const key = result.accessionNumber;
        if (!bacteriologySamples.has(key)) {
          bacteriologySamples.set(key, {
            accessionNumber: result.accessionNumber,
            analysisId: result.analysisId,
            sampleId: result.sampleId,
            results: [],
          });
        }
        bacteriologySamples.get(key).results.push(result);
      } else {
        standardResults.push(result);
      }
    });

    const grouped = {
      bacteriology: Array.from(bacteriologySamples.values()),
      standard: standardResults,
    };

    console.log("[Validation] Grouped results:", {
      bacteriologyCount: grouped.bacteriology.length,
      standardCount: grouped.standard.length,
      bacteriologySamples: grouped.bacteriology,
    });

    return grouped;
  };

  const groupedResults = groupResultsBySample();

  // Only the last row of each sample (by LabNo) in the standard results
  // table carries the sample-interpretation field, so it isn't repeated once
  // per test when a sample has several results. Keyed by object reference
  // (not a row "id") since the backend doesn't populate a per-row id.
  const lastRowByLabNo = {};
  groupedResults.standard.forEach((result) => {
    const labNo = getLabNo(result.accessionNumber);
    if (labNo != null) {
      lastRowByLabNo[labNo] = result;
    }
  });

  const validateResults = (e, rowId) => {
    handleChange(e, rowId);
  };

  const findPriorityByValue = (searchValue) => {
    return priorities.find((item) => item.value === searchValue);
  };

  // Expandable panel shown below the last row of each sample (native
  // DataTable row-expansion, same pattern as SearchResultForm's referral row).
  const renderSampleInterpretation = ({ data: row }) => (
    <div style={{ padding: "12px 16px" }}>
      <label
        htmlFor={`interpretation-${getLabNo(row.accessionNumber)}`}
        style={{ display: "block", marginBottom: "5px", fontWeight: "500" }}
      >
        {intl.formatMessage({ id: "validation.sampleInterpretation.label" })}
      </label>
      <TextArea
        id={`interpretation-${getLabNo(row.accessionNumber)}`}
        labelText=""
        maxCount={200}
        placeholder={intl.formatMessage({
          id: "validation.sampleInterpretation.placeholder",
        })}
        value={row.sampleInterpretation || ""}
        onChange={(e) =>
          handleInterpretationChange(e, getLabNo(row.accessionNumber))
        }
        rows={3}
        style={{ width: "75%" }}
      />
    </div>
  );

  const renderCell = (row, index, column, id) => {
    let formatLabNum = configurationProperties.AccessionFormat === "ALPHANUM";
    const fullTestName = row.testName;
    const splitIndex = fullTestName.lastIndexOf("(");
    const testName = fullTestName.substring(0, splitIndex);
    const sampleType = fullTestName.substring(splitIndex);
    switch (column.id) {
      case "priority": {
        const priorityObj = priorities.find((p) => p.value === row.priority);
        return (
          <div
            style={{
              display: "flex",
              justifyContent: "center",
              alignItems: "center",
              height: "100%",
            }}
          >
            {priorityObj ? priorityObj.icon : null}
          </div>
        );
      }
      case "sampleInfo": {
        const labNo = getLabNo(row.accessionNumber);
        const isLastOfSample = labNo != null && lastRowByLabNo[labNo] === row;
        const isExpanded = labNo != null && labNo === expandedLabNo;
        return (
          <>
            <Button
              onClick={async () => {
                if ("clipboard" in navigator) {
                  return await navigator.clipboard.writeText(
                    row.accessionNumber,
                  );
                } else {
                  return document.execCommand(
                    "copy",
                    true,
                    row.accessionNumber,
                  );
                }
              }}
              kind="ghost"
              iconDescription={intl.formatMessage({
                id: "instructions.copy.labnum",
              })}
              hasIconOnly
              renderIcon={Copy}
            />
            <div className="sampleInfo" data-testid="LabNo">
              <br></br>
              {formatLabNum
                ? convertAlphaNumLabNumForDisplay(row.accessionNumber)
                : row.accessionNumber}
              <br></br>
              {isLastOfSample && (
                <button
                  type="button"
                  onClick={() => setExpandedLabNo(isExpanded ? null : labNo)}
                  style={{
                    background: "none",
                    border: "none",
                    padding: 0,
                    cursor: "pointer",
                    font: "inherit",
                    fontSize: "0.75rem",
                    color: "#0f62fe",
                    textDecoration: "underline",
                  }}
                >
                  {intl.formatMessage({
                    id: isExpanded
                      ? "validation.sampleInterpretation.hide"
                      : "validation.sampleInterpretation.add",
                  })}
                </button>
              )}
              <br></br>
            </div>
            {row.nonconforming && (
              <picture>
                <img
                  src={config.serverBaseUrl + "/images/nonconforming.gif"}
                  alt="nonconforming"
                  width="20"
                  height="15"
                />
              </picture>
            )}
          </>
        );
      }
      case "testName":
        return (
          <div className="sampleInfo" data-testid="sampleInfo">
            <br></br>
            {testName}
            <br></br>
            {sampleType}
          </div>
        );

      case "save":
        return (
          <>
            <div data-testid="Checkbox">
              <Field name="isAccepted">
                {({ field }) => (
                  <Checkbox
                    id={"resultList" + row.id + ".isAccepted"}
                    name={"resultList[" + row.id + "].isAccepted"}
                    labelText=""
                    value={true}
                    onChange={(e) => handleCheckBox(e, row.id)}
                  />
                )}
              </Field>
            </div>
          </>
        );

      case "retest":
        return (
          <>
            <Field name="isRejected">
              {({ field }) => (
                <Checkbox
                  id={"resultList" + row.id + ".isRejected"}
                  name={"resultList[" + row.id + "].isRejected"}
                  labelText=""
                  value={true}
                  onChange={(e) => handleCheckBox(e, row.id)}
                />
              )}
            </Field>
          </>
        );

      case "notes":
        return (
          <>
            <div className="note">
              <TextArea
                id={"resultList" + row.id + ".note"}
                name={"resultList[" + row.id + "].note"}
                disabled={false}
                type="text"
                labelText=""
                rows={2}
                onChange={(e) => handleChange(e, row.id)}
              ></TextArea>
            </div>
          </>
        );

      case "pastNotes":
        return (
          <>
            <div
              className="note"
              dangerouslySetInnerHTML={{ __html: row.pastNotes }}
            />
          </>
        );

      case "result":
        switch (row.resultType) {
          case "M":
          case "C":
          case "D":
            return (
              <>
                {
                  row.dictionaryResults.find(
                    (result) => result.id == row.result,
                  )?.value
                }
              </>
            );
          default: {
            // Get validation classes for numeric results
            const validation = validationState[row.id];
            const className = validation?.className || "";

            // Display numeric results with SI conversion if available
            if (row.valueSi && row.uomSiName) {
              return (
                <div className={className}>
                  <SiValueDisplay
                    traditionalValue={row.result}
                    traditionalUom={row.unitOfMeasureName || ""}
                    siValue={row.valueSi}
                    siUom={row.uomSiName}
                    className="compact"
                    showTooltip={true}
                    significantDigits={2}
                  />
                </div>
              );
            }
            return (
              <span className={className}>
                {row.result}
                {row.unitOfMeasureName && (
                  <span className="uom">
                    {"\u00a0"}
                    {row.unitOfMeasureName}
                  </span>
                )}
              </span>
            );
          }
        }

      default:
    }
    return row.result;
  };

  // Check if there are any non-bacteriology results
  const hasStandardResults = props.results?.resultList?.some(
    (result) => !isBacteriologyResult(result),
  );

  return (
    <>
      {props.results?.resultList?.length > 0 && hasStandardResults && (
        <Grid style={{ marginTop: "20px" }} className="gridBoundary">
          <Column lg={7} md={8} sm={2}>
            <picture>
              <img
                src={config.serverBaseUrl + "/images/nonconforming.gif"}
                alt="nonconforming"
                width="25" // Set your desired width
                height="20" // Set your desired height
              />
            </picture>
            <b>
              {" "}
              <FormattedMessage id="validation.label.nonconform" />
            </b>
            <br />
            {findPriorityByValue("ASAP").icon} ={" "}
            <FormattedMessage id="result.priority.asap" />
            <br />
            {findPriorityByValue("STAT").icon} ={" "}
            <FormattedMessage id="result.priority.stat" />
          </Column>
          <Column lg={3} md={2} sm={4}>
            <Checkbox
              id={"saveallnormal"}
              name={"autochecks"}
              labelText={intl.formatMessage({ id: "validation.accept.normal" })}
              onChange={(e) => {
                const nomalResults = props.results.resultList?.filter(
                  (result) =>
                    result.normal == true && !isBacteriologyResult(result),
                );
                nomalResults?.forEach((result) => {
                  const checkbox = document.getElementById(
                    "resultList" + result.id + ".isAccepted",
                  );
                  if (checkbox) {
                    checkbox.checked = e.target.checked;
                    handleAutomatedCheck(e.target.checked, checkbox.name);
                  }
                });
              }}
            />
          </Column>
          <Column lg={3} md={2} sm={4}>
            <Checkbox
              id={"saveallresults"}
              name={"autochecks"}
              labelText={intl.formatMessage({ id: "validation.accept.all" })}
              onChange={(e) => {
                const nomalResults = props.results.resultList?.filter(
                  (result) => !isBacteriologyResult(result),
                );
                nomalResults?.forEach((result) => {
                  const checkbox = document.getElementById(
                    "resultList" + result.id + ".isAccepted",
                  );
                  if (checkbox) {
                    checkbox.checked = e.target.checked;
                    handleAutomatedCheck(e.target.checked, checkbox.name);
                  }
                });
              }}
            />
          </Column>
          <Column lg={3} md={2} sm={4}>
            <Checkbox
              id={"retestalltests"}
              name={"autochecks"}
              labelText={intl.formatMessage({ id: "validation.reject.all" })}
              onChange={(e) => {
                const nomalResults = props.results.resultList?.filter(
                  (result) => !isBacteriologyResult(result),
                );
                nomalResults?.forEach((result) => {
                  const checkbox = document.getElementById(
                    "resultList" + result.id + ".isRejected",
                  );
                  if (checkbox) {
                    checkbox.checked = e.target.checked;
                    handleAutomatedCheck(e.target.checked, checkbox.name);
                  }
                });
              }}
            />
          </Column>
        </Grid>
      )}
      <Formik
        initialValues={ValidationSearchFormValues}
        //validationSchema={}
        onSubmit
        onChange
      >
        {({ values, errors, touched, handleChange }) => {
          return (
            <Form onChange={handleChange}>
              {/* Bacteriology Results Section */}
              {groupedResults.bacteriology.length > 0 && (
                <div style={{ marginTop: "20px", marginBottom: "40px" }}>
                  <div
                    style={{
                      display: "flex",
                      justifyContent: "space-between",
                      alignItems: "center",
                      marginBottom: "20px",
                    }}
                  >
                    <h4 style={{ margin: 0 }}>
                      <FormattedMessage id="bacteriology.validation.title" />
                    </h4>
                    <div
                      style={{
                        fontSize: "0.875rem",
                        color: "#525252",
                        fontWeight: "500",
                      }}
                    >
                      {groupedResults.bacteriology.length} échantillon
                      {groupedResults.bacteriology.length > 1 ? "s" : ""} |{" "}
                      {(() => {
                        const totalTests = groupedResults.bacteriology.reduce(
                          (sum, sample) => sum + (sample.results?.length || 0),
                          0,
                        );
                        return `${totalTests} test${totalTests > 1 ? "s" : ""}`;
                      })()}
                    </div>
                  </div>
                  {groupedResults.bacteriology
                    .slice(
                      (bacteriologyPage - 1) * bacteriologyPageSize,
                      bacteriologyPage * bacteriologyPageSize,
                    )
                    .map((bacterioSample) => {
                      // Extract test name and sample type from first result
                      let testName = "";
                      let sampleType = "";
                      if (
                        bacterioSample.results &&
                        bacterioSample.results.length > 0
                      ) {
                        const fullTestName =
                          bacterioSample.results[0].testName || "";
                        const splitIndex = fullTestName.lastIndexOf("(");
                        if (splitIndex > 0) {
                          testName = fullTestName
                            .substring(0, splitIndex)
                            .trim();
                          sampleType = fullTestName
                            .substring(splitIndex + 1, fullTestName.length - 1)
                            .trim();
                        } else {
                          testName = fullTestName;
                        }
                      }

                      return (
                        <div
                          key={bacterioSample.accessionNumber}
                          style={{ marginBottom: "30px" }}
                        >
                          <BacteriologyValidation
                            analysisId={bacterioSample.analysisId}
                            accessionNumber={bacterioSample.accessionNumber}
                            sampleId={bacterioSample.sampleId}
                            testName={testName}
                            sampleType={sampleType}
                            onSave={() => {
                              // Reload validation results after save
                              window.location.reload();
                            }}
                          />
                        </div>
                      );
                    })}
                  {groupedResults.bacteriology.length >
                    bacteriologyPageSize && (
                    <Pagination
                      page={bacteriologyPage}
                      pageSize={bacteriologyPageSize}
                      pageSizes={[5, 10, 20]}
                      totalItems={groupedResults.bacteriology.length}
                      onChange={({ page, pageSize }) => {
                        setBacteriologyPage(page);
                        setBacteriologyPageSize(pageSize);
                      }}
                    />
                  )}
                </div>
              )}

              {/* Standard Results DataTable (non-bacteriology only) */}
              {groupedResults.standard.length > 0 && (
                <>
                  <DataTable
                    data={groupedResults.standard.slice(
                      (page - 1) * pageSize,
                      page * pageSize,
                    )}
                    columns={columns}
                    isSortable
                    keyField="__rowKey"
                    expandableRows
                    expandableRowsHideExpander
                    expandableRowsComponent={renderSampleInterpretation}
                    expandableRowDisabled={(row) => {
                      const labNo = getLabNo(row.accessionNumber);
                      return !(labNo != null && lastRowByLabNo[labNo] === row);
                    }}
                    expandableRowExpanded={(row) => {
                      const labNo = getLabNo(row.accessionNumber);
                      if (labNo == null || lastRowByLabNo[labNo] !== row) {
                        // Never auto-expand (or allow expanding) anything but
                        // the one row that actually owns the panel for this
                        // LabNo - otherwise every row matching a search would
                        // each render their own copy of the panel.
                        return false;
                      }
                      const isSearched =
                        props.searchedAccessionNumber &&
                        labNo === props.searchedAccessionNumber.split("-")[0];
                      return labNo === expandedLabNo || isSearched;
                    }}
                    conditionalRowStyles={
                      props.searchedAccessionNumber
                        ? [
                            {
                              when: (row) =>
                                row.accessionNumber &&
                                row.accessionNumber.split("-")[0] ===
                                  props.searchedAccessionNumber.split("-")[0],
                              style: {
                                backgroundColor: "#fff3cd",
                                borderLeft: "4px solid #f0ad4e",
                                animation: "labno-pulse 1.6s ease-in-out 3",
                              },
                            },
                          ]
                        : []
                    }
                    customStyles={{
                      cells: {
                        style: {
                          "&:nth-child(5)": {
                            paddingLeft: "0px",
                            paddingRight: "0px",
                          },
                        },
                      },
                    }}
                  ></DataTable>
                  <Pagination
                    onChange={handlePageChange}
                    page={page}
                    pageSize={pageSize}
                    pageSizes={[10, 20, 30, 50, 100]}
                    totalItems={groupedResults.standard.length}
                    forwardText={intl.formatMessage({
                      id: "pagination.forward",
                    })}
                    backwardText={intl.formatMessage({
                      id: "pagination.backward",
                    })}
                    itemRangeText={(min, max, total) =>
                      intl.formatMessage(
                        { id: "pagination.item-range" },
                        { min: min, max: max, total: total },
                      )
                    }
                    itemsPerPageText={intl.formatMessage({
                      id: "pagination.items-per-page",
                    })}
                    itemText={(min, max) =>
                      intl.formatMessage(
                        { id: "pagination.item" },
                        { min: min, max: max },
                      )
                    }
                    pageNumberText={intl.formatMessage({
                      id: "pagination.page-number",
                    })}
                    pageRangeText={(_current, total) =>
                      intl.formatMessage(
                        { id: "pagination.page-range" },
                        { total: total },
                      )
                    }
                    pageText={(page, pagesUnknown) =>
                      intl.formatMessage(
                        { id: "pagination.page" },
                        { page: pagesUnknown ? "" : page },
                      )
                    }
                  />

                  <Button
                    type="button"
                    onClick={() => handleSave(values)}
                    id="submit"
                    style={{ marginTop: "16px" }}
                    data-testid="Save-btn"
                    disabled={isSubmitting}
                  >
                    <FormattedMessage id="label.button.save" />
                  </Button>
                </>
              )}
            </Form>
          );
        }}
      </Formik>
    </>
  );
};

export default Validation;

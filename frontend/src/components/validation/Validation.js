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

  const handleInterpretationChange = (e, sampleId) => {
    const { value } = e.target;
    const limitedValue = value.slice(0, 199);
    let form = props.results;
    var jp = require("jsonpath");

    // Update interpretation for all results with the same sampleId
    if (form.resultList) {
      form.resultList.forEach((result, index) => {
        if (result.sampleId === sampleId) {
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

  const getUniqueSamples = () => {
    if (!props.results?.resultList) {
      return [];
    }

    const samplesMap = new Map();
    props.results.resultList.forEach((result) => {
      if (result.sampleId && !samplesMap.has(result.sampleId)) {
        samplesMap.set(result.sampleId, {
          sampleId: result.sampleId,
          accessionNumber: result.accessionNumber,
          sampleInterpretation: result.sampleInterpretation || "",
        });
      }
    });
    return Array.from(samplesMap.values());
  };

  const validateResults = (e, rowId) => {
    handleChange(e, rowId);
  };

  const findPriorityByValue = (searchValue) => {
    return priorities.find((item) => item.value === searchValue);
  };

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
      case "sampleInfo":
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

  return (
    <>
      {props.results?.resultList?.length > 0 && (
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
                  (result) => result.normal == true,
                );
                nomalResults.forEach((result) => {
                  const checkbox = document.getElementById(
                    "resultList" + result.id + ".isAccepted",
                  );
                  checkbox.checked = e.target.checked;
                  handleAutomatedCheck(e.target.checked, checkbox.name);
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
                const nomalResults = props.results.resultList;
                nomalResults.forEach((result) => {
                  const checkbox = document.getElementById(
                    "resultList" + result.id + ".isAccepted",
                  );
                  checkbox.checked = e.target.checked;
                  handleAutomatedCheck(e.target.checked, checkbox.name);
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
                const nomalResults = props.results.resultList;
                nomalResults.forEach((result) => {
                  const checkbox = document.getElementById(
                    "resultList" + result.id + ".isRejected",
                  );
                  checkbox.checked = e.target.checked;
                  handleAutomatedCheck(e.target.checked, checkbox.name);
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
        {({ values, errors, touched, handleChange }) => (
          <Form onChange={handleChange}>
            {/* Sample Interpretation Section - One per unique sample */}
            {props.results?.resultList?.length > 0 &&
              getUniqueSamples().length > 0 && (
                <Grid
                  className="gridBoundary"
                  style={{ marginTop: "20px", marginBottom: "20px" }}
                >
                  <Column lg={16} md={8} sm={4}>
                    <h6 style={{ marginBottom: "10px", fontWeight: "bold" }}>
                      <FormattedMessage id="validation.sampleInterpretation.label" />
                    </h6>
                    {getUniqueSamples().map((sample) => (
                      <div
                        key={sample.sampleId}
                        style={{ marginBottom: "15px" }}
                      >
                        <label
                          style={{
                            display: "block",
                            marginBottom: "5px",
                            fontWeight: "500",
                          }}
                        >
                          {intl.formatMessage({ id: "column.name.sampleInfo" })}
                          :{" "}
                          {configurationProperties.AccessionFormat ===
                          "ALPHANUM"
                            ? convertAlphaNumLabNumForDisplay(
                                sample.accessionNumber,
                              )
                            : sample.accessionNumber}
                        </label>
                        <TextArea
                          id={`interpretation-${sample.sampleId}`}
                          labelText=""
                          maxCount={200}
                          placeholder={intl.formatMessage({
                            id: "validation.sampleInterpretation.placeholder",
                          })}
                          value={sample.sampleInterpretation || ""}
                          onChange={(e) =>
                            handleInterpretationChange(e, sample.sampleId)
                          }
                          rows={3}
                          style={{ width: "100%" }}
                        />
                      </div>
                    ))}
                  </Column>
                </Grid>
              )}
            <DataTable
              data={
                props.results
                  ? props?.results?.resultList?.slice(
                      (page - 1) * pageSize,
                      page * pageSize,
                    )
                  : []
              }
              columns={columns}
              isSortable
              customStyles={{
                cells: {
                  style: {
                    "&:nth-child(5)": {
                      // Target the result column (5th column)
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
              totalItems={
                props.results
                  ? props.results.resultList
                    ? props.results.resultList.length
                    : 0
                  : 0
              }
              forwardText={intl.formatMessage({ id: "pagination.forward" })}
              backwardText={intl.formatMessage({ id: "pagination.backward" })}
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
          </Form>
        )}
      </Formik>
    </>
  );
};

export default Validation;

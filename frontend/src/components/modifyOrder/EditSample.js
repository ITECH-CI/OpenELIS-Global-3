import React, { useEffect, useRef, useState } from "react";
import {
  Button,
  Checkbox,
  Column,
  DataTable,
  FilterableMultiSelect,
  Grid,
  Link,
  Pagination,
  RadioButton,
  RadioButtonGroup,
  Row,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableHeader,
  TableRow,
  Tag,
  TextInput,
} from "@carbon/react";
import { Add } from "@carbon/react/icons";
import { getFromOpenElisServer } from "../utils/Utils";
import SampleType from "../addOrder/SampleType";
import { FormattedMessage, useIntl } from "react-intl";
import {
  OrderCurrentTestsHeaders,
  OrderPossibleTestsHeaders,
} from "../data/orderCurrentTestsHeaders";

const BACTERIOLOGY_PROGRAM_CODE = "RTN_BACTER";

const ANTIBIOTHERAPY_CATEGORY = "THERAPEUTIC_ANTIBIOTICS";
const CLINICAL_INFO_CATEGORY = "CLINICAL_INFOS";
const INVASIVE_GESTURES_CATEGORY = "INVASIVE_GESTURES";
const INDWELLING_DEVICE_CATEGORY = "INDWELLING_DEVICES";

const OTHER_MATCHER = (opt) =>
  opt &&
  typeof opt.value === "string" &&
  opt.value.toLowerCase().startsWith("autre");

const CLINICAL_INFO_FALLBACK = [
  "Toux",
  "Fièvre",
  "Diarrhée",
  "Brûlure mictionnelle",
  "Ictère",
  "Ecoulement urétrale",
  "Bilan de fertilité",
  "Leucorrhées",
  "Pus",
  "Prurit",
  "Douleur abdominale",
  "Infection vaginale",
  "Autres",
].map((value) => ({ id: value, value }));

const INVASIVE_GESTURES_FALLBACK = [
  "cathétérisme",
  "sondage urinaire",
  "drain",
  "Intervention Chirurgicale",
].map((value) => ({ id: value, value }));

const INDWELLING_DEVICE_FALLBACK = [
  "Cathéter veineux",
  "Prothèse",
  "Sonde Urinaire",
  "matériel de chirurgie",
].map((value) => ({ id: value, value }));

const normalizeDictionaryOptions = (items, fallback) => {
  const mapped = (Array.isArray(items) ? items : [])
    .map((item) => ({
      id: item.id || item.value,
      value: item.value || item.name || "",
    }))
    .filter((item) => item.value);
  return mapped.length > 0 ? mapped : fallback;
};

const buildSelectedItems = (values, options) =>
  (values || [])
    .map((val) =>
      options.find(
        (opt) => String(opt.id) === String(val) || opt.value === String(val),
      ),
    )
    .filter(Boolean);

const renderSelectedTags = (selectedItems, keyPrefix) => {
  if (!selectedItems || selectedItems.length === 0) return null;
  return (
    <div className="selected-tags">
      {selectedItems.map((item, idx) => (
        <Tag key={`${keyPrefix}-${item.id || item.value || idx}`} type="gray">
          {item.value}
        </Tag>
      ))}
    </div>
  );
};

const isOtherSelected = (selectedValues, options) =>
  (selectedValues || []).some((val) => {
    const opt = options.find((o) => o.id === val || o.value === val);
    return OTHER_MATCHER(opt);
  });

const EditSample = (props) => {
  const { samples, setSamples, orderFormValues, setOrderFormValues, error } =
    props;

  const componentMounted = useRef(false);

  const intl = useIntl();

  const [elementsCounter, setElementsCounter] = useState(0);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(5);
  const [page2, setPage2] = useState(1);
  const [pageSize2, setPageSize2] = useState(5);

  const [rejectSampleReasons, setRejectSampleReasons] = useState([]);
  const [clinicalInfoOptions, setClinicalInfoOptions] = useState([]);
  const [antibiotherapyOptions, setAntibiotherapyOptions] = useState([]);
  const [invasiveGesturesOptions, setInvasiveGesturesOptions] = useState([]);
  const [indwellingDeviceOptions, setIndwellingDeviceOptions] = useState([]);

  const isBacterio =
    orderFormValues?.sampleOrderItems?.programCode ===
    BACTERIOLOGY_PROGRAM_CODE;

  const setBacterioField = (field, value) => {
    setOrderFormValues({
      ...orderFormValues,
      patientRoutineBacterioInfo: {
        ...orderFormValues.patientRoutineBacterioInfo,
        [field]: value,
      },
    });
  };

  useEffect(() => {
    if (!isBacterio) return;
    getFromOpenElisServer(
      `/rest/displayList/${encodeURIComponent(CLINICAL_INFO_CATEGORY)}`,
      (res) =>
        setClinicalInfoOptions(
          normalizeDictionaryOptions(res, CLINICAL_INFO_FALLBACK),
        ),
    );
    getFromOpenElisServer(
      `/rest/displayList/${encodeURIComponent(ANTIBIOTHERAPY_CATEGORY)}`,
      (res) => setAntibiotherapyOptions(normalizeDictionaryOptions(res, [])),
    );
    getFromOpenElisServer(
      `/rest/displayList/${encodeURIComponent(INVASIVE_GESTURES_CATEGORY)}`,
      (res) =>
        setInvasiveGesturesOptions(
          normalizeDictionaryOptions(res, INVASIVE_GESTURES_FALLBACK),
        ),
    );
    getFromOpenElisServer(
      `/rest/displayList/${encodeURIComponent(INDWELLING_DEVICE_CATEGORY)}`,
      (res) =>
        setIndwellingDeviceOptions(
          normalizeDictionaryOptions(res, INDWELLING_DEVICE_FALLBACK),
        ),
    );
  }, [isBacterio]);

  const handleAddNewSample = () => {
    let updateSamples = [...samples];
    let count = elementsCounter + 1;
    updateSamples.push({
      index: count,
      sampleRejected: false,
      rejectionReason: "",
      requestReferralEnabled: false,
      referralItems: [],
      sampleTypeId: "",
      sampleXML: null,
      panels: [],
      tests: [],
    });
    setSamples(updateSamples);
    setElementsCounter(count);
  };
  const formatTestsObject = (tests) => {
    return tests.map((test) => {
      test.id = test.testId;
      if (!test.accessionNumber) {
        test.accessionNumber = "";
      }
      if (!test.sampleType) {
        test.sampleType = "";
      }
      if (!test.collectionDate) {
        test.collectionDate = "";
      }
      if (!test.collectionTime) {
        test.collectionTime = "";
      }
      return test;
    });
  };
  const handleChecked = (e, testId) => {
    var tests = [];
    var updatedTests = [];
    if (e.currentTarget.name === "add") {
      tests = orderFormValues.possibleTests;
      updatedTests = tests.map((test) => {
        if (test.testId === testId) {
          return { ...test, add: e.currentTarget.checked };
        } else {
          return test;
        }
      });
      setOrderFormValues({
        ...orderFormValues,
        possibleTests: updatedTests,
      });
    } else if (e.currentTarget.name === "removeSample") {
      tests = orderFormValues.existingTests;
      updatedTests = tests.map((test) => {
        if (test.testId === testId) {
          return { ...test, removeSample: e.currentTarget.checked };
        }
        {
          return test;
        }
      });
      setOrderFormValues({
        ...orderFormValues,
        existingTests: updatedTests,
      });
    } else if (e.currentTarget.name === "canceled") {
      tests = orderFormValues.existingTests;
      updatedTests = tests.map((test) => {
        if (test.testId === testId) {
          return { ...test, canceled: e.currentTarget.checked };
        }
        {
          return test;
        }
      });
      setOrderFormValues({
        ...orderFormValues,
        existingTests: updatedTests,
      });
    }
  };

  const sampleTypeObject = (object) => {
    let newState = [...samples];
    switch (true) {
      case object.sampleTypeId !== undefined && object.sampleTypeId !== "":
        newState[object.sampleObjectIndex].sampleTypeId = object.sampleTypeId;
        break;
      case object.sampleRejected:
        newState[object.sampleObjectIndex].sampleRejected =
          object.sampleRejected;
        break;
      case object.rejectionReason !== undefined &&
        object.rejectionReason !== null:
        newState[object.sampleObjectIndex].rejectionReason =
          object.rejectionReason;
        break;
      case object.selectedTests !== undefined &&
        object.selectedTests.length > 0:
        newState[object.sampleObjectIndex].tests = object.selectedTests;
        break;
      case object.selectedPanels !== undefined &&
        object.selectedPanels.length > 0:
        newState[object.sampleObjectIndex].panels = object.selectedPanels;
        break;
      case object.sampleXML !== undefined && object.sampleXML !== null:
        newState[object.sampleObjectIndex].sampleXML = object.sampleXML;
        break;
      case object.requestReferralEnabled:
        newState[object.sampleObjectIndex].requestReferralEnabled =
          object.requestReferralEnabled;
        break;
      case object.referralItems !== undefined &&
        object.referralItems.length > 0:
        newState[object.sampleObjectIndex].referralItems = object.referralItems;
        break;
      default:
        props.setSamples(newState);
    }
  };

  const handlePageChange = (pageInfo) => {
    if (page != pageInfo.page) {
      setPage(pageInfo.page);
    }

    if (pageSize != pageInfo.pageSize) {
      setPageSize(pageInfo.pageSize);
    }
  };

  const handlePageChange2 = (pageInfo) => {
    if (page2 != pageInfo.page) {
      setPage2(pageInfo.page);
    }

    if (pageSize2 != pageInfo.pageSize) {
      setPageSize2(pageInfo.pageSize);
    }
  };

  const removeSample = (index) => {
    let updateSamples = samples.splice(index, 1);
    setSamples(updateSamples);
  };

  const fetchRejectSampleReasons = (res) => {
    if (componentMounted.current) {
      setRejectSampleReasons(res);
    }
  };

  const handleRemoveSample = (e, sample) => {
    e.preventDefault();
    let filtered = samples.filter(function (element) {
      return element !== sample;
    });
    setSamples(filtered);
  };

  useEffect(() => {
    componentMounted.current = true;
    getFromOpenElisServer(
      "/rest/test-rejection-reasons",
      fetchRejectSampleReasons,
    );
    window.scrollTo(0, 0);
    return () => {
      componentMounted.current = false;
    };
  }, []);

  useEffect(() => {
    getFromOpenElisServer(
      "/rest/test-rejection-reasons",
      fetchRejectSampleReasons,
    );
    window.scrollTo(0, 0);
    return () => {
      componentMounted.current = false;
    };
  }, []);

  const renderCell = (cell, row) => {
    var accession = row.cells.find(
      (e) => e.info.header === "accessionNumber",
    ).value;
    if (cell.info.header === "accessionNumber") {
      return <TableCell key={cell.id}>{cell.value}</TableCell>;
    } else if (cell.info.header === "sampleType") {
      return <TableCell key={cell.id}>{cell.value}</TableCell>;
    } else if (cell.info.header === "collectionDate") {
      return (
        <TableCell key={cell.id}>
          <TextInput
            id={cell.id + cell.info.header}
            labelText=""
            value={cell.value}
          ></TextInput>
        </TableCell>
      );
    } else if (cell.info.header === "collectionTime") {
      return (
        <TableCell key={cell.id}>
          <TextInput
            id={cell.id + cell.info.header}
            labelText=""
            value={cell.value}
          ></TextInput>
        </TableCell>
      );
    } else if (cell.info.header === "removeSample") {
      return (
        <>
          {accession !== "" ? (
            <TableCell key={cell.id}>
              <Checkbox
                id={cell.id + cell.info.header}
                labelText=""
                name="removeSample"
                checked={cell.value}
                onChange={(e) => handleChecked(e, row.id)}
              ></Checkbox>
            </TableCell>
          ) : (
            <TableCell key={cell.id}></TableCell>
          )}
        </>
      );
    } else if (cell.info.header === "testName") {
      return <TableCell key={cell.id}>{cell.value}</TableCell>;
    } else if (cell.info.header === "hasResults") {
      return (
        <TableCell key={cell.id}>
          <Checkbox
            id={cell.id + cell.info.header}
            labelText=""
            checked={cell.value}
          ></Checkbox>
        </TableCell>
      );
    } else if (cell.info.header === "canceled") {
      return (
        <TableCell key={cell.id}>
          <Checkbox
            id={cell.id + cell.info.header}
            labelText=""
            name="canceled"
            checked={cell.value}
            onChange={(e) => handleChecked(e, row.id)}
          ></Checkbox>
        </TableCell>
      );
    } else if (cell.info.header === "add") {
      return (
        <TableCell key={cell.id}>
          <Checkbox
            id={cell.id + cell.info.header}
            labelText=""
            name="add"
            checked={cell.value}
            onChange={(e) => handleChecked(e, row.id)}
          ></Checkbox>
        </TableCell>
      );
    } else {
      return <TableCell key={cell.id}></TableCell>;
    }
  };

  return (
    <>
      <div className="orderLegendBody">
        <Column lg={16}>
          <DataTable
            rows={formatTestsObject(orderFormValues.existingTests)}
            headers={OrderCurrentTestsHeaders}
            isSortable
          >
            {({ rows, headers, getHeaderProps, getTableProps }) => (
              <TableContainer
                title={intl.formatMessage({ id: "currentests.title" })}
              >
                <Table {...getTableProps()}>
                  <TableHead>
                    <TableRow>
                      {headers.map((header) => (
                        <TableHeader
                          key={header.key}
                          {...getHeaderProps({ header })}
                        >
                          {header.header}
                        </TableHeader>
                      ))}
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    <>
                      {rows
                        .slice((page - 1) * pageSize)
                        .slice(0, pageSize)
                        .map((row) => (
                          <TableRow key={row.id}>
                            {row.cells.map((cell) => renderCell(cell, row))}
                          </TableRow>
                        ))}
                    </>
                  </TableBody>
                </Table>
              </TableContainer>
            )}
          </DataTable>
          <Pagination
            onChange={handlePageChange}
            page={page}
            pageSize={pageSize}
            pageSizes={[5, 10, 20, 30]}
            totalItems={orderFormValues.existingTests.length}
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
        </Column>
      </div>
      <div className="orderLegendBody">
        <Column lg={16}>
          <DataTable
            rows={formatTestsObject(orderFormValues.possibleTests)}
            headers={OrderPossibleTestsHeaders}
            isSortable
          >
            {({ rows, headers, getHeaderProps, getTableProps }) => (
              <TableContainer
                title={intl.formatMessage({ id: "availabletests.title" })}
              >
                <Table {...getTableProps()}>
                  <TableHead>
                    <TableRow>
                      {headers.map((header) => (
                        <TableHeader
                          key={header.key}
                          {...getHeaderProps({ header })}
                        >
                          {header.header}
                        </TableHeader>
                      ))}
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    <>
                      {rows
                        .slice((page2 - 1) * pageSize2)
                        .slice(0, pageSize2)
                        .map((row) => (
                          <TableRow key={row.id}>
                            {row.cells.map((cell) => renderCell(cell, row))}
                          </TableRow>
                        ))}
                    </>
                  </TableBody>
                </Table>
              </TableContainer>
            )}
          </DataTable>
          <Pagination
            onChange={handlePageChange2}
            page={page2}
            pageSize={pageSize2}
            pageSizes={[5, 10, 20, 30]}
            totalItems={orderFormValues.possibleTests.length}
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
        </Column>
      </div>
      {isBacterio && orderFormValues && (
        <div className="orderLegendBody" style={{ marginTop: "1rem" }}>
          <Grid>
            <Column lg={8} md={4} sm={4}>
              <RadioButtonGroup
                legendText={intl.formatMessage({
                  id: "patient.hospitalization.current",
                  defaultMessage: "Hospitalisation en cours",
                })}
                valueSelected={
                  orderFormValues.patientRoutineBacterioInfo
                    ?.currentHospitalization
                    ? "true"
                    : "false"
                }
                name="currentHospitalization"
                onChange={(val) => {
                  const boolVal = val === "true";
                  setBacterioField("currentHospitalization", boolVal);
                  if (!boolVal) setBacterioField("roomNumber", "");
                }}
              >
                <RadioButton
                  id="editCurrentHospYes"
                  value="true"
                  labelText="Oui"
                />
                <RadioButton
                  id="editCurrentHospNo"
                  value="false"
                  labelText="Non"
                />
              </RadioButtonGroup>
            </Column>
            <Column lg={8} md={4} sm={4}>
              {orderFormValues.patientRoutineBacterioInfo
                ?.currentHospitalization && (
                <TextInput
                  name="roomNumber"
                  value={
                    orderFormValues.patientRoutineBacterioInfo?.roomNumber || ""
                  }
                  labelText={intl.formatMessage({
                    id: "patient.room.number",
                    defaultMessage: "Numéro de chambre",
                  })}
                  id="editRoomNumber"
                  onChange={(e) =>
                    setBacterioField("roomNumber", e.target.value)
                  }
                  placeholder={intl.formatMessage({
                    id: "patient.room.number",
                    defaultMessage: "Numéro de chambre",
                  })}
                />
              )}
            </Column>
            <Column lg={16} md={8} sm={4}>
              <br />
            </Column>
            <Column lg={8} md={4} sm={4}>
              <FilterableMultiSelect
                id="editClinicalInformation"
                titleText={intl.formatMessage({
                  id: "patient.clinical.info",
                  defaultMessage: "Renseignements cliniques",
                })}
                items={clinicalInfoOptions}
                itemToString={(item) => (item ? item.value : "")}
                selectedItems={buildSelectedItems(
                  orderFormValues.patientRoutineBacterioInfo
                    ?.clinicalInformations,
                  clinicalInfoOptions,
                )}
                onChange={(changes) =>
                  setBacterioField(
                    "clinicalInformations",
                    changes.selectedItems.map((item) => item.id || item.value),
                  )
                }
                selectionFeedback="top-after-reopen"
              />
              {renderSelectedTags(
                buildSelectedItems(
                  orderFormValues.patientRoutineBacterioInfo
                    ?.clinicalInformations,
                  clinicalInfoOptions,
                ),
                "editClinicalInformationTags",
              )}
            </Column>
            <Column lg={8} md={4} sm={4}>
              {isOtherSelected(
                orderFormValues.patientRoutineBacterioInfo?.clinicalInformations,
                clinicalInfoOptions,
              ) && (
                <TextInput
                  name="clinicalInformationOther"
                  value={
                    orderFormValues.patientRoutineBacterioInfo
                      ?.clinicalInformationOther || ""
                  }
                  labelText={intl.formatMessage({
                    id: "patient.clinical.info.other",
                    defaultMessage: "Autres renseignements cliniques",
                  })}
                  id="editClinicalInformationOther"
                  onChange={(e) =>
                    setBacterioField("clinicalInformationOther", e.target.value)
                  }
                  placeholder={intl.formatMessage({
                    id: "patient.clinical.info.other.placeholder",
                    defaultMessage: "Préciser",
                  })}
                />
              )}
            </Column>
            <Column lg={16} md={8} sm={4}>
              <br />
            </Column>
            <Column lg={8} md={4} sm={4}>
              <RadioButtonGroup
                legendText={intl.formatMessage({
                  id: "patient.antibiotherapy.recent",
                  defaultMessage: "Antibiothérapie dans les 3 derniers mois",
                })}
                valueSelected={
                  orderFormValues.patientRoutineBacterioInfo
                    ?.recentAntibiotherapy
                    ? "true"
                    : "false"
                }
                name="recentAntibiotherapy"
                onChange={(val) => {
                  const boolVal = val === "true";
                  setBacterioField("recentAntibiotherapy", boolVal);
                  if (!boolVal) setBacterioField("recentAntibiotherapyList", []);
                }}
              >
                <RadioButton
                  id="editRecentAtbYes"
                  value="true"
                  labelText="Oui"
                />
                <RadioButton
                  id="editRecentAtbNo"
                  value="false"
                  labelText="Non"
                />
              </RadioButtonGroup>
            </Column>
            <Column lg={8} md={4} sm={4}>
              {orderFormValues.patientRoutineBacterioInfo
                ?.recentAntibiotherapy && (
                <>
                  <FilterableMultiSelect
                    id="editRecentAntibiotherapyList"
                    titleText={intl.formatMessage({
                      id: "patient.antibiotherapy.label",
                      defaultMessage: "Antibiothérapie",
                    })}
                    items={antibiotherapyOptions}
                    itemToString={(item) => (item ? item.value : "")}
                    selectedItems={buildSelectedItems(
                      orderFormValues.patientRoutineBacterioInfo
                        ?.recentAntibiotherapyList,
                      antibiotherapyOptions,
                    )}
                    onChange={(changes) =>
                      setBacterioField(
                        "recentAntibiotherapyList",
                        changes.selectedItems.map(
                          (item) => item.id || item.value,
                        ),
                      )
                    }
                    selectionFeedback="top-after-reopen"
                  />
                  {renderSelectedTags(
                    buildSelectedItems(
                      orderFormValues.patientRoutineBacterioInfo
                        ?.recentAntibiotherapyList,
                      antibiotherapyOptions,
                    ),
                    "editRecentAntibiotherapyListTags",
                  )}
                </>
              )}
            </Column>
            <Column lg={16} md={8} sm={4}>
              <br />
            </Column>
            <Column lg={8} md={4} sm={4}>
              <RadioButtonGroup
                legendText={intl.formatMessage({
                  id: "patient.antibiotherapy.current",
                  defaultMessage: "Antibiothérapie en cours",
                })}
                valueSelected={
                  orderFormValues.patientRoutineBacterioInfo
                    ?.currentAntibiotherapy
                    ? "true"
                    : "false"
                }
                name="currentAntibiotherapy"
                onChange={(val) => {
                  const boolVal = val === "true";
                  setBacterioField("currentAntibiotherapy", boolVal);
                  if (!boolVal) {
                    setBacterioField("currentAntibiotherapyList", []);
                    setBacterioField("currentAntibiotherapyDuration", "");
                  }
                }}
              >
                <RadioButton
                  id="editCurrentAtbYes"
                  value="true"
                  labelText="Oui"
                />
                <RadioButton
                  id="editCurrentAtbNo"
                  value="false"
                  labelText="Non"
                />
              </RadioButtonGroup>
            </Column>
            <Column lg={8} md={4} sm={4}>
              {orderFormValues.patientRoutineBacterioInfo
                ?.currentAntibiotherapy && (
                <>
                  <FilterableMultiSelect
                    id="editCurrentAntibiotherapyList"
                    titleText={intl.formatMessage({
                      id: "patient.antibiotherapy.label",
                      defaultMessage: "Antibiothérapie",
                    })}
                    items={antibiotherapyOptions}
                    itemToString={(item) => (item ? item.value : "")}
                    selectedItems={buildSelectedItems(
                      orderFormValues.patientRoutineBacterioInfo
                        ?.currentAntibiotherapyList,
                      antibiotherapyOptions,
                    )}
                    onChange={(changes) =>
                      setBacterioField(
                        "currentAntibiotherapyList",
                        changes.selectedItems.map(
                          (item) => item.id || item.value,
                        ),
                      )
                    }
                    selectionFeedback="top-after-reopen"
                  />
                  {renderSelectedTags(
                    buildSelectedItems(
                      orderFormValues.patientRoutineBacterioInfo
                        ?.currentAntibiotherapyList,
                      antibiotherapyOptions,
                    ),
                    "editCurrentAntibiotherapyListTags",
                  )}
                  <TextInput
                    name="currentAntibiotherapyDuration"
                    value={
                      orderFormValues.patientRoutineBacterioInfo
                        ?.currentAntibiotherapyDuration || ""
                    }
                    labelText={intl.formatMessage({
                      id: "patient.antibiotherapy.duration",
                      defaultMessage: "Durée du traitement (jours)",
                    })}
                    id="editCurrentAntibiotherapyDuration"
                    type="number"
                    min="0"
                    onChange={(e) =>
                      setBacterioField(
                        "currentAntibiotherapyDuration",
                        e.target.value,
                      )
                    }
                    placeholder={intl.formatMessage({
                      id: "patient.antibiotherapy.duration.placeholder",
                      defaultMessage: "Durée du traitement",
                    })}
                  />
                </>
              )}
            </Column>
            <Column lg={16} md={8} sm={4}>
              <br />
            </Column>
            <Column lg={8} md={4} sm={4}>
              <RadioButtonGroup
                legendText={intl.formatMessage({
                  id: "patient.hospitalization.recent",
                  defaultMessage:
                    "Antécédent hospitalisation dans les 3 derniers mois",
                })}
                valueSelected={
                  orderFormValues.patientRoutineBacterioInfo
                    ?.recentHospitalization
                    ? "true"
                    : "false"
                }
                name="recentHospitalization"
                onChange={(val) => {
                  const boolVal = val === "true";
                  setBacterioField("recentHospitalization", boolVal);
                  if (!boolVal)
                    setBacterioField("recentHospitalizationCount", "");
                }}
              >
                <RadioButton
                  id="editRecentHospYes"
                  value="true"
                  labelText="Oui"
                />
                <RadioButton
                  id="editRecentHospNo"
                  value="false"
                  labelText="Non"
                />
              </RadioButtonGroup>
            </Column>
            <Column lg={8} md={4} sm={4}>
              {orderFormValues.patientRoutineBacterioInfo
                ?.recentHospitalization && (
                <TextInput
                  name="recentHospitalizationCount"
                  value={
                    orderFormValues.patientRoutineBacterioInfo
                      ?.recentHospitalizationCount || ""
                  }
                  labelText={intl.formatMessage({
                    id: "patient.hospitalization.recent.count",
                    defaultMessage: "Nombre d'hospitalisations",
                  })}
                  id="editRecentHospitalizationCount"
                  type="number"
                  min="0"
                  onChange={(e) =>
                    setBacterioField(
                      "recentHospitalizationCount",
                      e.target.value,
                    )
                  }
                  placeholder={intl.formatMessage({
                    id: "patient.hospitalization.recent.count",
                    defaultMessage: "Nombre d'hospitalisations",
                  })}
                />
              )}
            </Column>
            <Column lg={16} md={8} sm={4}>
              <br />
            </Column>
            <Column lg={8} md={4} sm={4}>
              <FilterableMultiSelect
                id="editRecentInvasiveGestures"
                titleText={intl.formatMessage({
                  id: "patient.invasive.gestures",
                  defaultMessage:
                    "Antécédents des gestes invasifs (<30 jours)",
                })}
                items={invasiveGesturesOptions}
                itemToString={(item) => (item ? item.value : "")}
                selectedItems={buildSelectedItems(
                  orderFormValues.patientRoutineBacterioInfo
                    ?.recentInvasiveGestures,
                  invasiveGesturesOptions,
                )}
                onChange={(changes) =>
                  setBacterioField(
                    "recentInvasiveGestures",
                    changes.selectedItems.map((item) => item.id || item.value),
                  )
                }
                selectionFeedback="top-after-reopen"
              />
              {renderSelectedTags(
                buildSelectedItems(
                  orderFormValues.patientRoutineBacterioInfo
                    ?.recentInvasiveGestures,
                  invasiveGesturesOptions,
                ),
                "editRecentInvasiveGesturesTags",
              )}
            </Column>
            <Column lg={8} md={4} sm={4}>
              <FilterableMultiSelect
                id="editIndwellingDevice"
                titleText={intl.formatMessage({
                  id: "patient.indwelling.device",
                  defaultMessage: "Dispositif à demeure",
                })}
                items={indwellingDeviceOptions}
                itemToString={(item) => (item ? item.value : "")}
                selectedItems={buildSelectedItems(
                  orderFormValues.patientRoutineBacterioInfo?.indwellingDevice,
                  indwellingDeviceOptions,
                )}
                onChange={(changes) =>
                  setBacterioField(
                    "indwellingDevice",
                    changes.selectedItems.map((item) => item.id || item.value),
                  )
                }
                selectionFeedback="top-after-reopen"
              />
              {renderSelectedTags(
                buildSelectedItems(
                  orderFormValues.patientRoutineBacterioInfo?.indwellingDevice,
                  indwellingDeviceOptions,
                ),
                "editIndwellingDeviceTags",
              )}
            </Column>
            <Column lg={16} md={8} sm={4}>
              <br />
            </Column>
          </Grid>
        </div>
      )}
      <Stack gap={10}>
        <div className="orderLegendBody">
          <h3>
            <FormattedMessage id="order.label.add" />
          </h3>
          {samples.map((sample, i) => {
            return (
              <div className="sampleType" key={i}>
                <h4>
                  <FormattedMessage id="label.button.sample" /> {i + 1}
                </h4>
                <Link href="#" onClick={(e) => handleRemoveSample(e, sample)}>
                  {<FormattedMessage id="sample.remove.action" />}
                </Link>
                <SampleType
                  index={i}
                  rejectSampleReasons={rejectSampleReasons}
                  removeSample={removeSample}
                  sample={sample}
                  setSample={(newSample) => {
                    let newSamples = [...samples];
                    newSamples[i] = newSample;
                    setSamples(newSamples);
                  }}
                  sampleTypeObject={sampleTypeObject}
                  error={error}
                />
              </div>
            );
          })}
          <Row>
            <div className="inlineDiv">
              <Button onClick={handleAddNewSample}>
                {<FormattedMessage id="sample.add.action" />}
                &nbsp; &nbsp;
                <Add size={16} />
              </Button>
            </div>
          </Row>
        </div>
      </Stack>
    </>
  );
};

export default EditSample;

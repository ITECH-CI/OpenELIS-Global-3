import React, { useContext, useEffect, useRef, useState } from "react";
import {
  Checkbox,
  FormGroup,
  Layer,
  Search,
  Select,
  SelectItem,
  Tag,
  Tile,
  Loading,
  Column,
  TextInput,
} from "@carbon/react";
import CustomCheckBox from "../common/CustomCheckBox";
import CustomSelect from "../common/CustomSelect";
import CustomDatePicker from "../common/CustomDatePicker";
import CustomTimePicker from "../common/CustomTimePicker";
import { NotificationKinds } from "../common/CustomNotification";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer } from "../utils/Utils";
import { NotificationContext, ConfigurationContext } from "../layout/Layout";
import { sampleTypeTestsStructure } from "../data/SampleEntryTestsForTypeProvider";
import CustomTextInput from "../common/CustomTextInput";
import OrderReferralRequest from "../addOrder/OrderReferralRequest";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";

const SampleType = (props) => {
  const { userSessionDetails } = useContext(UserSessionDetailsContext);
  const { configurationProperties } = useContext(ConfigurationContext);

  const intl = useIntl();

  const componentMounted = useRef(false);
  const sampleTypesRef = useRef(null);
  const sampleMethodsRef = useRef(null);

  const { index, rejectSampleReasons, removeSample, sample, isTb } = props;

  const [sampleTypes, setSampleTypes] = useState([]);
  const [selectedSampleType, setSelectedSampleType] = useState({
    id: null,
    name: "",
    element_index: 0,
  });
  const [selectedTbSampleMethod, setSelectedTbSampleMethod] = useState({
    id: null,
    name: "",
    element_index: 0,
  });
  const [sampleTypeTests, setSampleTypeTests] = useState(
    sampleTypeTestsStructure,
  );
  const [selectedTests, setSelectedTests] = useState([]);
  const [searchBoxTests, setSearchBoxTests] = useState([]);
  const [requestTestReferral, setRequestTestReferral] = useState(false);
  const [referralReasons, setReferralReasons] = useState([]);
  const [referralOrganizations, setReferralOrganizations] = useState([]);
  const [testSearchTerm, setTestSearchTerm] = useState("");
  const [referralRequests, setReferralRequests] = useState([]);
  const { setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const [rejectionReasonsDisabled, setRejectionReasonsDisabled] =
    useState(true);
  const [selectedPanels, setSelectedPanels] = useState([]);
  const [panelSearchTerm, setPanelSearchTerm] = useState("");
  const [searchBoxPanels, setSearchBoxPanels] = useState([]);
  const [sampleXml, setSampleXml] = useState(
    sample?.sampleXML != null
      ? sample.sampleXML
      : {
          collectionDate:
            configurationProperties?.AUTOFILL_COLLECTION_DATE === "true"
              ? configurationProperties.currentDateAsText
              : "",
          collector: "",
          rejected: false,
          rejectionReason: "",
          collectionTime:
            configurationProperties?.AUTOFILL_COLLECTION_DATE === "true"
              ? configurationProperties.currentTimeAsText
              : "",
        },
  );
  const [loading, setLoading] = useState(true);
  const [tbReason, setTbResaon] = useState("");
  const [tbOrderReasons, setTbOrderReasons] = useState([]);
  const [reasons, setReasons] = useState([]);
  const [tbDiagnosticMethods, setTbDiagnosticMethods] = useState([]);
  const [tbSampleAspect, setTbSampleAspect] = useState([]);
  const [followupReason, setFollowupReason] = useState("");
  const [followupLines, setFollowupLines] = useState([]);
  const defaultSelect = { id: "", value: "Choose Rejection Reason" };
  const [tbData, setTbData] = useState({
    tbOrderReason: "",
    tbDiagnosticReason: "",
    tbFollowupReason: "",
    tbFollowupPeriodLine1: "",
    tbFollowupPeriodLine2: "",
    tbAspect: "",
    tbSpecimenNature: "",
    tbSubjectNumber: "",
    selectedTbMethod: "",
  });
  const [tbReasonDiagnostic, setTbReasonDiagnostic] = useState("");
  const [tbReasonFollowUp, setTbReasonFollowUp] = useState("");

  const [tbFollowUpLine1, setTbFollowUpLine1] = useState("");
  const [tbFollowUpLine2, setTbFollowUpLine2] = useState("");

  var MicroscopieTB = "1368";
  var followupLine1 = "1405";
  var followupLine2 = "1406";

  // const item = data.find(item => item.value === "Diagnostic");

  function handleCollectionDate(date) {
    setSampleXml({
      ...sampleXml,
      collectionDate: date,
    });
  }

  function handleReasons(value) {
    setSampleXml({
      ...sampleXml,
      rejectionReason: value,
    });
    props.sampleTypeObject({
      rejectionReason: value,
      sampleObjectIndex: index,
    });
  }

  function handleCollectionTime(time) {
    setSampleXml({
      ...sampleXml,
      collectionTime: time,
    });
  }

  function handleCollector(value) {
    setSampleXml({
      ...sampleXml,
      collector: value,
    });
  }

  useEffect(() => {
    updateSampleXml(sampleXml, index);
  }, [sampleXml]);

  const handleRemoveSampleTest = (index) => {
    removeSample(index);
  };

  const handleReferralRequest = () => {
    setRequestTestReferral(!requestTestReferral);
    if (selectedTests.length > 0) {
      const defaultReferralRequest = [];
      selectedTests.map((test) => {
        defaultReferralRequest.push({
          reasonForReferral: referralReasons[0].id,
          referrer:
            userSessionDetails.firstName + " " + userSessionDetails.lastName,
          institute: referralOrganizations[0].id,
          sentDate: "",
          testId: test.id,
        });
      });
      setReferralRequests(defaultReferralRequest);
    }
  };

  const handleTestSearchChange = (event) => {
    const query = event.target.value;
    setTestSearchTerm(query);
    const results = sampleTypeTests.tests.filter((test) => {
      return test.name.toLowerCase().includes(query.toLowerCase());
    });
    setSearchBoxTests(results);
  };

  const handleRemoveSelectedTest = (test) => {
    removedTestFromSelectedTests(test);
  };

  const handleFilterSelectTest = (test) => {
    setTestSearchTerm("");
    addTestToSelectedTests(test);
  };

  const handleTestCheckbox = (e, test) => {
    if (e.currentTarget.checked) {
      addTestToSelectedTests(test);
    } else {
      removedTestFromSelectedTests(test);
    }
  };

  function findTestById(testId) {
    return sampleTypeTests.tests.find((test) => test.id === testId);
  }

  function findTestIndex(testId) {
    return sampleTypeTests.tests.findIndex((test) => test.id === testId);
  }

  const panelIsSelected = (panelId) => {
    for (let i in selectedPanels) {
      if (selectedPanels[i].id === panelId) {
        return true;
      }
    }
    return false;
  };

  const testIsSelected = (testId) => {
    for (let i in selectedTests) {
      if (selectedTests[i].id === testId) {
        return true;
      }
    }
    return false;
  };

  const handleChange = (field, value) => {
    setTbData((prev) => ({
      ...prev,
      [field]: value,
    }));
  };

  const triggerPanelCheckBoxChange = (isChecked, testIds) => {
    if (!testIds) {
      console.warn("testIds is undefined or null");
      return;
    }
    const testIdsList = testIds.split(",").map((id) => id.trim());
    testIdsList.map((testId) => {
      let testIndex = findTestIndex(testId);
      let test = findTestById(testId);
      if (testIndex !== -1) {
        if (isChecked) {
          if (!testIsSelected(test.id)) {
            setSelectedTests((prevState) => {
              return [...prevState, { id: test.id, name: test.name }];
            });
          }
        } else {
          removedTestFromSelectedTests(test);
        }
      }
    });
  };

  const removedTestFromSelectedTests = (test) => {
    let index = 0;
    for (let i in selectedTests) {
      if (selectedTests[i].id === test.id) {
        const newTests = selectedTests;
        newTests.splice(index, 1);
        setSelectedTests([...newTests]);
        break;
      }
      index++;
    }
  };

  function addReferralRequest(test) {
    setReferralRequests([
      ...referralRequests,
      {
        reasonForReferral: referralReasons[0].id,
        referrer:
          userSessionDetails.firstName + " " + userSessionDetails.lastName,
        institute: referralOrganizations[0].id,
        sentDate: "",
        testId: test.id,
      },
    ]);
  }

  function removeReferralRequest(test) {
    let index = 0;
    for (let x in referralRequests) {
      if (referralRequests[x].testId === test.id) {
        const newReferralRequests = referralRequests;
        newReferralRequests.splice(index, 1);
        setReferralRequests([...newReferralRequests]);
        break;
      }
      index++;
    }
  }

  const handleFetchSampleTypeTests = (e, index) => {
    setSelectedTests([]);
    setReferralRequests([]);
    const { value } = e.target;
    const selectedSampleTypeOption =
      sampleTypesRef.current.options[sampleTypesRef.current.selectedIndex].text;
    setSelectedSampleType({
      ...selectedSampleType,
      id: value,
      name: selectedSampleTypeOption,
      element_index: index,
    });
    props.sampleTypeObject({ sampleTypeId: value, sampleObjectIndex: index });
  };

  const handleFetchSampleTbTypeTests = (e, index) => {
    setSelectedTests([]);
    setReferralRequests([]);
    const { value } = e.target;
    const selectedSampleTypeOption =
      sampleMethodsRef.current.options[sampleMethodsRef.current.selectedIndex]
        .text;
    setSelectedTbSampleMethod({
      ...selectedTbSampleMethod,
      id: value,
      name: selectedSampleTypeOption,
      element_index: index,
    });
  };

  const updateSampleXml = (sampleXML, index) => {
    props.sampleTypeObject({ sampleXML: sampleXML, sampleObjectIndex: index });
  };

  const fetchSamplesTypes = (res) => {
    if (componentMounted.current) {
      setSampleTypes(res);
      setLoading(false);
    }
  };

  const fetchSampleTypeTests = (res) => {
    if (componentMounted.current) {
      setSampleTypeTests(res);
    }
  };

  useEffect(() => {
    if (props.sample.referralItems.length > 0 && referralReasons.length > 0) {
      setRequestTestReferral(props.sample.requestReferralEnabled);
      setReferralRequests(props.sample.referralItems);
    }
  }, [referralReasons]);

  useEffect(() => {
    props.sampleTypeObject({
      requestReferralEnabled: requestTestReferral,
      sampleObjectIndex: index,
    });
    if (!requestTestReferral) {
      setReferralRequests([]);
    }
  }, [requestTestReferral]);

  useEffect(() => {
    props.sampleTypeObject({
      referralItems: referralRequests,
      sampleObjectIndex: index,
    });
  }, [referralRequests]);

  const displayReferralReasonsOptions = (res) => {
    if (componentMounted.current) {
      setReferralReasons(res);
    }
  };
  const displayReferralOrgOptions = (res) => {
    if (componentMounted.current) {
      setReferralOrganizations(res);
    }
  };

  const displayTbAnalysisMethodOptions = (res) => {
    if (res) {
      setTbDiagnosticMethods(res);
    }
  };

  const displayTbFollowupLinesOptions = (res) => {
    if (res) {
      setFollowupLines(res);
    }
  };

  const displayTbOrderResonOptions = (res) => {
    if (res) {
      setTbReasonDiagnostic(
        res.find((item) => item.value === "Diagnostic")?.id ?? null,
      );
      setTbReasonFollowUp(
        res.find((item) => item.value === "Follow up")?.id ?? null,
      );
      setTbOrderReasons(res);
    }
  };

  function handleRejection(checked) {
    if (checked) {
      addNotification({
        kind: NotificationKinds.warning,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "reject.order.sample.notification" }),
      });
      setNotificationVisible(true);
    }
    setSampleXml({
      ...sampleXml,
      rejected: checked,
    });
    setRejectionReasonsDisabled(!rejectionReasonsDisabled);
  }

  const removedPanelFromSelectedPanels = (panel) => {
    let index = 0;
    for (let i in selectedPanels) {
      if (selectedPanels[i].id === panel.id) {
        triggerPanelCheckBoxChange(
          false,
          selectedPanels[i].testIds || selectedPanels[i].test_ids,
        );
        const newPanels = selectedPanels;
        newPanels.splice(index, 1);
        setSelectedPanels([...newPanels]);
        break;
      }
      index++;
    }
  };

  const handlePanelSearchChange = (event) => {
    const query = event.target.value;
    setPanelSearchTerm(query);
    const results = sampleTypeTests.panels.filter((panel) => {
      return panel.name.toLowerCase().includes(query.toLowerCase());
    });
    setSearchBoxPanels(results);
  };

  const handleFilterSelectPanel = (panel) => {
    setPanelSearchTerm("");
    addPanelToSelectedPanels(panel);
  };

  const handlePanelCheckbox = (panel) => {
    if (!panelIsSelected(panel.id)) {
      addPanelToSelectedPanels(panel);
    } else {
      removedPanelFromSelectedPanels(panel);
    }
  };

  const handleRemoveSelectedPanel = (panel) => {
    removedPanelFromSelectedPanels(panel);
  };

  function addTestToSelectedTests(test) {
    if (!testIsSelected(test.id)) {
      setSelectedTests([...selectedTests, { id: test.id, name: test.name }]);
    }
  }

  function handleTbReason(e) {
    setTbResaon(e.target.value);
  }

  function handleFollowupreason(e) {
    const value = e.target.value;
    const selectedOption = e.target.options[e.target.selectedIndex];
    const label =
      selectedOption.text === "Examen de suivi 1ère ligne (TB Sensible)"
        ? followupLine1
        : followupLine2;
    setFollowupReason(value);
    getFromOpenElisServer(
      `/rest/Dictionary-by-ByCategory?category=${encodeURIComponent(label)}`,
      displayTbFollowupLinesOptions,
    );
  }

  useEffect(() => {
    const category =
      tbData.tbDiagnosticReason === tbReasonDiagnostic
        ? "TB Diagnostic Reasons"
        : "TB Followup Reasons";

    getFromOpenElisServer(
      `/rest/Dictionary-by-ByCategory?category=${encodeURIComponent(category)}`,
      fetTbReasons,
    );
  }, [tbData.tbDiagnosticReason]);

  function fetTbReasons(res) {
    if (res) {
      setReasons(res);
      setTbFollowUpLine1(
        res.find(
          (item) => item.value === "Examen de suivi 1ère ligne (TB Sensible)",
        )?.id ?? null,
      );
      setTbFollowUpLine2(
        res.find(
          (item) =>
            item.value === "Examen de suivi 2ième ligne (TB/RR ; TBXDR)",
        )?.id ?? null,
      );
    }
  }

  function fetchTbSampleAspects(res) {
    if (res) {
      setTbSampleAspect(res);
    }
  }

  const addPanelToSelectedPanels = (panel) => {
    setSelectedPanels([
      ...selectedPanels,
      {
        id: panel.id,
        name: panel.name,
        testIds: panel.testIds || panel.test_ids,
        test_ids: panel.test_ids || panel.testIds,
      },
    ]);
  };

  useEffect(() => {
    componentMounted.current = true;
    if (tbData.selectedTbMethod !== "" && tbData.selectedTbMethod != null) {
      if (isTb) {
        getFromOpenElisServer(
          `/MicrobiologyTb/panel_test?method=${tbData.selectedTbMethod}`,
          fetchSampleTypeTests,
        );
        if (tbData.selectedTbMethod === MicroscopieTB) {
          getFromOpenElisServer(
            `/rest/Dictionary-by-ByCategory?category=TB Sample Aspects`,
            fetchTbSampleAspects,
          );
        }
      } else {
        getFromOpenElisServer(
          `/rest/sample-type-tests?sampleType=${tbData.selectedTbMethod}`,
          fetchSampleTypeTests,
        );
      }
    }
    return () => {
      componentMounted.current = false;
    };
  }, [tbData.selectedTbMethod]);

  useEffect(() => {
    componentMounted.current = true;
    if (selectedTbSampleMethod.id !== "" && selectedTbSampleMethod.id != null) {
      if (isTb) {
        getFromOpenElisServer(
          `/MicrobiologyTb/panel_test?method=${selectedTbSampleMethod.id}`,
          fetchSampleTypeTests,
        );
        if (selectedTbSampleMethod.id === MicroscopieTB) {
          getFromOpenElisServer(
            `/rest/Dictionary-by-ByCategory?category=TB Sample Aspects`,
            fetchTbSampleAspects,
          );
        }
      }
    }
    return () => {
      componentMounted.current = false;
    };
  }, [selectedTbSampleMethod.id]);

  useEffect(() => {
    props.sampleTypeObject({
      sampleRejected: rejectionReasonsDisabled,
      sampleObjectIndex: index,
    });
  }, [rejectionReasonsDisabled]);

  useEffect(() => {
    props.sampleTypeObject({
      selectedTests: selectedTests,
      sampleObjectIndex: index,
    });
  }, [selectedTests]);

  useEffect(() => {
    props.sampleTypeObject({
      selectedPanels: selectedPanels,
      sampleObjectIndex: index,
    });
    for (let i in selectedPanels) {
      if (isTb) {
        triggerPanelCheckBoxChange(true, selectedPanels[i].test_ids);
      } else {
        triggerPanelCheckBoxChange(true, selectedPanels[i].testIds);
      }
    }
  }, [selectedPanels, sampleTypeTests]);

  // Send tbData to parent component whenever it changes
  useEffect(() => {
    if (isTb) {
      props.sampleTypeObject({
        tbData: tbData,
        sampleObjectIndex: index,
      });
    }
  }, [tbData, isTb]);

  const repopulateUI = () => {
    if (props.sample !== null) {
      setSelectedTests(props.sample.tests);
      setSelectedPanels(props.sample.panels);
      setSelectedSampleType({
        id: props.sample.sampleTypeId,
      });
    }
  };

  useEffect(() => {
    componentMounted.current = true;
    getFromOpenElisServer(
      "/rest/referral-reasons",
      displayReferralReasonsOptions,
    );
    getFromOpenElisServer(
      "/rest/referral-organizations",
      displayReferralOrgOptions,
    );
    getFromOpenElisServer(
      "/rest/Dictionary-by-ByCategory?category=TB Analysis Methods",
      displayTbAnalysisMethodOptions,
    );
    getFromOpenElisServer(
      "/rest/Dictionary-by-ByCategory?category=TB Order Reasons",
      displayTbOrderResonOptions,
    );
    repopulateUI();
    getFromOpenElisServer("/rest/user-sample-types", fetchSamplesTypes);
    return () => {
      componentMounted.current = false;
    };
  }, []);

  return (
    <>
      {loading && <Loading />}
      <div className="sampleBody">
        <Select
          className="selectSampleType"
          id={"sampleId_" + index}
          ref={sampleTypesRef}
          value={
            props.sample.sampleTypeId === "" ? "" : props.sample.sampleTypeId
          }
          name="sampleId"
          labelText=""
          onChange={(e) => {
            handleFetchSampleTypeTests(e, index);
          }}
          required
        >
          <SelectItem text="Select sample type" value="" />
          {sampleTypes?.map((sampleType, i) => (
            <SelectItem text={sampleType.value} value={sampleType.id} key={i} />
          ))}
        </Select>

        <CustomCheckBox
          id={"reject_" + index}
          onChange={(value) => handleRejection(value)}
          label={intl.formatMessage({ id: "sample.reject.label" })}
        />
        {sampleXml.rejected && (
          <CustomSelect
            id={"rejectedReasonId_" + index}
            options={rejectSampleReasons}
            disabled={rejectionReasonsDisabled}
            defaultSelect={defaultSelect}
            onChange={(e) => handleReasons(e)}
          />
        )}

        <div className="inlineDiv">
          <CustomDatePicker
            id={"collectionDate_" + index}
            autofillDate={
              configurationProperties?.AUTOFILL_COLLECTION_DATE === "true"
            }
            onChange={(date) => handleCollectionDate(date)}
            value={sampleXml.collectionDate}
            labelText={intl.formatMessage({ id: "sample.collection.date" })}
            className="inputText"
            disallowFutureDate={true}
          />

          <CustomTimePicker
            id={"collectionTime_" + index}
            autofillTime={
              configurationProperties?.AUTOFILL_COLLECTION_DATE === "true"
            }
            onChange={(time) => handleCollectionTime(time)}
            value={sampleXml.collectionTime}
            className="inputText"
            labelText={intl.formatMessage({ id: "sample.collection.time" })}
          />
        </div>
        <div className="inlineDiv">
          <CustomTextInput
            id={"collector_" + index}
            onChange={(value) => handleCollector(value)}
            defaultValue={""}
            value={sampleXml.collector}
            labelText={intl.formatMessage({ id: "collector.label" })}
            className="inputText"
          />
        </div>
        {isTb && (
          <div id="tbSection">
            <Column lg={8} md={4} sm={4}>
              <Select
                id={"tbReason_" + index}
                value={tbData.tbDiagnosticReason}
                onChange={(e) =>
                  handleChange("tbDiagnosticReason", e.target.value)
                }
                labelText={<FormattedMessage id="sample.tb.examen.reason" />}
              >
                <SelectItem value="" text="" />
                {tbOrderReasons.map((option) => {
                  return (
                    <SelectItem
                      key={option.id}
                      value={option.id}
                      text={option.value}
                    />
                  );
                })}
              </Select>
            </Column>
            {tbData.tbDiagnosticReason === tbReasonFollowUp && (
              <>
                <Column lg={8} md={4} sm={4}>
                  <TextInput
                    value={tbData.tbSubjectNumber}
                    onChange={(e) =>
                      handleChange("tbSubjectNumber", e.target.value)
                    }
                    labelText={intl.formatMessage({
                      id: "sample.tb.followup.code",
                    })}
                    id={"followUpCode_" + index}
                  />
                </Column>
                <Column lg={8} md={4} sm={4}>
                  <Select
                    id={"followReason_" + index}
                    value={tbData.tbFollowupReason}
                    onChange={(e) => {
                      handleChange("tbFollowupReason", e.target.value);
                      handleFollowupreason(e);
                    }}
                    labelText={
                      <FormattedMessage id="sample.tb.followup.reason" />
                    }
                  >
                    <SelectItem value="" text="" />
                    {reasons.map((option) => {
                      return (
                        <SelectItem
                          key={option.id}
                          value={option.id}
                          text={option.value}
                        />
                      );
                    })}
                  </Select>
                  {tbData.tbFollowupReason === tbFollowUpLine1 && (
                    <Select
                      id="testLocationCodeId"
                      name="testLocationCode"
                      value={tbData.tbFollowupPeriodLine1}
                      onChange={(e) =>
                        handleChange("tbFollowupPeriodLine1", e.target.value)
                      }
                      labelText={
                        <FormattedMessage id="sample.tb.followup.period" />
                      }
                    >
                      <SelectItem value="" text="" />
                      {followupLines.map((option) => {
                        return (
                          <SelectItem
                            key={option.id}
                            value={option.id}
                            text={option.value}
                          />
                        );
                      })}
                    </Select>
                  )}
                  {tbData.tbFollowupReason === tbFollowUpLine2 && (
                    <div className="inlineDiv">
                      <Select
                        id="testLocationCodeId"
                        name="testLocationCode"
                        value={tbData.tbFollowupPeriodLine2}
                        onChange={(e) =>
                          handleChange("tbFollowupPeriodLine2", e.target.value)
                        }
                        labelText={
                          <FormattedMessage id="sample.tb.followup.period" />
                        }
                      >
                        <SelectItem value="" text="" />
                        {followupLines.map((option) => {
                          return (
                            <SelectItem
                              key={option.id}
                              value={option.id}
                              text={option.value}
                            />
                          );
                        })}
                      </Select>
                      {selectedTbSampleMethod.id === MicroscopieTB && (
                        <Column lg={8} md={4} sm={4}>
                          <Select
                            id={"tbAspect_" + index}
                            value={tbData.tbAspect}
                            onChange={(e) =>
                              handleChange("tbAspect", e.target.value)
                            }
                            required
                            labelText={
                              <FormattedMessage id="sample.tb.aspect" />
                            }
                          >
                            <SelectItem value="" text="" />
                            {tbSampleAspect.map((option) => {
                              return (
                                <SelectItem
                                  key={option.id}
                                  value={option.id}
                                  text={option.value}
                                />
                              );
                            })}
                          </Select>
                        </Column>
                      )}
                    </div>
                  )}
                </Column>
              </>
            )}
            {tbData.tbDiagnosticReason === tbReasonDiagnostic && (
              <Column lg={8} md={4} sm={4}>
                <Select
                  id={"tbDiagnosticReason_" + index}
                  value={tbData.tbOrderReason}
                  onChange={(e) =>
                    handleChange("tbOrderReason", e.target.value)
                  }
                  labelText={
                    <FormattedMessage id="sample.tb.diagnostic.reason" />
                  }
                >
                  <SelectItem value="" text="" />
                  {reasons.map((option) => {
                    return (
                      <SelectItem
                        key={option.id}
                        value={option.id}
                        text={option.value}
                      />
                    );
                  })}
                </Select>
              </Column>
            )}
            <Column lg={8} md={4} sm={4}>
              <Select
                id={"tbAnalystMethod_" + index}
                value={tbData.selectedTbMethod}
                onChange={(e) => {
                  handleChange("selectedTbMethod", e.target.value);
                  handleFetchSampleTbTypeTests(e, index);
                }}
                ref={sampleMethodsRef}
                required
                labelText={<FormattedMessage id="sample.tb.analyse.method" />}
              >
                <SelectItem value="" text="" />
                {tbDiagnosticMethods.map((option) => {
                  return (
                    <SelectItem
                      key={option.id}
                      value={option.id}
                      text={option.value}
                    />
                  );
                })}
              </Select>
            </Column>
          </div>
        )}
        <div className="testPanels">
          <div className="cds--col">
            <h4>
              <FormattedMessage id="sample.label.orderpanel" />
            </h4>
            <div
              className={"searchTestText"}
              style={{ marginBottom: "1.188rem" }}
            >
              {selectedPanels && selectedPanels.length ? (
                <>
                  {selectedPanels.map((panel, panel_index) => (
                    <Tag
                      filter
                      key={`panelTags_` + panel_index}
                      onClose={() => handleRemoveSelectedPanel(panel)}
                      style={{ marginRight: "0.5rem" }}
                      type={"green"}
                    >
                      {panel.name}
                    </Tag>
                  ))}
                </>
              ) : (
                <></>
              )}
            </div>
            <FormGroup
              legendText={
                <FormattedMessage id="sample.search.panel.legend.text" />
              }
            >
              <Search
                size="lg"
                id={`panels_search_` + index}
                labelText={
                  <FormattedMessage id="label.search.availablepanel" />
                }
                placeholder={intl.formatMessage({
                  id: "choose.availablepanel",
                })}
                onChange={handlePanelSearchChange}
                value={(() => {
                  if (panelSearchTerm) {
                    return panelSearchTerm;
                  }
                  return "";
                })()}
              />
              <div>
                {(() => {
                  if (!panelSearchTerm) return null;
                  if (searchBoxPanels && searchBoxPanels.length) {
                    return (
                      <ul className={"searchTestsList"}>
                        {searchBoxPanels.map((panel, panel_index) => (
                          <li
                            role="menuitem"
                            className={"singleTest"}
                            key={`panelFilter_` + panel_index}
                            onClick={() => handleFilterSelectPanel(panel)}
                          >
                            {panel.name}
                          </li>
                        ))}
                      </ul>
                    );
                  }
                  return (
                    <>
                      <Layer>
                        <Tile className={"emptyFilterTests"}>
                          <span>
                            <FormattedMessage id="sample.panel.search.error.msg" />{" "}
                            <strong>"{panelSearchTerm}"</strong>{" "}
                          </span>
                        </Tile>
                      </Layer>
                    </>
                  );
                })()}
              </div>
            </FormGroup>
            {sampleTypeTests.panels != null &&
              sampleTypeTests.panels.map((panel) => {
                return panel.name === "" ? (
                  ""
                ) : (
                  <Checkbox
                    onChange={() => handlePanelCheckbox(panel)}
                    labelText={panel.name}
                    id={`panel_` + index + "_" + panel.id}
                    key={index + panel.id}
                    checked={
                      selectedPanels.filter((item) => item.id === panel.id)
                        .length > 0
                    }
                  />
                );
              })}
          </div>
        </div>

        <div className="cds--col">
          {selectedTests && !selectedTests.length ? "" : <h4>Order Tests</h4>}
          <div
            className={"searchTestText"}
            style={{ marginBottom: "1.188rem" }}
          >
            {selectedTests && selectedTests.length ? (
              <>
                {selectedTests.map((test, index) => (
                  <Tag
                    filter
                    key={`testTags_` + index}
                    onClose={() => handleRemoveSelectedTest(test)}
                    style={{ marginRight: "0.5rem" }}
                    type={"red"}
                  >
                    {test.name}
                  </Tag>
                ))}
              </>
            ) : (
              <></>
            )}
          </div>
          <FormGroup
            legendText={intl.formatMessage({
              id: "legend.search.availabletests",
            })}
          >
            <Search
              size="lg"
              id={`tests_search_` + index}
              labelText={
                <FormattedMessage id="label.search.available.targetest" />
              }
              placeholder={intl.formatMessage({
                id: "holder.choose.availabletest",
              })}
              onChange={handleTestSearchChange}
              value={(() => {
                if (testSearchTerm) {
                  return testSearchTerm;
                }
                return "";
              })()}
            />
            <div>
              {(() => {
                if (!testSearchTerm) return null;
                if (searchBoxTests && searchBoxTests.length) {
                  return (
                    <ul className={"searchTestsList"}>
                      {searchBoxTests.map((test, test_index) => (
                        <li
                          role="menuitem"
                          className={"singleTest"}
                          key={`filterTest_` + test_index}
                          onClick={() => handleFilterSelectTest(test)}
                        >
                          {test.name}
                        </li>
                      ))}
                    </ul>
                  );
                }
                return (
                  <>
                    <Layer>
                      <Tile className={"emptyFilterTests"}>
                        <span>
                          <FormattedMessage id="title.notestfoundmatching" />
                          <strong> "{testSearchTerm}"</strong>{" "}
                        </span>
                      </Tile>
                    </Layer>
                  </>
                );
              })()}
            </div>
          </FormGroup>
          {sampleTypeTests.tests != null &&
            sampleTypeTests.tests.map((test) => {
              return test.name === "" ? (
                ""
              ) : (
                <Checkbox
                  onChange={(e) => handleTestCheckbox(e, test)}
                  labelText={test.name}
                  id={`test_` + index + "_" + test.id}
                  key={`test_checkBox_` + index + test.id}
                  checked={
                    selectedTests.filter((item) => item.id === test.id).length >
                    0
                  }
                />
              );
            })}
        </div>

        <div className="requestTestReferral">
          <Checkbox
            id={`useReferral_` + index}
            labelText={intl.formatMessage({
              id: "label.refertest.referencelab",
            })}
            onChange={handleReferralRequest}
          />
          {requestTestReferral === true && (
            <OrderReferralRequest
              index={index}
              selectedTests={selectedTests}
              referralReasons={referralReasons}
              referralOrganizations={referralOrganizations}
              referralRequests={referralRequests}
              setReferralRequests={setReferralRequests}
            />
          )}
        </div>
      </div>
    </>
  );
};

export default SampleType;

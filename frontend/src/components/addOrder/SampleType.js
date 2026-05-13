import {
  Checkbox,
  Column,
  ComboBox,
  FormGroup,
  Layer,
  Loading,
  Search,
  Select,
  SelectItem,
  Tag,
  TextInput,
  Tile,
} from "@carbon/react";
import { useContext, useEffect, useRef, useState } from "react";
import { FormattedMessage, useIntl } from "react-intl";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";
import OrderReferralRequest from "../addOrder/OrderReferralRequest";
import CustomCheckBox from "../common/CustomCheckBox";
import CustomDatePicker from "../common/CustomDatePicker";
import { NotificationKinds } from "../common/CustomNotification";
import CustomSelect from "../common/CustomSelect";
import CustomTextInput from "../common/CustomTextInput";
import CustomTimePicker from "../common/CustomTimePicker";
import { sampleTypeTestsStructure } from "../data/SampleEntryTestsForTypeProvider";
import { ConfigurationContext, NotificationContext } from "../layout/Layout";
import { getFromOpenElisServer } from "../utils/Utils";

const SampleType = (props) => {
  const { userSessionDetails } = useContext(UserSessionDetailsContext);
  const { configurationProperties } = useContext(ConfigurationContext);

  const intl = useIntl();

  const componentMounted = useRef(false);
  const sampleTypesRef = useRef(null);
  const sampleMethodsRef = useRef(null);

  const {
    index,
    rejectSampleReasons,
    removeSample,
    sample,
    isTb,
    isBacterio = false,
  } = props;

  const [sampleTypes, setSampleTypes] = useState([]);
  const [selectedSampleType, setSelectedSampleType] = useState({
    id: null,
    name: "",
    element_index: 0,
  });
  const [selectedTbSampleMethod, setSelectedTbSampleMethod] = useState({
    id: sample?.tbData?.selectedTbMethod || null,
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
  const [uomList, setUomList] = useState([]);
  const [sampleXml, setSampleXml] = useState(
    sample?.sampleXML != null && Object.keys(sample.sampleXML).length > 0
      ? sample.sampleXML
      : {
          collectionDate:
            configurationProperties?.AUTOFILL_COLLECTION_DATE === "true"
              ? configurationProperties.currentDateAsText
              : "",
          collector: "",
          quantity: "",
          uom: "",
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

  const defaultSelect = { id: "", value: "Choose Rejection Reason" };
  const mapDictionaryToOptions = (list) =>
    (Array.isArray(list) ? list : []).map((item) => ({
      id: item.id || item.value,
      value: item.value || item.name || "",
    }));
  const [tbData, setTbData] = useState(
    sample?.tbData != null
      ? sample.tbData
      : {
          tbOrderReason: "",
          tbDiagnosticReason: "",
          tbFollowupReason: "",
          tbFollowupPeriodLine1: "",
          tbFollowupPeriodLine2: "",
          tbAspect: "",
          tbSpecimenNature: "",
          tbSubjectNumber: "",
          tbSubjectNumberRes: "",
          selectedTbMethod: "",
        },
  );
  const [tbReasonDiagnostic, setTbReasonDiagnostic] = useState("");
  const [tbReasonFollowUp, setTbReasonFollowUp] = useState("");

  const [tbFollowUpLine1, setTbFollowUpLine1] = useState("");
  const [tbFollowUpLine2, setTbFollowUpLine2] = useState("");
  const [tbFollowupPeriodsLine1, setTbFollowupPeriodsLine1] = useState([]);
  const [tbFollowupPeriodsLine2, setTbFollowupPeriodsLine2] = useState([]);
  const [microscopieTBId, setMicroscopieTBId] = useState("");
  const [followupLine1Id, setFollowupLine1Id] = useState("");
  const [followupLine2Id, setFollowupLine2Id] = useState("");

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

  function handleQuantity(value) {
    setSampleXml({
      ...sampleXml,
      quantity: value.target.value,
    });
  }

  function handleUom(value) {
    setSampleXml({
      ...sampleXml,
      uom: value,
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

  const handleFetchSampleTypeTests = (sampleTypeId, sampleTypeLabel, index) => {
    // Décocher les tests issus des panels précédemment sélectionnés.
    for (let i in selectedPanels) {
      const testIds = isTb
        ? selectedPanels[i].test_ids
        : selectedPanels[i].testIds;
      triggerPanelCheckBoxChange(false, testIds);
    }
    setSelectedPanels([]);
    setPanelSearchTerm("");
    setSearchBoxPanels([]);
    setSelectedTests([]);
    setReferralRequests([]);
    setSelectedSampleType({
      ...selectedSampleType,
      id: sampleTypeId,
      name: sampleTypeLabel,
      element_index: index,
    });
    props.sampleTypeObject({
      sampleTypeId: sampleTypeId,
      sampleObjectIndex: index,
    });
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
    console.log(res);
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
    setFollowupReason(value);
    // Periods are preloaded, just update the tbData
    handleChange("tbFollowupReason", value);
  }

  useEffect(() => {
    if (tbData.tbDiagnosticReason) {
      const category =
        tbData.tbDiagnosticReason === tbReasonDiagnostic
          ? "TB Diagnostic Reasons"
          : "TB Followup Reasons";

      getFromOpenElisServer(
        `/rest/Dictionary-by-ByCategory?category=${encodeURIComponent(category)}`,
        fetTbReasons,
      );
    }
  }, [tbData.tbDiagnosticReason, tbReasonDiagnostic]);

  // Followup periods are preloaded at component mount, no need to load them dynamically
  // The UI will display the appropriate list based on tbFollowupReason value

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
        if (tbData.selectedTbMethod === microscopieTBId) {
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
  }, [tbData.selectedTbMethod, isTb]);

  useEffect(() => {
    componentMounted.current = true;
    if (!isTb) {
      if (selectedSampleType.id !== "" && selectedSampleType.id != null) {
        getFromOpenElisServer(
          `/rest/sample-type-tests?sampleType=${selectedSampleType.id}`,
          fetchSampleTypeTests,
        );
      }
      return () => {
        componentMounted.current = false;
      };
    }
  }, [selectedSampleType.id]);

  useEffect(() => {
    getFromOpenElisServer(`/rest/UomCreate`, fetchUomCreate);
  }, []);

  // When the SHOW_SAMPLE_QUANTITY_AND_UOM config is OFF, ensure the hidden fields don't
  // submit any value (e.g. left over from a previous toggle or a stale default).
  useEffect(() => {
    if (configurationProperties?.SHOW_SAMPLE_QUANTITY_AND_UOM !== "true") {
      if (sampleXml.quantity !== "" || sampleXml.uom !== "") {
        setSampleXml({ ...sampleXml, quantity: "", uom: "" });
      }
    }
  }, [
    configurationProperties?.SHOW_SAMPLE_QUANTITY_AND_UOM,
    sampleXml.quantity,
    sampleXml.uom,
  ]);

  const fetchUomCreate = (res) => {
    if (componentMounted.current) {
      const all = res.existingUomList || [];
      // Sample entry only supports mm³ and mL. Match by display value (case-insensitive),
      // tolerating both "mm3"/"mm³" and "mL"/"ml" spellings stored in unit_of_measure.
      const allowed = ["mm3", "mm³", "ml"];
      const filtered = all.filter((u) => {
        const v = (u.value || u.label || u.unitOfMeasureName || "")
          .toString()
          .toLowerCase()
          .trim();
        return allowed.includes(v);
      });
      setUomList(filtered);
    }
  };

  useEffect(() => {
    componentMounted.current = true;
    if (selectedTbSampleMethod.id !== "" && selectedTbSampleMethod.id != null) {
      if (isTb) {
        getFromOpenElisServer(
          `/MicrobiologyTb/panel_test?method=${selectedTbSampleMethod.id}`,
          fetchSampleTypeTests,
        );
        if (selectedTbSampleMethod.id === microscopieTBId) {
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
  }, [selectedTbSampleMethod.id, isTb]);

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
  useEffect(() => {
    if (isBacterio) {
      props.sampleTypeObject({
        sampleObjectIndex: index,
      });
    }
  }, [isBacterio]);

  const repopulateUI = () => {
    console.log("repopulateUI called", {
      isTb,
      sampleTbData: props.sample?.tbData,
    });
    if (props.sample !== null) {
      setSelectedTests(props.sample.tests);
      setSelectedPanels(props.sample.panels);
      setSelectedSampleType({
        id: props.sample.sampleTypeId,
      });

      // Restore tbData if it exists
      if (props.sample.tbData) {
        setTbData(props.sample.tbData);

        // Restore selectedTbSampleMethod if selectedTbMethod exists
        if (props.sample.tbData.selectedTbMethod) {
          setSelectedTbSampleMethod({
            id: props.sample.tbData.selectedTbMethod,
            name: "",
            element_index: index,
          });
        }
      }
    }
  };

  // Reload dependent lists when TB data is restored on tab change
  useEffect(() => {
    if (
      isTb &&
      tbData.tbDiagnosticReason &&
      tbReasonDiagnostic &&
      tbReasonFollowUp
    ) {
      // Reload reasons list based on diagnostic reason
      const category =
        tbData.tbDiagnosticReason === tbReasonDiagnostic
          ? "TB Diagnostic Reasons"
          : "TB Followup Reasons";

      getFromOpenElisServer(
        `/rest/Dictionary-by-ByCategory?category=${encodeURIComponent(category)}`,
        fetTbReasons,
      );
    }
  }, [tbReasonDiagnostic, tbReasonFollowUp, isTb]);

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
    // Load TB dictionary mapping for dynamic IDs
    getFromOpenElisServer("/rest/tb-dictionary-mapping", (mapping) => {
      if (mapping) {
        if (mapping.Microsc) setMicroscopieTBId(mapping.Microsc);
        if (mapping["TB Line1"]) setFollowupLine1Id(mapping["TB Line1"]);
        if (mapping["TB Line2"]) setFollowupLine2Id(mapping["TB Line2"]);
      }
    });
    // Preload TB followup periods for both lines
    getFromOpenElisServer("/rest/tb-followup-periods-line1", (res) => {
      if (res) {
        setTbFollowupPeriodsLine1(res);
      }
    });
    getFromOpenElisServer("/rest/tb-followup-periods-line2", (res) => {
      if (res) {
        setTbFollowupPeriodsLine2(res);
      }
    });
    // Preload TB sample aspects
    getFromOpenElisServer(
      "/rest/Dictionary-by-ByCategory?category=TB Sample Aspects",
      fetchTbSampleAspects,
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
        <ComboBox
          className="selectSampleType"
          id={"sampleId_" + index}
          name="sampleId"
          items={sampleTypes || []}
          itemToString={(item) => (item && item.value ? item.value : "")}
          shouldFilterItem={({ item, inputValue }) => {
            if (!inputValue) return true;
            const label = (item && item.value ? item.value : "").toLowerCase();
            return label.includes(inputValue.toLowerCase());
          }}
          selectedItem={
            (sampleTypes || []).find(
              (st) => String(st.id) === String(props.sample.sampleTypeId),
            ) || null
          }
          placeholder={intl.formatMessage({ id: "sample.select.type" })}
          titleText=""
          onChange={({ selectedItem }) => {
            handleFetchSampleTypeTests(
              selectedItem ? selectedItem.id : "",
              selectedItem ? selectedItem.value : "",
              index,
            );
          }}
          required
        />
        {/* Hidden field kept for backwards-compat refs/forms */}
        <input
          type="hidden"
          ref={sampleTypesRef}
          name="sampleIdHidden"
          value={props.sample.sampleTypeId || ""}
        />

        <CustomCheckBox
          id={"reject_" + index}
          checked={sampleXml.rejected}
          onChange={(value) => handleRejection(value)}
          label={intl.formatMessage({ id: "sample.reject.label" })}
        />
        {sampleXml.rejected && (
          <CustomSelect
            id={"rejectedReasonId_" + index}
            options={rejectSampleReasons}
            disabled={rejectionReasonsDisabled}
            defaultSelect={defaultSelect}
            value={sampleXml.rejectionReason}
            onChange={(e) => handleReasons(e)}
          />
        )}
        {configurationProperties?.SHOW_SAMPLE_QUANTITY_AND_UOM === "true" && (
          <div className="inlineDiv" style={{ display: "flex", gap: "1rem" }}>
            <TextInput
              value={sampleXml.quantity}
              name="quantity"
              labelText={intl.formatMessage({
                id: "sample.quantity.label",
              })}
              id="quantity"
              type="number"
              min="0"
              onChange={(value) => handleQuantity(value)}
              placeholder={intl.formatMessage({
                id: "sample.quantity.label",
              })}
            />

            <CustomSelect
              id={"uomId_" + index}
              labelText={intl.formatMessage({ id: "sample.uom.label" })}
              options={uomList}
              disabled={false}
              value={sampleXml.uom}
              onChange={(value) => handleUom(value)}
            />
          </div>
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
            <Column lg={8} md={4} sm={4}>
              <TextInput
                value={tbData.tbSubjectNumber}
                onChange={(e) => {
                  let value = e.target.value.toUpperCase();
                  // Format: XXXXX/XX
                  value = value.replace(/[^0-9]/g, "");
                  if (value.length > 5) {
                    value = value.slice(0, 5) + "/" + value.slice(5);
                  }
                  if (value.length > 8) {
                    value = value.slice(0, 8);
                  }
                  // Clear the other field if this one has a value
                  setTbData((prev) => ({
                    ...prev,
                    tbSubjectNumber: value,
                    tbSubjectNumberRes:
                      value.length > 0 ? "" : prev.tbSubjectNumberRes,
                  }));
                }}
                labelText={intl.formatMessage({
                  id: "patient.subject.tbnumber",
                })}
                id={"tbSubjectNumber_" + index}
                placeholder="XXXXX/XX"
                maxLength={8}
              />
            </Column>
            <Column lg={8} md={4} sm={4}>
              <TextInput
                value={tbData.tbSubjectNumberRes}
                onChange={(e) => {
                  let value = e.target.value.toUpperCase();
                  // Format: AAAA/XX/XXX
                  value = value.replace(/[^0-9]/g, "");
                  if (value.length > 4) {
                    value = value.slice(0, 4) + "/" + value.slice(4);
                  }
                  if (value.length > 7) {
                    value = value.slice(0, 7) + "/" + value.slice(7);
                  }
                  if (value.length > 11) {
                    value = value.slice(0, 11);
                  }
                  // Clear the other field if this one has a value
                  setTbData((prev) => ({
                    ...prev,
                    tbSubjectNumberRes: value,
                    tbSubjectNumber:
                      value.length > 0 ? "" : prev.tbSubjectNumber,
                  }));
                }}
                labelText={intl.formatMessage({
                  id: "patient.subject.tbnumber_rr",
                })}
                id={"tbSubjectNumberRes_" + index}
                placeholder="AAAA/XX/XXX"
                maxLength={11}
              />
            </Column>
            {tbData.tbDiagnosticReason === tbReasonFollowUp && (
              <>
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
                      {tbFollowupPeriodsLine1.map((option) => {
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
                        {tbFollowupPeriodsLine2.map((option) => {
                          return (
                            <SelectItem
                              key={option.id}
                              value={option.id}
                              text={option.value}
                            />
                          );
                        })}
                      </Select>
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
            {isTb && tbData.selectedTbMethod === microscopieTBId && (
              <Column lg={8} md={4} sm={4}>
                <Select
                  id={"tbAspect_" + index}
                  value={tbData.tbAspect}
                  onChange={(e) => handleChange("tbAspect", e.target.value)}
                  required
                  labelText={<FormattedMessage id="sample.tb.aspect" />}
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
          {selectedTests && !selectedTests.length ? (
            ""
          ) : (
            <h4>
              <FormattedMessage id="ordertests.title" />
            </h4>
          )}
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

import { useContext, useEffect, useRef, useState } from "react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  Button,
  Checkbox,
  Select,
  SelectItem,
  TextInput,
  Loading,
  RadioButtonGroup,
  RadioButton,
} from "@carbon/react";
import AutoComplete from "../common/AutoComplete";
import CustomDatePicker from "../common/CustomDatePicker";
import { AlertDialog, NotificationKinds } from "../common/CustomNotification";
import PageBreadCrumb from "../common/PageBreadCrumb";
import { ConfigurationContext, NotificationContext } from "../layout/Layout";
import {
  getFromOpenElisServer,
  postToOpenElisServerFullResponse,
} from "../utils/Utils";
import { studyForms, LAB_PREFIXES } from "../data/ViralLoad";
import { CheckmarkFilled } from "@carbon/icons-react";
import config from "../../config.json";

// ─── Styles ────────────────────────────────────────────────────────────────────
const S = {
  page: { padding: "0 1rem 2rem" },
  sectionHeader: {
    background: "#295785",
    color: "#fff",
    padding: "6px 10px",
    fontWeight: "bold",
    fontSize: "14px",
    marginTop: "4px",
  },
  subHeader: {
    background: "#295785",
    color: "#fff",
    padding: "4px 10px",
    fontWeight: "bold",
    fontSize: "13px",
  },
  row: {
    display: "flex",
    alignItems: "center",
    borderBottom: "1px solid #e0e0e0",
    padding: "3px 10px",
    minHeight: "36px",
    background: "#fff",
  },
  label: {
    width: "42%",
    paddingRight: "8px",
    color: "#0f62fe",
    fontSize: "13px",
    flexShrink: 0,
  },
  inputWrap: { width: "58%" },
  asterisk: { color: "red", marginRight: "2px" },
  plus: { color: "green", marginRight: "2px" },
  inlineGroup: { display: "flex", alignItems: "center", gap: "4px" },
  inlineLabel: { fontSize: "13px", color: "#555", whiteSpace: "nowrap" },
  smallInput: { width: "70px" },
  timeInput: { maxWidth: "120px" },
  outerBox: { border: "1px solid #c6c6c6", marginBottom: "1rem" },
};

// ─── Composant de ligne ───────────────────────────────────────────────────────
const Row = ({ label, required, optional, children }) => (
  <div style={S.row}>
    <div style={S.label}>
      {required && <span style={S.asterisk}>*</span>}
      {optional && <span style={S.plus}>+</span>}
      {label}
    </div>
    <div style={S.inputWrap}>{children}</div>
  </div>
);

// ─── Breadcrumb ───────────────────────────────────────────────────────────────
const breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "sidenav.label.addorder", link: "/" },
  {
    label: "banner.menu.resultvalidation.viralload",
    link: "/SampleEntryByProject",
  },
];

// ─── État initial ──────────────────────────────────────────────────────────────
const EMPTY_FORM = {
  currentDate: "",
  project: "",
  domain: "C",
  patientUpdateStatus: "ADD",
  patientPK: "",
  patientLastUpdated: "",
  personLastUpdated: "",
  patientFhirUuid: "",
  samplePK: "",
  subjectNumber: "",
  siteSubjectNumber: "",
  labNo: "",
  gender: "",
  birthDateForDisplay: "",
  receivedDateForDisplay: "",
  receivedTimeForDisplay: "",
  interviewDate: "",
  interviewTime: "",
  projectData: {
    ARVcenterName: "",
    ARVcenterCode: "",
    underInvestigationNote: "",
    dryTubeTaken: false,
    edtaTubeTaken: false,
    serologyHIVTest: false,
    glycemiaTest: false,
    creatinineTest: false,
    transaminaseTest: false,
    transaminaseALTLTest: false,
    transaminaseASTLTest: false,
    murexTest: false,
    genscreenTest: false,
    vironostikaTest: false,
    innoliaTest: false,
    nfsTest: false,
    cd4cd8Test: false,
    gbTest: false,
    lymphTest: false,
    monoTest: false,
    eoTest: false,
    basoTest: false,
    grTest: false,
    hbTest: false,
    hctTest: false,
    vgmTest: false,
    tcmhTest: false,
    ccmhTest: false,
    plqTest: false,
    cd4CountTest: false,
    cd3CountTest: false,
    viralLoadTest: false,
    genotypingTest: false,
    dnaPCR: false,
    dbsvlTaken: false,
    pscvlTaken: false,
    EIDSiteName: "",
    EIDsiteCode: "",
    dbsTaken: false,
    INDsiteName: "",
    address: "",
    phoneNumber: "",
    faxNumber: "",
    email: "",
    plasmaTaken: false,
    serumTaken: false,
    asanteTest: false,
    preservCytTaken: false,
    hpvTest: false,
    abbottOrRocheAnalysis: false,
    geneXpertAnalysis: false,
  },
  observations: {
    projectFormName: "",
    nameOfDoctor: "",
    nameOfSampler: "",
    nameOfRequestor: "",
    underInvestigation: "",
    hivStatus: "",
    currentARVTreatment: "",
    arvTreatmentInitDate: "",
    arvTreatmentRegime: "",
    currentARVTreatmentINNsList: ["", "", "", ""],
    vlReasonForRequest: "",
    vlOtherReasonForRequest: "",
    initcd4Count: "",
    initcd4Percent: "",
    initcd4Date: "",
    demandcd4Count: "",
    demandcd4Percent: "",
    demandcd4Date: "",
    vlBenefit: "",
    priorVLValue: "",
    priorVLDate: "",
    priorVLLab: "",
    vlPregnancy: "",
    vlSuckle: "",
    whichPCR: "",
    reasonForSecondPCRTest: "",
    eidInfantPTME: "",
    eidTypeOfClinic: "",
    eidTypeOfClinicOther: "",
    eidHowChildFed: "",
    eidStoppedBreastfeeding: "",
    eidInfantSymptomatic: "",
    eidInfantsARV: "",
    eidInfantCotrimoxazole: "",
    eidMothersHIVStatus: "",
    eidMothersARV: "",
    indFirstTestDate: "",
    indFirstTestName: "",
    indFirstTestResult: "",
    indSecondTestDate: "",
    indSecondTestName: "",
    indSecondTestResult: "",
    indSiteFinalResult: "",
    reasonForRequest: "",
    hpvSamplingMethod: "",
  },
  electronicOrder: { externalId: "" },
};

// ─── Calcul de l'âge ──────────────────────────────────────────────────────────
const computeAgeYears = (birthDate) => {
  if (!birthDate) return "";
  const parts = birthDate.includes("/")
    ? birthDate.split("/")
    : birthDate.split("-");
  if (parts.length !== 3) return "";
  const year = parseInt(birthDate.includes("/") ? parts[2] : parts[0], 10);
  return isNaN(year) ? "" : String(new Date().getFullYear() - year);
};

const computeAgeMonths = (birthDate) => {
  if (!birthDate) return "";
  const parts = birthDate.includes("/")
    ? birthDate.split("/")
    : birthDate.split("-");
  if (parts.length !== 3) return "";
  const [d, m, y] = birthDate.includes("/")
    ? [parseInt(parts[0]), parseInt(parts[1]), parseInt(parts[2])]
    : [parseInt(parts[2]), parseInt(parts[1]), parseInt(parts[0])];
  const now = new Date();
  let months = (now.getFullYear() - y) * 12 + (now.getMonth() + 1 - m);
  if (now.getDate() < d) months -= 1;
  return months >= 0 ? String(months % 12) : "";
};

// ─── Composant principal ──────────────────────────────────────────────────────
const ViralLoadEntry = () => {
  const intl = useIntl();
  const componentMounted = useRef(false);
  const { configurationProperties } = useContext(ConfigurationContext);
  const { notificationVisible, addNotification, setNotificationVisible } =
    useContext(NotificationContext);
  const serologyControlEnabled =
    configurationProperties.SEROLOGY_CONTROL === "true";

  const [form, setForm] = useState(EMPTY_FORM);
  const [genders, setGenders] = useState([]);
  const [yesNoList, setYesNoList] = useState([]);
  const [dictionaryLists, setDictionaryLists] = useState({
    YES_NO: [],
    YES_NO_UNKNOWN: [],
    HIV_STATUSES: [],
    HIV_TYPES: [],
    ARV_REGIME: [],
    ARV_REGIMEN_LIST: [],
    ARV_REASON_FOR_VL_DEMAND: [],
    EID_WHICH_PCR: [],
    EID_SECOND_PCR_REASON: [],
    EID_TYPE_OF_CLINIC: [],
    EID_HOW_CHILD_FED: [],
    EID_STOPPED_BREASTFEEDING: [],
    EID_INFANT_PROPHYLAXIS_ARV: [],
    EID_MOTHERS_HIV_STATUS: [],
    EID_MOTHERS_ARV_TREATMENT: [],
    EID_INFANT_COTRIMOXAZOLE: [],
    HPV_SAMPLING_METHOD: [],
    SPECIAL_REQUEST_REASONS: [],
    REFERRING_SITES: [],
  });
  const [arvOrgsByName, setArvOrgsByName] = useState([]);
  const [eidOrgsByCode, setEidOrgsByCode] = useState([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [showSuccess, setShowSuccess] = useState(false);
  const [savedLabNo, setSavedLabNo] = useState("");
  const [labNoError, setLabNoError] = useState("");
  const [labNoValid, setLabNoValid] = useState(false);
  const [patientLookupStatus, setPatientLookupStatus] = useState("idle"); // "idle" | "searching" | "found" | "notfound"
  const [innManualMode, setInnManualMode] = useState(false);
  const [serologyStatus, setSerologyStatus] = useState("idle"); // "idle" | "searching" | "found" | "notfound"
  const [vlPatientSearchDone, setVlPatientSearchDone] = useState(false);
  // Champs de recherche VL (non soumis au backend, servent uniquement à
  // pré-remplir les champs du formulaire après la recherche du patient).
  const [vlSearchSubjectNumber, setVlSearchSubjectNumber] = useState("");
  const [vlSearchSiteSubjectNumber, setVlSearchSiteSubjectNumber] =
    useState("");

  // ─── Chargement initial ──────────────────────────────────────────────────────
  useEffect(() => {
    componentMounted.current = true;

    getFromOpenElisServer("/rest/SamplePatientEntry", (data) => {
      if (!componentMounted.current) return;
      if (data) {
        if (data.formLists?.GENDERS) setGenders(data.formLists.GENDERS);
        if (data.dictionaryLists)
          setDictionaryLists((prev) => ({ ...prev, ...data.dictionaryLists }));
        if (data.sampleOrderItems) {
          setArvOrgsByName(data.sampleOrderItems.referringSiteList || []);
          // setArvOrgsByCode(data.organizationTypeLists.ARV_ORGS || []);
          //setEidOrgsByName(data.organizationTypeLists.EID_ORGS_BY_NAME || []);
          //setEidOrgsByCode(data.organizationTypeLists.EID_ORGS || []);
        }
        if (data.patientProperties) {
          setGenders(data.patientProperties.genders || []);
        }
        if (data.referralOrganizations) {
          setDictionaryLists((prev) => ({
            ...prev,
            REFERRING_SITES: data.referralOrganizations,
          }));
        }
      }
      setLoading(false);
    });

    getFromOpenElisServer("/rest/dictionary/category/Yes%20No", (data) => {
      if (!componentMounted.current) return;
      if (Array.isArray(data))
        setDictionaryLists((prev) => ({ ...prev, YES_NO: data }));
    });
    getFromOpenElisServer("/rest/dictionary/category/HIV%20Status", (data) => {
      if (!componentMounted.current) return;
      if (Array.isArray(data))
        setDictionaryLists((prev) => ({ ...prev, HIV_STATUSES: data }));
    });
    getFromOpenElisServer(
      "/rest/dictionary/category/ARV%20Treatment%20Regime",
      (data) => {
        if (!componentMounted.current) return;
        if (Array.isArray(data))
          setDictionaryLists((prev) => ({ ...prev, ARV_REGIME: data }));
      },
    );
    getFromOpenElisServer(
      "/rest/dictionary/category/Reason%20for%20Viral%20Load%20Request",
      (data) => {
        if (!componentMounted.current) return;
        if (Array.isArray(data))
          setDictionaryLists((prev) => ({
            ...prev,
            ARV_REASON_FOR_VL_DEMAND: data,
          }));
      },
    );
    getFromOpenElisServer(
      "/rest/dictionary/category/EID%20Which%20PCR%20Test",
      (data) => {
        if (!componentMounted.current) return;
        if (Array.isArray(data))
          setDictionaryLists((prev) => ({ ...prev, EID_WHICH_PCR: data }));
      },
    );
    getFromOpenElisServer(
      "/rest/dictionary/category/Reason%20for%20Second%20PCR%20Test",
      (data) => {
        if (!componentMounted.current) return;
        if (Array.isArray(data))
          setDictionaryLists((prev) => ({
            ...prev,
            EID_SECOND_PCR_REASON: data,
          }));
      },
    );
    getFromOpenElisServer(
      "/rest/dictionary/category/EID%20Type%20of%20Clinic",
      (data) => {
        if (!componentMounted.current) return;
        if (Array.isArray(data))
          setDictionaryLists((prev) => ({ ...prev, EID_TYPE_OF_CLINIC: data }));
      },
    );
    getFromOpenElisServer(
      "/rest/dictionary/category/EID%20How%20Infant%20Eating",
      (data) => {
        if (!componentMounted.current) return;
        if (Array.isArray(data))
          setDictionaryLists((prev) => ({ ...prev, EID_HOW_CHILD_FED: data }));
      },
    );
    getFromOpenElisServer(
      "/rest/dictionary/category/EID%20Stopped%20Breastfeeding",
      (data) => {
        if (!componentMounted.current) return;
        if (Array.isArray(data))
          setDictionaryLists((prev) => ({
            ...prev,
            EID_STOPPED_BREASTFEEDING: data,
          }));
      },
    );
    getFromOpenElisServer(
      "/rest/dictionary/category/EID%20Infant%27s%20ARV%20Prophylaxis",
      (data) => {
        if (!componentMounted.current) return;
        if (Array.isArray(data))
          setDictionaryLists((prev) => ({
            ...prev,
            EID_INFANT_PROPHYLAXIS_ARV: data,
          }));
      },
    );
    getFromOpenElisServer(
      "/rest/dictionary/category/Yes%20No%20Unknown",
      (data) => {
        if (!componentMounted.current) return;
        if (Array.isArray(data))
          setDictionaryLists((prev) => ({ ...prev, YES_NO_UNKNOWN: data }));
      },
    );
    getFromOpenElisServer(
      "/rest/dictionary/category/Mother%27s%20HIV%20Status",
      (data) => {
        if (!componentMounted.current) return;
        if (Array.isArray(data))
          setDictionaryLists((prev) => ({
            ...prev,
            EID_MOTHERS_HIV_STATUS: data,
          }));
      },
    );
    getFromOpenElisServer(
      "/rest/dictionary/category/Mother%27s%20ARV%20Treatment",
      (data) => {
        if (!componentMounted.current) return;
        if (Array.isArray(data))
          setDictionaryLists((prev) => ({
            ...prev,
            EID_MOTHERS_ARV_TREATMENT: data,
          }));
      },
    );
    getFromOpenElisServer(
      "/rest/dictionary/category/HPV%20Sampling%20Method",
      (data) => {
        if (!componentMounted.current) return;
        if (Array.isArray(data))
          setDictionaryLists((prev) => ({
            ...prev,
            HPV_SAMPLING_METHOD: data,
          }));
      },
    );
    getFromOpenElisServer(
      "/rest/dictionary/category/ARV%20Treatment%20Regime%20List",
      (data) => {
        if (!componentMounted.current) return;
        if (Array.isArray(data))
          setDictionaryLists((prev) => ({ ...prev, ARV_REGIMEN_LIST: data }));
      },
    );

    return () => {
      componentMounted.current = false;
    };
  }, []);

  // ─── Date/heure par défaut ───────────────────────────────────────────────────
  useEffect(() => {
    if (configurationProperties.currentDateAsText) {
      setForm((prev) => ({
        ...prev,
        currentDate: configurationProperties.currentDateAsText,
        receivedDateForDisplay:
          prev.receivedDateForDisplay ||
          configurationProperties.currentDateAsText,
        interviewDate:
          prev.interviewDate || configurationProperties.currentDateAsText,
        receivedTimeForDisplay:
          prev.receivedTimeForDisplay ||
          configurationProperties.currentTimeAsText ||
          "",
        interviewTime:
          prev.interviewTime || configurationProperties.currentTimeAsText || "",
      }));
    }
  }, [
    configurationProperties.currentDateAsText,
    configurationProperties.currentTimeAsText,
  ]);

  // ─── Helpers ─────────────────────────────────────────────────────────────────
  const set = (field, value) =>
    setForm((prev) => ({ ...prev, [field]: value }));
  const setPD = (field, value) =>
    setForm((prev) => ({
      ...prev,
      projectData: { ...prev.projectData, [field]: value },
    }));
  const setObs = (field, value) =>
    setForm((prev) => ({
      ...prev,
      observations: { ...prev.observations, [field]: value },
    }));
  const setINN = (i, value) =>
    setForm((prev) => {
      const list = [...prev.observations.currentARVTreatmentINNsList];
      list[i] = value;
      return {
        ...prev,
        observations: {
          ...prev.observations,
          currentARVTreatmentINNsList: list,
        },
      };
    });

  const lookupPatient = () => {
    const code = (form.siteSubjectNumber || "").trim();
    if (!code) {
      setPatientLookupStatus("idle");
      return;
    }
    setPatientLookupStatus("searching");
    getFromOpenElisServer(
      "/rest/patient-search-results?subjectNumber=" +
        encodeURIComponent(code) +
        "&suppressExternalSearch=true",
      (data) => {
        const results =
          data && data.patientSearchResults ? data.patientSearchResults : [];
        if (results.length > 0) {
          const p = results[0];
          setPatientLookupStatus("found");
          if (p.birthdate) set("birthDateForDisplay", p.dob);
          if (p.gender) set("gender", p.gender);
        } else {
          setPatientLookupStatus("notfound");
        }
      },
    );
  };

  // ─── Sérologie VIH existante (Charge Virale) ─────────────────────────────────
  // Mapping résultat sérologie (dictionnaire "HIVResult") -> entrée du
  // dictionnaire "HIV Status" affiché dans le select Type VIH du formulaire VL.
  const SEROLOGY_TO_HIV_STATUS_KEY = {
    HIV1: "HIVStatus.HIV_1",
    HIV2: "HIVStatus.HIV_2",
    HIVD: "HIVStatus.HIVDual",
  };

  const applyHivStatusFromSerology = (serologyResult) => {
    const targetKey = SEROLOGY_TO_HIV_STATUS_KEY[serologyResult];
    if (!targetKey) return false;
    const match = (dictionaryLists.HIV_TYPES || []).find(
      (d) => d.displayKey === targetKey,
    );
    if (match) {
      setObs("hivStatus", match.id);
      return true;
    }
    return false;
  };

  const searchVLSerology = (subjectNo, siteSubjectNo) => {
    if (!serologyControlEnabled) return;
    setSerologyStatus("searching");
    getFromOpenElisServer(
      "/rest/serology-result-by-patient?subjectNumber=" +
        encodeURIComponent(subjectNo) +
        "&siteSubjectNumber=" +
        encodeURIComponent(siteSubjectNo),
      (data) => {
        const validResults = ["HIV1", "HIV2", "HIVD"];
        const found = Boolean(
          data?.serologyResult && validResults.includes(data.serologyResult),
        );
        if (found && applyHivStatusFromSerology(data.serologyResult)) {
          setSerologyStatus("found");
          setPD("serologyHIVTest", false);
        } else {
          setSerologyStatus("notfound");
          setObs("hivStatus", "");
          setPD("serologyHIVTest", true);
          setPD("dryTubeTaken", true);
        }
      },
    );
  };

  // ─── Recherche patient (Charge Virale) ───────────────────────────────────────
  // Avant l'affichage du formulaire VL : on saisit Sujet No./Site Sujet No. et on
  // recherche. Si le patient est trouvé, le formulaire apparaît pré-rempli
  // (date de naissance, sexe). Sinon il apparaît vide pour une nouvelle saisie.
  const searchVLPatient = () => {
    const subjectNo = (vlSearchSubjectNumber || "").trim();
    const siteSubjectNo = (vlSearchSiteSubjectNumber || "").trim();
    if (!subjectNo && !siteSubjectNo) {
      setPatientLookupStatus("idle");
      return;
    }
    setPatientLookupStatus("searching");
    setVlPatientSearchDone(false);
    // Pré-remplit les champs du formulaire (soumis au backend) avec les valeurs
    // saisies dans la recherche ; le formulaire les affiche, modifiables.
    set("subjectNumber", subjectNo);
    set("siteSubjectNumber", siteSubjectNo);
    const searchParam = siteSubjectNo
      ? "subjectNumber=" + encodeURIComponent(siteSubjectNo)
      : "nationalID=" + encodeURIComponent(subjectNo);
    getFromOpenElisServer(
      "/rest/patient-search-results?" +
        searchParam +
        "&suppressExternalSearch=true",
      (data) => {
        const results =
          data && data.patientSearchResults ? data.patientSearchResults : [];
        if (results.length > 0) {
          const p = results[0];
          setPatientLookupStatus("found");
          if (p.birthdate) set("birthDateForDisplay", p.dob);
          if (p.gender) set("gender", p.gender);
        } else {
          setPatientLookupStatus("notfound");
        }
        setVlPatientSearchDone(true);
      },
    );
    if (serologyControlEnabled) searchVLSerology(subjectNo, siteSubjectNo);
  };

  const handleStudyChange = (studyId) => {
    setPatientLookupStatus("idle");
    setSerologyStatus("idle");
    setVlPatientSearchDone(false);
    setVlSearchSubjectNumber("");
    setVlSearchSiteSubjectNumber("");
    setForm((prev) => ({
      ...prev,
      project: studyId,
      labNo: "",
      observations: { ...prev.observations, projectFormName: studyId },
      projectData: {
        ...prev.projectData,
        //Vl default values
        viralLoadTest: studyId === "VL_Id",

        //HPV default values
        preservCytTaken: studyId === "HPV_Id",
        abbottOrRocheAnalysis: studyId === "HPV_Id",
        hpvTest: studyId === "HPV_Id",

        //EID default values
        dnaPCR: studyId === "EID_Id",
        dbsTaken: studyId === "EID_Id",

        //ARV INIT default values
        dryTubeTaken:
          studyId === "InitialARV_Id" || studyId === "FollowUpARV_Id",
        edtaTubeTaken: studyId === "InitialARV_Id",
        serologyHIVTest: studyId === "InitialARV_Id",
        creatinineTest:
          studyId === "InitialARV_Id" || studyId === "FollowUpARV_Id",
        nfsTest: studyId === "InitialARV_Id",
        cd4cd8Test: studyId === "InitialARV_Id",
      },
    }));
    setLabNoError("");
    setLabNoValid(false);
    //setSuccessMsg(false);
  };

  // ─── Logique conditionnelle ──────────────────────────────────────────────────
  const isFemale = form.gender === "F";

  const isOnARV = Boolean(
    dictionaryLists.YES_NO?.find(
      (d) =>
        d.id === form.observations.currentARVTreatment &&
        d.displayKey?.toLowerCase().includes("answer.yes"),
    ),
  );

  const isSecondPCR = Boolean(
    (dictionaryLists.EID_WHICH_PCR || []).find(
      (d) =>
        d.id === form.observations.whichPCR && d.displayKey === "EID.secondPCR",
    ),
  );

  const vlReasonIsOther = Boolean(
    dictionaryLists.ARV_REASON_FOR_VL_DEMAND?.find(
      (d) =>
        d.id === form.observations.vlReasonForRequest &&
        (d.displayKey?.toLowerCase().includes("other") ||
          d.displayKey?.toLowerCase().includes("autre")),
    ),
  );

  const eidTypeClinicIsOther = Boolean(
    dictionaryLists.EID_TYPE_OF_CLINIC?.find(
      (d) =>
        d.id === form.observations.eidTypeOfClinic &&
        (d.displayKey?.toLowerCase().includes("other") ||
          d.displayKey?.toLowerCase().includes("autre")),
    ),
  );

  const hasHadVL = Boolean(
    dictionaryLists.YES_NO?.find(
      (d) =>
        d.id === form.observations.vlBenefit &&
        d.displayKey?.toLowerCase().includes("yes"),
    ),
  );

  // ─── Validation ──────────────────────────────────────────────────────────────
  const validate = () => {
    const errors = [];
    const study = form.project;

    if (!study) {
      errors.push(
        intl.formatMessage({
          id: "error.select.study",
          defaultMessage: "Veuillez sélectionner une étude.",
        }),
      );
      return errors;
    }

    const req = (val, label) => {
      if (!val) errors.push(`${label} est obligatoire.`);
    };

    // Champs communs à toutes les études
    req(form.receivedDateForDisplay, "Date de Réception");
    req(form.interviewDate, "Date de Prélèvements");

    // La date de réception doit être strictement postérieure (jour calendaire) à la date de prélèvement
    if (form.interviewDate && form.receivedDateForDisplay) {
      const parseDate = (dateStr) => {
        const parts = dateStr.split("/");
        if (parts.length !== 3) return null;
        const isFr = configurationProperties.DEFAULT_DATE_LOCALE === "fr-FR";
        const [a, b, y] = parts.map(Number);
        const [d, m] = isFr ? [a, b] : [b, a];
        return new Date(y, m - 1, d);
      };
      const datePrelevement = parseDate(form.interviewDate);
      const dateReception = parseDate(form.receivedDateForDisplay);
      if (datePrelevement && dateReception && dateReception < datePrelevement) {
        errors.push(
          intl.formatMessage({
            id: "error.date.reception.must.be.after.collection",
            defaultMessage:
              "La date de réception doit être strictement superieur ou egale à la date de prélèvement.",
          }),
        );
      }
    }

    switch (study) {
      case "InitialARV_Id":
      case "FollowUpARV_Id":
        req(form.labNo, "N° Lab");
        req(form.projectData.ARVcenterCode, "Code du Centre");
        req(form.gender, "Sexe");
        req(form.birthDateForDisplay, "Date de Naissance");
        if (!form.subjectNumber && !form.siteSubjectNumber)
          errors.push("Numéro Sujet ou Numéro Site Sujet est requis.");
        break;
      case "RTN_Id":
        req(form.labNo, "N° Lab");
        req(form.gender, "Sexe");
        req(form.birthDateForDisplay, "Date de Naissance");
        break;
      case "EID_Id":
        req(form.gender, "Sexe");
        req(form.birthDateForDisplay, "Date de Naissance");
        break;
      case "Indeterminate_Id":
        req(form.labNo, "N° Lab");
        req(form.projectData.INDsiteName, "Nom du Centre");
        req(form.gender, "Sexe");
        req(form.birthDateForDisplay, "Date de Naissance");
        if (!form.subjectNumber && !form.siteSubjectNumber)
          errors.push("Numéro Sujet ou Numéro Site Sujet est requis.");
        break;
      case "Special_Request_Id":
        req(form.labNo, "N° Lab");
        req(form.gender, "Sexe");
        if (!form.subjectNumber && !form.siteSubjectNumber)
          errors.push("Numéro Sujet ou Numéro Site Sujet est requis.");
        break;
      case "VL_Id":
        req(form.gender, "Sexe");
        req(form.birthDateForDisplay, "Date de Naissance");
        if (serologyControlEnabled && serologyStatus === "notfound") {
          if (!form.projectData.serologyHIVTest) {
            errors.push(
              intl.formatMessage({
                id: "error.serology.test.required",
                defaultMessage:
                  "Un test de sérologie VIH est requis car aucun résultat existant n'a été trouvé.",
              }),
            );
          }
        } else {
          req(form.observations.hivStatus, "Type VIH");
        }
        break;
      case "Recency_Id":
        req(form.projectData.ARVcenterCode, "Code du Centre");
        req(form.labNo, "N° Lab");
        req(form.siteSubjectNumber, "Numéro Recency");
        req(form.gender, "Sexe");
        req(form.birthDateForDisplay, "Date de Naissance");
        break;
      case "HPV_Id":
        req(form.projectData.ARVcenterCode, "Code du Centre");
        req(form.labNo, "N° Lab");
        req(form.siteSubjectNumber, "Numéro Sujet HPV");
        req(form.birthDateForDisplay, "Date de Naissance");
        break;
      default:
        break;
    }
    return errors;
  };

  // ─── Soumission ───────────────────────────────────────────────────────────────
  const handleSubmit = () => {
    const errors = validate();
    if (errors.length > 0) {
      addNotification({
        title: intl.formatMessage({ id: "notification.title" }),
        message: errors.join(" | "),
        kind: NotificationKinds.error,
      });
      setNotificationVisible(true);
      return;
    }
    setSubmitting(true);
    const labNoToSave = form.labNo;
    postToOpenElisServerFullResponse(
      "/rest/SampleEntryByProjectSudyViralLoad",
      JSON.stringify(form),
      (res) => {
        setSubmitting(false);
        if (res.status === 200) {
          setSavedLabNo(labNoToSave);
          setShowSuccess(true);
          setSerologyStatus("idle");
          setVlPatientSearchDone(false);
          setVlSearchSubjectNumber("");
          setVlSearchSiteSubjectNumber("");
          setForm({
            ...EMPTY_FORM,
            currentDate: configurationProperties.currentDateAsText || "",
            receivedDateForDisplay:
              configurationProperties.currentDateAsText || "",
            interviewDate: configurationProperties.currentDateAsText || "",
          });
          window.scrollTo(0, 0);
        } else {
          addNotification({
            title: intl.formatMessage({ id: "notification.title" }),
            message: intl.formatMessage({
              id: "server.error.msg",
              defaultMessage: "Erreur lors de l'enregistrement",
            }),
            kind: NotificationKinds.error,
          });
          setNotificationVisible(true);
        }
      },
    );
  };

  const handleReset = () => {
    setSerologyStatus("idle");
    setVlPatientSearchDone(false);
    setVlSearchSubjectNumber("");
    setVlSearchSiteSubjectNumber("");
    setForm({
      ...EMPTY_FORM,
      project: form.project,
      currentDate: configurationProperties.currentDateAsText || "",
      receivedDateForDisplay: configurationProperties.currentDateAsText || "",
      interviewDate: configurationProperties.currentDateAsText || "",
      observations: {
        ...EMPTY_FORM.observations,
        projectFormName: form.project,
      },
    });
  };

  // ─── Éléments d'UI réutilisables ─────────────────────────────────────────────
  const placeholder = intl.formatMessage({
    id: "label.select",
    defaultMessage: "-- Sélectionner --",
  });

  // Retourne la date du lendemain au format dd/MM/yyyy (ou MM/dd/yyyy)
  const nextDay = (dateStr) => {
    if (!dateStr) return "";
    const isFr = configurationProperties.DEFAULT_DATE_LOCALE === "fr-FR";
    const parts = dateStr.split("/");
    if (parts.length !== 3) return "";
    const [a, b, y] = parts.map(Number);
    const [d, m] = isFr ? [a, b] : [b, a];
    const dt = new Date(y, m - 1, d + 1);
    const dd = String(dt.getDate()).padStart(2, "0");
    const mm = String(dt.getMonth() + 1).padStart(2, "0");
    return isFr
      ? `${dd}/${mm}/${dt.getFullYear()}`
      : `${mm}/${dd}/${dt.getFullYear()}`;
  };

  const fldReceivedDate = () => (
    <Row
      required
      label={intl.formatMessage({
        id: "sample.receivedDate",
        defaultMessage: "Date de Réception (jj/mm/aaaa)",
      })}
    >
      <CustomDatePicker
        id="f_receivedDate"
        labelText=""
        autofillDate
        value={form.receivedDateForDisplay}
        minDate={form.interviewDate}
        onChange={(d) => set("receivedDateForDisplay", d)}
      />
    </Row>
  );
  // ─── Saisie guidée des heures (HH:MM, chiffres uniquement, valeurs valides) ──
  const handleTimeChange = (field, rawValue) => {
    // Extraire uniquement les chiffres (ignore lettres et le ":" déjà présent)
    const digits = rawValue.replace(/\D/g, "").slice(0, 4);

    // 1er chiffre des heures : 0-2 uniquement
    if (digits.length >= 1 && parseInt(digits[0], 10) > 2) return;
    // 2 premiers chiffres : heures 00-23
    if (digits.length >= 2 && parseInt(digits.slice(0, 2), 10) > 23) return;
    // 1er chiffre des minutes : 0-5 uniquement
    if (digits.length >= 3 && parseInt(digits[2], 10) > 5) return;

    // Formatage : insérer ":" automatiquement après les heures
    const formatted =
      digits.length > 2 ? digits.slice(0, 2) + ":" + digits.slice(2) : digits;

    set(field, formatted);
  };

  const fldReceivedTime = () => (
    <Row
      label={intl.formatMessage({
        id: "order.reception.time",
        defaultMessage: "Heure de Réception (HH:mm)",
      })}
    >
      <TextInput
        id="f_receivedTime"
        labelText=""
        hideLabel
        placeholder="HH:MM"
        maxLength={5}
        value={form.receivedTimeForDisplay}
        onChange={(e) =>
          handleTimeChange("receivedTimeForDisplay", e.target.value)
        }
        style={S.timeInput}
      />
    </Row>
  );
  const fldInterviewDate = () => (
    <Row
      required
      label={intl.formatMessage({
        id: "label.interviewDate",
        defaultMessage: "Date de Prélèvements (jj/mm/aaaa)",
      })}
    >
      <CustomDatePicker
        id="f_interviewDate"
        labelText=""
        autofillDate
        value={form.interviewDate}
        disallowFutureDate
        onChange={(d) => set("interviewDate", d)}
      />
    </Row>
  );
  const fldInterviewTime = () => (
    <Row
      label={intl.formatMessage({
        id: "label.interviewTime",
        defaultMessage: "Heure de Prélèvements (HH:mm)",
      })}
    >
      <TextInput
        id="f_interviewTime"
        labelText=""
        hideLabel
        placeholder="HH:MM"
        maxLength={5}
        value={form.interviewTime}
        onChange={(e) => handleTimeChange("interviewTime", e.target.value)}
        style={S.timeInput}
      />
    </Row>
  );
  const fldGender = (required = true) => (
    <Row
      required={required}
      label={intl.formatMessage({
        id: "patient.gender",
        defaultMessage: "Sexe",
      })}
    >
      <Select
        id="f_gender"
        hideLabel
        labelText=""
        value={form.gender}
        onChange={(e) => set("gender", e.target.value)}
        style={{ maxWidth: "200px" }}
      >
        <SelectItem value="" text={placeholder} />
        {genders.map((g) => (
          <SelectItem
            key={g.id}
            value={g.id}
            text={g.localizedName || g.value}
          />
        ))}
      </Select>
    </Row>
  );
  const fldBirthDate = () => (
    <Row
      required
      label={intl.formatMessage({
        id: "patient.dob",
        defaultMessage: "Date de Naissance (jj/mm/aaaa)",
      })}
    >
      <CustomDatePicker
        id="f_birthDate"
        labelText=""
        value={form.birthDateForDisplay}
        disallowFutureDate
        updateStateValue
        onChange={(d) => set("birthDateForDisplay", d)}
      />
    </Row>
  );
  const fldAge = (showMonths = false, showWeeks = false) => {
    const handleYearsChange = (e) => {
      const val = e.target.value;
      if (val === "") {
        set("birthDateForDisplay", "");
        return;
      }
      const years = parseInt(val, 10);
      if (!isNaN(years) && years >= 0 && years <= 120) {
        const now = new Date();
        const birthYear = now.getFullYear() - years;
        const day = String(now.getDate()).padStart(2, "0");
        const month = String(now.getMonth() + 1).padStart(2, "0");
        set("birthDateForDisplay", `${day}/${month}/${birthYear}`);
      }
    };

    return (
      <Row
        label={intl.formatMessage({ id: "patient.age", defaultMessage: "Âge" })}
      >
        <div style={S.inlineGroup}>
          <span
            style={{ ...S.inlineLabel, fontSize: "15px", fontWeight: "600" }}
          >
            <FormattedMessage id="label.years" defaultMessage="Ans" />
          </span>
          <TextInput
            id="f_ageYears"
            hideLabel
            labelText=""
            size="sm"
            style={{ width: "90px" }}
            value={computeAgeYears(form.birthDateForDisplay)}
            onChange={handleYearsChange}
          />
          {(showMonths || showWeeks) && (
            <>
              <span style={S.inlineLabel}>
                <FormattedMessage id="label.months" defaultMessage="Mois" />
              </span>
              <TextInput
                id="f_ageMonths"
                hideLabel
                labelText=""
                size="sm"
                readOnly
                style={S.smallInput}
                value={computeAgeMonths(form.birthDateForDisplay)}
              />
            </>
          )}
          {showWeeks && (
            <>
              <span style={S.inlineLabel}>
                <FormattedMessage id="label.weeks" defaultMessage="Sem." />
              </span>
              <TextInput
                id="f_ageWeeks"
                hideLabel
                labelText=""
                size="sm"
                readOnly
                style={S.smallInput}
                value=""
              />
            </>
          )}
        </div>
      </Row>
    );
  };
  const fldSubjectNumber = (maxLength = 7, onBlurExtra) => (
    <Row
      optional
      label={intl.formatMessage({
        id: "sample.entry.project.subjectNumber",
        defaultMessage: "Sujet No.",
      })}
    >
      <TextInput
        id="f_subjectNumber"
        hideLabel
        labelText=""
        size="sm"
        maxLength={maxLength}
        style={{ maxWidth: "160px" }}
        value={form.subjectNumber}
        onChange={(e) => set("subjectNumber", e.target.value)}
        onBlur={() => {
          if (onBlurExtra) onBlurExtra();
        }}
      />
    </Row>
  );
  const fldSiteSubjectNumber = (required = false, withLookup = false) => (
    <Row
      optional={!required}
      required={required}
      label={intl.formatMessage({
        id: "patient.site.subject.number",
        defaultMessage: "Site Sujet No.",
      })}
    >
      <div style={{ position: "relative", display: "inline-block" }}>
        <TextInput
          id="f_siteSubjectNumber"
          hideLabel
          labelText=""
          size="sm"
          maxLength={17}
          placeholder="00000/AA/AA/00000"
          style={{ maxWidth: "220px" }}
          value={form.siteSubjectNumber}
          onChange={(e) => {
            const raw = e.target.value.replace(/\//g, "").toUpperCase();
            let validated = "";
            for (let i = 0; i < raw.length && i < 14; i++) {
              if (i < 5 || i >= 9) {
                if (/\d/.test(raw[i])) validated += raw[i];
              } else {
                if (/[A-Z0-9]/.test(raw[i])) validated += raw[i];
              }
            }
            let formatted = validated;
            if (validated.length > 9) {
              formatted =
                validated.slice(0, 5) +
                "/" +
                validated.slice(5, 7) +
                "/" +
                validated.slice(7, 9) +
                "/" +
                validated.slice(9);
            } else if (validated.length > 7) {
              formatted =
                validated.slice(0, 5) +
                "/" +
                validated.slice(5, 7) +
                "/" +
                validated.slice(7);
            } else if (validated.length > 5) {
              formatted = validated.slice(0, 5) + "/" + validated.slice(5);
            }
            set("siteSubjectNumber", formatted);
            if (withLookup && patientLookupStatus !== "idle")
              setPatientLookupStatus("idle");
          }}
          onBlur={() => {
            if (withLookup) lookupPatient();
          }}
        />
        {withLookup && patientLookupStatus === "searching" && (
          <span
            style={{
              position: "absolute",
              left: "calc(100% + 6px)",
              top: "50%",
              transform: "translateY(-50%)",
              fontSize: "12px",
              color: "#6f6f6f",
              fontStyle: "italic",
              whiteSpace: "nowrap",
            }}
          >
            Recherche…
          </span>
        )}
        {withLookup && patientLookupStatus === "found" && (
          <div
            style={{
              position: "absolute",
              left: "calc(100% + 6px)",
              top: "50%",
              transform: "translateY(-50%)",
              display: "flex",
              alignItems: "center",
              gap: "4px",
              color: "#24a148",
              whiteSpace: "nowrap",
            }}
          >
            <CheckmarkFilled size={16} />
            <span style={{ fontSize: "12px", fontWeight: "600" }}>
              Patient trouvé
            </span>
          </div>
        )}
        {withLookup && patientLookupStatus === "notfound" && (
          <span
            style={{
              position: "absolute",
              left: "calc(100% + 6px)",
              top: "50%",
              transform: "translateY(-50%)",
              fontSize: "12px",
              color: "#da1e28",
              fontWeight: "600",
              whiteSpace: "nowrap",
            }}
          >
            Patient non trouvé
          </span>
        )}
      </div>
    </Row>
  );
  const fldLabNo = (prefixKey, required = true) => {
    const resolvedPrefix = prefixKey
      ? intl.formatMessage({ id: prefixKey, defaultMessage: prefixKey })
      : "";
    const displayDigits = form.labNo.startsWith(resolvedPrefix)
      ? form.labNo.slice(resolvedPrefix.length)
      : form.labNo;
    return (
      <Row
        required={required}
        label={intl.formatMessage({
          id: "quick.entry.accession.number",
          defaultMessage: "N° Lab",
        })}
      >
        <div style={S.inlineGroup}>
          {resolvedPrefix && (
            <span style={S.inlineLabel}>{resolvedPrefix}</span>
          )}
          <div style={{ position: "relative", display: "inline-block" }}>
            <TextInput
              id="f_labNo"
              hideLabel
              labelText=""
              size="sm"
              maxLength={5}
              placeholder="00000"
              style={{ maxWidth: "100px" }}
              value={displayDigits}
              invalid={!!labNoError}
              invalidText={labNoError}
              onChange={(e) => {
                const digits = e.target.value.replace(/\D/g, "").slice(0, 5);
                setLabNoError("");
                setLabNoValid(false);
                set("labNo", resolvedPrefix + digits);
              }}
              onBlur={() => {
                if (displayDigits.length === 5 && resolvedPrefix) {
                  getFromOpenElisServer(
                    "/rest/SampleEntryAccessionNumberValidation?ignoreYear=false&ignoreUsage=false&field=labNo&accessionNumber=" +
                      encodeURIComponent(resolvedPrefix + displayDigits),
                    (res) => {
                      if (res && res.status === false) {
                        setLabNoError(
                          res.body ||
                            intl.formatMessage({
                              id: "error.field.required",
                              defaultMessage: "Numéro invalide ou déjà utilisé",
                            }),
                        );
                        setLabNoValid(false);
                      } else {
                        setLabNoValid(true);
                      }
                    },
                  );
                }
              }}
            />
            {labNoValid && !labNoError && (
              <CheckmarkFilled
                size={16}
                style={{
                  position: "absolute",
                  right: "-22px",
                  top: "50%",
                  transform: "translateY(-50%)",
                  color: "#24a148",
                  pointerEvents: "none",
                }}
              />
            )}
          </div>
        </div>
      </Row>
    );
  };
  // ─── N° Lab HPV : saisie libre 18 caractères ─────────────────────────────────
  const fldLabNoHPV = () => (
    <Row
      required
      label={intl.formatMessage({
        id: "quick.entry.accession.number",
        defaultMessage: "N° Lab",
      })}
    >
      <TextInput
        id="f_labNo_hpv"
        hideLabel
        labelText=""
        size="sm"
        maxLength={18}
        placeholder="__________________ (18 car. max)"
        style={{ maxWidth: "220px" }}
        value={form.labNo}
        onChange={(e) => set("labNo", e.target.value.slice(0, 18))}
      />
    </Row>
  );

  const fldNameOfDoctor = () => (
    <Row
      label={intl.formatMessage({
        id: "patient.project.nameOfClinician",
        defaultMessage: "Nom du clinicien",
      })}
    >
      <TextInput
        id="f_nameOfDoctor"
        hideLabel
        labelText=""
        size="sm"
        style={{ maxWidth: "360px" }}
        value={form.observations.nameOfDoctor}
        onChange={(e) => setObs("nameOfDoctor", e.target.value)}
      />
    </Row>
  );
  const fldNameOfSampler = () => (
    <Row
      label={intl.formatMessage({
        id: "patient.project.nameOfSampler",
        defaultMessage: "Nom du préleveur",
      })}
    >
      <TextInput
        id="f_nameOfSampler"
        hideLabel
        labelText=""
        size="sm"
        style={{ maxWidth: "360px" }}
        value={form.observations.nameOfSampler}
        onChange={(e) => setObs("nameOfSampler", e.target.value)}
      />
    </Row>
  );
  const fldUnderInvestigation = () => (
    <>
      <Row
        label={intl.formatMessage({
          id: "patient.project.underInvestigation",
          defaultMessage: "Suivi Requis",
        })}
      >
        <Select
          id="f_underInv"
          hideLabel
          labelText=""
          value={form.observations.underInvestigation}
          onChange={(e) => setObs("underInvestigation", e.target.value)}
          style={{ maxWidth: "200px" }}
        >
          <SelectItem value="" text={placeholder} />
          {(dictionaryLists.YES_NO || []).map((d) => (
            <SelectItem
              key={d.id}
              value={d.id}
              text={intl.formatMessage({
                id: d.displayKey,
                defaultMessage: d.value,
              })}
            />
          ))}
        </Select>
      </Row>
      <Row
        label={intl.formatMessage({
          id: "patient.project.underInvestigationComment",
          defaultMessage: "Note",
        })}
      >
        <TextInput
          id="f_underInvNote"
          hideLabel
          labelText=""
          size="sm"
          maxLength={1000}
          style={{ maxWidth: "500px" }}
          value={form.projectData.underInvestigationNote}
          onChange={(e) => setPD("underInvestigationNote", e.target.value)}
        />
      </Row>
    </>
  );
  const fldARVCenter = () => (
    <>
      <Row
        required
        label={intl.formatMessage({
          id: "sample.entry.project.ARV.centerName",
          defaultMessage: "Nom du Centre",
        })}
      >
        <div style={{ display: "flex", alignItems: "center", gap: "12px" }}>
          <div style={{ flex: "1 1 auto" }}>
            <AutoComplete
              id="f_centerName"
              name="f_centerName"
              label=""
              value={form.projectData.ARVcenterName}
              suggestions={arvOrgsByName.map((o) => ({
                id: o.id,
                value: o.organizationName || o.value,
              }))}
              onChange={(e) => {
                const val = e?.currentTarget?.value ?? e?.target?.value ?? "";
                setPD("ARVcenterName", val);
                if (!val) setPD("ARVcenterCode", "");
              }}
              onSelect={(id) => {
                const org = arvOrgsByName.find((o) => o.id === id);
                if (org) {
                  setPD("ARVcenterName", org.organizationName || org.value);
                  setPD("ARVcenterCode", org.id);
                }
              }}
            />
          </div>
          {form.projectData.ARVcenterCode && (
            <span
              style={{
                fontSize: "13px",
                color: "#0f62fe",
                fontWeight: "bold",
                background: "#e8f0fb",
                padding: "2px 8px",
                borderRadius: "4px",
              }}
            >
              {intl.formatMessage({
                id: "label.centerCode",
                defaultMessage: "Code",
              })}
              : {form.projectData.ARVcenterCode}
            </span>
          )}
        </div>
      </Row>
      {/* <Row
        required
        label={intl.formatMessage({
          id: "patient.project.centerCode",
          defaultMessage: "Code du Centre",
        })}
      >
         <Select
          id="f_centerCode"
          hideLabel
          labelText=""
          value={form.projectData.ARVcenterCode}
          onChange={(e) => setPD("ARVcenterCode", e.target.value)}
          style={{ maxWidth: "280px" }}
        >
          <SelectItem value="" text={placeholder} />
          {arvOrgsByCode.map((o) => (
            <SelectItem
              key={o.id}
              value={o.id}
              text={o.doubleName || o.value}
            />
          ))}
        </Select> 
      </Row>*/}
    </>
  );
  // ─── Nom du site EID : autocomplete identique à fldARVCenter ────────────────
  const fldEIDSite = () => (
    <Row
      required
      label={intl.formatMessage({
        id: "sample.entry.project.siteName",
        defaultMessage: "Nom du Site",
      })}
    >
      <div style={{ display: "flex", alignItems: "center", gap: "12px" }}>
        <div style={{ flex: "1 1 auto", maxWidth: "280px" }}>
          <AutoComplete
            id="eid_siteName"
            name="eid_siteName"
            label=""
            value={form.projectData.EIDSiteName}
            suggestions={arvOrgsByName.map((o) => ({
              id: o.id,
              value: o.organizationName || o.value,
            }))}
            onChange={(e) => {
              const val = e?.currentTarget?.value ?? e?.target?.value ?? "";
              setPD("EIDSiteName", val);
              if (!val) setPD("EIDsiteCode", "");
            }}
            onSelect={(id) => {
              const org = arvOrgsByName.find((o) => o.id === id);
              if (org) {
                setPD("EIDSiteName", org.organizationName || org.value);
                setPD("EIDsiteCode", org.id);
              }
            }}
          />
        </div>
        {form.projectData.EIDsiteCode && (
          <span
            style={{
              fontSize: "13px",
              color: "#0f62fe",
              fontWeight: "bold",
              background: "#e8f0fb",
              padding: "2px 8px",
              borderRadius: "4px",
            }}
          >
            {intl.formatMessage({
              id: "label.centerCode",
              defaultMessage: "Code",
            })}
            : {form.projectData.EIDsiteCode}
          </span>
        )}
      </div>
    </Row>
  );

  const fldHivStatus = (listKey = "HIV_STATUSES", required = false) => (
    <Row
      required={required}
      label={intl.formatMessage({
        id: "patient.project.hivStatus",
        defaultMessage: "Type/Statut VIH",
      })}
    >
      <Select
        id="f_hivStatus"
        hideLabel
        labelText=""
        value={form.observations.hivStatus}
        onChange={(e) => setObs("hivStatus", e.target.value)}
        style={{ maxWidth: "240px" }}
      >
        <SelectItem value="" text={placeholder} />
        {(dictionaryLists.HIV_STATUSES || []).map((d) => (
          <SelectItem
            key={d.id}
            value={d.id}
            text={intl.formatMessage({
              id: d.displayKey,
              defaultMessage: d.value,
            })}
          />
        ))}
      </Select>
    </Row>
  );
  const fldDryTubeSpecimens = () => (
    <>
      <div style={S.subHeader}>
        <FormattedMessage
          id="sample.entry.project.title.specimen"
          defaultMessage="Échantillons"
        />
      </div>
      <div style={S.row}>
        <div style={S.label}>
          <FormattedMessage
            id="sample.entry.project.ARV.dryTubeTaken"
            defaultMessage="Tube Sec"
          />
        </div>
        <div style={S.inputWrap}>
          <Checkbox
            id="f_dryTubeTaken"
            labelText=""
            checked={form.projectData.dryTubeTaken}
            onChange={(_, { checked }) => setPD("dryTubeTaken", checked)}
          />
        </div>
      </div>
      <div style={S.row}>
        <div style={S.label}>
          <FormattedMessage
            id="sample.entry.project.ARV.edtaTubeTaken"
            defaultMessage="Tube EDTA"
          />
        </div>
        <div style={S.inputWrap}>
          <Checkbox
            id="f_edtaTubeTaken"
            labelText=""
            checked={form.projectData.edtaTubeTaken}
            onChange={(_, { checked }) => setPD("edtaTubeTaken", checked)}
          />
        </div>
      </div>
    </>
  );
  const fldDryTubeTests = () => (
    <>
      <div style={S.subHeader}>
        <FormattedMessage
          id="sample.entry.project.title.dryTube"
          defaultMessage="Tests Tube Sec"
        />
      </div>
      {[
        [
          "serologyHIVTest",
          "Sérologie VIH",
          "sample.entry.project.serologyHIVTest",
        ],
        ["glycemiaTest", "Glycémie", "sample.entry.project.ARV.glycemiaTest"],
        [
          "creatinineTest",
          "Créatinine",
          "sample.entry.project.ARV.creatinineTest",
        ],
        [
          "transaminaseTest",
          "Transaminases",
          "sample.entry.project.ARV.transaminaseTest",
        ],
      ].map(([field, def, id]) => (
        <div key={field} style={S.row}>
          <div style={S.label}>
            <FormattedMessage id={id} defaultMessage={def} />
          </div>
          <div style={S.inputWrap}>
            <Checkbox
              id={`f_${field}`}
              labelText=""
              checked={form.projectData[field]}
              onChange={(_, { checked }) => setPD(field, checked)}
            />
          </div>
        </div>
      ))}
    </>
  );
  const fldEdtaTubeTests = () => (
    <>
      <div style={S.subHeader}>
        <FormattedMessage
          id="sample.entry.project.title.edtaTube"
          defaultMessage="Tests Tube EDTA"
        />
      </div>
      {[
        ["nfsTest", "NFS", "sample.entry.project.ARV.nfsTest"],
        ["cd4cd8Test", "CD4/CD8", "sample.entry.project.ARV.cd4cd8Test"],
      ].map(([field, def, id]) => (
        <div key={field} style={S.row}>
          <div style={S.label}>
            <FormattedMessage id={id} defaultMessage={def} />
          </div>
          <div style={S.inputWrap}>
            <Checkbox
              id={`f_${field}`}
              labelText=""
              checked={form.projectData[field]}
              onChange={(_, { checked }) => setPD(field, checked)}
            />
          </div>
        </div>
      ))}
    </>
  );
  const fldOtherTests = () => (
    <>
      <div style={S.subHeader}>
        <FormattedMessage
          id="sample.entry.project.title.otherTests"
          defaultMessage="Autres Tests"
        />
      </div>
      {[
        [
          "viralLoadTest",
          "Charge Virale",
          "sample.entry.project.ARV.viralLoadTest",
        ],
        [
          "genotypingTest",
          "Génotypage",
          "sample.entry.project.ARV.genotypingTest",
        ],
      ].map(([field, def, id]) => (
        <div key={field} style={S.row}>
          <div style={S.label}>
            <FormattedMessage id={id} defaultMessage={def} />
          </div>
          <div style={S.inputWrap}>
            <Checkbox
              id={`f_${field}`}
              labelText=""
              checked={form.projectData[field]}
              onChange={(_, { checked }) => setPD(field, checked)}
            />
          </div>
        </div>
      ))}
    </>
  );

  // ─── Rendu: ARV Initial ───────────────────────────────────────────────────────
  const renderInitialARV = () => (
    <div style={S.outerBox}>
      <div style={S.sectionHeader}>
        <FormattedMessage
          id="sample.entry.project.initialARV.title"
          defaultMessage="ARV - Bilan Initial"
        />
      </div>
      {fldARVCenter()}
      {fldNameOfDoctor()}
      {fldInterviewDate()}
      {fldInterviewTime()}
      {fldReceivedDate()}
      {fldReceivedTime()}
      {fldSubjectNumber(7)}
      {fldSiteSubjectNumber(false, true)}
      {fldLabNo(LAB_PREFIXES.InitialARV_Id)}
      {fldGender()}
      {fldBirthDate()}
      {fldAge(false, false)}
      {fldDryTubeSpecimens()}
      {fldDryTubeTests()}
      {fldEdtaTubeTests()}
      {fldOtherTests()}
      {fldUnderInvestigation()}
    </div>
  );

  // ─── Rendu: ARV Suivi ─────────────────────────────────────────────────────────
  const renderFollowUpARV = () => (
    <div style={S.outerBox}>
      <div style={S.sectionHeader}>
        <FormattedMessage
          id="sample.entry.project.followupARV.title"
          defaultMessage="ARV - Bilan de Suivi"
        />
      </div>
      {fldARVCenter()}
      {fldNameOfDoctor()}
      {fldInterviewDate()}
      {fldInterviewTime()}
      {fldReceivedDate()}
      {fldReceivedTime()}
      {fldSubjectNumber(7)}
      {fldSiteSubjectNumber(false, true)}
      {fldLabNo(LAB_PREFIXES.FollowUpARV_Id)}
      {fldGender()}
      {fldBirthDate()}
      {fldAge(false, false)}
      {fldHivStatus("HIV_STATUSES")}
      {fldDryTubeSpecimens()}
      {fldDryTubeTests()}
      {fldEdtaTubeTests()}
      {fldOtherTests()}
      {fldUnderInvestigation()}
    </div>
  );

  // ─── Rendu: RTN ───────────────────────────────────────────────────────────────
  const renderRTN = () => (
    <div style={S.outerBox}>
      <div style={S.sectionHeader}>
        <FormattedMessage
          id="sample.entry.project.RTN.title"
          defaultMessage="RTN"
        />
      </div>
      {fldInterviewDate()}
      {fldInterviewTime()}
      {fldReceivedDate()}
      {fldReceivedTime()}
      {fldBirthDate()}
      {fldAge(true, false)}
      {fldGender()}
      {fldLabNo(LAB_PREFIXES.RTN_Id)}
      <div style={S.subHeader}>
        <FormattedMessage
          id="sample.entry.project.title.specimen"
          defaultMessage="Échantillons"
        />
      </div>
      <div style={S.row}>
        <div style={S.label}>
          <FormattedMessage
            id="sample.entry.project.ARV.dryTubeTaken"
            defaultMessage="Tube Sec"
          />
        </div>
        <div style={S.inputWrap}>
          <Checkbox
            id="rtn_dryTubeTaken"
            labelText=""
            checked={form.projectData.dryTubeTaken}
            onChange={(_, { checked }) => setPD("dryTubeTaken", checked)}
          />
        </div>
      </div>
      <div style={S.subHeader}>
        <FormattedMessage
          id="sample.entry.project.title.dryTube"
          defaultMessage="Tests Tube Sec"
        />
      </div>
      <div style={S.row}>
        <div style={S.label}>
          <FormattedMessage
            id="sample.entry.project.serologyHIVTest"
            defaultMessage="Sérologie VIH"
          />
        </div>
        <div style={S.inputWrap}>
          <Checkbox
            id="rtn_serologyHIVTest"
            labelText=""
            checked={form.projectData.serologyHIVTest}
            onChange={(_, { checked }) => setPD("serologyHIVTest", checked)}
          />
        </div>
      </div>
      {fldUnderInvestigation()}
    </div>
  );

  // ─── Rendu: EID ───────────────────────────────────────────────────────────────
  const renderEID = () => (
    <div style={S.outerBox}>
      <div style={S.sectionHeader}>
        <FormattedMessage
          id="sample.entry.project.EID.title"
          defaultMessage="EID"
        />
      </div>
      {fldInterviewDate()}
      {fldInterviewTime()}
      {fldReceivedDate()}
      {fldReceivedTime()}
      {fldEIDSite()}
      <Row
        optional
        label={intl.formatMessage({
          id: "sample.entry.project.EID.infantNumber",
          defaultMessage: "Numéro d'Enfant DBS",
        })}
      >
        <div style={S.inlineGroup}>
          <span style={S.inlineLabel}>DBS</span>
          <TextInput
            id="eid_infantNo"
            hideLabel
            labelText=""
            size="sm"
            style={{ maxWidth: "120px" }}
            value={form.subjectNumber}
            onChange={(e) => set("subjectNumber", e.target.value)}
          />
        </div>
      </Row>
      {fldSiteSubjectNumber(false, true)}
      {fldLabNo(LAB_PREFIXES.EID_Id)}
      <Row
        label={intl.formatMessage({
          id: "patient.project.eidWhichPCR",
          defaultMessage: "Quel PCR",
        })}
      >
        <Select
          id="eid_reasonPCR"
          hideLabel
          labelText=""
          value={form.observations.whichPCR}
          onChange={(e) => setObs("whichPCR", e.target.value)}
          style={{ maxWidth: "240px" }}
        >
          <SelectItem value="" text={placeholder} />
          {(dictionaryLists.EID_WHICH_PCR || []).map((d) => (
            <SelectItem
              key={d.id}
              value={d.id}
              text={intl.formatMessage({
                id: d.displayKey,
                defaultMessage: d.value,
              })}
            />
          ))}
        </Select>
      </Row>
      <Row
        label={intl.formatMessage({
          id: "sample.entry.project.EID.reasonForPCRTest",
          defaultMessage: "Raison du 2e PCR",
        })}
      >
        <Select
          id="eid_reasonPCR"
          hideLabel
          labelText=""
          disabled={!isSecondPCR}
          value={form.observations.reasonForSecondPCRTest}
          onChange={(e) => setObs("reasonForSecondPCRTest", e.target.value)}
          style={{ maxWidth: "280px" }}
        >
          <SelectItem value="" text={placeholder} />
          {(dictionaryLists.EID_SECOND_PCR_REASON || []).map((d) => (
            <SelectItem
              key={d.id}
              value={d.id}
              text={intl.formatMessage({
                id: d.displayKey,
                defaultMessage: d.value,
              })}
            />
          ))}
        </Select>
      </Row>
      <Row
        label={intl.formatMessage({
          id: "patient.project.nameOfRequestor",
          defaultMessage: "Nom du Demandeur",
        })}
      >
        <TextInput
          id="eid_requestor"
          hideLabel
          labelText=""
          size="sm"
          style={{ maxWidth: "280px" }}
          value={form.observations.nameOfRequestor}
          onChange={(e) => setObs("nameOfRequestor", e.target.value)}
        />
      </Row>
      {fldNameOfSampler()}
      <div style={S.subHeader}>
        <FormattedMessage
          id="sample.entry.project.title.infantInformation"
          defaultMessage="Informations Enfant"
        />
      </div>
      {fldBirthDate()}
      {fldAge(true, true)}
      {fldGender()}
      <Row
        label={intl.formatMessage({
          id: "patient.project.eidBenefitPTME",
          defaultMessage: "Bénéfice PTME",
        })}
      >
        <Select
          id="eid_ptme"
          hideLabel
          labelText=""
          value={form.observations.eidInfantPTME}
          onChange={(e) => setObs("eidInfantPTME", e.target.value)}
          style={{ maxWidth: "200px" }}
        >
          <SelectItem value="" text={placeholder} />
          {(dictionaryLists.YES_NO || []).map((d) => (
            <SelectItem
              key={d.id}
              value={d.id}
              text={intl.formatMessage({
                id: d.displayKey,
                defaultMessage: d.value,
              })}
            />
          ))}
        </Select>
      </Row>
      <Row
        label={intl.formatMessage({
          id: "patient.project.eidTypeOfClinic",
          defaultMessage: "Type de Clinique",
        })}
      >
        <Select
          id="eid_typeClinic"
          hideLabel
          labelText=""
          value={form.observations.eidTypeOfClinic}
          onChange={(e) => setObs("eidTypeOfClinic", e.target.value)}
          style={{ maxWidth: "240px" }}
        >
          <SelectItem value="" text={placeholder} />
          {(dictionaryLists.EID_TYPE_OF_CLINIC || []).map((d) => (
            <SelectItem
              key={d.id}
              value={d.id}
              text={intl.formatMessage({
                id: d.displayKey,
                defaultMessage: d.value,
              })}
            />
          ))}
        </Select>
      </Row>
      {eidTypeClinicIsOther && (
        <Row
          label={intl.formatMessage({
            id: "patient.project.specify",
            defaultMessage: "Préciser",
          })}
        >
          <TextInput
            id="eid_typeClinicOther"
            hideLabel
            labelText=""
            size="sm"
            style={{ maxWidth: "240px" }}
            value={form.observations.eidTypeOfClinicOther}
            onChange={(e) => setObs("eidTypeOfClinicOther", e.target.value)}
          />
        </Row>
      )}
      <Row
        label={intl.formatMessage({
          id: "patient.project.eidHowChildFed",
          defaultMessage: "Comment nourri",
        })}
      >
        <Select
          id="eid_howFed"
          hideLabel
          labelText=""
          value={form.observations.eidHowChildFed}
          onChange={(e) => setObs("eidHowChildFed", e.target.value)}
          style={{ maxWidth: "240px" }}
        >
          <SelectItem value="" text={placeholder} />
          {(dictionaryLists.EID_HOW_CHILD_FED || []).map((d) => (
            <SelectItem
              key={d.id}
              value={d.id}
              text={intl.formatMessage({
                id: d.displayKey,
                defaultMessage: d.value,
              })}
            />
          ))}
        </Select>
      </Row>
      <Row
        label={intl.formatMessage({
          id: "patient.project.eidStoppedBreastfeeding",
          defaultMessage: "Arrêt Allaitement",
        })}
      >
        <Select
          id="eid_stoppedBF"
          hideLabel
          labelText=""
          value={form.observations.eidStoppedBreastfeeding}
          onChange={(e) => setObs("eidStoppedBreastfeeding", e.target.value)}
          style={{ maxWidth: "240px" }}
        >
          <SelectItem value="" text={placeholder} />
          {(dictionaryLists.EID_STOPPED_BREASTFEEDING || []).map((d) => (
            <SelectItem
              key={d.id}
              value={d.id}
              text={intl.formatMessage({
                id: d.displayKey,
                defaultMessage: d.value,
              })}
            />
          ))}
        </Select>
      </Row>
      <Row
        label={intl.formatMessage({
          id: "patient.project.eidInfantSymptomatic",
          defaultMessage: "Enfant Symptomatique",
        })}
      >
        <Select
          id="eid_symptomatic"
          hideLabel
          labelText=""
          value={form.observations.eidInfantSymptomatic}
          onChange={(e) => setObs("eidInfantSymptomatic", e.target.value)}
          style={{ maxWidth: "200px" }}
        >
          <SelectItem value="" text={placeholder} />
          {(dictionaryLists.YES_NO || []).map((d) => (
            <SelectItem
              key={d.id}
              value={d.id}
              text={intl.formatMessage({
                id: d.displayKey,
                defaultMessage: d.value,
              })}
            />
          ))}
        </Select>
      </Row>
      <Row
        label={intl.formatMessage({
          id: "patient.project.eidInfantProphy",
          defaultMessage: "Prophylaxie ARV",
        })}
      >
        <Select
          id="eid_arvProphy"
          hideLabel
          labelText=""
          value={form.observations.eidInfantsARV}
          onChange={(e) => setObs("eidInfantsARV", e.target.value)}
          style={{ maxWidth: "240px" }}
        >
          <SelectItem value="" text={placeholder} />
          {(dictionaryLists.EID_INFANT_PROPHYLAXIS_ARV || []).map((d) => (
            <SelectItem
              key={d.id}
              value={d.id}
              text={intl.formatMessage({
                id: d.displayKey,
                defaultMessage: d.value,
              })}
            />
          ))}
        </Select>
      </Row>
      <Row
        label={intl.formatMessage({
          id: "patient.project.eidInfantCotrimoxazole",
          defaultMessage: "Cotrimoxazole",
        })}
      >
        <Select
          id="eid_cotrim"
          hideLabel
          labelText=""
          value={form.observations.eidInfantCotrimoxazole}
          onChange={(e) => setObs("eidInfantCotrimoxazole", e.target.value)}
          style={{ maxWidth: "240px" }}
        >
          <SelectItem value="" text={placeholder} />
          {(dictionaryLists.YES_NO_UNKNOWN || []).map((d) => (
            <SelectItem
              key={d.id}
              value={d.id}
              text={intl.formatMessage({
                id: d.displayKey,
                defaultMessage: d.value,
              })}
            />
          ))}
        </Select>
      </Row>
      <div style={S.subHeader}>
        <FormattedMessage
          id="sample.entry.project.title.mothersInformation"
          defaultMessage="Informations Mère"
        />
      </div>
      <Row
        label={intl.formatMessage({
          id: "patient.project.eidMothersStatus",
          defaultMessage: "Statut VIH Mère",
        })}
      >
        <Select
          id="eid_motherHIV"
          hideLabel
          labelText=""
          value={form.observations.eidMothersHIVStatus}
          onChange={(e) => setObs("eidMothersHIVStatus", e.target.value)}
          style={{ maxWidth: "240px" }}
        >
          <SelectItem value="" text={placeholder} />
          {(dictionaryLists.EID_MOTHERS_HIV_STATUS || []).map((d) => (
            <SelectItem
              key={d.id}
              value={d.id}
              text={intl.formatMessage({
                id: d.displayKey,
                defaultMessage: d.value,
              })}
            />
          ))}
        </Select>
      </Row>
      <Row
        label={intl.formatMessage({
          id: "patient.project.eidMothersARV",
          defaultMessage: "ARV Mère",
        })}
      >
        <Select
          id="eid_motherARV"
          hideLabel
          labelText=""
          value={form.observations.eidMothersARV}
          onChange={(e) => setObs("eidMothersARV", e.target.value)}
          style={{ maxWidth: "240px" }}
        >
          <SelectItem value="" text={placeholder} />
          {(dictionaryLists.EID_MOTHERS_ARV_TREATMENT || []).map((d) => (
            <SelectItem
              key={d.id}
              value={d.id}
              text={intl.formatMessage({
                id: d.displayKey,
                defaultMessage: d.value,
              })}
            />
          ))}
        </Select>
      </Row>
      {fldUnderInvestigation()}
      <div style={S.subHeader}>
        <FormattedMessage
          id="sample.entry.project.title.specimen"
          defaultMessage="Échantillons"
        />
      </div>
      <div style={S.row}>
        <div style={S.label}>
          <FormattedMessage
            id="sample.entry.project.ARV.dryTubeTaken"
            defaultMessage="Tube Sec"
          />
        </div>
        <div style={S.inputWrap}>
          <Checkbox
            id="eid_dryTube"
            labelText=""
            checked={form.projectData.dryTubeTaken}
            onChange={(_, { checked }) => setPD("dryTubeTaken", checked)}
          />
        </div>
      </div>
      <div style={S.row}>
        <div style={S.label}>
          <FormattedMessage
            id="sample.entry.project.title.dryBloodSpot"
            defaultMessage="DBS"
          />
        </div>
        <div style={S.inputWrap}>
          <Checkbox
            id="eid_dbs"
            labelText=""
            checked={form.projectData.dbsTaken}
            onChange={(_, { checked }) => setPD("dbsTaken", checked)}
          />
        </div>
      </div>
      <div style={S.subHeader}>
        <FormattedMessage
          id="sample.entry.project.title.tests"
          defaultMessage="Tests"
        />
      </div>
      <div style={S.row}>
        <div style={S.label}>
          <FormattedMessage
            id="sample.entry.project.dnaPCR"
            defaultMessage="DNA PCR"
          />
        </div>
        <div style={S.inputWrap}>
          <Checkbox
            id="eid_dnaPCR"
            labelText=""
            checked={form.projectData.dnaPCR}
            onChange={(_, { checked }) => setPD("dnaPCR", checked)}
          />
        </div>
      </div>
    </div>
  );

  // ─── Rendu: Indeterminate ─────────────────────────────────────────────────────
  const renderIndeterminate = () => (
    <div style={S.outerBox}>
      <div style={S.sectionHeader}>
        <FormattedMessage
          id="sample.entry.project.indeterminate.title"
          defaultMessage="Indeterminate"
        />
      </div>
      {fldInterviewDate()}
      {fldInterviewTime()}
      {fldReceivedDate()}
      {fldReceivedTime()}
      <Row
        required
        label={intl.formatMessage({
          id: "sample.entry.project.siteName",
          defaultMessage: "Nom du Site",
        })}
      >
        <Select
          id="ind_siteName"
          hideLabel
          labelText=""
          value={form.projectData.INDsiteName}
          onChange={(e) => setPD("INDsiteName", e.target.value)}
          style={{ maxWidth: "280px" }}
        >
          <SelectItem value="" text={placeholder} />
          {eidOrgsByCode.map((o) => (
            <SelectItem
              key={o.id}
              value={o.id}
              text={o.doubleName || o.value}
            />
          ))}
        </Select>
      </Row>
      <Row
        label={intl.formatMessage({
          id: "sample.entry.project.address",
          defaultMessage: "Adresse",
        })}
      >
        <TextInput
          id="ind_address"
          hideLabel
          labelText=""
          size="sm"
          style={{ maxWidth: "360px" }}
          value={form.projectData.address}
          onChange={(e) => setPD("address", e.target.value)}
        />
      </Row>
      <Row
        label={intl.formatMessage({
          id: "sample.entry.project.phoneNumber",
          defaultMessage: "Téléphone",
        })}
      >
        <TextInput
          id="ind_phone"
          hideLabel
          labelText=""
          size="sm"
          style={{ maxWidth: "200px" }}
          value={form.projectData.phoneNumber}
          onChange={(e) => setPD("phoneNumber", e.target.value)}
        />
      </Row>
      <Row
        label={intl.formatMessage({
          id: "sample.entry.project.faxNumber",
          defaultMessage: "Fax",
        })}
      >
        <TextInput
          id="ind_fax"
          hideLabel
          labelText=""
          size="sm"
          style={{ maxWidth: "200px" }}
          value={form.projectData.faxNumber}
          onChange={(e) => setPD("faxNumber", e.target.value)}
        />
      </Row>
      <Row
        label={intl.formatMessage({
          id: "sample.entry.project.email",
          defaultMessage: "Email",
        })}
      >
        <TextInput
          id="ind_email"
          hideLabel
          labelText=""
          size="sm"
          style={{ maxWidth: "280px" }}
          value={form.projectData.email}
          onChange={(e) => setPD("email", e.target.value)}
        />
      </Row>
      {fldSubjectNumber(7)}
      {fldSiteSubjectNumber(false, true)}
      {fldLabNo(LAB_PREFIXES.Indeterminate_Id)}
      {fldGender()}
      {fldBirthDate()}
      {fldAge()}
      <div style={S.subHeader}>
        <FormattedMessage
          id="sample.entry.project.firstTest"
          defaultMessage="Premier Test"
        />
      </div>
      <Row
        label={intl.formatMessage({
          id: "sample.entry.project.date",
          defaultMessage: "Date",
        })}
      >
        <CustomDatePicker
          id="ind_firstTestDate"
          labelText=""
          value={form.observations.indFirstTestDate}
          disallowFutureDate
          onChange={(d) => setObs("indFirstTestDate", d)}
        />
      </Row>
      <Row
        label={intl.formatMessage({
          id: "sample.entry.project.testName",
          defaultMessage: "Nom du test",
        })}
      >
        <TextInput
          id="ind_firstTestName"
          hideLabel
          labelText=""
          size="sm"
          style={{ maxWidth: "280px" }}
          value={form.observations.indFirstTestName}
          onChange={(e) => setObs("indFirstTestName", e.target.value)}
        />
      </Row>
      <Row
        label={intl.formatMessage({
          id: "sample.entry.project.result",
          defaultMessage: "Résultat",
        })}
      >
        <TextInput
          id="ind_firstTestResult"
          hideLabel
          labelText=""
          size="sm"
          style={{ maxWidth: "280px" }}
          value={form.observations.indFirstTestResult}
          onChange={(e) => setObs("indFirstTestResult", e.target.value)}
        />
      </Row>
      <div style={S.subHeader}>
        <FormattedMessage
          id="sample.entry.project.secondTest"
          defaultMessage="Deuxième Test"
        />
      </div>
      <Row
        label={intl.formatMessage({
          id: "sample.entry.project.date",
          defaultMessage: "Date",
        })}
      >
        <CustomDatePicker
          id="ind_secondTestDate"
          labelText=""
          value={form.observations.indSecondTestDate}
          disallowFutureDate
          onChange={(d) => setObs("indSecondTestDate", d)}
        />
      </Row>
      <Row
        label={intl.formatMessage({
          id: "sample.entry.project.testName",
          defaultMessage: "Nom du test",
        })}
      >
        <TextInput
          id="ind_secondTestName"
          hideLabel
          labelText=""
          size="sm"
          style={{ maxWidth: "280px" }}
          value={form.observations.indSecondTestName}
          onChange={(e) => setObs("indSecondTestName", e.target.value)}
        />
      </Row>
      <Row
        label={intl.formatMessage({
          id: "sample.entry.project.result",
          defaultMessage: "Résultat",
        })}
      >
        <TextInput
          id="ind_secondTestResult"
          hideLabel
          labelText=""
          size="sm"
          style={{ maxWidth: "280px" }}
          value={form.observations.indSecondTestResult}
          onChange={(e) => setObs("indSecondTestResult", e.target.value)}
        />
      </Row>
      <Row
        label={intl.formatMessage({
          id: "sample.entry.project.finalResultOfSite",
          defaultMessage: "Résultat Final du Site",
        })}
      >
        <TextInput
          id="ind_finalResult"
          hideLabel
          labelText=""
          size="sm"
          style={{ maxWidth: "280px" }}
          value={form.observations.indSiteFinalResult}
          onChange={(e) => setObs("indSiteFinalResult", e.target.value)}
        />
      </Row>
      <div style={S.subHeader}>
        <FormattedMessage
          id="sample.entry.project.title.specimen"
          defaultMessage="Échantillons"
        />
      </div>
      <div style={S.row}>
        <div style={S.label}>
          <FormattedMessage
            id="sample.entry.project.ARV.dryTubeTaken"
            defaultMessage="Tube Sec"
          />
        </div>
        <div style={S.inputWrap}>
          <Checkbox
            id="ind_dryTube"
            labelText=""
            checked={form.projectData.dryTubeTaken}
            onChange={(_, { checked }) => setPD("dryTubeTaken", checked)}
          />
        </div>
      </div>
      <div style={S.subHeader}>
        <FormattedMessage
          id="sample.entry.project.title.dryTube"
          defaultMessage="Tests Tube Sec"
        />
      </div>
      <div style={S.row}>
        <div style={S.label}>
          <FormattedMessage
            id="sample.entry.project.serologyHIVTest"
            defaultMessage="Sérologie VIH"
          />
        </div>
        <div style={S.inputWrap}>
          <Checkbox
            id="ind_serologyHIV"
            labelText=""
            checked={form.projectData.serologyHIVTest}
            onChange={(_, { checked }) => setPD("serologyHIVTest", checked)}
          />
        </div>
      </div>
      {fldUnderInvestigation()}
    </div>
  );

  // ─── Rendu: Requête Spéciale ──────────────────────────────────────────────────
  const renderSpecialRequest = () => (
    <div style={S.outerBox}>
      <div style={S.sectionHeader}>
        <FormattedMessage
          id="sample.entry.project.specialRequest.title"
          defaultMessage="Requête Spéciale"
        />
      </div>
      {fldInterviewDate()}
      {fldInterviewTime()}
      {fldReceivedDate()}
      {fldReceivedTime()}
      {fldSubjectNumber(7)}
      {fldSiteSubjectNumber(false, true)}
      {fldBirthDate()}
      {fldAge()}
      {fldGender()}
      {fldLabNo(LAB_PREFIXES.Special_Request_Id)}
      <Row
        label={intl.formatMessage({
          id: "sample.entry.project.specialRequest.reason",
          defaultMessage: "Motif de la demande",
        })}
      >
        <Select
          id="spe_reason"
          hideLabel
          labelText=""
          value={form.observations.reasonForRequest}
          onChange={(e) => setObs("reasonForRequest", e.target.value)}
          style={{ maxWidth: "280px" }}
        >
          <SelectItem value="" text={placeholder} />
          {(dictionaryLists.SPECIAL_REQUEST_REASONS || []).map((d) => (
            <SelectItem key={d.id} value={d.id} text={d.dictEntry} />
          ))}
        </Select>
      </Row>
      <div style={S.subHeader}>
        <FormattedMessage
          id="sample.entry.project.title.specimen"
          defaultMessage="Échantillons"
        />
      </div>
      {[
        ["dryTubeTaken", "Tube Sec", "sample.entry.project.ARV.dryTubeTaken"],
        [
          "edtaTubeTaken",
          "Tube EDTA",
          "sample.entry.project.ARV.edtaTubeTaken",
        ],
        ["dbsTaken", "DBS", "sample.entry.project.title.dryBloodSpot"],
      ].map(([field, def, id]) => (
        <div key={field} style={S.row}>
          <div style={S.label}>
            <FormattedMessage id={id} defaultMessage={def} />
          </div>
          <div style={S.inputWrap}>
            <Checkbox
              id={`spe_${field}`}
              labelText=""
              checked={form.projectData[field]}
              onChange={(_, { checked }) => setPD(field, checked)}
            />
          </div>
        </div>
      ))}
      <div style={S.subHeader}>
        <FormattedMessage
          id="sample.entry.project.title.dryTube"
          defaultMessage="Tests Tube Sec"
        />
      </div>
      {[
        ["murexTest", "Murex", "sample.entry.project.murexTest"],
        ["genscreenTest", "Genscreen", "sample.entry.project.genscreenTest"],
        [
          "vironostikaTest",
          "Vironostika",
          "sample.entry.project.vironostikaTest",
        ],
        ["innoliaTest", "Innolia", "sample.entry.project.innoliaTest"],
        ["glycemiaTest", "Glycémie", "sample.entry.project.ARV.glycemiaTest"],
        [
          "creatinineTest",
          "Créatinine",
          "sample.entry.project.ARV.creatinineTest",
        ],
        [
          "transaminaseTest",
          "Transaminases",
          "sample.entry.project.ARV.transaminaseTest",
        ],
        [
          "transaminaseALTLTest",
          "Transaminases ALTL",
          "sample.entry.project.transaminaseALTLTest",
        ],
        [
          "transaminaseASTLTest",
          "Transaminases ASTL",
          "sample.entry.project.transaminaseASTLTest",
        ],
      ].map(([field, def, id]) => (
        <div key={field} style={S.row}>
          <div style={S.label}>
            <FormattedMessage id={id} defaultMessage={def} />
          </div>
          <div style={S.inputWrap}>
            <Checkbox
              id={`spe_${field}`}
              labelText=""
              checked={form.projectData[field]}
              onChange={(_, { checked }) => setPD(field, checked)}
            />
          </div>
        </div>
      ))}
      <div style={S.subHeader}>
        <FormattedMessage
          id="sample.entry.project.title.edtaTube"
          defaultMessage="Tests Tube EDTA"
        />
      </div>
      {[
        ["nfsTest", "NFS", "sample.entry.project.ARV.nfsTest"],
        ["gbTest", "GB", "sample.entry.project.gbTest"],
        ["lymphTest", "Lymphocytes", "sample.entry.project.lymphTest"],
        ["monoTest", "Monocytes", "sample.entry.project.monoTest"],
        ["eoTest", "Éosinophiles", "sample.entry.project.eoTest"],
        ["basoTest", "Basophiles", "sample.entry.project.basoTest"],
        ["grTest", "GR", "sample.entry.project.grTest"],
        ["hbTest", "Hb", "sample.entry.project.hbTest"],
        ["hctTest", "Hct", "sample.entry.project.hctTest"],
        ["vgmTest", "VGM", "sample.entry.project.vgmTest"],
        ["tcmhTest", "TCMH", "sample.entry.project.tcmhTest"],
        ["ccmhTest", "CCMH", "sample.entry.project.ccmhTest"],
        ["plqTest", "Plaquettes", "sample.entry.project.plqTest"],
        ["cd4cd8Test", "CD4/CD8", "sample.entry.project.ARV.cd4cd8Test"],
        ["cd3CountTest", "CD3", "sample.entry.project.cd3CountTest"],
        ["cd4CountTest", "CD4", "sample.entry.project.cd4CountTest"],
      ].map(([field, def, id]) => (
        <div key={field} style={S.row}>
          <div style={S.label}>
            <FormattedMessage id={id} defaultMessage={def} />
          </div>
          <div style={S.inputWrap}>
            <Checkbox
              id={`spe_${field}`}
              labelText=""
              checked={form.projectData[field]}
              onChange={(_, { checked }) => setPD(field, checked)}
            />
          </div>
        </div>
      ))}
      <div style={S.subHeader}>
        <FormattedMessage
          id="sample.entry.project.title.otherTests"
          defaultMessage="Autres Tests"
        />
      </div>
      {[
        ["dnaPCR", "DNA PCR", "sample.entry.project.dnaPCR"],
        [
          "viralLoadTest",
          "Charge Virale",
          "sample.entry.project.ARV.viralLoadTest",
        ],
        [
          "genotypingTest",
          "Génotypage",
          "sample.entry.project.ARV.genotypingTest",
        ],
      ].map(([field, def, id]) => (
        <div key={field} style={S.row}>
          <div style={S.label}>
            <FormattedMessage id={id} defaultMessage={def} />
          </div>
          <div style={S.inputWrap}>
            <Checkbox
              id={`spe_${field}`}
              labelText=""
              checked={form.projectData[field]}
              onChange={(_, { checked }) => setPD(field, checked)}
            />
          </div>
        </div>
      ))}
      {fldUnderInvestigation()}
    </div>
  );

  // ─── Rendu: Charge Virale (VL) ────────────────────────────────────────────────
  const renderVL = () => (
    <div style={S.outerBox}>
      <div style={S.sectionHeader}>
        <FormattedMessage
          id="sample.entry.project.VL.title"
          defaultMessage="ARV - Charge Virale"
        />
      </div>
      {serologyControlEnabled && (
        <>
          <Row
            optional
            label={intl.formatMessage({
              id: "sample.entry.project.subjectNumber",
              defaultMessage: "Sujet No.",
            })}
          >
            <TextInput
              id="vl_search_subjectNumber"
              hideLabel
              labelText=""
              size="sm"
              maxLength={7}
              style={{ maxWidth: "160px" }}
              value={vlSearchSubjectNumber}
              onChange={(e) => setVlSearchSubjectNumber(e.target.value)}
            />
          </Row>
          <Row
            optional
            label={intl.formatMessage({
              id: "patient.site.subject.number",
              defaultMessage: "Site Sujet No.",
            })}
          >
            <TextInput
              id="vl_search_siteSubjectNumber"
              hideLabel
              labelText=""
              size="sm"
              maxLength={17}
              placeholder="00000/AA/AA/00000"
              style={{ maxWidth: "220px" }}
              value={vlSearchSiteSubjectNumber}
              onChange={(e) => {
                const raw = e.target.value.replace(/\//g, "").toUpperCase();
                let validated = "";
                for (let i = 0; i < raw.length && i < 14; i++) {
                  if (i < 5 || i >= 9) {
                    if (/\d/.test(raw[i])) validated += raw[i];
                  } else {
                    if (/[A-Z0-9]/.test(raw[i])) validated += raw[i];
                  }
                }
                let formatted = validated;
                if (validated.length > 9) {
                  formatted =
                    validated.slice(0, 5) +
                    "/" +
                    validated.slice(5, 7) +
                    "/" +
                    validated.slice(7, 9) +
                    "/" +
                    validated.slice(9);
                } else if (validated.length > 7) {
                  formatted =
                    validated.slice(0, 5) +
                    "/" +
                    validated.slice(5, 7) +
                    "/" +
                    validated.slice(7);
                } else if (validated.length > 5) {
                  formatted = validated.slice(0, 5) + "/" + validated.slice(5);
                }
                setVlSearchSiteSubjectNumber(formatted);
              }}
            />
          </Row>
          <Row label="">
            <div style={{ display: "flex", alignItems: "center", gap: "12px" }}>
              <Button size="sm" kind="tertiary" onClick={searchVLPatient}>
                <FormattedMessage
                  id="label.button.search"
                  defaultMessage="Rechercher"
                />
              </Button>
              {patientLookupStatus === "searching" && (
                <span
                  style={{
                    fontSize: "12px",
                    color: "#6f6f6f",
                    fontStyle: "italic",
                  }}
                >
                  <FormattedMessage
                    id="label.searching"
                    defaultMessage="Recherche…"
                  />
                </span>
              )}
              {patientLookupStatus === "found" && (
                <span
                  style={{
                    display: "flex",
                    alignItems: "center",
                    gap: "4px",
                    color: "#24a148",
                    fontSize: "12px",
                    fontWeight: "600",
                  }}
                >
                  <CheckmarkFilled size={16} />
                  <FormattedMessage
                    id="sample.entry.project.patientSearch.found"
                    defaultMessage="Patient trouvé"
                  />
                </span>
              )}
              {patientLookupStatus === "notfound" && (
                <span
                  style={{
                    fontSize: "12px",
                    color: "#da1e28",
                    fontWeight: "600",
                  }}
                >
                  <FormattedMessage
                    id="sample.entry.project.patientSearch.notFound"
                    defaultMessage="Patient non trouvé"
                  />
                </span>
              )}
            </div>
          </Row>
        </>
      )}
      {(!serologyControlEnabled || vlPatientSearchDone) && (
        <>
          {fldARVCenter()}
          {fldNameOfDoctor()}
          {fldNameOfSampler()}
          {fldInterviewDate()}
          {fldInterviewTime()}
          {fldReceivedDate()}
          {fldReceivedTime()}
          {fldSubjectNumber(7)}
          {fldSiteSubjectNumber(false, false)}
          {fldLabNo(LAB_PREFIXES.VL_Id)}
          {fldBirthDate()}
          {fldAge(true, false)}
          {fldGender()}
          {isFemale && (
            <>
              <Row
                label={intl.formatMessage({
                  id: "sample.project.vlPregnancy",
                  defaultMessage: "Grossesse en cours",
                })}
              >
                <Select
                  id="vl_pregnancy"
                  hideLabel
                  labelText=""
                  value={form.observations.vlPregnancy}
                  onChange={(e) => setObs("vlPregnancy", e.target.value)}
                  style={{ maxWidth: "200px" }}
                >
                  <SelectItem value="" text={placeholder} />
                  {(dictionaryLists.YES_NO || []).map((d) => (
                    <SelectItem
                      key={d.id}
                      value={d.id}
                      text={intl.formatMessage({
                        id: d.displayKey,
                        defaultMessage: d.value,
                      })}
                    />
                  ))}
                </Select>
              </Row>
              <Row
                label={intl.formatMessage({
                  id: "sample.project.vlSuckle",
                  defaultMessage: "Allaitement en cours",
                })}
              >
                <Select
                  id="vl_suckle"
                  hideLabel
                  labelText=""
                  value={form.observations.vlSuckle}
                  onChange={(e) => setObs("vlSuckle", e.target.value)}
                  style={{ maxWidth: "200px" }}
                >
                  <SelectItem value="" text={placeholder} />
                  {(dictionaryLists.YES_NO || []).map((d) => (
                    <SelectItem
                      key={d.id}
                      value={d.id}
                      text={intl.formatMessage({
                        id: d.displayKey,
                        defaultMessage: d.value,
                      })}
                    />
                  ))}
                </Select>
              </Row>
            </>
          )}
          {serologyControlEnabled ? (
            <Row
              required
              label={intl.formatMessage({
                id: "patient.project.hivType",
                defaultMessage: "Type VIH",
              })}
            >
              <div
                style={{ display: "flex", alignItems: "center", gap: "8px" }}
              >
                <Select
                  id="f_hivStatus"
                  hideLabel
                  labelText=""
                  disabled={serologyStatus === "found"}
                  value={form.observations.hivStatus}
                  onChange={(e) => setObs("hivStatus", e.target.value)}
                  style={{ maxWidth: "240px" }}
                >
                  <SelectItem value="" text={placeholder} />
                  {(dictionaryLists.HIV_TYPES || []).map((d) => (
                    <SelectItem
                      key={d.id}
                      value={d.id}
                      text={intl.formatMessage({
                        id: d.displayKey,
                        defaultMessage: d.value,
                      })}
                    />
                  ))}
                </Select>
                {serologyStatus === "searching" && (
                  <span
                    style={{
                      fontSize: "12px",
                      color: "#6f6f6f",
                      fontStyle: "italic",
                    }}
                  >
                    <FormattedMessage
                      id="label.searching"
                      defaultMessage="Recherche sérologie…"
                    />
                  </span>
                )}
                {serologyStatus === "found" && (
                  <span
                    style={{
                      fontSize: "12px",
                      color: "#24a148",
                      fontWeight: "600",
                    }}
                  >
                    <FormattedMessage
                      id="sample.entry.project.serology.found"
                      defaultMessage="Sérologie existante détectée"
                    />
                  </span>
                )}
                {serologyStatus === "notfound" && (
                  <span
                    style={{
                      fontSize: "12px",
                      color: "#da1e28",
                      fontWeight: "600",
                    }}
                  >
                    <FormattedMessage
                      id="sample.entry.project.serology.notfound"
                      defaultMessage="Aucune sérologie existante : test sérologie requis"
                    />
                  </span>
                )}
              </div>
            </Row>
          ) : (
            fldHivStatus("HIV_TYPES", true)
          )}
          <Row
            label={intl.formatMessage({
              id: "sample.entry.project.arv.treatment",
              defaultMessage: "Sous traitement ARV ?",
            })}
          >
            <Select
              id="vl_arvTreatment"
              hideLabel
              labelText=""
              value={form.observations.currentARVTreatment}
              onChange={(e) => setObs("currentARVTreatment", e.target.value)}
              style={{ maxWidth: "200px" }}
            >
              <SelectItem value="" text={placeholder} />
              {(dictionaryLists.YES_NO || []).map((d) => (
                <SelectItem
                  key={d.id}
                  value={d.id}
                  text={intl.formatMessage({
                    id: d.displayKey,
                    defaultMessage: d.value,
                  })}
                />
              ))}
            </Select>
          </Row>
          {isOnARV && (
            <>
              <Row
                label={intl.formatMessage({
                  id: "sample.entry.project.arv.treatment.initDate",
                  defaultMessage: "Date début traitement ARV",
                })}
              >
                <CustomDatePicker
                  id="vl_arvInitDate"
                  labelText=""
                  value={form.observations.arvTreatmentInitDate}
                  disallowFutureDate
                  onChange={(d) => setObs("arvTreatmentInitDate", d)}
                />
              </Row>
              <Row
                label={intl.formatMessage({
                  id: "sample.entry.project.arv.treatment.therap.line",
                  defaultMessage: "Ligne thérapeutique",
                })}
              >
                <Select
                  id="vl_arvRegime"
                  hideLabel
                  labelText=""
                  value={form.observations.arvTreatmentRegime}
                  onChange={(e) => setObs("arvTreatmentRegime", e.target.value)}
                  style={{ maxWidth: "240px" }}
                >
                  <SelectItem value="" text={placeholder} />
                  {(dictionaryLists.ARV_REGIME || []).map((d) => (
                    <SelectItem
                      key={d.id}
                      value={d.id}
                      text={intl.formatMessage({
                        id: d.displayKey,
                        defaultMessage: d.value,
                      })}
                    />
                  ))}
                </Select>
              </Row>
              <Row
                label={intl.formatMessage({
                  id: "sample.entry.project.arv.treatment.therap.regime",
                  defaultMessage: "Régime thérapeutique",
                })}
              >
                <div
                  style={{
                    maxWidth: "500px",
                    display: "flex",
                    gap: "8px",
                    alignItems: "center",
                  }}
                >
                  <div
                    style={{
                      flex: 1,
                      opacity: innManualMode ? 0.4 : 1,
                      pointerEvents: innManualMode ? "none" : "auto",
                    }}
                  >
                    <AutoComplete
                      id="vl_arvRegimenList"
                      allowFreeText={false}
                      suggestions={(dictionaryLists.ARV_REGIMEN_LIST || []).map(
                        (d) => ({
                          id: d.value,
                          value: d.value,
                        }),
                      )}
                      onSelect={(val) => {
                        const parts = (val || "").split(" ").filter(Boolean);
                        setForm((prev) => {
                          const list = ["", "", "", ""];
                          parts.forEach((p, i) => {
                            if (i < 4) list[i] = p;
                          });
                          return {
                            ...prev,
                            observations: {
                              ...prev.observations,
                              currentARVTreatmentINNsList: list,
                            },
                          };
                        });
                      }}
                    />
                  </div>
                  <Checkbox
                    id="vl_inn_manual_mode"
                    labelText={intl.formatMessage({
                      id: "sample.entry.project.arv.inn.other",
                      defaultMessage: "Autre",
                    })}
                    checked={innManualMode}
                    onChange={(_, { checked }) => {
                      setInnManualMode(checked);
                      if (!checked) {
                        setForm((prev) => ({
                          ...prev,
                          observations: {
                            ...prev.observations,
                            currentARVTreatmentINNsList: ["", "", "", ""],
                          },
                        }));
                      }
                    }}
                  />
                </div>
              </Row>
              {[0, 1, 2, 3].map((i) => (
                <Row key={i} label={`ARV INN ${i + 1}`}>
                  <TextInput
                    id={`vl_inn_${i}`}
                    hideLabel
                    labelText=""
                    size="sm"
                    disabled={!innManualMode}
                    style={{ maxWidth: "200px" }}
                    value={
                      form.observations.currentARVTreatmentINNsList[i] || ""
                    }
                    onChange={(e) => setINN(i, e.target.value)}
                  />
                </Row>
              ))}
            </>
          )}
          <Row
            label={intl.formatMessage({
              id: "sample.entry.project.vl.reason",
              defaultMessage: "Motif de la demande CV",
            })}
          >
            <Select
              id="vl_reason"
              hideLabel
              labelText=""
              value={form.observations.vlReasonForRequest}
              onChange={(e) => setObs("vlReasonForRequest", e.target.value)}
              style={{ maxWidth: "280px" }}
            >
              <SelectItem value="" text={placeholder} />
              {(dictionaryLists.ARV_REASON_FOR_VL_DEMAND || []).map((d) => (
                <SelectItem
                  key={d.displayKey}
                  value={d.id}
                  text={intl.formatMessage({
                    id: d.displayKey,
                    defaultMessage: d.value,
                  })}
                />
              ))}
            </Select>
          </Row>
          {vlReasonIsOther && (
            <Row
              label={intl.formatMessage({
                id: "sample.entry.project.vl.specify",
                defaultMessage: "Autre motif (préciser)",
              })}
            >
              <TextInput
                id="vl_otherReason"
                hideLabel
                labelText=""
                size="sm"
                style={{ maxWidth: "360px" }}
                value={form.observations.vlOtherReasonForRequest}
                onChange={(e) =>
                  setObs("vlOtherReasonForRequest", e.target.value)
                }
              />
            </Row>
          )}
          <div style={S.subHeader}>
            <FormattedMessage
              id="sample.project.cd4init"
              defaultMessage="A l'initiation du traitement"
            />
          </div>
          <Row
            label={intl.formatMessage({
              id: "sample.project.cd4Count",
              defaultMessage: "Nombre de CD4",
            })}
          >
            <TextInput
              id="vl_initCD4"
              hideLabel
              labelText=""
              size="sm"
              style={{ maxWidth: "120px" }}
              value={form.observations.initcd4Count}
              onChange={(e) =>
                setObs("initcd4Count", e.target.value.replace(/[^0-9,]/g, ""))
              }
            />
          </Row>
          <Row
            label={intl.formatMessage({
              id: "sample.project.cd4Percent",
              defaultMessage: "Pourcentage CD4",
            })}
          >
            <TextInput
              id="vl_initCD4pct"
              hideLabel
              labelText=""
              size="sm"
              style={{ maxWidth: "120px" }}
              value={form.observations.initcd4Percent}
              onChange={(e) =>
                setObs("initcd4Percent", e.target.value.replace(/[^0-9,]/g, ""))
              }
            />
          </Row>
          <Row
            label={intl.formatMessage({
              id: "sample.project.Cd4Date",
              defaultMessage: "Date",
            })}
          >
            <CustomDatePicker
              id="vl_initCD4Date"
              labelText=""
              value={form.observations.initcd4Date}
              disallowFutureDate
              onChange={(d) => setObs("initcd4Date", d)}
            />
          </Row>
          <div style={S.subHeader}>
            <FormattedMessage
              id="sample.project.cd4demand"
              defaultMessage="A la demande de charge virale"
            />
          </div>
          <Row
            label={intl.formatMessage({
              id: "sample.project.cd4Count",
              defaultMessage: "Nombre de CD4",
            })}
          >
            <TextInput
              id="vl_demCD4"
              hideLabel
              labelText=""
              size="sm"
              style={{ maxWidth: "120px" }}
              value={form.observations.demandcd4Count}
              onChange={(e) =>
                setObs("demandcd4Count", e.target.value.replace(/[^0-9,]/g, ""))
              }
            />
          </Row>
          <Row
            label={intl.formatMessage({
              id: "sample.project.cd4Percent",
              defaultMessage: "Pourcentage CD4",
            })}
          >
            <TextInput
              id="vl_demCD4pct"
              hideLabel
              labelText=""
              size="sm"
              style={{ maxWidth: "120px" }}
              value={form.observations.demandcd4Percent}
              onChange={(e) =>
                setObs(
                  "demandcd4Percent",
                  e.target.value.replace(/[^0-9,]/g, ""),
                )
              }
            />
          </Row>
          <Row
            label={intl.formatMessage({
              id: "sample.project.Cd4Date",
              defaultMessage: "Date",
            })}
          >
            <CustomDatePicker
              id="vl_demCD4Date"
              labelText=""
              value={form.observations.demandcd4Date}
              disallowFutureDate
              onChange={(d) => setObs("demandcd4Date", d)}
            />
          </Row>
          <Row
            label={intl.formatMessage({
              id: "sample.project.priorVLRequest",
              defaultMessage: "CV antérieure ?",
            })}
          >
            <Select
              id="vl_benefit"
              hideLabel
              labelText=""
              value={form.observations.vlBenefit}
              onChange={(e) => setObs("vlBenefit", e.target.value)}
              style={{ maxWidth: "200px" }}
            >
              <SelectItem value="" text={placeholder} />
              {(dictionaryLists.YES_NO || []).map((d) => (
                <SelectItem
                  key={d.id}
                  value={d.id}
                  text={intl.formatMessage({
                    id: d.displayKey,
                    defaultMessage: d.value,
                  })}
                />
              ))}
            </Select>
          </Row>
          {hasHadVL && (
            <>
              <Row
                label={intl.formatMessage({
                  id: "sample.project.priorVLLab",
                  defaultMessage: "Laboratoire CV antérieure",
                })}
              >
                <AutoComplete
                  id="vl_priorLab"
                  name="vl_priorLab"
                  label=""
                  value={form.observations.priorVLLab}
                  suggestions={dictionaryLists.REFERRING_SITES.map((o) => ({
                    id: o.id,
                    value: o.organizationName || o.value,
                  }))}
                  onChange={(e) => {
                    const val =
                      e?.currentTarget?.value ?? e?.target?.value ?? "";
                    setObs("priorVLLab", val);
                  }}
                  onSelect={(id) => {
                    const org = dictionaryLists.REFERRING_SITES.find(
                      (o) => o.id === id,
                    );
                    if (org) {
                      setObs("priorVLLab", org.organizationName || org.value);
                    }
                  }}
                />
              </Row>
              <Row
                label={intl.formatMessage({
                  id: "sample.project.VLValue",
                  defaultMessage: "Valeur CV antérieure",
                })}
              >
                <TextInput
                  id="vl_priorVal"
                  hideLabel
                  labelText=""
                  size="sm"
                  style={{ maxWidth: "120px" }}
                  value={form.observations.priorVLValue}
                  onChange={(e) => setObs("priorVLValue", e.target.value)}
                />
              </Row>
              <Row
                label={intl.formatMessage({
                  id: "sample.project.VLDate",
                  defaultMessage: "Date CV antérieure",
                })}
              >
                <CustomDatePicker
                  id="vl_priorDate"
                  labelText=""
                  value={form.observations.priorVLDate}
                  disallowFutureDate
                  onChange={(d) => setObs("priorVLDate", d)}
                />
              </Row>
            </>
          )}
          {fldUnderInvestigation()}
          <div style={S.subHeader}>
            <FormattedMessage
              id="sample.entry.project.title.specimen"
              defaultMessage="Échantillons"
            />
          </div>
          {serologyControlEnabled && serologyStatus === "notfound" && (
            <>
              <div style={S.row}>
                <div style={S.label}>
                  <FormattedMessage
                    id="sample.entry.project.ARV.dryTubeTaken"
                    defaultMessage="Tube Sec"
                  />
                </div>
                <div style={S.inputWrap}>
                  <Checkbox
                    id="vl_dryTubeTaken"
                    labelText=""
                    checked={form.projectData.dryTubeTaken}
                    onChange={(_, { checked }) =>
                      setPD("dryTubeTaken", checked)
                    }
                  />
                </div>
              </div>
              <div style={S.row}>
                <div style={S.label}>
                  <span style={S.asterisk}>*</span>
                  <FormattedMessage
                    id="sample.entry.project.serologyHIVTest"
                    defaultMessage="Sérologie VIH"
                  />
                </div>
                <div style={S.inputWrap}>
                  <Checkbox
                    id="vl_serologyHIVTest"
                    labelText=""
                    checked={form.projectData.serologyHIVTest}
                    onChange={(_, { checked }) =>
                      setPD("serologyHIVTest", checked)
                    }
                  />
                </div>
              </div>
            </>
          )}
          <div style={S.row}>
            <div style={S.label}>
              <FormattedMessage
                id="sample.entry.project.ARV.edtaTubeTaken"
                defaultMessage="Tube EDTA"
              />
            </div>
            <div style={S.inputWrap}>
              <Checkbox
                id="vl_edta"
                labelText=""
                checked={form.projectData.edtaTubeTaken}
                onChange={(_, { checked }) => setPD("edtaTubeTaken", checked)}
              />
            </div>
          </div>
          <div style={S.row}>
            <div style={S.label}>
              <FormattedMessage
                id="sample.entry.project.title.dryBloodSpot"
                defaultMessage="DBS"
              />
            </div>
            <div style={S.inputWrap}>
              <Checkbox
                id="vl_dbs"
                labelText=""
                checked={form.projectData.dbsvlTaken}
                onChange={(_, { checked }) => setPD("dbsvlTaken", checked)}
              />
            </div>
          </div>
          <div style={S.row}>
            <div style={S.label}>
              <FormattedMessage
                id="sample.entry.project.title.psc"
                defaultMessage="PSC"
              />
            </div>
            <div style={S.inputWrap}>
              <Checkbox
                id="vl_psc"
                labelText=""
                checked={form.projectData.pscvlTaken}
                onChange={(_, { checked }) => setPD("pscvlTaken", checked)}
              />
            </div>
          </div>
          <div style={S.subHeader}>
            <FormattedMessage
              id="sample.entry.project.title.tests"
              defaultMessage="Tests"
            />
          </div>
          <div style={S.row}>
            <div style={S.label}>
              <FormattedMessage
                id="sample.entry.project.ARV.viralLoadTest"
                defaultMessage="Charge Virale Plasmatique"
              />
            </div>
            <div style={S.inputWrap}>
              <Checkbox
                id="vl_vlTest"
                labelText=""
                checked={form.projectData.viralLoadTest}
              />
            </div>
          </div>
        </>
      )}
    </div>
  );

  // ─── Rendu: Recency ───────────────────────────────────────────────────────────
  const renderRecency = () => (
    <div style={S.outerBox}>
      <div style={S.sectionHeader}>
        <FormattedMessage
          id="sample.entry.project.RT.title"
          defaultMessage="Recency"
        />
      </div>
      <div style={S.subHeader}>
        <FormattedMessage
          id="sample.entry.project.title.org"
          defaultMessage="Organisation"
        />
      </div>
      {fldARVCenter()}
      <div style={S.subHeader}>
        <FormattedMessage
          id="sample.entry.project.title.patientInfo"
          defaultMessage="Informations Patient"
        />
      </div>
      {fldLabNo(LAB_PREFIXES.Recency_Id)}
      <Row
        required
        label={intl.formatMessage({
          id: "sample.entry.project.recencyNumber",
          defaultMessage: "Numéro Recency",
        })}
      >
        <TextInput
          id="rt_siteSubject"
          hideLabel
          labelText=""
          size="sm"
          maxLength={18}
          style={{ maxWidth: "220px" }}
          value={form.siteSubjectNumber}
          onChange={(e) => set("siteSubjectNumber", e.target.value)}
        />
      </Row>
      {fldBirthDate()}
      {fldAge()}
      {fldGender()}
      {isFemale && (
        <>
          <Row
            label={intl.formatMessage({
              id: "sample.project.vlPregnancy",
              defaultMessage: "Grossesse en cours",
            })}
          >
            <Select
              id="rt_pregnancy"
              hideLabel
              labelText=""
              value={form.observations.vlPregnancy}
              onChange={(e) => setObs("vlPregnancy", e.target.value)}
              style={{ maxWidth: "200px" }}
            >
              <SelectItem value="" text={placeholder} />
              {(dictionaryLists.YES_NO || []).map((d) => (
                <SelectItem key={d.id} value={d.id} text={d.dictEntry} />
              ))}
            </Select>
          </Row>
          <Row
            label={intl.formatMessage({
              id: "sample.project.vlSuckle",
              defaultMessage: "Allaitement en cours",
            })}
          >
            <Select
              id="rt_suckle"
              hideLabel
              labelText=""
              value={form.observations.vlSuckle}
              onChange={(e) => setObs("vlSuckle", e.target.value)}
              style={{ maxWidth: "200px" }}
            >
              <SelectItem value="" text={placeholder} />
              {(dictionaryLists.YES_NO || []).map((d) => (
                <SelectItem key={d.id} value={d.id} text={d.dictEntry} />
              ))}
            </Select>
          </Row>
        </>
      )}
      <div style={S.subHeader}>
        <FormattedMessage
          id="sample.entry.project.title.sample"
          defaultMessage="Collecte"
        />
      </div>
      {fldNameOfDoctor()}
      {fldNameOfSampler()}
      {fldReceivedDate()}
      {fldReceivedTime()}
      {fldInterviewDate()}
      {fldInterviewTime()}
      <div style={S.subHeader}>
        <FormattedMessage
          id="sample.entry.project.title.sampleType"
          defaultMessage="Type d'Échantillon"
        />
      </div>
      <div style={S.row}>
        <div style={S.label}>
          <FormattedMessage
            id="sample.entry.project.recency.plasma"
            defaultMessage="Plasma"
          />
        </div>
        <div style={S.inputWrap}>
          <Checkbox
            id="rt_plasma"
            labelText=""
            checked={form.projectData.plasmaTaken}
            onChange={(_, { checked }) => setPD("plasmaTaken", checked)}
          />
        </div>
      </div>
      <div style={S.row}>
        <div style={S.label}>
          <FormattedMessage
            id="sample.entry.project.recency.serum"
            defaultMessage="Sérum"
          />
        </div>
        <div style={S.inputWrap}>
          <Checkbox
            id="rt_serum"
            labelText=""
            checked={form.projectData.serumTaken}
            onChange={(_, { checked }) => setPD("serumTaken", checked)}
          />
        </div>
      </div>
      <div style={S.subHeader}>
        <FormattedMessage
          id="sample.entry.project.title.tests"
          defaultMessage="Tests"
        />
      </div>
      <div style={S.row}>
        <div style={S.label}>
          <FormattedMessage
            id="sample.entry.project.recency.asanteKit"
            defaultMessage="Asante Kit"
          />
        </div>
        <div style={S.inputWrap}>
          <Checkbox
            id="rt_asante"
            labelText=""
            checked={form.projectData.asanteTest}
            onChange={(_, { checked }) => setPD("asanteTest", checked)}
          />
        </div>
      </div>
    </div>
  );

  // ─── Rendu: HPV ───────────────────────────────────────────────────────────────
  const renderHPV = () => (
    <div style={S.outerBox}>
      <div style={S.sectionHeader}>
        <FormattedMessage
          id="sample.entry.project.HPV.title"
          defaultMessage="HPV Testing"
        />
      </div>
      <div style={S.subHeader}>
        <FormattedMessage
          id="sample.entry.project.title.org"
          defaultMessage="Organisation"
        />
      </div>
      {fldARVCenter()}
      <div style={S.subHeader}>
        <FormattedMessage
          id="sample.entry.project.title.patientInfo"
          defaultMessage="Informations Patient"
        />
      </div>
      {fldLabNoHPV()}
      <Row
        required
        label={intl.formatMessage({
          id: "sample.entry.project.hpvSubjectNumber",
          defaultMessage: "Numéro Sujet HPV",
        })}
      >
        <TextInput
          id="hpv_siteSubject"
          hideLabel
          labelText=""
          size="sm"
          maxLength={18}
          style={{ maxWidth: "220px" }}
          value={form.siteSubjectNumber}
          onChange={(e) => set("siteSubjectNumber", e.target.value)}
        />
      </Row>
      {fldHivStatus("HIV_STATUSES")}
      {fldBirthDate()}
      {fldAge()}
      <div style={S.subHeader}>
        <FormattedMessage
          id="sample.entry.project.title.sample"
          defaultMessage="Collecte"
        />
      </div>
      {fldNameOfDoctor()}
      {fldReceivedDate()}
      {fldReceivedTime()}
      {fldInterviewDate()}
      {fldInterviewTime()}
      {dictionaryLists.HPV_SAMPLING_METHOD?.length > 0 && (
        <Row
          label={intl.formatMessage({
            id: "sample.entry.project.title.sampleType",
            defaultMessage: "Type de Prélèvement",
          })}
        >
          <RadioButtonGroup
            name="hpv_samplingMethod"
            valueSelected={form.observations.hpvSamplingMethod}
            onChange={(val) => setObs("hpvSamplingMethod", val)}
          >
            {dictionaryLists.HPV_SAMPLING_METHOD.map((d) => (
              <RadioButton
                key={d.id}
                value={d.id}
                labelText={intl.formatMessage({
                  id: d.displayKey,
                  defaultMessage: d.value,
                })}
                id={`hpv_sm_${d.id}`}
              />
            ))}
          </RadioButtonGroup>
        </Row>
      )}
      <div style={S.subHeader}>
        <FormattedMessage
          id="sample.entry.project.title.specimen"
          defaultMessage="Échantillons"
        />
      </div>
      <div style={S.row}>
        <div style={S.label}>
          <FormattedMessage
            id="sample.entry.project.HPV.preservCytTaken"
            defaultMessage="Cytologie en préservation"
          />
        </div>
        <div style={S.inputWrap}>
          <Checkbox
            id="hpv_preservCyt"
            labelText=""
            checked={form.projectData.preservCytTaken}
            onChange={(_, { checked }) => setPD("preservCytTaken", checked)}
          />
        </div>
      </div>
      <div style={S.subHeader}>
        <FormattedMessage
          id="sample.entry.project.title.tests"
          defaultMessage="Tests"
        />
      </div>
      <div style={S.row}>
        <div style={S.label}>
          <FormattedMessage
            id="sample.entry.project.hpv.hpvKit"
            defaultMessage="HPV Kit"
          />
        </div>
        <div style={S.inputWrap}>
          <Checkbox
            id="hpv_kit"
            labelText=""
            checked={form.projectData.hpvTest}
            onChange={(_, { checked }) => setPD("hpvTest", checked)}
          />
        </div>
      </div>
      <div style={S.row}>
        <div style={S.label}>
          <FormattedMessage
            id="sample.entry.project.hpv.abottOrRocheAnalysis"
            defaultMessage="Analyse Abbott/Roche"
          />
        </div>
        <div style={S.inputWrap}>
          <Checkbox
            id="hpv_abbottRoche"
            labelText=""
            checked={form.projectData.abbottOrRocheAnalysis}
            onChange={(_, { checked }) =>
              setPD("abbottOrRocheAnalysis", checked)
            }
          />
        </div>
      </div>
      <div style={S.row}>
        <div style={S.label}>
          <FormattedMessage
            id="sample.entry.project.hpv.geneXpertAnalysis"
            defaultMessage="Analyse GeneXpert"
          />
        </div>
        <div style={S.inputWrap}>
          <Checkbox
            id="hpv_geneXpert"
            labelText=""
            checked={form.projectData.geneXpertAnalysis}
            onChange={(_, { checked }) => setPD("geneXpertAnalysis", checked)}
          />
        </div>
      </div>
    </div>
  );

  // ─── Écran de succès ─────────────────────────────────────────────────────────
  const renderSuccessView = () => (
    <div className="orderLegendBody">
      <div className="orderEntrySuccessMsg">
        <img
          src="images/success-icon.png"
          alt="Enregistrement réussi"
          width="120"
          height="120"
        />
        <h4>
          <FormattedMessage
            id="save.success"
            defaultMessage="Enregistrement réussi"
          />
        </h4>
        {savedLabNo && (
          <div style={{ marginTop: "12px" }}>
            <Button
              onClick={() =>
                window.open(
                  `${config.serverBaseUrl}/LabelMakerServlet?labNo=${savedLabNo}`,
                )
              }
            >
              <FormattedMessage
                id="print.barcode"
                defaultMessage="Imprimer le code-barres"
              />
            </Button>
          </div>
        )}
        <div style={{ marginTop: "12px" }}>
          <Button kind="tertiary" onClick={() => setShowSuccess(false)}>
            <FormattedMessage
              id="label.newentry"
              defaultMessage="Nouvelle saisie"
            />
          </Button>
        </div>
      </div>
    </div>
  );

  // ─── Boutons ──────────────────────────────────────────────────────────────────
  const renderButtons = () => (
    <div
      style={{
        display: "flex",
        gap: "8px",
        padding: "12px 10px",
        justifyContent: "flex-end",
        borderTop: "1px solid #e0e0e0",
      }}
    >
      <Button
        onClick={handleSubmit}
        disabled={submitting || !form.project}
        size="sm"
      >
        {submitting ? (
          <FormattedMessage
            id="label.saving"
            defaultMessage="Enregistrement..."
          />
        ) : (
          <FormattedMessage
            id="label.button.save"
            defaultMessage="Sauvegarder"
          />
        )}
      </Button>
      <Button
        kind="secondary"
        onClick={handleReset}
        disabled={submitting}
        size="sm"
      >
        <FormattedMessage id="label.button.cancel" defaultMessage="Annuler" />
      </Button>
    </div>
  );

  // ─── Rendu du formulaire actif ────────────────────────────────────────────────
  const renderActiveForm = () => {
    switch (form.project) {
      case "InitialARV_Id":
        return renderInitialARV();
      case "FollowUpARV_Id":
        return renderFollowUpARV();
      case "RTN_Id":
        return renderRTN();
      case "EID_Id":
        return renderEID();
      case "Indeterminate_Id":
        return renderIndeterminate();
      case "Special_Request_Id":
        return renderSpecialRequest();
      case "VL_Id":
        return renderVL();
      case "Recency_Id":
        return renderRecency();
      case "HPV_Id":
        return renderHPV();
      default:
        return null;
    }
  };

  if (loading) return <Loading description="Chargement du formulaire..." />;

  return (
    <div style={S.page}>
      {notificationVisible && <AlertDialog />}
      <PageBreadCrumb breadcrumbs={breadcrumbs} />

      {/* ── Sélection de l'étude ───────────────────────────────────────────── */}
      {!showSuccess && (
        <div
          style={{
            ...S.row,
            background: "#f4f4f4",
            marginBottom: "4px",
            position: "sticky",
            top: 0,
            zIndex: 100,
            boxShadow: "0 2px 6px rgba(0,0,0,0.12)",
          }}
        >
          <div style={{ ...S.label }}>
            <div
              style={{ fontSize: "11px", color: "#888", marginBottom: "2px" }}
            >
              <FormattedMessage
                id="sample.entry.project.form"
                defaultMessage="Formulaire d'étude"
              />
            </div>
            {form.project ? (
              <div
                style={{
                  fontSize: "15px",
                  fontWeight: "800",
                  color: "#295785",
                  letterSpacing: "0.01em",
                }}
              >
                {studyForms.find((s) => s.value === form.project)?.label}
              </div>
            ) : (
              <div
                style={{ fontSize: "13px", color: "#aaa", fontStyle: "italic" }}
              >
                — non sélectionné —
              </div>
            )}
          </div>
          <div
            style={{
              ...S.inputWrap,
              display: "flex",
              alignItems: "center",
              gap: "12px",
              flexWrap: "wrap",
            }}
          >
            <Select
              id="studyForms"
              hideLabel
              labelText=""
              value={form.project}
              onChange={(e) => handleStudyChange(e.target.value)}
              style={{ maxWidth: "320px" }}
            >
              <SelectItem
                value=""
                text={intl.formatMessage({
                  id: "label.select",
                  defaultMessage: "-- Sélectionner --",
                })}
              />
              {studyForms.map((s) => (
                <SelectItem key={s.value} value={s.value} text={s.label} />
              ))}
            </Select>
            {form.project &&
              (() => {
                const selected = studyForms.find(
                  (s) => s.value === form.project,
                );
                return selected ? (
                  <div
                    style={{
                      display: "inline-flex",
                      alignItems: "center",
                      gap: "6px",
                      background: "#0f62fe",
                      color: "#fff",
                      padding: "4px 12px",
                      borderRadius: "16px",
                      fontWeight: "bold",
                      fontSize: "13px",
                      boxShadow: "0 2px 6px rgba(15,98,254,0.4)",
                      letterSpacing: "0.02em",
                    }}
                  >
                    <CheckmarkFilled size={16} />
                    {selected.label}
                  </div>
                ) : null;
              })()}
          </div>
        </div>
      )}

      {/* ── Succès ou formulaire actif ───────────────────────────────────────── */}
      {showSuccess ? (
        renderSuccessView()
      ) : (
        <>
          {renderActiveForm()}
          {form.project && renderButtons()}
        </>
      )}
    </div>
  );
};

export default ViralLoadEntry;

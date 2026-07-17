import { useContext, useState } from "react";
import { FormattedMessage, useIntl } from "react-intl";
import { Button, Select, SelectItem, TextInput, Loading } from "@carbon/react";
import PageBreadCrumb from "../common/PageBreadCrumb";
import { AlertDialog } from "../common/CustomNotification";
import { NotificationContext } from "../layout/Layout";
import { getFromOpenElisServer } from "../utils/Utils";
import ViralLoadEntry from "./ViralLoadEntry";

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
  outerBox: { border: "1px solid #c6c6c6", marginBottom: "1rem" },
  searchRow: {
    display: "flex",
    alignItems: "flex-end",
    gap: "12px",
    padding: "12px 10px",
    background: "#fff",
    flexWrap: "wrap",
  },
  table: { width: "100%", borderCollapse: "collapse" },
  th: {
    background: "#e0e0e0",
    padding: "8px 10px",
    textAlign: "left",
    fontWeight: "bold",
    fontSize: "13px",
    borderBottom: "2px solid #c6c6c6",
  },
  td: {
    padding: "7px 10px",
    fontSize: "13px",
    borderBottom: "1px solid #e0e0e0",
    background: "#fff",
  },
  trHover: { cursor: "pointer" },
  noResult: { padding: "12px 10px", color: "#555", fontStyle: "italic" },
  pageTitle: {
    margin: "1rem 0 0.5rem",
    color: "#295785",
    fontSize: "18px",
    fontWeight: "bold",
  },
};

const breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "banner.menu.patient.Create", link: "/SampleEntryByProject" },
  { label: "banner.menu.study.modifier", link: "/SampleModify?type=readwrite" },
];

const CRITERIA_OPTIONS = [
  {
    value: "lastName",
    labelId: "search.criteria.lastname",
    defaultLabel: "Nom de famille",
  },
  {
    value: "firstName",
    labelId: "search.criteria.firstname",
    defaultLabel: "Prénoms",
  },
  {
    value: "subjectNumber",
    labelId: "search.criteria.patientcode",
    defaultLabel: "Code patient",
  },
  {
    value: "labNumber",
    labelId: "search.criteria.labno",
    defaultLabel: "N° Lab",
  },
];

const ViralLoadModify = () => {
  const intl = useIntl();
  const { notificationVisible } = useContext(NotificationContext);

  const [criteria, setCriteria] = useState("lastName");
  const [searchValue, setSearchValue] = useState("");
  const [results, setResults] = useState([]);
  const [searching, setSearching] = useState(false);
  const [searched, setSearched] = useState(false);
  const [hoveredRow, setHoveredRow] = useState(null);
  const [selectedPatient, setSelectedPatient] = useState(null);
  const [loadingModify, setLoadingModify] = useState(false);

  const handleSearch = () => {
    const val = searchValue.trim();
    if (!val) return;
    setSearching(true);
    setSearched(false);
    getFromOpenElisServer(
      `/rest/patient-search-results?${criteria}=${encodeURIComponent(val)}&suppressExternalSearch=true`,
      (data) => {
        setResults(data?.patientSearchResults || []);
        setSearching(false);
        setSearched(true);
      },
    );
  };

  const handleKeyDown = (e) => {
    if (e.key === "Enter") handleSearch();
  };

  const handleModify = (patient) => {
    setSelectedPatient(null);
    setLoadingModify(true);
    getFromOpenElisServer(
      `/rest/SampleEntryByProjectStudyViralLoad?patientId=${encodeURIComponent(patient.patientID || "")}`,
      (data) => {
        setSelectedPatient({
          patientPK: data.patientPK || patient.patientID || "",
          project: data.studyId || "VL_Id",
          subjectNumber: patient.subjectNumber || "",
          nationalId: data.nationalId || patient.nationalId || "",
          externalId: data.externalId || patient.externalId || "",
          siteSubjectNumber:
            patient.referringSitePatientId ||
            data.externalId ||
            patient.externalId ||
            "",
          firstName: data.firstName || patient.firstName || "",
          lastName: data.lastName || patient.lastName || "",
          dob: data.dob || patient.dob || "",
          gender: data.gender || patient.gender || "",
          samplePK: data.samplePK || "",
          labNo: data.labNo || "",
          receivedDateForDisplay: data.receivedDateForDisplay || "",
          observations: data.observations || {},
          projectData: data.projectData || {},
        });
        setLoadingModify(false);
        setTimeout(() => {
          document
            .getElementById("vl-entry-inline")
            ?.scrollIntoView({ behavior: "smooth" });
        }, 100);
      },
    );
  };

  const COLUMNS = [
    {
      key: "lastName",
      labelId: "patient.last.name",
      defaultLabel: "Nom de famille",
    },
    {
      key: "firstName",
      labelId: "patient.first.name",
      defaultLabel: "Prénoms",
    },
    {
      key: "externalId",
      labelId: "search.criteria.patientcode",
      defaultLabel: "Code patient",
    },
    {
      key: "nationalId",
      labelId: "patient.natioanalid",
      defaultLabel: "ID National",
    },
    { key: "dob", labelId: "patient.dob", defaultLabel: "Date de naissance" },
    { key: "gender", labelId: "patient.gender", defaultLabel: "Sexe" },
  ];

  return (
    <div style={S.page}>
      {notificationVisible && <AlertDialog />}
      <PageBreadCrumb breadcrumbs={breadcrumbs} />

      <div style={S.pageTitle}>
        <FormattedMessage
          id="banner.menu.study.modifier"
          defaultMessage="Modifier"
        />
      </div>

      {/* Search Panel */}
      <div style={S.outerBox}>
        <div style={S.sectionHeader}>
          <FormattedMessage id="label.search" defaultMessage="Recherche" />
        </div>
        <div style={S.searchRow}>
          <div style={{ minWidth: "220px" }}>
            <Select
              id="search-criteria"
              labelText={intl.formatMessage({
                id: "label.search.criteria",
                defaultMessage: "Critère de recherche",
              })}
              value={criteria}
              onChange={(e) => {
                setCriteria(e.target.value);
                setSearchValue("");
                setResults([]);
                setSearched(false);
              }}
            >
              {CRITERIA_OPTIONS.map((opt) => (
                <SelectItem
                  key={opt.value}
                  value={opt.value}
                  text={intl.formatMessage({
                    id: opt.labelId,
                    defaultMessage: opt.defaultLabel,
                  })}
                />
              ))}
            </Select>
          </div>
          <div style={{ width: "500px", flexShrink: 0 }}>
            <TextInput
              id="search-value"
              labelText={intl.formatMessage({
                id: "label.search.value",
                defaultMessage: "Valeur",
              })}
              value={searchValue}
              onChange={(e) => setSearchValue(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder={intl.formatMessage({
                id: "label.search.placeholder",
                defaultMessage: "Saisir une valeur…",
              })}
            />
          </div>
          <Button
            onClick={handleSearch}
            disabled={searching || !searchValue.trim()}
          >
            {searching ? (
              <Loading small withOverlay={false} />
            ) : (
              <FormattedMessage
                id="label.button.search"
                defaultMessage="Rechercher"
              />
            )}
          </Button>
        </div>
      </div>

      {/* Results Table */}
      {searched && (
        <div style={S.outerBox}>
          <div style={S.sectionHeader}>
            <FormattedMessage
              id="label.search.results"
              defaultMessage="Résultats"
            />
            {results.length > 0 && ` (${results.length})`}
          </div>
          {results.length === 0 ? (
            <div style={S.noResult}>
              <FormattedMessage
                id="label.no.results"
                defaultMessage="Aucun résultat trouvé."
              />
            </div>
          ) : (
            <div style={{ overflowX: "auto" }}>
              <table style={S.table}>
                <thead>
                  <tr>
                    {COLUMNS.map((col) => (
                      <th key={col.key} style={S.th}>
                        <FormattedMessage
                          id={col.labelId}
                          defaultMessage={col.defaultLabel}
                        />
                      </th>
                    ))}
                    <th style={S.th}>
                      <FormattedMessage
                        id="label.actions"
                        defaultMessage="Actions"
                      />
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {results.map((patient, idx) => (
                    <tr
                      key={patient.patientID || idx}
                      onMouseEnter={() => setHoveredRow(idx)}
                      onMouseLeave={() => setHoveredRow(null)}
                      style={{
                        background: hoveredRow === idx ? "#f4f8ff" : "#fff",
                      }}
                    >
                      {COLUMNS.map((col) => (
                        <td key={col.key} style={S.td}>
                          {patient[col.key] || ""}
                        </td>
                      ))}
                      <td style={S.td}>
                        <Button
                          size="sm"
                          kind="primary"
                          onClick={() => handleModify(patient)}
                        >
                          <FormattedMessage
                            id="label.button.modify"
                            defaultMessage="Modifier"
                          />
                        </Button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {/* Chargement des données patient */}
      {loadingModify && (
        <div
          style={{
            display: "flex",
            alignItems: "center",
            gap: "8px",
            padding: "12px 0",
          }}
        >
          <Loading small withOverlay={false} />
          <FormattedMessage id="label.loading" defaultMessage="Chargement…" />
        </div>
      )}

      {/* Formulaire de modification inline */}
      {selectedPatient && (
        <div id="vl-entry-inline" style={{ marginTop: "1.5rem" }}>
          <div style={S.sectionHeader}>
            <FormattedMessage
              id="banner.menu.study.modifier"
              defaultMessage="Modifier"
            />
            {" — "}
            {selectedPatient.firstName} {selectedPatient.lastName}
          </div>
          <ViralLoadEntry
            embedded
            initialPatientData={selectedPatient}
            onSuccess={() => {
              setResults([]);
              setSearched(false);
            }}
          />
        </div>
      )}
    </div>
  );
};

export default ViralLoadModify;

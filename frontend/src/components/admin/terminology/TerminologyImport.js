import React, { useContext, useState, useEffect } from "react";
import config from "../../../config.json";
import {
  Grid,
  Column,
  Section,
  Heading,
  Loading,
  Button,
  Tag,
  Select,
  SelectItem,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  TableContainer,
  Pagination,
  InlineLoading,
  FileUploader,
  TextArea,
  Modal,
} from "@carbon/react";
import { getFromOpenElisServer } from "../../utils/Utils.js";
import { NotificationContext } from "../../layout/Layout.js";
import {
  AlertDialog,
  NotificationKinds,
} from "../../common/CustomNotification.js";
import { FormattedMessage, injectIntl, useIntl } from "react-intl";
import PageBreadCrumb from "../../common/PageBreadCrumb.js";

const breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
  {
    label: "terminology.import.title",
    link: "/MasterListsPage#TerminologyImport",
  },
];

// Libellés lisibles pour chaque cible d'import. Fallback = valeur brute.
const TARGET_LABELS = {
  TEST: "terminology.import.target.test",
  DICTIONARY: "terminology.import.target.dictionary",
  OBSERVATION_HISTORY_TYPE: "terminology.import.target.observationHistoryType",
};

function TerminologyImport() {
  const { notificationVisible, addNotification, setNotificationVisible } =
    useContext(NotificationContext);
  const intl = useIntl();

  const [targets, setTargets] = useState([]);
  const [target, setTarget] = useState("");
  const [csvContent, setCsvContent] = useState("");
  const [report, setReport] = useState(null);
  const [previewing, setPreviewing] = useState(false);
  const [applying, setApplying] = useState(false);
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  useEffect(() => {
    getFromOpenElisServer("/rest/terminology-import/targets", (res) => {
      if (Array.isArray(res)) {
        setTargets(res);
        if (res.length > 0) setTarget(res[0]);
      }
    });
  }, []);

  const targetLabel = (t) =>
    TARGET_LABELS[t]
      ? intl.formatMessage({ id: TARGET_LABELS[t], defaultMessage: t })
      : t;

  // Le CSV part en text/csv dans le body : postToOpenElisServerJsonResponse
  // force application/json, on utilise donc fetch() directement, avec la même
  // stratégie CSRF (X-CSRF-Token depuis localStorage) que Utils.js.
  const postCsv = (mode) => {
    const setLoading = mode === "apply" ? setApplying : setPreviewing;
    setLoading(true);
    fetch(
      `${config.serverBaseUrl}/rest/terminology-import/${mode}?target=${encodeURIComponent(
        target,
      )}`,
      {
        credentials: "include",
        method: "POST",
        headers: {
          "Content-Type": "text/csv",
          "X-CSRF-Token": localStorage.getItem("CSRF"),
        },
        body: csvContent,
      },
    )
      .then((response) => response.json())
      .then((json) => {
        setLoading(false);
        setReport(json);
        setPage(1);
        addNotification({
          title: intl.formatMessage({ id: "notification.title" }),
          message:
            mode === "apply"
              ? intl.formatMessage({
                  id: "terminology.import.apply.done",
                  defaultMessage: "Import appliqué.",
                })
              : intl.formatMessage({
                  id: "terminology.import.preview.done",
                  defaultMessage: "Prévisualisation terminée.",
                }),
          kind: NotificationKinds.success,
        });
        setNotificationVisible(true);
      })
      .catch((error) => {
        console.error(error);
        setLoading(false);
        addNotification({
          title: intl.formatMessage({ id: "notification.title" }),
          message: intl.formatMessage({
            id: "terminology.import.error",
            defaultMessage: "Erreur lors de l'import terminologique.",
          }),
          kind: NotificationKinds.error,
        });
        setNotificationVisible(true);
      });
  };

  const onCsvNotEmpty = () => csvContent && csvContent.trim().length > 0;

  const onPreview = () => {
    if (!target || !onCsvNotEmpty()) {
      addNotification({
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({
          id: "terminology.import.needCsv",
          defaultMessage: "Veuillez fournir un fichier CSV et une cible.",
        }),
        kind: NotificationKinds.warning,
      });
      setNotificationVisible(true);
      return;
    }
    postCsv("preview");
  };

  const onApplyConfirmed = () => {
    setConfirmOpen(false);
    postCsv("apply");
  };

  const onFileAdded = (event) => {
    const file = event?.target?.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = (e) => setCsvContent(e.target.result || "");
    reader.readAsText(file);
  };

  const headers = [
    {
      key: "key",
      header: intl.formatMessage({ id: "terminology.import.col.key" }),
    },
    {
      key: "action",
      header: intl.formatMessage({ id: "terminology.import.col.action" }),
    },
    {
      key: "loinc",
      header: intl.formatMessage({ id: "terminology.import.col.loinc" }),
    },
    {
      key: "snomed",
      header: intl.formatMessage({ id: "terminology.import.col.snomed" }),
    },
    {
      key: "message",
      header: intl.formatMessage({ id: "terminology.import.col.message" }),
    },
  ];

  const actionTag = (action) => {
    if (action === "UPDATED" || action === "WOULD_UPDATE")
      return <Tag type="green">{action}</Tag>;
    if (action === "NOT_FOUND" || action === "ERROR")
      return <Tag type="red">{action}</Tag>;
    return <Tag type="gray">{action}</Tag>;
  };

  const lines = report && Array.isArray(report.lines) ? report.lines : [];

  return (
    <>
      {notificationVisible === true ? <AlertDialog /> : ""}
      <div className="adminPageContent">
        <PageBreadCrumb breadcrumbs={breadcrumbs} />
        <Grid fullWidth>
          <Column lg={16} md={8} sm={4}>
            <Section>
              <Heading>
                <FormattedMessage id="terminology.import.title" />
              </Heading>
            </Section>
          </Column>
        </Grid>
        <br />
        {/* Cible d'import */}
        <Grid fullWidth>
          <Column lg={8} md={4} sm={4}>
            <Select
              id="terminologyImportTarget"
              labelText={intl.formatMessage({
                id: "terminology.import.target.label",
                defaultMessage: "Cible",
              })}
              value={target}
              onChange={(e) => setTarget(e.target.value)}
            >
              {targets.map((t) => (
                <SelectItem key={t} value={t} text={targetLabel(t)} />
              ))}
            </Select>
          </Column>
        </Grid>
        <br />
        {/* Fourniture du CSV : upload de fichier ou collage direct */}
        <Grid fullWidth>
          <Column lg={8} md={4} sm={4}>
            <FileUploader
              buttonLabel={intl.formatMessage({
                id: "terminology.import.file.button",
                defaultMessage: "Charger un CSV",
              })}
              labelDescription={intl.formatMessage({
                id: "terminology.import.file.hint",
                defaultMessage: "Fichier .csv uniquement",
              })}
              iconDescription="csv upload"
              multiple={false}
              accept={[".csv", "text/csv"]}
              buttonKind="tertiary"
              size="md"
              filenameStatus="edit"
              onChange={onFileAdded}
            />
          </Column>
          <Column lg={8} md={4} sm={4}>
            <TextArea
              id="terminologyImportCsv"
              labelText={intl.formatMessage({
                id: "terminology.import.csv.label",
                defaultMessage: "Contenu CSV",
              })}
              placeholder={intl.formatMessage({
                id: "terminology.import.csv.placeholder",
                defaultMessage: "Collez ici le contenu CSV…",
              })}
              rows={6}
              value={csvContent}
              onChange={(e) => setCsvContent(e.target.value)}
            />
          </Column>
        </Grid>
        <br />
        {/* Actions : dry-run puis apply */}
        <Grid fullWidth>
          <Column lg={16} md={8} sm={4}>
            <div style={{ display: "flex", gap: "0.5rem", alignItems: "center" }}>
              {previewing ? (
                <InlineLoading
                  status="active"
                  description={intl.formatMessage({
                    id: "terminology.import.preview.running",
                    defaultMessage: "Prévisualisation…",
                  })}
                />
              ) : (
                <Button
                  kind="primary"
                  size="md"
                  disabled={applying}
                  onClick={onPreview}
                >
                  <FormattedMessage
                    id="terminology.import.preview.button"
                    defaultMessage="Prévisualiser (dry-run)"
                  />
                </Button>
              )}
              {applying ? (
                <InlineLoading
                  status="active"
                  description={intl.formatMessage({
                    id: "terminology.import.apply.running",
                    defaultMessage: "Application…",
                  })}
                />
              ) : (
                <Button
                  kind="danger"
                  size="md"
                  disabled={!report || previewing}
                  onClick={() => setConfirmOpen(true)}
                >
                  <FormattedMessage
                    id="terminology.import.apply.button"
                    defaultMessage="Appliquer"
                  />
                </Button>
              )}
            </div>
          </Column>
        </Grid>
        <br />
        {/* Résumé + tableau des lignes */}
        {report ? (
          <>
            <Grid fullWidth>
              <Column lg={16} md={8} sm={4}>
                <Tag type="gray">
                  <FormattedMessage
                    id="terminology.import.summary.total"
                    defaultMessage="Total"
                  />
                  {": "}
                  {report.totalRows != null ? report.totalRows : 0}
                </Tag>
                <Tag type="green">
                  <FormattedMessage
                    id="terminology.import.summary.updated"
                    defaultMessage="À mettre à jour / mis à jour"
                  />
                  {": "}
                  {(report.updated != null ? report.updated : 0) +
                    (report.wouldUpdate != null ? report.wouldUpdate : 0)}
                </Tag>
                <Tag type="red">
                  <FormattedMessage
                    id="terminology.import.summary.notFound"
                    defaultMessage="Introuvables"
                  />
                  {": "}
                  {report.notFound != null ? report.notFound : 0}
                </Tag>
                <Tag type="gray">
                  <FormattedMessage
                    id="terminology.import.summary.skipped"
                    defaultMessage="Ignorés"
                  />
                  {": "}
                  {report.skipped != null ? report.skipped : 0}
                </Tag>
              </Column>
            </Grid>
            <br />
            <TableContainer>
              <Table>
                <TableHead>
                  <TableRow>
                    {headers.map((h) => (
                      <TableHeader key={h.key}>{h.header}</TableHeader>
                    ))}
                  </TableRow>
                </TableHead>
                <TableBody>
                  {lines
                    .slice((page - 1) * pageSize, page * pageSize)
                    .map((line, idx) => (
                      <TableRow key={`${line.key}-${idx}`}>
                        <TableCell>{line.key}</TableCell>
                        <TableCell>{actionTag(line.action)}</TableCell>
                        <TableCell>{line.loinc}</TableCell>
                        <TableCell>{line.snomed}</TableCell>
                        <TableCell style={{ maxWidth: "28rem" }}>
                          {line.message}
                        </TableCell>
                      </TableRow>
                    ))}
                </TableBody>
              </Table>
            </TableContainer>
            <Pagination
              page={page}
              pageSize={pageSize}
              pageSizes={[10, 25, 50, 100]}
              totalItems={lines.length}
              onChange={({ page: p, pageSize: ps }) => {
                setPage(p);
                setPageSize(ps);
              }}
            />
          </>
        ) : previewing || applying ? (
          <Loading />
        ) : (
          ""
        )}
      </div>
      <Modal
        open={confirmOpen}
        modalHeading={intl.formatMessage({
          id: "terminology.import.confirm.heading",
          defaultMessage: "Confirmer l'application",
        })}
        primaryButtonText={intl.formatMessage({
          id: "terminology.import.confirm.primary",
          defaultMessage: "Appliquer",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "terminology.import.confirm.secondary",
          defaultMessage: "Annuler",
        })}
        danger
        onRequestClose={() => setConfirmOpen(false)}
        onRequestSubmit={onApplyConfirmed}
      >
        <FormattedMessage
          id="terminology.import.confirm.body"
          defaultMessage="Les codes terminologiques seront écrits en base pour la cible sélectionnée. Continuer ?"
        />
      </Modal>
    </>
  );
}

export default injectIntl(TerminologyImport);

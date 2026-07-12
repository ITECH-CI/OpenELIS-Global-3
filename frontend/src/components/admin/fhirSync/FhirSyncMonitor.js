import React, { useContext, useState, useEffect } from "react";
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
  DataTable,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  TableContainer,
  Pagination,
  InlineLoading,
  Accordion,
  AccordionItem,
  TextInput,
} from "@carbon/react";
import CustomDatePicker from "../../common/CustomDatePicker.js";
import { Renew, Search as SearchIcon } from "@carbon/icons-react";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../utils/Utils.js";
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
    label: "fhir.sync.monitor.title",
    link: "/MasterListsPage#FhirSyncMonitor",
  },
];

function FhirSyncMonitor() {
  const { notificationVisible, addNotification, setNotificationVisible } =
    useContext(NotificationContext);
  const intl = useIntl();

  const [loading, setLoading] = useState(true);
  const [summary, setSummary] = useState({});
  const [statusFilter, setStatusFilter] = useState("FAILED");
  const [rows, setRows] = useState([]);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [replayStart, setReplayStart] = useState("");
  const [replayEnd, setReplayEnd] = useState("");
  const [replaying, setReplaying] = useState(false);
  const [expandedErrors, setExpandedErrors] = useState({});
  const [searchAccession, setSearchAccession] = useState("");
  const [searchDate, setSearchDate] = useState("");
  const [retryingId, setRetryingId] = useState(null);

  const toggleError = (id) => {
    setExpandedErrors((prev) => ({ ...prev, [id]: !prev[id] }));
  };

  const loadSummary = () => {
    getFromOpenElisServer("/rest/fhir-sync/summary", (res) => {
      if (res) setSummary(res);
    });
  };

  // Charge la liste avec filtres optionnels. accession/date déclenchent une
  // recherche tous statuts confondus côté backend.
  const loadList = (status, accession, date) => {
    setLoading(true);
    const params = new URLSearchParams({
      status: status || "FAILED",
      max: 200,
    });
    if (accession) params.append("accession", accession);
    if (date) params.append("date", date);
    getFromOpenElisServer(
      `/rest/fhir-sync/list?${params.toString()}`,
      (res) => {
        setRows(Array.isArray(res) ? res : []);
        setPage(1);
        setLoading(false);
      },
    );
  };

  useEffect(() => {
    loadSummary();
    loadList(statusFilter);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const refresh = () => {
    loadSummary();
    loadList(statusFilter, searchAccession, searchDate);
  };

  const onSearch = () => {
    loadList(statusFilter, searchAccession, searchDate);
  };

  const onClearSearch = () => {
    setSearchAccession("");
    setSearchDate("");
    loadList(statusFilter);
  };

  const onRetry = (id) => {
    setRetryingId(id);
    postToOpenElisServerJsonResponse(
      `/rest/fhir-sync/retry/${id}`,
      JSON.stringify({}),
      (res) => {
        setRetryingId(null);
        if (res && res.success) {
          addNotification({
            title: intl.formatMessage({ id: "notification.title" }),
            message: intl.formatMessage({
              id: "notification.user.post.save.success",
            }),
            kind: NotificationKinds.success,
          });
        } else {
          addNotification({
            title: intl.formatMessage({ id: "notification.title" }),
            message: res && res.message ? res.message : "retry failed",
            kind: NotificationKinds.error,
          });
        }
        setNotificationVisible(true);
        refresh();
      },
    );
  };

  const onReplay = () => {
    if (!replayStart) {
      addNotification({
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({
          id: "fhir.sync.replay.needStart",
          defaultMessage: "Veuillez indiquer une date de début.",
        }),
        kind: NotificationKinds.warning,
      });
      setNotificationVisible(true);
      return;
    }
    setReplaying(true);
    const params = new URLSearchParams({ dateStart: replayStart });
    if (replayEnd) params.append("dateEnd", replayEnd);
    postToOpenElisServerJsonResponse(
      `/rest/fhir-sync/replay?${params.toString()}`,
      JSON.stringify({}),
      (res) => {
        setReplaying(false);
        if (res && res.success) {
          addNotification({
            title: intl.formatMessage({ id: "notification.title" }),
            message: intl.formatMessage(
              {
                id: "fhir.sync.replay.done",
                defaultMessage:
                  "Rejeu terminé : {succeeded} réussi(s), {failed} échec(s) sur {total}.",
              },
              {
                succeeded: res.succeeded,
                failed: res.failed,
                total: res.total,
              },
            ),
            kind:
              res.failed > 0
                ? NotificationKinds.warning
                : NotificationKinds.success,
          });
        } else {
          addNotification({
            title: intl.formatMessage({ id: "notification.title" }),
            message: res && res.message ? res.message : "replay failed",
            kind: NotificationKinds.error,
          });
        }
        setNotificationVisible(true);
        refresh();
      },
    );
  };

  const headers = [
    {
      key: "triggerType",
      header: intl.formatMessage({ id: "fhir.sync.col.trigger" }),
    },
    {
      key: "targetId",
      header: intl.formatMessage({ id: "fhir.sync.col.target" }),
    },
    {
      key: "status",
      header: intl.formatMessage({ id: "fhir.sync.col.status" }),
    },
    {
      key: "attemptCount",
      header: intl.formatMessage({ id: "fhir.sync.col.attempts" }),
    },
    {
      key: "lastAttemptAt",
      header: intl.formatMessage({ id: "fhir.sync.col.last" }),
    },
    {
      key: "errorMessage",
      header: intl.formatMessage({ id: "fhir.sync.col.error" }),
    },
    { key: "actions", header: "" },
  ];

  const statusTag = (status) => {
    if (status === "SUCCESS") return <Tag type="green">{status}</Tag>;
    if (status === "FAILED") return <Tag type="red">{status}</Tag>;
    return <Tag type="gray">{status}</Tag>;
  };

  return (
    <>
      {notificationVisible === true ? <AlertDialog /> : ""}
      <div className="adminPageContent">
        <PageBreadCrumb breadcrumbs={breadcrumbs} />
        <Grid fullWidth>
          <Column lg={16} md={8} sm={4}>
            <Section>
              <Heading>
                <FormattedMessage id="fhir.sync.monitor.title" />
              </Heading>
            </Section>
          </Column>
        </Grid>
        <br />
        {/* Résumé compact + actualiser */}
        <Grid fullWidth>
          <Column lg={16} md={8} sm={4}>
            <Tag type="gray">
              PENDING: {summary.PENDING != null ? summary.PENDING : 0}
            </Tag>
            <Tag type="green">
              SUCCESS: {summary.SUCCESS != null ? summary.SUCCESS : 0}
            </Tag>
            <Tag type="red">
              FAILED: {summary.FAILED != null ? summary.FAILED : 0}
            </Tag>
            <Button kind="ghost" size="sm" renderIcon={Renew} onClick={refresh}>
              <FormattedMessage
                id="fhir.sync.refresh"
                defaultMessage="Actualiser"
              />
            </Button>
          </Column>
        </Grid>
        <br />
        {/* Rejeu historique : replié par défaut pour désencombrer */}
        <Accordion>
          <AccordionItem
            title={intl.formatMessage({
              id: "fhir.sync.replay.title",
              defaultMessage: "Rejeu des données historiques",
            })}
          >
            <Grid fullWidth>
              <Column lg={5} md={3} sm={4}>
                <CustomDatePicker
                  id="fhirReplayStart"
                  labelText={intl.formatMessage({
                    id: "fhir.sync.replay.start",
                    defaultMessage: "Date de début",
                  })}
                  value={replayStart}
                  disallowFutureDate={true}
                  onChange={(d) => setReplayStart(d)}
                />
              </Column>
              <Column lg={5} md={3} sm={4}>
                <CustomDatePicker
                  id="fhirReplayEnd"
                  labelText={intl.formatMessage({
                    id: "fhir.sync.replay.end",
                    defaultMessage: "Date de fin (optionnelle)",
                  })}
                  value={replayEnd}
                  disallowFutureDate={true}
                  onChange={(d) => setReplayEnd(d)}
                />
              </Column>
              <Column lg={6} md={2} sm={4}>
                <div style={{ marginTop: "1.5rem" }}>
                  {replaying ? (
                    <InlineLoading
                      status="active"
                      description={intl.formatMessage({
                        id: "fhir.sync.replay.running",
                        defaultMessage: "Rejeu en cours…",
                      })}
                    />
                  ) : (
                    <Button kind="tertiary" size="md" onClick={onReplay}>
                      <FormattedMessage
                        id="fhir.sync.replay.button"
                        defaultMessage="Rejouer la période"
                      />
                    </Button>
                  )}
                </div>
              </Column>
            </Grid>
          </AccordionItem>
        </Accordion>
        <br />
        {/* Recherche : statut + numéro labo + date de transaction */}
        <Grid fullWidth>
          <Column lg={4} md={2} sm={4}>
            <Select
              id="fhirSyncStatusFilter"
              labelText={intl.formatMessage({ id: "fhir.sync.col.status" })}
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
            >
              <SelectItem value="FAILED" text="FAILED" />
              <SelectItem value="PENDING" text="PENDING" />
              <SelectItem value="SUCCESS" text="SUCCESS" />
              <SelectItem
                value="ALL"
                text={intl.formatMessage({
                  id: "fhir.sync.filter.all",
                  defaultMessage: "Tous",
                })}
              />
            </Select>
          </Column>
          <Column lg={4} md={2} sm={4}>
            <TextInput
              id="fhirSearchAccession"
              labelText={intl.formatMessage({
                id: "fhir.sync.search.accession",
                defaultMessage: "Numéro labo",
              })}
              value={searchAccession}
              onChange={(e) => setSearchAccession(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === "Enter") onSearch();
              }}
            />
          </Column>
          <Column lg={4} md={2} sm={4}>
            <CustomDatePicker
              id="fhirSearchDate"
              labelText={intl.formatMessage({
                id: "fhir.sync.search.date",
                defaultMessage: "Date de transaction",
              })}
              value={searchDate}
              disallowFutureDate={true}
              onChange={(d) => setSearchDate(d)}
            />
          </Column>
          <Column lg={4} md={2} sm={4}>
            <div
              style={{ marginTop: "1.5rem", display: "flex", gap: "0.5rem" }}
            >
              <Button
                kind="primary"
                size="md"
                renderIcon={SearchIcon}
                onClick={onSearch}
              >
                <FormattedMessage
                  id="fhir.sync.search.button"
                  defaultMessage="Rechercher"
                />
              </Button>
              <Button kind="ghost" size="md" onClick={onClearSearch}>
                <FormattedMessage
                  id="fhir.sync.search.clear"
                  defaultMessage="Effacer"
                />
              </Button>
            </div>
          </Column>
        </Grid>
        <br />
        {loading ? (
          <Loading />
        ) : (
          <>
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
                  {rows
                    .slice((page - 1) * pageSize, page * pageSize)
                    .map((row) => (
                      <TableRow key={row.id}>
                        <TableCell>{row.triggerType}</TableCell>
                        <TableCell>
                          {row.accessionNumber || row.targetId}
                        </TableCell>
                        <TableCell>{statusTag(row.status)}</TableCell>
                        <TableCell>{row.attemptCount}</TableCell>
                        <TableCell>{row.lastAttemptAt}</TableCell>
                        <TableCell style={{ maxWidth: "28rem" }}>
                          {row.errorMessage ? (
                            expandedErrors[row.id] ? (
                              <div>
                                <div
                                  style={{
                                    whiteSpace: "pre-wrap",
                                    wordBreak: "break-word",
                                    fontSize: "0.75rem",
                                  }}
                                >
                                  {row.errorMessage}
                                </div>
                                <Button
                                  kind="ghost"
                                  size="sm"
                                  onClick={() => toggleError(row.id)}
                                >
                                  <FormattedMessage
                                    id="fhir.sync.error.less"
                                    defaultMessage="Réduire"
                                  />
                                </Button>
                              </div>
                            ) : (
                              <div>
                                <span
                                  style={{
                                    display: "inline-block",
                                    maxWidth: "22rem",
                                    overflow: "hidden",
                                    textOverflow: "ellipsis",
                                    whiteSpace: "nowrap",
                                    verticalAlign: "middle",
                                  }}
                                  title={row.errorMessage}
                                >
                                  {row.errorMessage}
                                </span>
                                <Button
                                  kind="ghost"
                                  size="sm"
                                  onClick={() => toggleError(row.id)}
                                >
                                  <FormattedMessage
                                    id="fhir.sync.error.more"
                                    defaultMessage="Voir plus"
                                  />
                                </Button>
                              </div>
                            )
                          ) : (
                            ""
                          )}
                        </TableCell>
                        <TableCell>
                          {retryingId === row.id ? (
                            <InlineLoading
                              status="active"
                              description={intl.formatMessage({
                                id: "fhir.sync.retry.running",
                                defaultMessage: "Rejeu…",
                              })}
                            />
                          ) : (
                            <Button
                              kind="tertiary"
                              size="sm"
                              disabled={retryingId !== null}
                              onClick={() => onRetry(row.id)}
                            >
                              <FormattedMessage
                                id="fhir.sync.retry"
                                defaultMessage="Rejouer"
                              />
                            </Button>
                          )}
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
              totalItems={rows.length}
              onChange={({ page: p, pageSize: ps }) => {
                setPage(p);
                setPageSize(ps);
              }}
            />
          </>
        )}
      </div>
    </>
  );
}

export default injectIntl(FhirSyncMonitor);

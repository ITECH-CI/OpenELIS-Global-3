import React, { useCallback, useContext, useEffect, useState } from "react";
import {
  Button,
  Column,
  Grid,
  Heading,
  InlineLoading,
  Modal,
  Pagination,
  Section,
  Select,
  SelectItem,
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
import { FormattedMessage, useIntl } from "react-intl";
import PageBreadCrumb from "../common/PageBreadCrumb";
import CustomDatePicker from "../common/CustomDatePicker";
import { AlertDialog, NotificationKinds } from "../common/CustomNotification";
import { NotificationContext } from "../layout/Layout";
import { getFromOpenElisServer, postToOpenElisServer } from "../utils/Utils";

// Fenêtre « Demandes reçues » : demandes de routine (plateforme, saisie
// locale), une ligne par demande, ouverte sur les demandes à traiter et
// rafraîchie toutes les 60 s. Lecture sur les demandes électroniques
// d'OpenELIS, sans appel au serveur FHIR.

export const REFRESH_MS = 60000;
const STEPS = [
  "A_TRAITER",
  "PRISE_EN_CHARGE",
  "REFUSEE",
  "ANNULEE",
  "ANNULEE_DEMANDEUR",
  "TOUTES",
];
const ORIGINS = ["PLATEFORME", "SAISIE_LOCALE"];
const breadcrumbs = [{ label: "home.label", link: "/" }];

export const formatAge = (minutes, intl) => {
  if (minutes == null) {
    return "";
  }
  if (minutes < 60) {
    return intl.formatMessage(
      { id: "receivedOrders.age.minutes" },
      { value: minutes },
    );
  }
  if (minutes < 24 * 60) {
    return intl.formatMessage(
      { id: "receivedOrders.age.hours" },
      { value: Math.floor(minutes / 60) },
    );
  }
  return intl.formatMessage(
    { id: "receivedOrders.age.days" },
    { value: Math.floor(minutes / (24 * 60)) },
  );
};

const stepTagType = (etape) => {
  switch (etape) {
    case "A_TRAITER":
      return "blue";
    case "PRISE_EN_CHARGE":
      return "green";
    case "REFUSEE":
      return "red";
    default:
      return "gray";
  }
};

const ReceivedOrders = ({
  refreshMs = REFRESH_MS,
  newOrderButton = null,
  reloadKey = 0,
}) => {
  const intl = useIntl();
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const [filters, setFilters] = useState({
    etape: "A_TRAITER",
    origine: "",
    etablissement: "",
    dateDebut: "",
    dateFin: "",
    recherche: "",
  });
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(25);
  const [result, setResult] = useState({ rows: [], total: 0 });
  const [loading, setLoading] = useState(false);
  const [qaEvents, setQaEvents] = useState([]);
  const [closeAction, setCloseAction] = useState(null); // { kind, row }
  const [closeReasonId, setCloseReasonId] = useState("");
  const [closing, setClosing] = useState(false);
  const [detail, setDetail] = useState(null);

  const load = useCallback(() => {
    const params = new URLSearchParams({ page, pageSize });
    Object.entries(filters).forEach(([key, value]) => {
      if (value) {
        params.append(key, value);
      }
    });
    setLoading(true);
    getFromOpenElisServer(
      "/rest/ReceivedOrders?" + params.toString(),
      (response) => {
        setLoading(false);
        if (response && Array.isArray(response.rows)) {
          setResult(response);
        } else {
          setResult({ rows: [], total: 0 });
        }
      },
    );
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [filters, page, pageSize, reloadKey]);

  useEffect(() => {
    load();
    const timer = setInterval(load, refreshMs);
    return () => clearInterval(timer);
  }, [load, refreshMs]);

  useEffect(() => {
    getFromOpenElisServer("/rest/displayList/QA_EVENTS", (res) =>
      setQaEvents(Array.isArray(res) ? res : []),
    );
  }, []);

  const setFilter = (key, value) => {
    setPage(1);
    setFilters((previous) => ({ ...previous, [key]: value }));
  };

  const takeCharge = (row) => {
    const externalId = row.exams.length > 0 ? row.exams[0].externalOrderId : "";
    window.open(
      "SamplePatientEntry?ID=" + encodeURIComponent(externalId),
      "_blank",
    );
  };

  const notify = (kind, message) => {
    addNotification({
      kind,
      title: intl.formatMessage({ id: "notification.title" }),
      message,
    });
    setNotificationVisible(true);
  };

  const submitClose = () => {
    if (!closeAction) {
      return;
    }
    if (!closeReasonId) {
      notify(
        NotificationKinds.error,
        intl.formatMessage({ id: "eorder.close.reason.required" }),
      );
      return;
    }
    const { kind, row } = closeAction;
    const endpoint =
      kind === "reject"
        ? "/rest/rejectElectronicOrders"
        : "/rest/cancelElectronicOrders";
    // une demande = un ou plusieurs examens : chacun est refusé (ou annulé)
    const pending = [...row.exams];
    let failed = false;
    setClosing(true);
    const next = () => {
      const exam = pending.shift();
      if (!exam) {
        setClosing(false);
        setCloseAction(null);
        notify(
          failed ? NotificationKinds.error : NotificationKinds.success,
          intl.formatMessage({
            id: failed
              ? "receivedOrders.close.partial"
              : kind === "reject"
                ? "eorder.close.rejected"
                : "eorder.close.cancelled",
          }),
        );
        load();
        return;
      }
      postToOpenElisServer(
        endpoint,
        JSON.stringify({
          externalOrderId: exam.externalOrderId,
          qaEventId: closeReasonId,
        }),
        (status) => {
          if (status !== 200) {
            failed = true;
          }
          next();
        },
      );
    };
    next();
  };

  const openDetail = (row) => {
    setDetail({ row, loading: true });
    getFromOpenElisServer(
      "/rest/ReceivedOrders/detail?id=" + encodeURIComponent(row.id),
      (response) => setDetail({ row, loading: false, data: response || {} }),
    );
  };

  const headers = [
    "date",
    "priority",
    "patient",
    "facility",
    "prescriber",
    "exams",
    "origin",
    "step",
    "age",
    "actions",
  ];

  return (
    <>
      {notificationVisible === true ? <AlertDialog /> : ""}
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <Grid fullWidth={true}>
        <Column lg={12} md={6} sm={4}>
          <Section>
            <Heading>
              <FormattedMessage id="receivedOrders.title" />
            </Heading>
          </Section>
        </Column>
        <Column lg={4} md={2} sm={4} style={{ textAlign: "right" }}>
          {newOrderButton}
        </Column>
      </Grid>
      <div className="orderLegendBody">
        <Grid fullWidth={true}>
          <Column lg={3} md={2} sm={4}>
            <Select
              id="receivedOrdersStep"
              labelText={intl.formatMessage({ id: "receivedOrders.step" })}
              value={filters.etape}
              onChange={(e) => setFilter("etape", e.target.value)}
            >
              {STEPS.map((step) => (
                <SelectItem
                  key={step}
                  value={step}
                  text={intl.formatMessage({
                    id: "receivedOrders.etape." + step,
                  })}
                />
              ))}
            </Select>
          </Column>
          <Column lg={3} md={2} sm={4}>
            <Select
              id="receivedOrdersOrigin"
              labelText={intl.formatMessage({ id: "receivedOrders.origin" })}
              value={filters.origine}
              onChange={(e) => setFilter("origine", e.target.value)}
            >
              <SelectItem
                value=""
                text={intl.formatMessage({ id: "receivedOrders.all" })}
              />
              {ORIGINS.map((origin) => (
                <SelectItem
                  key={origin}
                  value={origin}
                  text={intl.formatMessage({
                    id: "receivedOrders.origine." + origin,
                  })}
                />
              ))}
            </Select>
          </Column>
          <Column lg={4} md={4} sm={4}>
            <TextInput
              id="receivedOrdersFacility"
              labelText={intl.formatMessage({ id: "receivedOrders.facility" })}
              value={filters.etablissement}
              onChange={(e) => setFilter("etablissement", e.target.value)}
            />
          </Column>
          <Column lg={3} md={2} sm={2}>
            <CustomDatePicker
              id="receivedOrdersFrom"
              labelText={intl.formatMessage({ id: "eorder.date.start" })}
              value={filters.dateDebut}
              className="inputDate"
              onChange={(date) => setFilter("dateDebut", date)}
            />
          </Column>
          <Column lg={3} md={2} sm={2}>
            <CustomDatePicker
              id="receivedOrdersTo"
              labelText={intl.formatMessage({ id: "eorder.date.end" })}
              value={filters.dateFin}
              className="inputDate"
              onChange={(date) => setFilter("dateFin", date)}
            />
          </Column>
          <Column lg={16} md={8} sm={4}>
            <TextInput
              id="receivedOrdersSearch"
              labelText={intl.formatMessage({ id: "receivedOrders.search" })}
              value={filters.recherche}
              onChange={(e) => setFilter("recherche", e.target.value)}
            />
          </Column>
          <Column lg={16} md={8} sm={4}>
            {loading && <InlineLoading />}
            {!loading && result.rows.length === 0 && (
              <p data-testid="receivedOrdersEmpty">
                <FormattedMessage id="receivedOrders.empty" />
              </p>
            )}
            {result.rows.length > 0 && (
              <TableContainer>
                <Table size="sm">
                  <TableHead>
                    <TableRow>
                      {headers.map((header) => (
                        <TableHeader key={header}>
                          <FormattedMessage
                            id={"receivedOrders.column." + header}
                          />
                        </TableHeader>
                      ))}
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {result.rows.map((row) => (
                      <TableRow key={row.id} data-testid="receivedOrderRow">
                        <TableCell>{row.requestDate}</TableCell>
                        <TableCell>
                          <FormattedMessage
                            id={"receivedOrders.priority." + row.priority}
                            defaultMessage={row.priority}
                          />
                        </TableCell>
                        <TableCell>
                          <div>
                            {[row.patientLastName, row.patientFirstName]
                              .filter(Boolean)
                              .join(" ")}
                          </div>
                          <div>{row.cmuNumber}</div>
                          {row.provisionalIdentity && (
                            <Tag type="magenta" size="sm">
                              <FormattedMessage id="receivedOrders.provisionalIdentity" />
                            </Tag>
                          )}
                        </TableCell>
                        <TableCell>{row.requestingFacility}</TableCell>
                        <TableCell>{row.prescriber}</TableCell>
                        <TableCell>
                          {row.exams
                            .map(
                              (exam) => exam.testName || exam.externalOrderId,
                            )
                            .join(", ")}
                        </TableCell>
                        <TableCell>
                          {row.origine && (
                            <FormattedMessage
                              id={"receivedOrders.origine." + row.origine}
                              defaultMessage={row.origine}
                            />
                          )}
                        </TableCell>
                        <TableCell>
                          <Tag type={stepTagType(row.etape)} size="sm">
                            <FormattedMessage
                              id={"receivedOrders.etape." + row.etape}
                            />
                          </Tag>
                          {row.cancelledByRequester &&
                            row.etape !== "ANNULEE_DEMANDEUR" && (
                              <Tag type="red" size="sm">
                                <FormattedMessage id="receivedOrders.etape.ANNULEE_DEMANDEUR" />
                              </Tag>
                            )}
                          {row.labNumber && <div>{row.labNumber}</div>}
                        </TableCell>
                        <TableCell>{formatAge(row.ageMinutes, intl)}</TableCell>
                        <TableCell>
                          <div
                            style={{
                              display: "flex",
                              gap: "0.25rem",
                              flexWrap: "wrap",
                            }}
                          >
                            {!row.readOnly && (
                              <>
                                <Button
                                  size="sm"
                                  kind="primary"
                                  onClick={() => takeCharge(row)}
                                >
                                  <FormattedMessage id="receivedOrders.action.take" />
                                </Button>
                                <Button
                                  size="sm"
                                  kind="danger--tertiary"
                                  onClick={() => {
                                    setCloseReasonId("");
                                    setCloseAction({ kind: "reject", row });
                                  }}
                                >
                                  <FormattedMessage id="eorder.button.reject" />
                                </Button>
                                <Button
                                  size="sm"
                                  kind="ghost"
                                  onClick={() => {
                                    setCloseReasonId("");
                                    setCloseAction({ kind: "cancel", row });
                                  }}
                                >
                                  <FormattedMessage id="eorder.button.cancelOrder" />
                                </Button>
                              </>
                            )}
                            <Button
                              size="sm"
                              kind="ghost"
                              onClick={() => openDetail(row)}
                            >
                              <FormattedMessage id="receivedOrders.action.detail" />
                            </Button>
                          </div>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            )}
            {result.total > 0 && (
              <Pagination
                page={page}
                pageSize={pageSize}
                pageSizes={[10, 25, 50, 100]}
                totalItems={result.total}
                onChange={({ page: newPage, pageSize: newSize }) => {
                  setPage(newPage);
                  setPageSize(newSize);
                }}
              />
            )}
          </Column>
        </Grid>
      </div>

      <Modal
        open={!!closeAction}
        danger
        modalHeading={intl.formatMessage({
          id:
            closeAction && closeAction.kind === "reject"
              ? "eorder.button.reject"
              : "eorder.button.cancelOrder",
        })}
        primaryButtonText={intl.formatMessage({ id: "label.button.confirm" })}
        secondaryButtonText={intl.formatMessage({ id: "label.button.cancel" })}
        primaryButtonDisabled={closing}
        onRequestClose={() => setCloseAction(null)}
        onRequestSubmit={submitClose}
      >
        <Select
          id="receivedOrdersCloseReason"
          labelText={intl.formatMessage({ id: "eorder.close.reason" })}
          value={closeReasonId}
          onChange={(e) => setCloseReasonId(e.target.value)}
        >
          <SelectItem value="" text="" />
          {qaEvents.map((o) => (
            <SelectItem key={o.id} value={o.id} text={o.value} />
          ))}
        </Select>
      </Modal>

      <Modal
        open={!!detail}
        passiveModal
        modalHeading={intl.formatMessage({ id: "receivedOrders.detail" })}
        onRequestClose={() => setDetail(null)}
      >
        {detail && detail.loading && <InlineLoading />}
        {detail && !detail.loading && (
          <>
            <h5>
              <FormattedMessage id="receivedOrders.detail.notes" />
            </h5>
            {(detail.data.notes || []).length === 0 ? (
              <p>
                <FormattedMessage id="receivedOrders.detail.none" />
              </p>
            ) : (
              (detail.data.notes || []).map((note, index) => (
                <p key={"note" + index}>{note}</p>
              ))
            )}
            <h5 style={{ marginTop: "1rem" }}>
              <FormattedMessage id="receivedOrders.detail.clinical" />
            </h5>
            {(detail.data.clinicalInformation || []).length === 0 ? (
              <p>
                <FormattedMessage id="receivedOrders.detail.none" />
              </p>
            ) : (
              <ul>
                {(detail.data.clinicalInformation || []).map((item, index) => (
                  <li key={"ci" + index}>
                    {item.label} : {item.value}
                  </li>
                ))}
              </ul>
            )}
          </>
        )}
      </Modal>
    </>
  );
};

export default ReceivedOrders;

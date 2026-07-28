import React, { useState, useEffect, useContext } from "react";
import {
  Grid,
  Column,
  Section,
  Heading,
  TextInput,
  TextArea,
  Button,
  Select,
  SelectItem,
  Modal,
  Loading,
  Link,
  DataTable,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
} from "@carbon/react";
import {
  ArrowLeft,
  ArrowRight,
  Search,
  FilterRemove,
} from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import PageBreadCrumb from "../common/PageBreadCrumb";
import CustomDatePicker from "../common/CustomDatePicker";
import { getFromOpenElisServer, postToOpenElisServer } from "../utils/Utils";
import { NotificationContext } from "../layout/Layout";
import { NotificationKinds, AlertDialog } from "../common/CustomNotification";

let breadcrumbs = [{ label: "home.label", link: "/" }];

/**
 * Page "Demande electronique" du menu Étude (/StudyElectronicOrders).
 * Équivalent React de la page JSP studyElectronicOrderView.jsp : recherche
 * par code patient ou par date/statut, liste des demandes électroniques
 * (charge virale) avec les colonnes spécifiques à l'Étude (sexe, date de
 * naissance, date de prélèvement...) et 3 actions par ligne : Annuler,
 * Editer, Rejeter (motif/biologiste/note saisis dans une modale).
 */
const VleOrder = () => {
  const intl = useIntl();
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const [searchValue, setSearchValue] = useState("");
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");
  const [statusId, setStatusId] = useState("");
  const [statusOptions, setStatusOptions] = useState([]);
  const [qaEventOptions, setQaEventOptions] = useState([]);

  const [eOrders, setEOrders] = useState([]);
  const [searchCompleted, setSearchCompleted] = useState(false);
  const [loading, setLoading] = useState(false);
  const [nextPage, setNextPage] = useState(null);
  const [previousPage, setPreviousPage] = useState(null);
  const [pagination, setPagination] = useState(false);
  const [currentApiPage, setCurrentApiPage] = useState(null);
  const [totalApiPages, setTotalApiPages] = useState(null);
  const [totalOrdersCount, setTotalOrdersCount] = useState(0);

  const [rejectModalRow, setRejectModalRow] = useState(null);
  const [rejectDraft, setRejectDraft] = useState({
    qaEventId: "",
    qaAuthorizer: "",
    qaNote: "",
  });
  const [submittingRejectId, setSubmittingRejectId] = useState(null);

  const [cancelModalRow, setCancelModalRow] = useState(null);
  const [submittingCancelId, setSubmittingCancelId] = useState(null);

  useEffect(() => {
    getFromOpenElisServer(
      "/rest/displayList/ELECTRONIC_ORDER_STATUSES",
      (res) => setStatusOptions(res || []),
    );
    getFromOpenElisServer("/rest/displayList/QA_EVENTS", (res) =>
      setQaEventOptions(res || []),
    );
  }, []);

  const parseResponse = (response) => {
    setLoading(false);
    setSearchCompleted(true);
    if (!response) {
      // Échec réseau/serveur : prévenir l'utilisateur au lieu de rester muet
      // (l'écran semblait afficher « 0 résultat » alors que la requête a échoué).
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "server.error.msg" }),
      });
      setNotificationVisible(true);
      return;
    }
    if (response.paging) {
      const { totalPages, currentPage } = response.paging;
      if (totalPages > 1) {
        setPagination(true);
        setCurrentApiPage(currentPage);
        setTotalApiPages(totalPages);
        setNextPage(
          parseInt(currentPage) < parseInt(totalPages)
            ? parseInt(currentPage) + 1
            : null,
        );
        setPreviousPage(
          parseInt(currentPage) > 1 ? parseInt(currentPage) - 1 : null,
        );
      } else {
        setPagination(false);
        setNextPage(null);
        setPreviousPage(null);
      }
    }
    const orders = (response.eOrders || []).map((item) => ({
      ...item,
      id: item.electronicOrderId,
    }));
    setEOrders(orders);
    const searchTermToPage = response.paging?.searchTermToPage;
    setTotalOrdersCount(
      searchTermToPage ? searchTermToPage.length : orders.length,
    );
    if (orders.length === 0) {
      addNotification({
        kind: NotificationKinds.warning,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "eorder.search.noresults" }),
      });
      setNotificationVisible(true);
    }
  };

  const handleSearch = () => {
    setLoading(true);
    // Recherche combinée : code patient, dates et statut sont chacun des
    // filtres optionnels envoyés ensemble, appliqués en ET par le backend
    // (searchStudyElectronicOrdersCombined), sur le même principe que la
    // recherche par date et statut.
    const params = new URLSearchParams({
      searchType: "DATE_STATUS",
      searchValue: searchValue,
      startDate: startDate,
      endDate: endDate,
      statusId: statusId,
    });
    getFromOpenElisServer(
      "/rest/StudyElectronicOrders?" + params.toString(),
      parseResponse,
    );
  };

  const clearFilters = () => {
    setSearchValue("");
    setStartDate("");
    setEndDate("");
    setStatusId("");
    setEOrders([]);
    setSearchCompleted(false);
    setPagination(false);
    setNextPage(null);
    setPreviousPage(null);
    setCurrentApiPage(null);
    setTotalApiPages(null);
    setTotalOrdersCount(0);
  };

  const loadPage = (page) => {
    setLoading(true);
    getFromOpenElisServer(
      "/rest/StudyElectronicOrders?page=" + page,
      parseResponse,
    );
  };

  const editOrder = (row) => {
    const externalOrderId = row?.externalOrderId ?? row;
    let url =
      "/SampleEntryByProject?type=initial&ID=" +
      encodeURIComponent(externalOrderId);
    // Transmettre le N° de labo déjà présent sur la demande (le formulaire de
    // saisie le lit via ?labNumber= pour le pré-remplir). Sans ça, le N° saisi
    // était perdu à l'ouverture de l'édition.
    if (row?.labNumber) {
      url += "&labNumber=" + encodeURIComponent(row.labNumber);
    }
    window.open(url, "_blank", "noopener,noreferrer");
  };

  const isRowLocked = (row) =>
    !!row.labNumber || !!row.qaEventId || !!row.cancelled;

  const openRejectModal = (row) => {
    setRejectDraft({ qaEventId: "", qaAuthorizer: "", qaNote: "" });
    setRejectModalRow(row);
  };

  const closeRejectModal = () => {
    setRejectModalRow(null);
  };

  const submitReject = () => {
    if (!rejectModalRow) {
      return;
    }
    if (!rejectDraft.qaEventId) {
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({
          id: "study.eorder.reject.reason.required",
        }),
      });
      setNotificationVisible(true);
      return;
    }
    setSubmittingRejectId(rejectModalRow.electronicOrderId);
    postToOpenElisServer(
      "/rest/rejectElectronicOrders",
      JSON.stringify({
        externalOrderId: rejectModalRow.externalOrderId,
        qaEventId: rejectDraft.qaEventId,
        qaAuthorizer: rejectDraft.qaAuthorizer || "",
        qaNote: rejectDraft.qaNote || "",
      }),
      (status, body) => {
        setSubmittingRejectId(null);
        if (status === 200) {
          setEOrders((prev) =>
            prev.map((o) =>
              o.electronicOrderId === rejectModalRow.electronicOrderId
                ? { ...o, qaEventId: rejectDraft.qaEventId }
                : o,
            ),
          );
          addNotification({
            kind: NotificationKinds.success,
            title: intl.formatMessage({ id: "notification.title" }),
            message: intl.formatMessage({ id: "study.eorder.action.reject" }),
          });
          setRejectModalRow(null);
        } else {
          addNotification({
            kind: NotificationKinds.error,
            title: intl.formatMessage({ id: "notification.title" }),
            message: (body && body.message) || status,
          });
        }
        setNotificationVisible(true);
      },
    );
  };

  const openCancelModal = (row) => {
    setCancelModalRow(row);
  };

  const closeCancelModal = () => {
    setCancelModalRow(null);
  };

  const confirmCancel = () => {
    if (!cancelModalRow) {
      return;
    }
    setSubmittingCancelId(cancelModalRow.electronicOrderId);
    postToOpenElisServer(
      "/rest/cancelElectronicOrders",
      JSON.stringify({ externalOrderId: cancelModalRow.externalOrderId }),
      (status, body) => {
        setSubmittingCancelId(null);
        if (status === 200) {
          setEOrders((prev) =>
            prev.map((o) =>
              o.electronicOrderId === cancelModalRow.electronicOrderId
                ? { ...o, cancelled: true }
                : o,
            ),
          );
          addNotification({
            kind: NotificationKinds.success,
            title: intl.formatMessage({ id: "notification.title" }),
            message: intl.formatMessage({
              id: "study.eorder.action.cancelled",
            }),
          });
          setCancelModalRow(null);
        } else {
          addNotification({
            kind: NotificationKinds.error,
            title: intl.formatMessage({ id: "notification.title" }),
            message: (body && body.message) || status,
          });
        }
        setNotificationVisible(true);
      },
    );
  };

  const renderCell = (cell) => (
    <TableCell
      key={cell.id}
      style={{ overflowWrap: "anywhere", wordBreak: "break-word" }}
    >
      {cell.value}
    </TableCell>
  );

  const COLUMN_WIDTHS = {
    requestingFacility: "19%",
    patientNationalId: "14%",
    gender: "3%",
    birthDate: "8%",
    collectionDateDisplay: "8%",
    receptionDateDisplay: "8%",
    requestDateDisplay: "8%",
    status: "6%",
    labNumber: "6%",
    actions: "18%",
  };

  const createDataTable = () => (
    <DataTable
      id="vleOrderTable"
      rows={eOrders}
      headers={[
        {
          key: "requestingFacility",
          header: intl.formatMessage({ id: "study.eorder.requester.facility" }),
        },
        {
          key: "patientNationalId",
          header: intl.formatMessage({ id: "study.eorder.patient.code" }),
        },
        {
          key: "gender",
          header: intl.formatMessage({ id: "study.eorder.patient.gender" }),
        },
        {
          key: "birthDate",
          header: intl.formatMessage({ id: "study.eorder.patient.birth_date" }),
        },
        {
          key: "collectionDateDisplay",
          header: intl.formatMessage({ id: "study.eorder.collection.date" }),
        },
        {
          key: "receptionDateDisplay",
          header: intl.formatMessage({ id: "study.eorder.reception.date" }),
        },
        {
          key: "requestDateDisplay",
          header: intl.formatMessage({ id: "study.eorder.request.date" }),
        },
        {
          key: "status",
          header: intl.formatMessage({ id: "study.eorder.request.status" }),
        },
        {
          key: "labNumber",
          header: intl.formatMessage({ id: "study.eorder.lab_number" }),
        },
      ]}
      isSortable
    >
      {({
        rows,
        headers,
        getHeaderProps,
        getRowProps,
        getTableProps,
        getTableContainerProps,
      }) => (
        <TableContainer title="" description="" {...getTableContainerProps()}>
          <Table
            {...getTableProps()}
            size="sm"
            style={{ tableLayout: "fixed" }}
          >
            <TableHead>
              <TableRow>
                {headers.map((header) => (
                  <TableHeader
                    key={header.key}
                    {...getHeaderProps({ header })}
                    style={{ width: COLUMN_WIDTHS[header.key] }}
                  >
                    {header.header}
                  </TableHeader>
                ))}
                <TableHeader style={{ width: COLUMN_WIDTHS.actions }}>
                  <FormattedMessage id="study.eorder.action.title" />
                </TableHeader>
              </TableRow>
            </TableHead>
            <TableBody>
              {rows.map((row) => {
                const originalRow = eOrders.find((o) => o.id === row.id);
                const locked = originalRow ? isRowLocked(originalRow) : false;
                return (
                  <TableRow key={row.id} {...getRowProps({ row })}>
                    {row.cells.map((cell) => renderCell(cell))}
                    <TableCell>
                      <div
                        style={{
                          display: "flex",
                          flexWrap: "nowrap",
                          gap: "0.25rem",
                        }}
                      >
                        <Button
                          type="button"
                          kind="tertiary"
                          size="sm"
                          disabled={locked}
                          style={{ padding: "0 0.75rem", minWidth: "auto" }}
                          onClick={() => editOrder(originalRow)}
                        >
                          <FormattedMessage id="study.eorder.action.edit" />
                        </Button>
                        <Button
                          type="button"
                          kind="tertiary"
                          size="sm"
                          disabled={locked}
                          style={{ padding: "0 0.75rem", minWidth: "auto" }}
                          onClick={() => openCancelModal(originalRow)}
                        >
                          <FormattedMessage id="label.button.cancel" />
                        </Button>
                        <Button
                          type="button"
                          kind="danger"
                          size="sm"
                          disabled={locked}
                          style={{ padding: "0 0.75rem", minWidth: "auto" }}
                          onClick={() => openRejectModal(originalRow)}
                        >
                          <FormattedMessage id="study.eorder.action.reject" />
                        </Button>
                      </div>
                    </TableCell>
                  </TableRow>
                );
              })}
            </TableBody>
          </Table>
        </TableContainer>
      )}
    </DataTable>
  );

  return (
    <>
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      {notificationVisible === true ? <AlertDialog /> : ""}
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <Section>
            <Section>
              <Heading>
                <FormattedMessage id="study.eorder.browse.title" />
              </Heading>
            </Section>
          </Section>
        </Column>
      </Grid>
      <div
        style={{
          backgroundColor: "#ffffff",
          border: "1px solid #e0e0e0",
          borderRadius: "8px",
          padding: "1.5rem",
          margin: "1rem 0",
        }}
      >
        <Grid fullWidth={true}>
          <Column lg={4} md={4} sm={4}>
            <TextInput
              id="vleOrderSearchValue"
              labelText={intl.formatMessage({
                id: "study.eorder.patient.code",
              })}
              placeholder={intl.formatMessage({
                id: "study.eorder.patient.code.placeholder",
              })}
              value={searchValue}
              onChange={(e) => setSearchValue(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === "Enter") {
                  handleSearch();
                }
              }}
            />
          </Column>
          <Column lg={3} md={4} sm={4}>
            <CustomDatePicker
              id="vleOrderStartDate"
              labelText={intl.formatMessage({
                id: "study.eorder.search.date.start",
              })}
              value={startDate}
              className="inputDate"
              onChange={(date) => setStartDate(date)}
              updateStateValue
            />
          </Column>
          <Column lg={3} md={4} sm={4}>
            <CustomDatePicker
              id="vleOrderEndDate"
              labelText={intl.formatMessage({
                id: "study.eorder.search.date.end",
              })}
              value={endDate}
              className="inputDate"
              onChange={(date) => setEndDate(date)}
              updateStateValue
            />
          </Column>
          <Column lg={3} md={4} sm={4}>
            <Select
              id="vleOrderStatusId"
              labelText={intl.formatMessage({
                id: "study.eorder.request.status",
              })}
              value={statusId}
              onChange={(e) => setStatusId(e.target.value)}
            >
              <SelectItem
                value=""
                text={intl.formatMessage({ id: "study.eorder.all_status" })}
              />
              {statusOptions.map((statusOption) => (
                <SelectItem
                  key={statusOption.id}
                  value={statusOption.id}
                  text={statusOption.value}
                />
              ))}
            </Select>
          </Column>
          <Column lg={3} md={4} sm={4}>
            <div
              className="bottomAlign"
              style={{ display: "flex", gap: "0.5rem" }}
            >
              <Button onClick={handleSearch} renderIcon={Search}>
                <FormattedMessage id="label.button.search" />
              </Button>
              <Button
                kind="tertiary"
                onClick={clearFilters}
                hasIconOnly
                renderIcon={FilterRemove}
                iconDescription={intl.formatMessage({
                  id: "label.button.clear",
                })}
              />
            </div>
          </Column>
        </Grid>
      </div>
      <div className="orderLegendBody">
        <Grid fullWidth={true}>
          {searchCompleted && eOrders.length === 0 && (
            <Column lg={16} md={8} sm={4}>
              <FormattedMessage id="eorder.search.noresults" />
            </Column>
          )}
          <Column lg={16} md={8} sm={4}>
            {loading && (
              <Loading
                description={intl.formatMessage({ id: "loading.description" })}
                small={true}
              />
            )}
          </Column>
        </Grid>

        {eOrders.length > 0 && (
          <div
            style={{
              backgroundColor: "#d1e7dd",
              color: "#0f5132",
              border: "1px solid #a3cfbb",
              borderRadius: "4px",
              padding: "0.5rem 1rem",
              margin: "0.5rem 0",
              display: "inline-block",
            }}
          >
            <FormattedMessage
              id="study.eorder.count.summary"
              values={{ loaded: eOrders.length, total: totalOrdersCount }}
            />
          </div>
        )}

        {eOrders.length > 0 && createDataTable()}

        {pagination && (
          <Grid fullWidth={true}>
            <Column lg={14} />
            <Column
              lg={2}
              style={{
                display: "flex",
                flexDirection: "column",
                alignItems: "center",
                gap: "10px",
                width: "110%",
              }}
            >
              <Link>
                {currentApiPage} / {totalApiPages}
              </Link>
              <div style={{ display: "flex", gap: "10px" }}>
                <Button
                  hasIconOnly
                  id="vleOrderPreviousPage"
                  onClick={() => loadPage(previousPage)}
                  disabled={previousPage == null}
                  renderIcon={ArrowLeft}
                  iconDescription={intl.formatMessage({
                    id: "organization.previous",
                  })}
                />
                <Button
                  hasIconOnly
                  id="vleOrderNextPage"
                  onClick={() => loadPage(nextPage)}
                  disabled={nextPage == null}
                  renderIcon={ArrowRight}
                  iconDescription={intl.formatMessage({
                    id: "organization.next",
                  })}
                />
              </div>
            </Column>
          </Grid>
        )}
      </div>

      <Modal
        open={!!rejectModalRow}
        modalHeading={intl.formatMessage({ id: "study.eorder.action.reject" })}
        primaryButtonText={intl.formatMessage({ id: "label.button.save" })}
        secondaryButtonText={intl.formatMessage({ id: "label.button.cancel" })}
        danger
        primaryButtonDisabled={
          !!rejectModalRow &&
          submittingRejectId === rejectModalRow.electronicOrderId
        }
        onRequestClose={closeRejectModal}
        onRequestSubmit={submitReject}
      >
        <Select
          id="rejectQaEventId"
          labelText={intl.formatMessage({ id: "label.refusal.reason" })}
          value={rejectDraft.qaEventId}
          onChange={(e) =>
            setRejectDraft((prev) => ({ ...prev, qaEventId: e.target.value }))
          }
        >
          <SelectItem value="" text="" />
          {qaEventOptions.map((o) => (
            <SelectItem key={o.id} value={o.id} text={o.value} />
          ))}
        </Select>
        <TextInput
          id="rejectQaAuthorizer"
          labelText={intl.formatMessage({ id: "label.biologist" })}
          value={rejectDraft.qaAuthorizer}
          onChange={(e) =>
            setRejectDraft((prev) => ({
              ...prev,
              qaAuthorizer: e.target.value,
            }))
          }
        />
        <TextArea
          id="rejectQaNote"
          labelText={intl.formatMessage({ id: "nonconformity.note" })}
          value={rejectDraft.qaNote}
          onChange={(e) =>
            setRejectDraft((prev) => ({ ...prev, qaNote: e.target.value }))
          }
        />
      </Modal>

      <Modal
        open={!!cancelModalRow}
        modalHeading={intl.formatMessage({ id: "label.button.cancel" })}
        primaryButtonText={intl.formatMessage({ id: "label.button.confirm" })}
        secondaryButtonText={intl.formatMessage({ id: "label.button.cancel" })}
        danger
        primaryButtonDisabled={
          !!cancelModalRow &&
          submittingCancelId === cancelModalRow.electronicOrderId
        }
        onRequestClose={closeCancelModal}
        onRequestSubmit={confirmCancel}
      >
        <p>
          <FormattedMessage id="study.eorder.action.cancel.confirm" />
        </p>
      </Modal>
    </>
  );
};

export default VleOrder;

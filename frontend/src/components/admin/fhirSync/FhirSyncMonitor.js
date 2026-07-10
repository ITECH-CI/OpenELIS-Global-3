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
} from "@carbon/react";
import { Renew } from "@carbon/icons-react";
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

  const loadSummary = () => {
    getFromOpenElisServer("/rest/fhir-sync/summary", (res) => {
      if (res) setSummary(res);
    });
  };

  const loadList = (status) => {
    setLoading(true);
    getFromOpenElisServer(
      `/rest/fhir-sync/list?status=${status}&max=200`,
      (res) => {
        setRows(Array.isArray(res) ? res : []);
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
    loadList(statusFilter);
  };

  const onRetry = (id) => {
    postToOpenElisServerJsonResponse(
      `/rest/fhir-sync/retry/${id}`,
      JSON.stringify({}),
      (res) => {
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

  const headers = [
    { key: "triggerType", header: intl.formatMessage({ id: "fhir.sync.col.trigger" }) },
    { key: "targetId", header: intl.formatMessage({ id: "fhir.sync.col.target" }) },
    { key: "status", header: intl.formatMessage({ id: "fhir.sync.col.status" }) },
    { key: "attemptCount", header: intl.formatMessage({ id: "fhir.sync.col.attempts" }) },
    { key: "lastAttemptAt", header: intl.formatMessage({ id: "fhir.sync.col.last" }) },
    { key: "errorMessage", header: intl.formatMessage({ id: "fhir.sync.col.error" }) },
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
        {/* Résumé */}
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
            <Button
              kind="ghost"
              size="sm"
              renderIcon={Renew}
              onClick={refresh}
            >
              <FormattedMessage id="fhir.sync.refresh" defaultMessage="Actualiser" />
            </Button>
          </Column>
        </Grid>
        <br />
        <Grid fullWidth>
          <Column lg={4} md={4} sm={4}>
            <Select
              id="fhirSyncStatusFilter"
              labelText={intl.formatMessage({ id: "fhir.sync.col.status" })}
              value={statusFilter}
              onChange={(e) => {
                setStatusFilter(e.target.value);
                loadList(e.target.value);
              }}
            >
              <SelectItem value="FAILED" text="FAILED" />
              <SelectItem value="PENDING" text="PENDING" />
              <SelectItem value="SUCCESS" text="SUCCESS" />
            </Select>
          </Column>
        </Grid>
        <br />
        {loading ? (
          <Loading />
        ) : (
          <DataTable rows={rows.map((r) => ({ ...r })) } headers={headers}>
            {({ getTableProps, getHeaderProps }) => (
              <TableContainer>
                <Table {...getTableProps()}>
                  <TableHead>
                    <TableRow>
                      {headers.map((h) => (
                        <TableHeader key={h.key} {...getHeaderProps({ header: h })}>
                          {h.header}
                        </TableHeader>
                      ))}
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {rows.map((row) => (
                      <TableRow key={row.id}>
                        <TableCell>{row.triggerType}</TableCell>
                        <TableCell>{row.targetId}</TableCell>
                        <TableCell>{statusTag(row.status)}</TableCell>
                        <TableCell>{row.attemptCount}</TableCell>
                        <TableCell>{row.lastAttemptAt}</TableCell>
                        <TableCell
                          style={{
                            maxWidth: "24rem",
                            overflow: "hidden",
                            textOverflow: "ellipsis",
                          }}
                          title={row.errorMessage}
                        >
                          {row.errorMessage}
                        </TableCell>
                        <TableCell>
                          <Button
                            kind="tertiary"
                            size="sm"
                            onClick={() => onRetry(row.id)}
                          >
                            <FormattedMessage
                              id="fhir.sync.retry"
                              defaultMessage="Rejouer"
                            />
                          </Button>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            )}
          </DataTable>
        )}
      </div>
    </>
  );
}

export default injectIntl(FhirSyncMonitor);

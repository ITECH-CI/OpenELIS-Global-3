import React, { useContext, useState, useEffect } from "react";
import {
  Grid,
  Column,
  Section,
  Heading,
  Loading,
  Button,
  Tag,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  TableContainer,
  InlineLoading,
  TextInput,
  Modal,
} from "@carbon/react";
import { Renew, Copy } from "@carbon/icons-react";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../utils/Utils.js";
import config from "../../../config.json";
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
    label: "fhir.gateway.title",
    link: "/MasterListsPage#FhirGateway",
  },
];

function FhirGateway() {
  const { notificationVisible, addNotification, setNotificationVisible } =
    useContext(NotificationContext);
  const intl = useIntl();

  const [loading, setLoading] = useState(true);
  const [clients, setClients] = useState([]);
  const [newName, setNewName] = useState("");
  const [newDescription, setNewDescription] = useState("");
  const [adding, setAdding] = useState(false);
  const [togglingId, setTogglingId] = useState(null);
  const [issuingId, setIssuingId] = useState(null);
  // Jetons chargés par client (id -> tableau). Client déplié => présent dans la map.
  const [tokensByClient, setTokensByClient] = useState({});
  const [loadingTokensId, setLoadingTokensId] = useState(null);
  const [revokingId, setRevokingId] = useState(null);
  // Jeton fraîchement émis, affiché en clair une seule fois dans le modal.
  const [issuedToken, setIssuedToken] = useState(null);

  // POST vers un endpoint qui renvoie 200 sans corps JSON garanti (active,
  // revoke). Le helper standard fait response.json() et échouerait sur un
  // corps vide ; ici on considère tout statut HTTP ok comme un succès.
  const postExpectOk = (endPoint, onDone) => {
    fetch(config.serverBaseUrl + endPoint, {
      credentials: "include",
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "X-CSRF-Token": localStorage.getItem("CSRF"),
      },
      body: JSON.stringify({}),
    })
      .then((response) => onDone(response.ok))
      .catch(() => onDone(false));
  };

  const notify = (message, kind) => {
    addNotification({
      title: intl.formatMessage({ id: "notification.title" }),
      message,
      kind,
    });
    setNotificationVisible(true);
  };

  const loadClients = () => {
    setLoading(true);
    getFromOpenElisServer("/rest/fhir-gateway/clients", (res) => {
      setClients(Array.isArray(res) ? res : []);
      setLoading(false);
    });
  };

  useEffect(() => {
    loadClients();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const onAddClient = () => {
    if (!newName || newName.trim().length === 0) {
      notify(
        intl.formatMessage({
          id: "fhir.gateway.client.needName",
          defaultMessage: "Veuillez saisir un nom de tiers.",
        }),
        NotificationKinds.warning,
      );
      return;
    }
    setAdding(true);
    const params = new URLSearchParams({ name: newName });
    if (newDescription) params.append("description", newDescription);
    postToOpenElisServerJsonResponse(
      `/rest/fhir-gateway/clients?${params.toString()}`,
      JSON.stringify({}),
      (res) => {
        setAdding(false);
        if (res && res.id) {
          notify(
            intl.formatMessage({
              id: "fhir.gateway.client.added",
              defaultMessage: "Tiers ajouté.",
            }),
            NotificationKinds.success,
          );
          setNewName("");
          setNewDescription("");
          loadClients();
        } else {
          notify(
            intl.formatMessage({
              id: "fhir.gateway.client.addError",
              defaultMessage: "Échec de l'ajout du tiers.",
            }),
            NotificationKinds.error,
          );
        }
      },
    );
  };

  const onToggleActive = (client) => {
    setTogglingId(client.id);
    const nextActive = !client.active;
    postExpectOk(
      `/rest/fhir-gateway/clients/${client.id}/active?active=${nextActive}`,
      (ok) => {
        setTogglingId(null);
        if (ok) {
          notify(
            nextActive
              ? intl.formatMessage({
                  id: "fhir.gateway.client.activated",
                  defaultMessage: "Tiers activé.",
                })
              : intl.formatMessage({
                  id: "fhir.gateway.client.deactivated",
                  defaultMessage: "Tiers désactivé.",
                }),
            NotificationKinds.success,
          );
          loadClients();
        } else {
          notify(
            intl.formatMessage({
              id: "fhir.gateway.client.toggleError",
              defaultMessage: "Échec du changement de statut.",
            }),
            NotificationKinds.error,
          );
        }
      },
    );
  };

  const loadTokens = (clientId) => {
    setLoadingTokensId(clientId);
    getFromOpenElisServer(
      `/rest/fhir-gateway/clients/${clientId}/tokens`,
      (res) => {
        setLoadingTokensId(null);
        setTokensByClient((prev) => ({
          ...prev,
          [clientId]: Array.isArray(res) ? res : [],
        }));
      },
    );
  };

  const onToggleTokens = (clientId) => {
    // Déplié => on replie (retire de la map). Sinon on charge.
    if (Object.prototype.hasOwnProperty.call(tokensByClient, clientId)) {
      setTokensByClient((prev) => {
        const next = { ...prev };
        delete next[clientId];
        return next;
      });
    } else {
      loadTokens(clientId);
    }
  };

  const onIssueToken = (clientId) => {
    setIssuingId(clientId);
    postToOpenElisServerJsonResponse(
      `/rest/fhir-gateway/clients/${clientId}/tokens`,
      JSON.stringify({}),
      (res) => {
        setIssuingId(null);
        if (res && res.token) {
          setIssuedToken(res.token);
          notify(
            intl.formatMessage({
              id: "fhir.gateway.token.issued",
              defaultMessage: "Jeton émis.",
            }),
            NotificationKinds.success,
          );
          // Rafraîchit la liste des jetons si le client est déplié.
          if (Object.prototype.hasOwnProperty.call(tokensByClient, clientId)) {
            loadTokens(clientId);
          }
        } else {
          notify(
            intl.formatMessage({
              id: "fhir.gateway.token.issueError",
              defaultMessage: "Échec de l'émission du jeton.",
            }),
            NotificationKinds.error,
          );
        }
      },
    );
  };

  const onRevokeToken = (tokenId, clientId) => {
    setRevokingId(tokenId);
    postExpectOk(`/rest/fhir-gateway/tokens/${tokenId}/revoke`, (ok) => {
      setRevokingId(null);
      if (ok) {
        notify(
          intl.formatMessage({
            id: "fhir.gateway.token.revoked",
            defaultMessage: "Jeton révoqué.",
          }),
          NotificationKinds.success,
        );
        loadTokens(clientId);
      } else {
        notify(
          intl.formatMessage({
            id: "fhir.gateway.token.revokeError",
            defaultMessage: "Échec de la révocation du jeton.",
          }),
          NotificationKinds.error,
        );
      }
    });
  };

  const onCopyToken = () => {
    if (issuedToken && navigator.clipboard) {
      navigator.clipboard.writeText(issuedToken);
      notify(
        intl.formatMessage({
          id: "fhir.gateway.token.copied",
          defaultMessage: "Jeton copié dans le presse-papiers.",
        }),
        NotificationKinds.success,
      );
    }
  };

  const statusTag = (active) =>
    active ? (
      <Tag type="green">
        <FormattedMessage
          id="fhir.gateway.status.active"
          defaultMessage="Actif"
        />
      </Tag>
    ) : (
      <Tag type="gray">
        <FormattedMessage
          id="fhir.gateway.status.inactive"
          defaultMessage="Inactif"
        />
      </Tag>
    );

  const tokenStatusTag = (active) =>
    active ? (
      <Tag type="green">
        <FormattedMessage
          id="fhir.gateway.status.active"
          defaultMessage="Actif"
        />
      </Tag>
    ) : (
      <Tag type="gray">
        <FormattedMessage
          id="fhir.gateway.token.status.revoked"
          defaultMessage="Révoqué"
        />
      </Tag>
    );

  const headers = [
    {
      key: "name",
      header: intl.formatMessage({
        id: "fhir.gateway.col.name",
        defaultMessage: "Nom",
      }),
    },
    {
      key: "description",
      header: intl.formatMessage({
        id: "fhir.gateway.col.description",
        defaultMessage: "Description",
      }),
    },
    {
      key: "status",
      header: intl.formatMessage({
        id: "fhir.gateway.col.status",
        defaultMessage: "Statut",
      }),
    },
    {
      key: "actions",
      header: intl.formatMessage({
        id: "fhir.gateway.col.actions",
        defaultMessage: "Actions",
      }),
    },
  ];

  return (
    <>
      {notificationVisible === true ? <AlertDialog /> : ""}
      <div className="adminPageContent">
        <PageBreadCrumb breadcrumbs={breadcrumbs} />
        <Grid fullWidth>
          <Column lg={16} md={8} sm={4}>
            <Section>
              <Heading>
                <FormattedMessage
                  id="fhir.gateway.title"
                  defaultMessage="Passerelle FHIR (mini-HIE)"
                />
              </Heading>
            </Section>
          </Column>
        </Grid>
        <br />
        <Grid fullWidth>
          <Column lg={16} md={8} sm={4}>
            <p>
              <FormattedMessage
                id="fhir.gateway.intro"
                defaultMessage="Déclarez les systèmes tiers autorisés à lire les ressources FHIR (mini-HIE) et gérez leurs jetons d'accès."
              />
            </p>
          </Column>
        </Grid>
        <br />
        {/* Nouveau tiers */}
        <Grid fullWidth>
          <Column lg={16} md={8} sm={4}>
            <Section>
              <Heading>
                <FormattedMessage
                  id="fhir.gateway.newClient.title"
                  defaultMessage="Nouveau tiers"
                />
              </Heading>
            </Section>
          </Column>
        </Grid>
        <br />
        <Grid fullWidth>
          <Column lg={6} md={3} sm={4}>
            <TextInput
              id="fhirGatewayClientName"
              labelText={intl.formatMessage({
                id: "fhir.gateway.newClient.name",
                defaultMessage: "Nom",
              })}
              value={newName}
              onChange={(e) => setNewName(e.target.value)}
            />
          </Column>
          <Column lg={6} md={3} sm={4}>
            <TextInput
              id="fhirGatewayClientDescription"
              labelText={intl.formatMessage({
                id: "fhir.gateway.newClient.description",
                defaultMessage: "Description",
              })}
              value={newDescription}
              onChange={(e) => setNewDescription(e.target.value)}
            />
          </Column>
          <Column lg={4} md={2} sm={4}>
            <div style={{ marginTop: "1.5rem" }}>
              {adding ? (
                <InlineLoading
                  status="active"
                  description={intl.formatMessage({
                    id: "fhir.gateway.newClient.adding",
                    defaultMessage: "Ajout…",
                  })}
                />
              ) : (
                <Button kind="primary" size="md" onClick={onAddClient}>
                  <FormattedMessage
                    id="fhir.gateway.newClient.button"
                    defaultMessage="Ajouter le tiers"
                  />
                </Button>
              )}
            </div>
          </Column>
        </Grid>
        <br />
        <Grid fullWidth>
          <Column lg={16} md={8} sm={4}>
            <Button
              kind="ghost"
              size="sm"
              renderIcon={Renew}
              onClick={loadClients}
            >
              <FormattedMessage
                id="fhir.gateway.refresh"
                defaultMessage="Actualiser"
              />
            </Button>
          </Column>
        </Grid>
        <br />
        {loading ? (
          <Loading />
        ) : (
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
                {clients.map((client) => {
                  const expanded = Object.prototype.hasOwnProperty.call(
                    tokensByClient,
                    client.id,
                  );
                  const tokens = tokensByClient[client.id] || [];
                  return (
                    <React.Fragment key={client.id}>
                      <TableRow>
                        <TableCell>{client.name}</TableCell>
                        <TableCell>{client.description}</TableCell>
                        <TableCell>{statusTag(client.active)}</TableCell>
                        <TableCell>
                          <div
                            style={{
                              display: "flex",
                              gap: "0.5rem",
                              flexWrap: "wrap",
                              alignItems: "center",
                            }}
                          >
                            {togglingId === client.id ? (
                              <InlineLoading status="active" />
                            ) : (
                              <Button
                                kind="tertiary"
                                size="sm"
                                onClick={() => onToggleActive(client)}
                              >
                                {client.active ? (
                                  <FormattedMessage
                                    id="fhir.gateway.client.deactivate"
                                    defaultMessage="Désactiver"
                                  />
                                ) : (
                                  <FormattedMessage
                                    id="fhir.gateway.client.activate"
                                    defaultMessage="Activer"
                                  />
                                )}
                              </Button>
                            )}
                            {loadingTokensId === client.id ? (
                              <InlineLoading status="active" />
                            ) : (
                              <Button
                                kind="ghost"
                                size="sm"
                                onClick={() => onToggleTokens(client.id)}
                              >
                                <FormattedMessage
                                  id="fhir.gateway.client.viewTokens"
                                  defaultMessage="Voir jetons"
                                />
                              </Button>
                            )}
                            {issuingId === client.id ? (
                              <InlineLoading status="active" />
                            ) : (
                              <Button
                                kind="ghost"
                                size="sm"
                                onClick={() => onIssueToken(client.id)}
                              >
                                <FormattedMessage
                                  id="fhir.gateway.client.issueToken"
                                  defaultMessage="Émettre un jeton"
                                />
                              </Button>
                            )}
                          </div>
                        </TableCell>
                      </TableRow>
                      {expanded ? (
                        <TableRow>
                          <TableCell colSpan={headers.length}>
                            {tokens.length === 0 ? (
                              <p>
                                <FormattedMessage
                                  id="fhir.gateway.token.none"
                                  defaultMessage="Aucun jeton pour ce tiers."
                                />
                              </p>
                            ) : (
                              <Table size="sm">
                                <TableHead>
                                  <TableRow>
                                    <TableHeader>
                                      <FormattedMessage
                                        id="fhir.gateway.token.col.id"
                                        defaultMessage="Id"
                                      />
                                    </TableHeader>
                                    <TableHeader>
                                      <FormattedMessage
                                        id="fhir.gateway.token.col.createdAt"
                                        defaultMessage="Créé le"
                                      />
                                    </TableHeader>
                                    <TableHeader>
                                      <FormattedMessage
                                        id="fhir.gateway.token.col.lastUsedAt"
                                        defaultMessage="Dernier accès"
                                      />
                                    </TableHeader>
                                    <TableHeader>
                                      <FormattedMessage
                                        id="fhir.gateway.token.col.status"
                                        defaultMessage="Statut"
                                      />
                                    </TableHeader>
                                    <TableHeader>
                                      <FormattedMessage
                                        id="fhir.gateway.token.col.actions"
                                        defaultMessage="Actions"
                                      />
                                    </TableHeader>
                                  </TableRow>
                                </TableHead>
                                <TableBody>
                                  {tokens.map((token) => (
                                    <TableRow key={token.id}>
                                      <TableCell>{token.id}</TableCell>
                                      <TableCell>{token.createdAt}</TableCell>
                                      <TableCell>{token.lastUsedAt}</TableCell>
                                      <TableCell>
                                        {tokenStatusTag(token.active)}
                                      </TableCell>
                                      <TableCell>
                                        {token.active ? (
                                          revokingId === token.id ? (
                                            <InlineLoading status="active" />
                                          ) : (
                                            <Button
                                              kind="danger--ghost"
                                              size="sm"
                                              onClick={() =>
                                                onRevokeToken(
                                                  token.id,
                                                  client.id,
                                                )
                                              }
                                            >
                                              <FormattedMessage
                                                id="fhir.gateway.token.revoke"
                                                defaultMessage="Révoquer"
                                              />
                                            </Button>
                                          )
                                        ) : (
                                          ""
                                        )}
                                      </TableCell>
                                    </TableRow>
                                  ))}
                                </TableBody>
                              </Table>
                            )}
                          </TableCell>
                        </TableRow>
                      ) : (
                        ""
                      )}
                    </React.Fragment>
                  );
                })}
              </TableBody>
            </Table>
          </TableContainer>
        )}
      </div>
      <Modal
        open={issuedToken !== null}
        modalHeading={intl.formatMessage({
          id: "fhir.gateway.token.modal.heading",
          defaultMessage: "Jeton d'accès émis",
        })}
        passiveModal
        onRequestClose={() => setIssuedToken(null)}
      >
        <p style={{ marginBottom: "1rem" }}>
          <FormattedMessage
            id="fhir.gateway.token.modal.warning"
            defaultMessage="Conservez ce jeton, il ne sera plus jamais affiché."
          />
        </p>
        <div
          style={{
            display: "flex",
            gap: "0.5rem",
            alignItems: "center",
            flexWrap: "wrap",
          }}
        >
          <code
            style={{
              wordBreak: "break-all",
              padding: "0.5rem",
              background: "var(--cds-layer, #f4f4f4)",
              display: "inline-block",
              maxWidth: "100%",
            }}
          >
            {issuedToken}
          </code>
          <Button
            kind="tertiary"
            size="sm"
            renderIcon={Copy}
            onClick={onCopyToken}
          >
            <FormattedMessage
              id="fhir.gateway.token.modal.copy"
              defaultMessage="Copier"
            />
          </Button>
        </div>
      </Modal>
    </>
  );
}

export default injectIntl(FhirGateway);

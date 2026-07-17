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
  Select,
  SelectItem,
  Checkbox,
} from "@carbon/react";
import { getFromOpenElisServer } from "../../utils/Utils.js";
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
    label: "fhir.push.title",
    link: "/MasterListsPage#FhirPushTargets",
  },
];

// Types FHIR proposables au push (aligné sur la liste native du subscriber).
const FHIR_RESOURCE_TYPES = [
  "Task",
  "Patient",
  "ServiceRequest",
  "DiagnosticReport",
  "Observation",
  "Specimen",
  "Practitioner",
  "Organization",
  "Encounter",
  "QuestionnaireResponse",
];

const emptyForm = {
  id: null,
  name: "",
  description: "",
  endpoint: "",
  resources: [],
  intervalMinutes: "",
  authType: "NONE",
  authUsername: "",
  authSecret: "",
};

function FhirPushTargets() {
  const { notificationVisible, addNotification, setNotificationVisible } =
    useContext(NotificationContext);
  const intl = useIntl();

  const [loading, setLoading] = useState(true);
  const [targets, setTargets] = useState([]);
  const [form, setForm] = useState(null);
  const [saving, setSaving] = useState(false);
  const [togglingId, setTogglingId] = useState(null);
  const [deletingId, setDeletingId] = useState(null);

  const notify = (message, kind) => {
    addNotification({
      title: intl.formatMessage({ id: "notification.title" }),
      message,
      kind,
    });
    setNotificationVisible(true);
  };

  const loadTargets = () => {
    setLoading(true);
    getFromOpenElisServer("/rest/fhir-push-targets", (res) => {
      setTargets(Array.isArray(res) ? res : []);
      setLoading(false);
    });
  };

  useEffect(() => {
    loadTargets();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const openCreate = () => setForm({ ...emptyForm });

  const openEdit = (t) => {
    setForm({
      id: t.id,
      name: t.name || "",
      description: t.description || "",
      endpoint: t.endpoint || "",
      resources: t.allowedResources
        ? t.allowedResources
            .split(",")
            .map((s) => s.trim())
            .filter((s) => s.length > 0)
        : [],
      intervalMinutes:
        t.intervalMinutes != null ? String(t.intervalMinutes) : "",
      authType: t.authType || "NONE",
      authUsername: t.authUsername || "",
      // Le secret n'est jamais renvoyé ; champ vide = inchangé à l'enregistrement.
      authSecret: "",
    });
  };

  const setFormField = (field, value) =>
    setForm((prev) => ({ ...prev, [field]: value }));

  const toggleResource = (resource, checked) =>
    setForm((prev) => ({
      ...prev,
      resources: checked
        ? [...prev.resources, resource]
        : prev.resources.filter((r) => r !== resource),
    }));

  const onSave = () => {
    if (!form.name.trim() || !form.endpoint.trim()) {
      notify(
        intl.formatMessage({
          id: "fhir.push.needNameEndpoint",
          defaultMessage: "Le nom et l'endpoint sont obligatoires.",
        }),
        NotificationKinds.warning,
      );
      return;
    }
    setSaving(true);
    const rate = parseInt(form.intervalMinutes, 10);
    const body = {
      name: form.name.trim(),
      description: form.description.trim(),
      endpoint: form.endpoint.trim(),
      allowedResources:
        form.resources.length > 0 ? form.resources.join(",") : null,
      intervalMinutes: !isNaN(rate) && rate > 0 ? rate : null,
      authType: form.authType,
      authUsername: form.authUsername.trim(),
      authSecret: form.authSecret,
    };
    const isEdit = form.id != null;
    const endPoint = isEdit
      ? `/rest/fhir-push-targets/${form.id}`
      : "/rest/fhir-push-targets";
    fetch(config.serverBaseUrl + endPoint, {
      credentials: "include",
      method: isEdit ? "PUT" : "POST",
      headers: {
        "Content-Type": "application/json",
        "X-CSRF-Token": localStorage.getItem("CSRF"),
      },
      body: JSON.stringify(body),
    })
      .then((response) => {
        setSaving(false);
        if (response.ok) {
          notify(
            intl.formatMessage({
              id: "fhir.push.saved",
              defaultMessage: "Cible enregistrée.",
            }),
            NotificationKinds.success,
          );
          setForm(null);
          loadTargets();
        } else {
          notify(
            intl.formatMessage({
              id: "fhir.push.saveError",
              defaultMessage: "Échec de l'enregistrement de la cible.",
            }),
            NotificationKinds.error,
          );
        }
      })
      .catch(() => {
        setSaving(false);
        notify(
          intl.formatMessage({
            id: "fhir.push.saveError",
            defaultMessage: "Échec de l'enregistrement de la cible.",
          }),
          NotificationKinds.error,
        );
      });
  };

  // POST/DELETE simple attendant un 200 sans corps.
  const requestExpectOk = (endPoint, method, onDone) => {
    fetch(config.serverBaseUrl + endPoint, {
      credentials: "include",
      method,
      headers: {
        "Content-Type": "application/json",
        "X-CSRF-Token": localStorage.getItem("CSRF"),
      },
      body: JSON.stringify({}),
    })
      .then((response) => onDone(response.ok))
      .catch(() => onDone(false));
  };

  const onToggleActive = (t) => {
    setTogglingId(t.id);
    const next = !t.active;
    requestExpectOk(
      `/rest/fhir-push-targets/${t.id}/active?active=${next}`,
      "POST",
      (ok) => {
        setTogglingId(null);
        if (ok) {
          loadTargets();
        } else {
          notify(
            intl.formatMessage({
              id: "fhir.push.toggleError",
              defaultMessage: "Échec de la modification.",
            }),
            NotificationKinds.error,
          );
        }
      },
    );
  };

  const onDelete = (t) => {
    setDeletingId(t.id);
    // POST /{id}/delete (le conteneur n'autorise que GET/POST/PUT).
    requestExpectOk(`/rest/fhir-push-targets/${t.id}/delete`, "POST", (ok) => {
      setDeletingId(null);
      if (ok) {
        notify(
          intl.formatMessage({
            id: "fhir.push.deleted",
            defaultMessage: "Cible supprimée.",
          }),
          NotificationKinds.success,
        );
        loadTargets();
      } else {
        notify(
          intl.formatMessage({
            id: "fhir.push.deleteError",
            defaultMessage: "Échec de la suppression.",
          }),
          NotificationKinds.error,
        );
      }
    });
  };

  const statusTag = (active) =>
    active ? (
      <Tag type="green">
        <FormattedMessage id="fhir.push.active" defaultMessage="Actif" />
      </Tag>
    ) : (
      <Tag type="gray">
        <FormattedMessage id="fhir.push.inactive" defaultMessage="Inactif" />
      </Tag>
    );

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
                  id="fhir.push.title"
                  defaultMessage="Push FHIR distant (cibles)"
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
                id="fhir.push.intro"
                defaultMessage="Déclarez les serveurs FHIR distants vers lesquels OpenELIS pousse ses ressources (mini-HIE, sens sortant), avec fréquence, ressources et authentification."
              />
            </p>
            <Button kind="primary" size="sm" onClick={openCreate}>
              <FormattedMessage
                id="fhir.push.new"
                defaultMessage="Nouvelle cible"
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
                  <TableHeader>
                    <FormattedMessage
                      id="fhir.push.col.name"
                      defaultMessage="Nom"
                    />
                  </TableHeader>
                  <TableHeader>
                    <FormattedMessage
                      id="fhir.push.col.endpoint"
                      defaultMessage="Endpoint"
                    />
                  </TableHeader>
                  <TableHeader>
                    <FormattedMessage
                      id="fhir.push.col.interval"
                      defaultMessage="Fréquence (min)"
                    />
                  </TableHeader>
                  <TableHeader>
                    <FormattedMessage
                      id="fhir.push.col.lastSuccess"
                      defaultMessage="Dernier succès"
                    />
                  </TableHeader>
                  <TableHeader>
                    <FormattedMessage
                      id="fhir.push.col.status"
                      defaultMessage="Statut"
                    />
                  </TableHeader>
                  <TableHeader>
                    <FormattedMessage
                      id="fhir.push.col.actions"
                      defaultMessage="Actions"
                    />
                  </TableHeader>
                </TableRow>
              </TableHead>
              <TableBody>
                {targets.map((t) => (
                  <TableRow key={t.id}>
                    <TableCell>{t.name}</TableCell>
                    <TableCell>{t.endpoint}</TableCell>
                    <TableCell>{t.intervalMinutes}</TableCell>
                    <TableCell>{t.lastSuccess || "—"}</TableCell>
                    <TableCell>{statusTag(t.active)}</TableCell>
                    <TableCell>
                      <div
                        style={{
                          display: "flex",
                          gap: "0.5rem",
                          flexWrap: "wrap",
                        }}
                      >
                        <Button
                          kind="ghost"
                          size="sm"
                          onClick={() => openEdit(t)}
                        >
                          <FormattedMessage
                            id="fhir.push.edit"
                            defaultMessage="Modifier"
                          />
                        </Button>
                        {togglingId === t.id ? (
                          <InlineLoading status="active" />
                        ) : (
                          <Button
                            kind="tertiary"
                            size="sm"
                            onClick={() => onToggleActive(t)}
                          >
                            {t.active ? (
                              <FormattedMessage
                                id="fhir.push.deactivate"
                                defaultMessage="Désactiver"
                              />
                            ) : (
                              <FormattedMessage
                                id="fhir.push.activate"
                                defaultMessage="Activer"
                              />
                            )}
                          </Button>
                        )}
                        {deletingId === t.id ? (
                          <InlineLoading status="active" />
                        ) : (
                          <Button
                            kind="danger--ghost"
                            size="sm"
                            onClick={() => onDelete(t)}
                          >
                            <FormattedMessage
                              id="fhir.push.delete"
                              defaultMessage="Supprimer"
                            />
                          </Button>
                        )}
                      </div>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        )}
      </div>

      {/* Création / édition d'une cible */}
      <Modal
        open={form !== null}
        modalHeading={intl.formatMessage({
          id: "fhir.push.modal.heading",
          defaultMessage: "Cible de push FHIR",
        })}
        primaryButtonText={intl.formatMessage({
          id: "fhir.push.save",
          defaultMessage: "Enregistrer",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "fhir.push.cancel",
          defaultMessage: "Annuler",
        })}
        primaryButtonDisabled={saving}
        onRequestSubmit={onSave}
        onRequestClose={() => setForm(null)}
      >
        {form && (
          <>
            <TextInput
              id="fhirPushName"
              labelText={intl.formatMessage({
                id: "fhir.push.field.name",
                defaultMessage: "Nom",
              })}
              value={form.name}
              onChange={(e) => setFormField("name", e.target.value)}
            />
            <TextInput
              id="fhirPushDescription"
              labelText={intl.formatMessage({
                id: "fhir.push.field.description",
                defaultMessage: "Description",
              })}
              value={form.description}
              onChange={(e) => setFormField("description", e.target.value)}
            />
            <TextInput
              id="fhirPushEndpoint"
              labelText={intl.formatMessage({
                id: "fhir.push.field.endpoint",
                defaultMessage: "Endpoint FHIR (URL)",
              })}
              value={form.endpoint}
              onChange={(e) => setFormField("endpoint", e.target.value)}
            />
            <TextInput
              id="fhirPushInterval"
              type="number"
              min="0"
              labelText={intl.formatMessage({
                id: "fhir.push.field.interval",
                defaultMessage: "Fréquence en minutes (vide = défaut 5)",
              })}
              value={form.intervalMinutes}
              onChange={(e) => setFormField("intervalMinutes", e.target.value)}
            />
            <p style={{ margin: "1rem 0 0.5rem" }}>
              <FormattedMessage
                id="fhir.push.field.resources"
                defaultMessage="Ressources poussées (aucune cochée = liste par défaut) :"
              />
            </p>
            <div style={{ marginBottom: "1rem" }}>
              {FHIR_RESOURCE_TYPES.map((resource) => (
                <Checkbox
                  key={resource}
                  id={`push-res-${resource}`}
                  labelText={resource}
                  checked={form.resources.includes(resource)}
                  onChange={(e) => toggleResource(resource, e.target.checked)}
                />
              ))}
            </div>
            <Select
              id="fhirPushAuthType"
              labelText={intl.formatMessage({
                id: "fhir.push.field.authType",
                defaultMessage: "Authentification",
              })}
              value={form.authType}
              onChange={(e) => setFormField("authType", e.target.value)}
            >
              <SelectItem value="NONE" text="Aucune" />
              <SelectItem
                value="BASIC"
                text="Basic (utilisateur/mot de passe)"
              />
              <SelectItem value="TOKEN" text="Token (Bearer)" />
            </Select>
            {form.authType === "BASIC" && (
              <TextInput
                id="fhirPushAuthUser"
                labelText={intl.formatMessage({
                  id: "fhir.push.field.authUsername",
                  defaultMessage: "Utilisateur",
                })}
                value={form.authUsername}
                onChange={(e) => setFormField("authUsername", e.target.value)}
              />
            )}
            {(form.authType === "BASIC" || form.authType === "TOKEN") && (
              <TextInput.PasswordInput
                id="fhirPushAuthSecret"
                labelText={intl.formatMessage({
                  id: "fhir.push.field.authSecret",
                  defaultMessage:
                    "Secret (mot de passe / jeton ; vide = inchangé)",
                })}
                value={form.authSecret}
                onChange={(e) => setFormField("authSecret", e.target.value)}
              />
            )}
          </>
        )}
      </Modal>
    </>
  );
}

export default injectIntl(FhirPushTargets);

import React, { useContext, useState, useEffect } from "react";
import {
  Grid,
  Column,
  Section,
  Heading,
  Loading,
  Button,
  Toggle,
  TextInput,
  Checkbox,
  InlineNotification,
} from "@carbon/react";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../utils/Utils.js";
import { NotificationContext } from "../../layout/Layout.js";
import {
  AlertDialog,
  NotificationKinds,
} from "../../common/CustomNotification.js";
import { FormattedMessage, useIntl } from "react-intl";
import PageBreadCrumb from "../../common/PageBreadCrumb.js";

const breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
  {
    label: "consolidatedSync.title",
    link: "/MasterListsPage#ConsolidatedSyncConfig",
  },
];

// Valeurs par défaut du formulaire (le mot de passe n'est jamais pré-rempli).
const emptyForm = {
  syncEnabled: false,
  pullEnabled: false,
  pushEnabled: false,
  allowHttp: false,
  apiUrl: "",
  authUrl: "",
  username: "",
  password: "",
  labUuid: "",
};

function ConsolidatedSyncConfig() {
  const intl = useIntl();
  const { notificationVisible, addNotification, setNotificationVisible } =
    useContext(NotificationContext);

  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [form, setForm] = useState(emptyForm);
  // Indique qu'un mot de passe est déjà défini côté serveur (jamais sa valeur).
  const [hasPassword, setHasPassword] = useState(false);

  useEffect(() => {
    getFromOpenElisServer("/rest/consolidated-sync-config", (data) => {
      if (data) {
        setForm({
          syncEnabled: !!data.syncEnabled,
          pullEnabled: !!data.pullEnabled,
          pushEnabled: !!data.pushEnabled,
          allowHttp: !!data.allowHttp,
          apiUrl: data.apiUrl || "",
          authUrl: data.authUrl || "",
          username: data.username || "",
          password: "",
          labUuid: data.labUuid || "",
        });
        setHasPassword(!!data.hasPassword);
      }
      setLoading(false);
    });
  }, []);

  const setField = (name, value) => {
    setForm((prev) => ({ ...prev, [name]: value }));
  };

  const save = () => {
    setSaving(true);
    postToOpenElisServerJsonResponse(
      "/rest/consolidated-sync-config",
      JSON.stringify(form),
      (res) => {
        setSaving(false);
        if (res && res.saved) {
          addNotification({
            kind: NotificationKinds.success,
            title: intl.formatMessage({ id: "notification.title" }),
            message: intl.formatMessage({ id: "consolidatedSync.saved" }),
          });
          // Un mot de passe fourni devient « défini » ; on vide le champ saisi.
          if (form.password) {
            setHasPassword(true);
          }
          setField("password", "");
        } else {
          addNotification({
            kind: NotificationKinds.error,
            title: intl.formatMessage({ id: "notification.title" }),
            message: intl.formatMessage({ id: "consolidatedSync.saveError" }),
          });
        }
        setNotificationVisible(true);
      },
    );
  };

  if (loading) {
    return <Loading />;
  }

  return (
    <>
      {notificationVisible === true ? <AlertDialog /> : ""}
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <Section>
            <Heading>
              <FormattedMessage id="consolidatedSync.title" />
            </Heading>
          </Section>
        </Column>
      </Grid>
      <div className="orderLegendBody">
        <Grid fullWidth={true}>
          <Column lg={16} md={8} sm={4}>
            <InlineNotification
              kind="info"
              lowContrast
              hideCloseButton
              title={intl.formatMessage({ id: "consolidatedSync.title" })}
              subtitle={intl.formatMessage({
                id: "consolidatedSync.intervalNote",
              })}
            />
          </Column>

          {/* Activation des flux */}
          <Column lg={16} md={8} sm={4}>
            <br />
            <Toggle
              id="syncEnabled"
              labelText={intl.formatMessage({
                id: "consolidatedSync.syncEnabled",
              })}
              toggled={form.syncEnabled}
              onToggle={(v) => setField("syncEnabled", v)}
            />
          </Column>
          <Column lg={16} md={8} sm={4}>
            <Toggle
              id="pullEnabled"
              labelText={intl.formatMessage({
                id: "consolidatedSync.pullEnabled",
              })}
              toggled={form.pullEnabled}
              onToggle={(v) => setField("pullEnabled", v)}
            />
          </Column>
          <Column lg={16} md={8} sm={4}>
            <Toggle
              id="pushEnabled"
              labelText={intl.formatMessage({
                id: "consolidatedSync.pushEnabled",
              })}
              toggled={form.pushEnabled}
              onToggle={(v) => setField("pushEnabled", v)}
            />
          </Column>

          {/* Cible + identifiants */}
          <Column lg={8} md={8} sm={4}>
            <br />
            <TextInput
              id="apiUrl"
              labelText={intl.formatMessage({
                id: "consolidatedSync.apiUrl",
              })}
              value={form.apiUrl}
              onChange={(e) => setField("apiUrl", e.target.value)}
            />
          </Column>
          <Column lg={8} md={8} sm={4}>
            <br />
            <TextInput
              id="authUrl"
              labelText={intl.formatMessage({
                id: "consolidatedSync.authUrl",
              })}
              value={form.authUrl}
              onChange={(e) => setField("authUrl", e.target.value)}
            />
          </Column>
          <Column lg={8} md={8} sm={4}>
            <br />
            <TextInput
              id="labUuid"
              labelText={intl.formatMessage({
                id: "consolidatedSync.labUuid",
              })}
              value={form.labUuid}
              onChange={(e) => setField("labUuid", e.target.value)}
            />
          </Column>
          <Column lg={8} md={8} sm={4}>
            <br />
            <TextInput
              id="username"
              labelText={intl.formatMessage({
                id: "consolidatedSync.username",
              })}
              value={form.username}
              onChange={(e) => setField("username", e.target.value)}
            />
          </Column>
          <Column lg={8} md={8} sm={4}>
            <br />
            <TextInput.PasswordInput
              id="password"
              labelText={intl.formatMessage({
                id: "consolidatedSync.password",
              })}
              helperText={
                hasPassword
                  ? intl.formatMessage({
                      id: "consolidatedSync.password.unchanged",
                    })
                  : ""
              }
              value={form.password}
              onChange={(e) => setField("password", e.target.value)}
            />
          </Column>
          <Column lg={16} md={8} sm={4}>
            <br />
            <Checkbox
              id="allowHttp"
              labelText={intl.formatMessage({
                id: "consolidatedSync.allowHttp",
              })}
              checked={form.allowHttp}
              onChange={(_, { checked }) => setField("allowHttp", checked)}
            />
          </Column>

          <Column lg={16} md={8} sm={4}>
            <br />
            <Button onClick={save} disabled={saving}>
              <FormattedMessage id="label.button.save" />
            </Button>
          </Column>
        </Grid>
      </div>
    </>
  );
}

export default ConsolidatedSyncConfig;

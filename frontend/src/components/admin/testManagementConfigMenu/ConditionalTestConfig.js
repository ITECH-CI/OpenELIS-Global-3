import { useContext, useEffect, useMemo, useState } from "react";
import {
  Button,
  Column,
  ComboBox,
  Grid,
  Heading,
  InlineLoading,
  Section,
  Tag,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../utils/Utils.js";
import { NotificationContext } from "../../layout/Layout.js";
import {
  AlertDialog,
  NotificationKinds,
} from "../../common/CustomNotification.js";
import PageBreadCrumb from "../../common/PageBreadCrumb.js";

const breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
  {
    label: "master.lists.page.test.management",
    link: "/MasterListsPage#testManagementConfigMenu",
  },
  {
    label: "configuration.conditional.test",
    link: "/MasterListsPage#ConditionalTestConfig",
  },
];

const ConditionalTestConfig = () => {
  const intl = useIntl();
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const [tests, setTests] = useState([]);
  const [triggerValues, setTriggerValues] = useState([]);
  const [parentId, setParentId] = useState("");
  const [triggerValue, setTriggerValue] = useState("");
  const [childId, setChildId] = useState("");
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  // Cache parentTestId -> Map(dictId -> label) to resolve trigger labels in the mapping list
  const [triggerLabels, setTriggerLabels] = useState({});

  const loadTests = () => {
    setLoading(true);
    getFromOpenElisServer(
      "/rest/conditional-test/tests",
      (res) => {
        setTests(Array.isArray(res) ? res : []);
        setLoading(false);
      },
      () => setLoading(false),
    );
  };

  useEffect(() => {
    loadTests();
  }, []);

  useEffect(() => {
    if (!parentId) {
      setTriggerValues([]);
      setTriggerValue("");
      return;
    }
    getFromOpenElisServer(
      `/rest/conditional-test/trigger-values/${parentId}`,
      (res) => setTriggerValues(Array.isArray(res) ? res : []),
    );
  }, [parentId]);

  const existingMappings = useMemo(
    () => tests.filter((t) => t.parentTestId),
    [tests],
  );

  // For each unique parentTestId in existing mappings, load its trigger values once
  // so we can render the label instead of the raw dictionary id.
  useEffect(() => {
    const parentIdsToLoad = Array.from(
      new Set(existingMappings.map((m) => m.parentTestId).filter(Boolean)),
    ).filter((pid) => !triggerLabels[pid]);
    if (parentIdsToLoad.length === 0) return;
    parentIdsToLoad.forEach((pid) => {
      getFromOpenElisServer(
        `/rest/conditional-test/trigger-values/${pid}`,
        (res) => {
          if (!Array.isArray(res)) return;
          const map = {};
          res.forEach((tv) => {
            map[tv.id] = tv.label;
          });
          setTriggerLabels((prev) => ({ ...prev, [pid]: map }));
        },
      );
    });
  }, [existingMappings, triggerLabels]);

  const handleSave = () => {
    if (!parentId || !triggerValue || !childId) {
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({
          id: "conditional.test.missingFields",
          defaultMessage: "Parent, valeur déclenchante et enfant sont requis.",
        }),
      });
      setNotificationVisible(true);
      return;
    }
    if (parentId === childId) {
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({
          id: "conditional.test.sameParentChild",
          defaultMessage:
            "Le test parent et le test enfant doivent être différents.",
        }),
      });
      setNotificationVisible(true);
      return;
    }
    setSaving(true);
    postToOpenElisServerJsonResponse(
      "/rest/conditional-test/mapping",
      JSON.stringify({
        parentTestId: parentId,
        childTestId: childId,
        triggerValue,
      }),
      (res) => {
        setSaving(false);
        if (res && res.status === "ok") {
          addNotification({
            kind: NotificationKinds.success,
            title: intl.formatMessage({ id: "notification.title" }),
            message: intl.formatMessage({
              id: "notification.user.post.save.success",
            }),
          });
          setParentId("");
          setTriggerValue("");
          setChildId("");
          loadTests();
        } else {
          addNotification({
            kind: NotificationKinds.error,
            title: intl.formatMessage({ id: "notification.title" }),
            message:
              (res && res.error) ||
              intl.formatMessage({ id: "server.error.msg" }),
          });
        }
        setNotificationVisible(true);
      },
    );
  };

  const handleClear = (mapping) => {
    setSaving(true);
    postToOpenElisServerJsonResponse(
      "/rest/conditional-test/mapping/clear",
      JSON.stringify({ childTestId: mapping.id }),
      (res) => {
        setSaving(false);
        if (res && res.status === "ok") {
          addNotification({
            kind: NotificationKinds.success,
            title: intl.formatMessage({ id: "notification.title" }),
            message: intl.formatMessage({
              id: "notification.user.post.save.success",
            }),
          });
          loadTests();
        } else {
          addNotification({
            kind: NotificationKinds.error,
            title: intl.formatMessage({ id: "notification.title" }),
            message:
              (res && res.error) ||
              intl.formatMessage({ id: "server.error.msg" }),
          });
        }
        setNotificationVisible(true);
      },
    );
  };

  const renderTestComboBox = (id, label, value, onChange) => (
    <ComboBox
      id={id}
      items={tests}
      itemToString={(t) => (t ? `${t.name} (#${t.id})` : "")}
      shouldFilterItem={({ item, inputValue }) => {
        if (!inputValue) return true;
        return (item.name || "")
          .toLowerCase()
          .includes(inputValue.toLowerCase());
      }}
      selectedItem={tests.find((t) => t.id === value) || null}
      onChange={({ selectedItem }) =>
        onChange(selectedItem ? selectedItem.id : "")
      }
      titleText={label}
      placeholder={intl.formatMessage({
        id: "conditional.test.searchPlaceholder",
        defaultMessage: "Rechercher un test...",
      })}
    />
  );

  return (
    <>
      {notificationVisible === true ? <AlertDialog /> : ""}
      <div className="adminPageContent">
        <PageBreadCrumb breadcrumbs={breadcrumbs} />
        <Grid fullWidth={true}>
          <Column lg={16} md={8} sm={4}>
            <Section>
              <Heading>
                <FormattedMessage
                  id="configuration.conditional.test"
                  defaultMessage="Configurer un test conditionnel"
                />
              </Heading>
            </Section>
          </Column>
        </Grid>
        <br />
        <hr />
        <br />

        {loading ? (
          <InlineLoading
            description={intl.formatMessage({ id: "loading.message" })}
          />
        ) : (
          <>
            <Grid fullWidth={true}>
              <Column lg={5} md={4} sm={4}>
                {renderTestComboBox(
                  "parentTestId",
                  intl.formatMessage({
                    id: "conditional.test.parent",
                    defaultMessage: "Test parent (déclencheur)",
                  }),
                  parentId,
                  setParentId,
                )}
              </Column>
              <Column lg={5} md={4} sm={4}>
                <ComboBox
                  id="triggerValue"
                  items={triggerValues}
                  itemToString={(t) => (t ? t.label : "")}
                  shouldFilterItem={({ item, inputValue }) => {
                    if (!inputValue) return true;
                    return (item.label || "")
                      .toLowerCase()
                      .includes(inputValue.toLowerCase());
                  }}
                  selectedItem={
                    triggerValues.find((t) => t.id === triggerValue) || null
                  }
                  onChange={({ selectedItem }) =>
                    setTriggerValue(selectedItem ? selectedItem.id : "")
                  }
                  titleText={intl.formatMessage({
                    id: "conditional.test.trigger",
                    defaultMessage: "Valeur déclenchante",
                  })}
                  disabled={!parentId}
                  placeholder={
                    parentId
                      ? intl.formatMessage({
                          id: "conditional.test.trigger.select",
                          defaultMessage: "Choisir la valeur...",
                        })
                      : intl.formatMessage({
                          id: "conditional.test.trigger.pickParentFirst",
                          defaultMessage: "Choisissez d'abord le test parent",
                        })
                  }
                />
              </Column>
              <Column lg={5} md={4} sm={4}>
                {renderTestComboBox(
                  "childTestId",
                  intl.formatMessage({
                    id: "conditional.test.child",
                    defaultMessage: "Test enfant (à afficher)",
                  }),
                  childId,
                  setChildId,
                )}
              </Column>
              <Column lg={1} md={8} sm={4}>
                <div style={{ marginTop: "1.5rem" }}>
                  <Button onClick={handleSave} disabled={saving}>
                    <FormattedMessage
                      id="label.button.save"
                      defaultMessage="Enregistrer"
                    />
                  </Button>
                </div>
              </Column>
            </Grid>
            <br />
            <hr />
            <br />
            <Grid fullWidth={true}>
              <Column lg={16} md={8} sm={4}>
                <Section>
                  <Heading>
                    <FormattedMessage
                      id="conditional.test.existingMappings"
                      defaultMessage="Configurations existantes"
                    />
                  </Heading>
                </Section>
              </Column>
            </Grid>
            <br />
            {existingMappings.length === 0 ? (
              <p>
                <FormattedMessage
                  id="conditional.test.noMappings"
                  defaultMessage="Aucune configuration enregistrée."
                />
              </p>
            ) : (
              existingMappings.map((m) => {
                const parent = tests.find((t) => t.id === m.parentTestId);
                const triggerRaw = parent?.parentTriggerValue;
                const triggerLabel =
                  (triggerLabels[m.parentTestId] &&
                    triggerLabels[m.parentTestId][triggerRaw]) ||
                  triggerRaw ||
                  "?";
                return (
                  <div
                    key={m.id}
                    style={{
                      border: "1px solid #e0e0e0",
                      padding: "0.75rem 1rem",
                      marginBottom: "0.5rem",
                      borderRadius: "4px",
                      display: "flex",
                      alignItems: "center",
                      justifyContent: "space-between",
                    }}
                  >
                    <div>
                      <Tag type="blue">
                        {parent ? parent.name : `#${m.parentTestId}`}
                      </Tag>
                      <span style={{ margin: "0 0.5rem" }}>=</span>
                      <Tag type="green">{triggerLabel}</Tag>
                      <span style={{ margin: "0 0.5rem" }}>→</span>
                      <Tag type="purple">{m.name}</Tag>
                    </div>
                    <Button
                      kind="danger--tertiary"
                      onClick={() => handleClear(m)}
                      disabled={saving}
                    >
                      <FormattedMessage
                        id="label.button.delete"
                        defaultMessage="Supprimer"
                      />
                    </Button>
                  </div>
                );
              })
            )}
          </>
        )}
      </div>
    </>
  );
};

export default ConditionalTestConfig;

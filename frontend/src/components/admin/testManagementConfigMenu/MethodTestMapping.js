import { useContext, useEffect, useMemo, useState } from "react";
import {
  Button,
  Checkbox,
  Column,
  ComboBox,
  ContentSwitcher,
  Grid,
  Heading,
  InlineLoading,
  Section,
  Switch,
  TextInput,
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
    label: "configuration.methodTestMapping",
    link: "/MasterListsPage#MethodTestMapping",
  },
];

const MethodTestMapping = () => {
  const intl = useIntl();
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  // mode = "by-test" (pick a test then check methods) or "by-method"
  const [mode, setMode] = useState("by-test");
  const [tests, setTests] = useState([]);
  const [methods, setMethods] = useState([]);
  const [selectedTestId, setSelectedTestId] = useState("");
  const [selectedMethodId, setSelectedMethodId] = useState("");
  const [checkedIds, setCheckedIds] = useState(new Set());
  const [filter, setFilter] = useState("");
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    setLoading(true);
    let pending = 2;
    const done = () => {
      pending -= 1;
      if (pending === 0) setLoading(false);
    };
    getFromOpenElisServer(
      "/rest/method-test-map/tests",
      (res) => {
        setTests(Array.isArray(res) ? res : []);
        done();
      },
      () => done(),
    );
    getFromOpenElisServer(
      "/rest/method-test-map/methods",
      (res) => {
        setMethods(Array.isArray(res) ? res : []);
        done();
      },
      () => done(),
    );
  }, []);

  // Load current assignment when selection changes
  useEffect(() => {
    if (mode === "by-test") {
      if (!selectedTestId) {
        setCheckedIds(new Set());
        return;
      }
      getFromOpenElisServer(
        `/rest/method-test-map/methods-for-test/${selectedTestId}`,
        (res) => setCheckedIds(new Set(Array.isArray(res) ? res : [])),
      );
    } else {
      if (!selectedMethodId) {
        setCheckedIds(new Set());
        return;
      }
      getFromOpenElisServer(
        `/rest/method-test-map/tests-for-method/${selectedMethodId}`,
        (res) => setCheckedIds(new Set(Array.isArray(res) ? res : [])),
      );
    }
  }, [mode, selectedTestId, selectedMethodId]);

  const items = mode === "by-test" ? methods : tests;
  const filteredItems = useMemo(() => {
    if (!filter) return items;
    const q = filter.toLowerCase();
    return items.filter((it) => (it.name || "").toLowerCase().includes(q));
  }, [items, filter]);

  const toggle = (id) => {
    const next = new Set(checkedIds);
    if (next.has(id)) next.delete(id);
    else next.add(id);
    setCheckedIds(next);
  };

  const handleSave = () => {
    if (mode === "by-test" && !selectedTestId) return;
    if (mode === "by-method" && !selectedMethodId) return;
    setSaving(true);
    const url =
      mode === "by-test"
        ? "/rest/method-test-map/save-for-test"
        : "/rest/method-test-map/save-for-method";
    const body =
      mode === "by-test"
        ? { testId: selectedTestId, methodIds: Array.from(checkedIds) }
        : { methodId: selectedMethodId, testIds: Array.from(checkedIds) };
    postToOpenElisServerJsonResponse(url, JSON.stringify(body), (res) => {
      setSaving(false);
      if (res && res.status === "ok") {
        addNotification({
          kind: NotificationKinds.success,
          title: intl.formatMessage({ id: "notification.title" }),
          message: intl.formatMessage({
            id: "notification.user.post.save.success",
          }),
        });
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
    });
  };

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
                  id="configuration.methodTestMapping"
                  defaultMessage="Assigner des méthodes aux tests"
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
              <Column lg={16} md={8} sm={4}>
                <ContentSwitcher
                  selectedIndex={mode === "by-test" ? 0 : 1}
                  onChange={({ index }) =>
                    setMode(index === 0 ? "by-test" : "by-method")
                  }
                >
                  <Switch
                    name="by-test"
                    text={intl.formatMessage({
                      id: "methodTestMapping.byTest",
                      defaultMessage: "Par test (choisir les méthodes)",
                    })}
                  />
                  <Switch
                    name="by-method"
                    text={intl.formatMessage({
                      id: "methodTestMapping.byMethod",
                      defaultMessage: "Par méthode (choisir les tests)",
                    })}
                  />
                </ContentSwitcher>
              </Column>
            </Grid>
            <br />
            <Grid fullWidth={true}>
              <Column lg={8} md={4} sm={4}>
                {mode === "by-test" ? (
                  <ComboBox
                    id="testPicker"
                    items={tests}
                    itemToString={(t) => (t ? t.name : "")}
                    shouldFilterItem={({ item, inputValue }) => {
                      if (!inputValue) return true;
                      return (item.name || "")
                        .toLowerCase()
                        .includes(inputValue.toLowerCase());
                    }}
                    selectedItem={
                      tests.find((t) => t.id === selectedTestId) || null
                    }
                    onChange={({ selectedItem }) =>
                      setSelectedTestId(selectedItem ? selectedItem.id : "")
                    }
                    titleText={intl.formatMessage({
                      id: "methodTestMapping.pickTest",
                      defaultMessage: "Sélectionner un test",
                    })}
                  />
                ) : (
                  <ComboBox
                    id="methodPicker"
                    items={methods}
                    itemToString={(m) => (m ? m.name : "")}
                    shouldFilterItem={({ item, inputValue }) => {
                      if (!inputValue) return true;
                      return (item.name || "")
                        .toLowerCase()
                        .includes(inputValue.toLowerCase());
                    }}
                    selectedItem={
                      methods.find((m) => m.id === selectedMethodId) || null
                    }
                    onChange={({ selectedItem }) =>
                      setSelectedMethodId(selectedItem ? selectedItem.id : "")
                    }
                    titleText={intl.formatMessage({
                      id: "methodTestMapping.pickMethod",
                      defaultMessage: "Sélectionner une méthode",
                    })}
                  />
                )}
              </Column>
              <Column lg={8} md={4} sm={4}>
                <TextInput
                  id="filter"
                  labelText={intl.formatMessage({
                    id: "methodTestMapping.filter",
                    defaultMessage: "Filtrer la liste",
                  })}
                  value={filter}
                  onChange={(e) => setFilter(e.target.value)}
                  placeholder={intl.formatMessage({
                    id: "methodTestMapping.filterPlaceholder",
                    defaultMessage: "Tapez pour filtrer...",
                  })}
                />
              </Column>
            </Grid>
            <br />
            <Grid fullWidth={true}>
              <Column lg={16} md={8} sm={4}>
                <Section>
                  <Heading>
                    {mode === "by-test" ? (
                      <FormattedMessage
                        id="methodTestMapping.methodsAvailable"
                        defaultMessage="Méthodes disponibles"
                      />
                    ) : (
                      <FormattedMessage
                        id="methodTestMapping.testsAvailable"
                        defaultMessage="Tests disponibles"
                      />
                    )}
                  </Heading>
                </Section>
              </Column>
            </Grid>
            <div
              style={{
                maxHeight: "400px",
                overflow: "auto",
                border: "1px solid #e0e0e0",
                padding: "0.5rem 1rem",
                marginTop: "0.5rem",
              }}
            >
              {filteredItems.length === 0 ? (
                <p>
                  <FormattedMessage
                    id="methodTestMapping.empty"
                    defaultMessage="Aucun élément."
                  />
                </p>
              ) : (
                filteredItems.map((it) => {
                  const isInactive = it.isActive && it.isActive !== "Y";
                  const labelNode = (
                    <span>
                      {it.name}
                      {isInactive && (
                        <span
                          style={{
                            marginLeft: "0.5rem",
                            fontSize: "0.75rem",
                            color: "#a8071a",
                            fontStyle: "italic",
                          }}
                        >
                          (inactive)
                        </span>
                      )}
                    </span>
                  );
                  return (
                    <Checkbox
                      key={it.id}
                      id={`mtm-${it.id}`}
                      labelText={labelNode}
                      checked={checkedIds.has(it.id)}
                      onChange={() => toggle(it.id)}
                      disabled={
                        (mode === "by-test" && !selectedTestId) ||
                        (mode === "by-method" && !selectedMethodId)
                      }
                    />
                  );
                })
              )}
            </div>
            <br />
            <Grid fullWidth={true}>
              <Column lg={16} md={8} sm={4}>
                <Button
                  onClick={handleSave}
                  disabled={
                    saving ||
                    (mode === "by-test" && !selectedTestId) ||
                    (mode === "by-method" && !selectedMethodId)
                  }
                >
                  <FormattedMessage
                    id="label.button.save"
                    defaultMessage="Enregistrer"
                  />
                </Button>
              </Column>
            </Grid>
          </>
        )}
      </div>
    </>
  );
};

export default MethodTestMapping;

import React, {
  useContext,
  useState,
  useEffect,
  useRef,
  useMemo,
} from "react";
import {
  Heading,
  Loading,
  Grid,
  Column,
  Section,
  ClickableTile,
  Modal,
  FilterableMultiSelect,
  Tag,
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
import { FormattedMessage, injectIntl, useIntl } from "react-intl";
import PageBreadCrumb from "../../common/PageBreadCrumb.js";

let breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
  {
    label: "master.lists.page.test.management",
    link: "/MasterListsPage#testManagementConfigMenu",
  },
  {
    label: "configuration.sampleType.manage",
    link: "/MasterListsPage#SampleTypeManagement",
  },
  {
    label: "configuration.panel.assign",
    link: "/MasterListsPage#SampleTypeTestAssign",
  },
];

function SampleTypeTestAssign() {
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const intl = useIntl();
  const [isLoading, setIsLoading] = useState(false);
  const [sampleTypeTestAssignModal, setSampleTypeTestAssignModal] =
    useState(false);
  const [sampleTypeTestAssign, setSampleTypeTestAssign] = useState({});
  // Test en cours d'édition + types d'échantillon sélectionnés (multi).
  const [selectedTest, setSelectedTest] = useState({ id: "", value: "" });
  const [selectedSampleTypes, setSelectedSampleTypes] = useState([]);
  // Clé incrémentée à chaque ouverture -> force le remount du multi-select pour
  // que sa présélection (initialSelectedItems) soit ré-appliquée à chaque fois.
  const [modalKey, setModalKey] = useState(0);
  const componentMounted = useRef(false);

  const handleSampleTypeTestAssignList = (res) => {
    if (!res) {
      setIsLoading(true);
    } else {
      setSampleTypeTestAssign(res);
    }
  };

  // Options du multi-select : liste stable de tous les types d'échantillon actifs.
  // useMemo -> même RÉFÉRENCE d'objets entre les renders : indispensable pour que
  // Carbon FilterableMultiSelect reconnaisse les items présélectionnés.
  const sampleTypeOptions = useMemo(
    () => sampleTypeTestAssign?.sampleTypeList?.filter((item) => item.id) || [],
    [sampleTypeTestAssign],
  );

  const openAssignModal = (test) => {
    setSelectedTest({ id: test.id, value: test.value });
    setSelectedSampleTypes([]);
    // Récupérer les types déjà associés à ce test pour les présélectionner.
    getFromOpenElisServer(
      `/rest/SampleTypeTestAssign/test/${test.id}`,
      (assignedIds) => {
        const ids = (assignedIds || []).map((v) => String(v));
        // Retourner les MÊMES objets que sampleTypeOptions (comparaison par id,
        // types normalisés en String).
        const preselected = sampleTypeOptions.filter((opt) =>
          ids.includes(String(opt.id)),
        );
        setSelectedSampleTypes(preselected);
        setModalKey((k) => k + 1);
        setSampleTypeTestAssignModal(true);
      },
    );
  };

  const handleSaveAssignments = () => {
    if (!selectedTest.id) {
      window.location.reload();
      return;
    }
    postToOpenElisServerJsonResponse(
      "/rest/SampleTypeTestAssign",
      JSON.stringify({
        testId: selectedTest.id,
        sampleTypeIds: selectedSampleTypes.map((item) => item.id),
      }),
      (res) => {
        handleSaveAssignmentsCallBack(res);
      },
    );
  };

  const handleSaveAssignmentsCallBack = (res) => {
    if (res) {
      setIsLoading(false);
      addNotification({
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({
          id: "notification.user.post.save.success",
        }),
        kind: NotificationKinds.success,
      });
      setTimeout(() => {
        window.location.reload();
      }, 200);
    } else {
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "server.error.msg" }),
      });
      setNotificationVisible(true);
      setTimeout(() => {
        window.location.reload();
      }, 200);
    }
  };

  useEffect(() => {
    componentMounted.current = true;
    setIsLoading(true);
    getFromOpenElisServer(
      `/rest/SampleTypeTestAssign`,
      handleSampleTypeTestAssignList,
    );
    return () => {
      componentMounted.current = false;
      setIsLoading(false);
    };
  }, []);

  if (!isLoading) {
    return (
      <>
        <Loading />
      </>
    );
  }

  return (
    <>
      {notificationVisible === true ? <AlertDialog /> : ""}
      <div className="adminPageContent">
        <PageBreadCrumb breadcrumbs={breadcrumbs} />
        <div className="orderLegendBody">
          <Grid fullWidth={true}>
            <Column lg={16} md={8} sm={4}>
              <Section>
                <Heading>
                  <FormattedMessage id="label.button.select" />
                </Heading>
              </Section>
            </Column>
          </Grid>
          <br />
          <hr />
          <br />
          <Grid fullWidth={true}>
            <Column lg={16} md={8} sm={4}>
              <Section>
                <Heading>
                  <FormattedMessage id="configuration.panel.assign" />
                </Heading>
              </Section>
            </Column>
          </Grid>
          <br />
          <hr />
          <br />
          <Grid fullWidth={true}>
            <Column lg={16} md={8} sm={4}>
              <Section>
                <Heading>
                  <FormattedMessage id="configuration.sampleType.assign.explain" />
                </Heading>
              </Section>
            </Column>
          </Grid>
          <br />
          <Grid fullWidth={true}>
            {sampleTypeTestAssign &&
            sampleTypeTestAssign?.sampleTypeTestList &&
            Object.keys(sampleTypeTestAssign?.sampleTypeTestList).length > 0 ? (
              <>
                {Object.entries(sampleTypeTestAssign?.sampleTypeTestList).map(
                  ([sectionKey, tests]) => {
                    const sectionId = sectionKey
                      .split(", value=")[0]
                      .split("id=")[1];
                    const sectionName = sectionKey.split(", value=")[1];
                    return (
                      <React.Fragment key={`${sectionKey}-${sectionId}`}>
                        <Column lg={16} md={8} sm={4}>
                          <h4>{sectionName}</h4>
                        </Column>
                        {tests.map((test) => (
                          <Column
                            style={{ margin: "2px" }}
                            key={`${sectionId}-${test.id}`}
                            lg={4}
                            md={4}
                            sm={4}
                          >
                            <ClickableTile onClick={() => openAssignModal(test)}>
                              {test.value}
                            </ClickableTile>
                          </Column>
                        ))}
                      </React.Fragment>
                    );
                  },
                )}
              </>
            ) : (
              <></>
            )}
          </Grid>
        </div>
      </div>

      <Modal
        open={sampleTypeTestAssignModal}
        size="md"
        modalHeading={intl.formatMessage({ id: "banner.menu.patientEdit" })}
        primaryButtonText={intl.formatMessage({ id: "label.button.save" })}
        secondaryButtonText={intl.formatMessage({ id: "label.button.cancel" })}
        onRequestSubmit={() => {
          setSampleTypeTestAssignModal(false);
          handleSaveAssignments();
        }}
        onRequestClose={() => {
          setSampleTypeTestAssignModal(false);
          window.location.reload();
        }}
        preventCloseOnClickOutside={true}
      >
        <Grid fullWidth={true}>
          <Column lg={16} md={8} sm={4}>
            <Section>
              <Heading>
                <FormattedMessage id="Test" /> : {selectedTest?.value}
              </Heading>
            </Section>
            <br />
            {/* minHeight : réserve la place pour que le menu déroulant du
                multi-select reste visible dans le modal. */}
            <div style={{ minHeight: "22rem" }}>
              {sampleTypeOptions.length > 0 && (
                <FilterableMultiSelect
                  key={modalKey}
                  id="sampleTypeMultiSelect"
                  titleText={intl.formatMessage({
                    id: "configuration.sampleType.assign.types",
                    defaultMessage: "Types d'échantillon assignés",
                  })}
                  items={sampleTypeOptions}
                  itemToString={(item) => (item ? item.value : "")}
                  initialSelectedItems={selectedSampleTypes}
                  onChange={(changes) => {
                    setSelectedSampleTypes(changes.selectedItems || []);
                  }}
                />
              )}
              {/* Récapitulatif des types sélectionnés, sous le champ */}
              {selectedSampleTypes.length > 0 && (
                <div style={{ marginTop: "0.75rem" }}>
                  {selectedSampleTypes.map((item) => (
                    <Tag
                      key={`selected-${item.id}`}
                      type="blue"
                      style={{ marginRight: "0.25rem", marginBottom: "0.25rem" }}
                    >
                      {item.value}
                    </Tag>
                  ))}
                </div>
              )}
            </div>
          </Column>
        </Grid>
      </Modal>
    </>
  );
}

export default injectIntl(SampleTypeTestAssign);

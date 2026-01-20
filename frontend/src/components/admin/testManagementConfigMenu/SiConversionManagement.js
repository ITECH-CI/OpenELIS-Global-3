import React, { useContext, useState, useEffect, useRef } from "react";
import {
  Heading,
  Button,
  Loading,
  Grid,
  Column,
  Section,
  DataTable,
  Table,
  TableHead,
  TableRow,
  TableBody,
  TableHeader,
  TableCell,
  TableContainer,
  TableToolbar,
  TableToolbarContent,
  TableToolbarSearch,
  Modal,
  TextInput,
  Select,
  SelectItem,
  Tabs,
  TabList,
  Tab,
  TabPanels,
  TabPanel,
  Stack,
  Tag,
  NumberInput,
} from "@carbon/react";
import { Add, TrashCan, Edit } from "@carbon/icons-react";
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
    label: "configuration.siconversion.manage",
    link: "/MasterListsPage#SiConversionManagement",
  },
];

function SiConversionManagement() {
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const intl = useIntl();

  const componentMounted = useRef(false);

  const [loading, setLoading] = useState(true);
  const [formData, setFormData] = useState({
    tests: [],
    unitOfMeasures: [],
    simpleConversions: [],
    derivedFormulas: [],
    testAliases: [],
  });

  // Simple Conversion Modal
  const [isSimpleConversionModalOpen, setIsSimpleConversionModalOpen] =
    useState(false);
  const [currentSimpleConversion, setCurrentSimpleConversion] = useState({
    id: "",
    testId: "",
    fromUomId: "",
    toUomId: "",
    factor: "",
    offsetValue: "0",
    decimals: "2",
    active: true,
  });

  // Derived Formula Modal
  const [isDerivedFormulaModalOpen, setIsDerivedFormulaModalOpen] =
    useState(false);
  const [currentDerivedFormula, setCurrentDerivedFormula] = useState({
    id: "",
    testId: "",
    expression: "",
    fromUomId: "",
    toUomSiId: "",
    decimals: "2",
    active: true,
    dependencies: [],
  });
  const [currentDependency, setCurrentDependency] = useState({
    sourceTestId: "",
    testAlias: "",
  });

  // Test Alias Modal
  const [isTestAliasModalOpen, setIsTestAliasModalOpen] = useState(false);
  const [currentTestAlias, setCurrentTestAlias] = useState({
    id: "",
    testId: "",
    alias: "",
    active: true,
  });

  // Fetch initial data
  useEffect(() => {
    componentMounted.current = true;
    getFromOpenElisServer(`/rest/si-conversion-management`, handleDataResponse);
    return () => {
      componentMounted.current = false;
    };
  }, []);

  const handleDataResponse = (res) => {
    if (res) {
      setFormData(res);
      setLoading(false);
    }
  };

  const handleSaveSimpleConversion = () => {
    postToOpenElisServerJsonResponse(
      `/rest/si-conversion-management/simple-conversion`,
      JSON.stringify({
        id: currentSimpleConversion.id || null,
        testId: currentSimpleConversion.testId,
        fromUomId: currentSimpleConversion.fromUomId || null,
        toUomId: currentSimpleConversion.toUomId,
        factor: currentSimpleConversion.factor,
        offsetValue: currentSimpleConversion.offsetValue || "0",
        decimals: parseInt(currentSimpleConversion.decimals) || 2,
        active: currentSimpleConversion.active,
      }),
      (data) => {
        if (data.success) {
          addNotification({
            kind: NotificationKinds.success,
            title: intl.formatMessage({ id: "notification.title" }),
            message: data.message,
          });
          setNotificationVisible(true);
          setIsSimpleConversionModalOpen(false);
          // Reload data
          setTimeout(() => {
            window.location.reload();
          }, 200);
        } else {
          addNotification({
            kind: NotificationKinds.error,
            title: intl.formatMessage({ id: "notification.title" }),
            message: data.message,
          });
          setNotificationVisible(true);
        }
      },
    );
  };

  const handleDeleteSimpleConversion = (id) => {
    fetch(`/rest/si-conversion-management/simple-conversion/${id}`, {
      method: "DELETE",
    })
      .then((response) => response.json())
      .then((data) => {
        if (data.success) {
          addNotification({
            kind: NotificationKinds.success,
            title: intl.formatMessage({ id: "notification.title" }),
            message: data.message,
          });
          setNotificationVisible(true);
          setTimeout(() => {
            window.location.reload();
          }, 200);
        }
      });
  };

  const handleSaveDerivedFormula = () => {
    postToOpenElisServerJsonResponse(
      `/rest/si-conversion-management/derived-formula`,
      JSON.stringify({
        id: currentDerivedFormula.id || null,
        testId: currentDerivedFormula.testId,
        expression: currentDerivedFormula.expression,
        fromUomId: currentDerivedFormula.fromUomId || null,
        toUomSiId: currentDerivedFormula.toUomSiId,
        decimals: parseInt(currentDerivedFormula.decimals) || 2,
        active: currentDerivedFormula.active,
        dependencies: currentDerivedFormula.dependencies,
      }),
      (data) => {
        if (data.success) {
          addNotification({
            kind: NotificationKinds.success,
            title: intl.formatMessage({ id: "notification.title" }),
            message: data.message,
          });
          setNotificationVisible(true);
          setIsDerivedFormulaModalOpen(false);
          setTimeout(() => {
            window.location.reload();
          }, 200);
        } else {
          addNotification({
            kind: NotificationKinds.error,
            title: intl.formatMessage({ id: "notification.title" }),
            message: data.message,
          });
          setNotificationVisible(true);
        }
      },
    );
  };

  const handleDeleteDerivedFormula = (id) => {
    fetch(`/rest/si-conversion-management/derived-formula/${id}`, {
      method: "DELETE",
    })
      .then((response) => response.json())
      .then((data) => {
        if (data.success) {
          addNotification({
            kind: NotificationKinds.success,
            title: intl.formatMessage({ id: "notification.title" }),
            message: data.message,
          });
          setNotificationVisible(true);
          setTimeout(() => {
            window.location.reload();
          }, 200);
        }
      });
  };

  const handleSaveTestAlias = () => {
    postToOpenElisServerJsonResponse(
      `/rest/si-conversion-management/test-alias`,
      JSON.stringify({
        id: currentTestAlias.id || null,
        testId: currentTestAlias.testId,
        alias: currentTestAlias.alias,
        active: currentTestAlias.active,
      }),
      (data) => {
        if (data.success) {
          addNotification({
            kind: NotificationKinds.success,
            title: intl.formatMessage({ id: "notification.title" }),
            message: data.message,
          });
          setNotificationVisible(true);
          setIsTestAliasModalOpen(false);
          setTimeout(() => {
            window.location.reload();
          }, 200);
        } else {
          addNotification({
            kind: NotificationKinds.error,
            title: intl.formatMessage({ id: "notification.title" }),
            message: data.message,
          });
          setNotificationVisible(true);
        }
      },
    );
  };

  const handleDeleteTestAlias = (id) => {
    fetch(`/rest/si-conversion-management/test-alias/${id}`, {
      method: "DELETE",
    })
      .then((response) => response.json())
      .then((data) => {
        if (data.success) {
          addNotification({
            kind: NotificationKinds.success,
            title: intl.formatMessage({ id: "notification.title" }),
            message: data.message,
          });
          setNotificationVisible(true);
          setTimeout(() => {
            window.location.reload();
          }, 200);
        }
      });
  };

  const openEditSimpleConversion = (conversion) => {
    setCurrentSimpleConversion({
      id: conversion.id,
      testId: conversion.testId,
      fromUomId: conversion.fromUomId || "",
      toUomId: conversion.toUomId,
      factor: conversion.factor ? conversion.factor.toString() : "",
      offsetValue: conversion.offsetValue
        ? conversion.offsetValue.toString()
        : "0",
      decimals: conversion.decimals ? conversion.decimals.toString() : "2",
      active: conversion.active,
    });
    setIsSimpleConversionModalOpen(true);
  };

  const openEditDerivedFormula = (formula) => {
    setCurrentDerivedFormula({
      id: formula.id,
      testId: formula.testId,
      expression: formula.expression,
      fromUomId: formula.fromUomId || "",
      toUomSiId: formula.toUomSiId,
      decimals: formula.decimals ? formula.decimals.toString() : "2",
      active: formula.active,
      dependencies: formula.dependencies || [],
    });
    setIsDerivedFormulaModalOpen(true);
  };

  const openEditTestAlias = (alias) => {
    setCurrentTestAlias({
      id: alias.id,
      testId: alias.testId,
      alias: alias.alias,
      active: alias.active,
    });
    setIsTestAliasModalOpen(true);
  };

  const addDependencyToFormula = () => {
    if (currentDependency.sourceTestId && currentDependency.testAlias) {
      setCurrentDerivedFormula({
        ...currentDerivedFormula,
        dependencies: [
          ...currentDerivedFormula.dependencies,
          {
            sourceTestId: currentDependency.sourceTestId,
            testAlias: currentDependency.testAlias,
          },
        ],
      });
      setCurrentDependency({ sourceTestId: "", testAlias: "" });
    }
  };

  const removeDependencyFromFormula = (index) => {
    setCurrentDerivedFormula({
      ...currentDerivedFormula,
      dependencies: currentDerivedFormula.dependencies.filter(
        (_, i) => i !== index,
      ),
    });
  };

  // DataTable headers
  const simpleConversionHeaders = [
    { key: "testName", header: intl.formatMessage({ id: "test.label" }) },
    {
      key: "fromUomName",
      header: intl.formatMessage({ id: "field.siconversion.fromUom" }),
    },
    {
      key: "toUomName",
      header: intl.formatMessage({ id: "field.siconversion.toUom" }),
    },
    {
      key: "factor",
      header: intl.formatMessage({ id: "field.siconversion.factor" }),
    },
    {
      key: "offsetValue",
      header: intl.formatMessage({ id: "field.siconversion.offsetValue" }),
    },
    {
      key: "decimals",
      header: intl.formatMessage({ id: "field.siconversion.decimals" }),
    },
    {
      key: "active",
      header: intl.formatMessage({ id: "field.siconversion.active" }),
    },
    { key: "actions", header: intl.formatMessage({ id: "label.action" }) },
  ];

  const derivedFormulaHeaders = [
    { key: "testName", header: intl.formatMessage({ id: "test.label" }) },
    {
      key: "expression",
      header: intl.formatMessage({ id: "field.siconversion.expression" }),
    },
    {
      key: "toUomSiName",
      header: intl.formatMessage({ id: "field.siconversion.toUom" }),
    },
    {
      key: "decimals",
      header: intl.formatMessage({ id: "field.siconversion.decimals" }),
    },
    {
      key: "active",
      header: intl.formatMessage({ id: "field.siconversion.active" }),
    },
    { key: "actions", header: intl.formatMessage({ id: "label.action" }) },
  ];

  const testAliasHeaders = [
    { key: "testName", header: intl.formatMessage({ id: "test.label" }) },
    {
      key: "alias",
      header: intl.formatMessage({ id: "field.siconversion.alias" }),
    },
    {
      key: "active",
      header: intl.formatMessage({ id: "field.siconversion.active" }),
    },
    { key: "actions", header: intl.formatMessage({ id: "label.action" }) },
  ];

  // Format rows for DataTable
  const simpleConversionRows = formData.simpleConversions
    .filter((conv) => conv.active)
    .map((conv) => ({
      id: conv.id,
      testName: conv.testName,
      fromUomName: conv.fromUomName || "-",
      toUomName: conv.toUomName,
      factor: conv.factor ? conv.factor.toString() : "",
      offsetValue: conv.offsetValue ? conv.offsetValue.toString() : "0",
      decimals: conv.decimals,
      active: conv.active ? (
        <Tag type="green">
          <FormattedMessage id="label.yes" />
        </Tag>
      ) : (
        <Tag type="red">
          <FormattedMessage id="label.no" />
        </Tag>
      ),
      actions: (
        <Stack orientation="horizontal" gap={2}>
          <Button
            size="sm"
            kind="ghost"
            renderIcon={Edit}
            onClick={() => openEditSimpleConversion(conv)}
            iconDescription={intl.formatMessage({ id: "label.button.edit" })}
            hasIconOnly
          />
          <Button
            size="sm"
            kind="danger--ghost"
            renderIcon={TrashCan}
            onClick={() => handleDeleteSimpleConversion(conv.id)}
            iconDescription={intl.formatMessage({ id: "label.button.remove" })}
            hasIconOnly
          />
        </Stack>
      ),
    }));

  const derivedFormulaRows = formData.derivedFormulas
    .filter((formula) => formula.active)
    .map((formula) => ({
      id: formula.id,
      testName: formula.testName,
      expression: formula.expression,
      toUomSiName: formula.toUomSiName,
      decimals: formula.decimals,
      active: formula.active ? (
        <Tag type="green">
          <FormattedMessage id="label.yes" />
        </Tag>
      ) : (
        <Tag type="red">
          <FormattedMessage id="label.no" />
        </Tag>
      ),
      actions: (
        <Stack orientation="horizontal" gap={2}>
          <Button
            size="sm"
            kind="ghost"
            renderIcon={Edit}
            onClick={() => openEditDerivedFormula(formula)}
            iconDescription={intl.formatMessage({ id: "label.button.edit" })}
            hasIconOnly
          />
          <Button
            size="sm"
            kind="danger--ghost"
            renderIcon={TrashCan}
            onClick={() => handleDeleteDerivedFormula(formula.id)}
            iconDescription={intl.formatMessage({ id: "label.button.remove" })}
            hasIconOnly
          />
        </Stack>
      ),
    }));

  const testAliasRows = formData.testAliases
    .filter((alias) => alias.active)
    .map((alias) => ({
      id: alias.id,
      testName: alias.testName,
      alias: alias.alias,
      active: alias.active ? (
        <Tag type="green">
          <FormattedMessage id="label.yes" />
        </Tag>
      ) : (
        <Tag type="red">
          <FormattedMessage id="label.no" />
        </Tag>
      ),
      actions: (
        <Stack orientation="horizontal" gap={2}>
          <Button
            size="sm"
            kind="ghost"
            renderIcon={Edit}
            onClick={() => openEditTestAlias(alias)}
            iconDescription={intl.formatMessage({ id: "label.button.edit" })}
            hasIconOnly
          />
          <Button
            size="sm"
            kind="danger--ghost"
            renderIcon={TrashCan}
            onClick={() => handleDeleteTestAlias(alias.id)}
            iconDescription={intl.formatMessage({ id: "label.button.remove" })}
            hasIconOnly
          />
        </Stack>
      ),
    }));

  if (loading) {
    return <Loading />;
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
                  <FormattedMessage id="configuration.siconversion.manage" />
                </Heading>
              </Section>
            </Column>
          </Grid>
          <br />
          <hr />
          <br />

          <Tabs>
            <TabList aria-label="SI Conversion Management Tabs">
              <Tab>
                <FormattedMessage id="configuration.siconversion.simple" />
              </Tab>
              <Tab>
                <FormattedMessage id="configuration.siconversion.derived" />
              </Tab>
              <Tab>
                <FormattedMessage id="configuration.siconversion.aliases" />
              </Tab>
            </TabList>

            <TabPanels>
              {/* Simple Conversions Tab */}
              <TabPanel>
                <Grid fullWidth={true}>
                  <Column lg={16} md={8} sm={4}>
                    <DataTable
                      rows={simpleConversionRows}
                      headers={simpleConversionHeaders}
                    >
                      {({
                        rows,
                        headers,
                        getHeaderProps,
                        getTableProps,
                        getToolbarProps,
                      }) => (
                        <TableContainer>
                          <TableToolbar {...getToolbarProps()}>
                            <TableToolbarContent>
                              <TableToolbarSearch persistent />
                              <Button
                                size="sm"
                                renderIcon={Add}
                                onClick={() => {
                                  setCurrentSimpleConversion({
                                    id: "",
                                    testId: "",
                                    fromUomId: "",
                                    toUomId: "",
                                    factor: "",
                                    offsetValue: "0",
                                    decimals: "2",
                                    active: true,
                                  });
                                  setIsSimpleConversionModalOpen(true);
                                }}
                              >
                                <FormattedMessage id="configuration.siconversion.add" />
                              </Button>
                            </TableToolbarContent>
                          </TableToolbar>
                          <Table {...getTableProps()}>
                            <TableHead>
                              <TableRow>
                                {headers.map((header) => (
                                  <TableHeader
                                    key={header.key}
                                    {...getHeaderProps({ header })}
                                  >
                                    {header.header}
                                  </TableHeader>
                                ))}
                              </TableRow>
                            </TableHead>
                            <TableBody>
                              {rows.map((row) => (
                                <TableRow key={row.id}>
                                  {row.cells.map((cell) => (
                                    <TableCell key={cell.id}>
                                      {cell.value}
                                    </TableCell>
                                  ))}
                                </TableRow>
                              ))}
                            </TableBody>
                          </Table>
                        </TableContainer>
                      )}
                    </DataTable>
                  </Column>
                </Grid>
              </TabPanel>

              {/* Derived Formulas Tab */}
              <TabPanel>
                <Grid fullWidth={true}>
                  <Column lg={16} md={8} sm={4}>
                    <DataTable
                      rows={derivedFormulaRows}
                      headers={derivedFormulaHeaders}
                    >
                      {({
                        rows,
                        headers,
                        getHeaderProps,
                        getTableProps,
                        getToolbarProps,
                      }) => (
                        <TableContainer>
                          <TableToolbar {...getToolbarProps()}>
                            <TableToolbarContent>
                              <TableToolbarSearch persistent />
                              <Button
                                size="sm"
                                renderIcon={Add}
                                onClick={() => {
                                  setCurrentDerivedFormula({
                                    id: "",
                                    testId: "",
                                    expression: "",
                                    fromUomId: "",
                                    toUomSiId: "",
                                    decimals: "2",
                                    active: true,
                                    dependencies: [],
                                  });
                                  setIsDerivedFormulaModalOpen(true);
                                }}
                              >
                                <FormattedMessage id="configuration.siconversion.add" />
                              </Button>
                            </TableToolbarContent>
                          </TableToolbar>
                          <Table {...getTableProps()}>
                            <TableHead>
                              <TableRow>
                                {headers.map((header) => (
                                  <TableHeader
                                    key={header.key}
                                    {...getHeaderProps({ header })}
                                  >
                                    {header.header}
                                  </TableHeader>
                                ))}
                              </TableRow>
                            </TableHead>
                            <TableBody>
                              {rows.map((row) => (
                                <TableRow key={row.id}>
                                  {row.cells.map((cell) => (
                                    <TableCell key={cell.id}>
                                      {cell.value}
                                    </TableCell>
                                  ))}
                                </TableRow>
                              ))}
                            </TableBody>
                          </Table>
                        </TableContainer>
                      )}
                    </DataTable>
                  </Column>
                </Grid>
              </TabPanel>

              {/* Test Aliases Tab */}
              <TabPanel>
                <Grid fullWidth={true}>
                  <Column lg={16} md={8} sm={4}>
                    <DataTable rows={testAliasRows} headers={testAliasHeaders}>
                      {({
                        rows,
                        headers,
                        getHeaderProps,
                        getTableProps,
                        getToolbarProps,
                      }) => (
                        <TableContainer>
                          <TableToolbar {...getToolbarProps()}>
                            <TableToolbarContent>
                              <TableToolbarSearch persistent />
                              <Button
                                size="sm"
                                renderIcon={Add}
                                onClick={() => {
                                  setCurrentTestAlias({
                                    id: "",
                                    testId: "",
                                    alias: "",
                                    active: true,
                                  });
                                  setIsTestAliasModalOpen(true);
                                }}
                              >
                                <FormattedMessage id="configuration.siconversion.add" />
                              </Button>
                            </TableToolbarContent>
                          </TableToolbar>
                          <Table {...getTableProps()}>
                            <TableHead>
                              <TableRow>
                                {headers.map((header) => (
                                  <TableHeader
                                    key={header.key}
                                    {...getHeaderProps({ header })}
                                  >
                                    {header.header}
                                  </TableHeader>
                                ))}
                              </TableRow>
                            </TableHead>
                            <TableBody>
                              {rows.map((row) => (
                                <TableRow key={row.id}>
                                  {row.cells.map((cell) => (
                                    <TableCell key={cell.id}>
                                      {cell.value}
                                    </TableCell>
                                  ))}
                                </TableRow>
                              ))}
                            </TableBody>
                          </Table>
                        </TableContainer>
                      )}
                    </DataTable>
                  </Column>
                </Grid>
              </TabPanel>
            </TabPanels>
          </Tabs>
        </div>
      </div>

      {/* Simple Conversion Modal */}
      <Modal
        open={isSimpleConversionModalOpen}
        size="md"
        modalHeading={
          currentSimpleConversion.id
            ? intl.formatMessage({ id: "configuration.siconversion.modify" })
            : intl.formatMessage({ id: "configuration.siconversion.add" })
        }
        primaryButtonText={intl.formatMessage({ id: "label.button.save" })}
        secondaryButtonText={intl.formatMessage({ id: "label.button.cancel" })}
        onRequestSubmit={handleSaveSimpleConversion}
        onRequestClose={() => setIsSimpleConversionModalOpen(false)}
      >
        <Grid fullWidth={true}>
          <Column lg={16} md={8} sm={4}>
            <Select
              id="testId"
              labelText={intl.formatMessage({ id: "test.label" })}
              value={currentSimpleConversion.testId}
              onChange={(e) =>
                setCurrentSimpleConversion({
                  ...currentSimpleConversion,
                  testId: e.target.value,
                })
              }
            >
              <SelectItem value="" text="Select a test" />
              {formData.tests.map((test) => (
                <SelectItem key={test.id} value={test.id} text={test.value} />
              ))}
            </Select>
          </Column>
          <Column lg={16} md={8} sm={4}>
            <Select
              id="fromUomId"
              labelText={intl.formatMessage({
                id: "field.siconversion.fromUom",
              })}
              value={currentSimpleConversion.fromUomId}
              onChange={(e) =>
                setCurrentSimpleConversion({
                  ...currentSimpleConversion,
                  fromUomId: e.target.value,
                })
              }
            >
              <SelectItem value="" text="Any unit (optional)" />
              {formData.unitOfMeasures.map((uom) => (
                <SelectItem key={uom.id} value={uom.id} text={uom.value} />
              ))}
            </Select>
          </Column>
          <Column lg={16} md={8} sm={4}>
            <Select
              id="toUomId"
              labelText={intl.formatMessage({ id: "field.siconversion.toUom" })}
              value={currentSimpleConversion.toUomId}
              onChange={(e) =>
                setCurrentSimpleConversion({
                  ...currentSimpleConversion,
                  toUomId: e.target.value,
                })
              }
            >
              <SelectItem value="" text="Select SI unit" />
              {formData.unitOfMeasures.map((uom) => (
                <SelectItem key={uom.id} value={uom.id} text={uom.value} />
              ))}
            </Select>
          </Column>
          <Column lg={16} md={8} sm={4}>
            <TextInput
              id="factor"
              labelText={intl.formatMessage({
                id: "field.siconversion.factor",
              })}
              value={currentSimpleConversion.factor}
              onChange={(e) =>
                setCurrentSimpleConversion({
                  ...currentSimpleConversion,
                  factor: e.target.value,
                })
              }
            />
          </Column>
          <Column lg={16} md={8} sm={4}>
            <TextInput
              id="offsetValue"
              labelText={intl.formatMessage({
                id: "field.siconversion.offsetValue",
              })}
              value={currentSimpleConversion.offsetValue}
              onChange={(e) =>
                setCurrentSimpleConversion({
                  ...currentSimpleConversion,
                  offsetValue: e.target.value,
                })
              }
            />
          </Column>
          <Column lg={16} md={8} sm={4}>
            <TextInput
              id="decimals"
              labelText={intl.formatMessage({
                id: "field.siconversion.decimals",
              })}
              value={currentSimpleConversion.decimals}
              onChange={(e) =>
                setCurrentSimpleConversion({
                  ...currentSimpleConversion,
                  decimals: e.target.value,
                })
              }
            />
          </Column>
        </Grid>
      </Modal>

      {/* Derived Formula Modal */}
      <Modal
        open={isDerivedFormulaModalOpen}
        size="lg"
        modalHeading={
          currentDerivedFormula.id
            ? intl.formatMessage({ id: "configuration.siconversion.modify" })
            : intl.formatMessage({ id: "configuration.siconversion.add" })
        }
        primaryButtonText={intl.formatMessage({ id: "label.button.save" })}
        secondaryButtonText={intl.formatMessage({ id: "label.button.cancel" })}
        onRequestSubmit={handleSaveDerivedFormula}
        onRequestClose={() => setIsDerivedFormulaModalOpen(false)}
      >
        <Grid fullWidth={true}>
          <Column lg={16} md={8} sm={4}>
            <Select
              id="testId"
              labelText={intl.formatMessage({ id: "test.label" })}
              value={currentDerivedFormula.testId}
              onChange={(e) =>
                setCurrentDerivedFormula({
                  ...currentDerivedFormula,
                  testId: e.target.value,
                })
              }
            >
              <SelectItem value="" text="Select a test" />
              {formData.tests.map((test) => (
                <SelectItem key={test.id} value={test.id} text={test.value} />
              ))}
            </Select>
          </Column>
          <Column lg={16} md={8} sm={4}>
            <TextInput
              id="expression"
              labelText={intl.formatMessage({
                id: "field.siconversion.expression",
              })}
              placeholder="e.g., HCT * 10 / GR"
              value={currentDerivedFormula.expression}
              onChange={(e) =>
                setCurrentDerivedFormula({
                  ...currentDerivedFormula,
                  expression: e.target.value,
                })
              }
            />
          </Column>
          <Column lg={16} md={8} sm={4}>
            <Select
              id="toUomSiId"
              labelText={intl.formatMessage({ id: "field.siconversion.toUom" })}
              value={currentDerivedFormula.toUomSiId}
              onChange={(e) =>
                setCurrentDerivedFormula({
                  ...currentDerivedFormula,
                  toUomSiId: e.target.value,
                })
              }
            >
              <SelectItem value="" text="Select SI unit" />
              {formData.unitOfMeasures.map((uom) => (
                <SelectItem key={uom.id} value={uom.id} text={uom.value} />
              ))}
            </Select>
          </Column>
          <Column lg={16} md={8} sm={4}>
            <TextInput
              id="decimals"
              labelText={intl.formatMessage({
                id: "field.siconversion.decimals",
              })}
              value={currentDerivedFormula.decimals}
              onChange={(e) =>
                setCurrentDerivedFormula({
                  ...currentDerivedFormula,
                  decimals: e.target.value,
                })
              }
            />
          </Column>
          <Column lg={16} md={8} sm={4}>
            <Heading>
              <FormattedMessage id="field.siconversion.dependencies" />
            </Heading>
            <Stack gap={2}>
              {currentDerivedFormula.dependencies.map((dep, index) => (
                <div key={index}>
                  {dep.testAlias} (Test ID: {dep.sourceTestId})
                  <Button
                    size="sm"
                    kind="danger--ghost"
                    renderIcon={TrashCan}
                    onClick={() => removeDependencyFromFormula(index)}
                    hasIconOnly
                    iconDescription="Remove"
                  />
                </div>
              ))}
            </Stack>
          </Column>
          <Column lg={8} md={4} sm={2}>
            <Select
              id="sourceTestId"
              labelText={intl.formatMessage({ id: "test.label" })}
              value={currentDependency.sourceTestId}
              onChange={(e) =>
                setCurrentDependency({
                  ...currentDependency,
                  sourceTestId: e.target.value,
                })
              }
            >
              <SelectItem value="" text="Select source test" />
              {formData.tests.map((test) => (
                <SelectItem key={test.id} value={test.id} text={test.value} />
              ))}
            </Select>
          </Column>
          <Column lg={8} md={4} sm={2}>
            <TextInput
              id="testAlias"
              labelText={intl.formatMessage({
                id: "field.siconversion.alias",
              })}
              placeholder="e.g., HCT, GR, Hb"
              value={currentDependency.testAlias}
              onChange={(e) =>
                setCurrentDependency({
                  ...currentDependency,
                  testAlias: e.target.value,
                })
              }
            />
          </Column>
          <Column lg={16} md={8} sm={4}>
            <Button size="sm" onClick={addDependencyToFormula}>
              <FormattedMessage id="configuration.siconversion.addDependency" />
            </Button>
          </Column>
        </Grid>
      </Modal>

      {/* Test Alias Modal */}
      <Modal
        open={isTestAliasModalOpen}
        size="md"
        modalHeading={
          currentTestAlias.id
            ? intl.formatMessage({ id: "configuration.siconversion.modify" })
            : intl.formatMessage({ id: "configuration.siconversion.add" })
        }
        primaryButtonText={intl.formatMessage({ id: "label.button.save" })}
        secondaryButtonText={intl.formatMessage({ id: "label.button.cancel" })}
        onRequestSubmit={handleSaveTestAlias}
        onRequestClose={() => setIsTestAliasModalOpen(false)}
      >
        <Grid fullWidth={true}>
          <Column lg={16} md={8} sm={4}>
            <Select
              id="testId"
              labelText={intl.formatMessage({ id: "test.label" })}
              value={currentTestAlias.testId}
              onChange={(e) =>
                setCurrentTestAlias({
                  ...currentTestAlias,
                  testId: e.target.value,
                })
              }
            >
              <SelectItem value="" text="Select a test" />
              {formData.tests.map((test) => (
                <SelectItem key={test.id} value={test.id} text={test.value} />
              ))}
            </Select>
          </Column>
          <Column lg={16} md={8} sm={4}>
            <TextInput
              id="alias"
              labelText={intl.formatMessage({
                id: "field.siconversion.alias",
              })}
              placeholder="e.g., HCT, GR, Hb"
              value={currentTestAlias.alias}
              onChange={(e) =>
                setCurrentTestAlias({
                  ...currentTestAlias,
                  alias: e.target.value,
                })
              }
            />
          </Column>
        </Grid>
      </Modal>
    </>
  );
}

export default injectIntl(SiConversionManagement);

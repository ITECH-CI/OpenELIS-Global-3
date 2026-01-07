import { useState, useEffect, useRef } from "react";
import {
  Button,
  Column,
  Grid,
  Section,
  TextInput,
  TextArea,
  Select,
  SelectItem,
  DataTable,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  TableToolbar,
  TableToolbarContent,
  Modal,
  Loading,
  Pagination,
} from "@carbon/react";
import { Add, Download } from "@carbon/react/icons";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../utils/Utils";
import { FormattedMessage, useIntl } from "react-intl";
import AutoComplete from "../common/AutoComplete";

const NonConformityRegistry = () => {
  const intl = useIntl();
  const componentMounted = useRef(false);

  const [nonConformities, setNonConformities] = useState([]);
  const [filteredData, setFilteredData] = useState([]);
  const [loading, setLoading] = useState(false);

  const [showModal, setShowModal] = useState(false);
  const [isEditing, setIsEditing] = useState(false);
  const [siteNames, setSiteNames] = useState([]);
  const [sampleTypes, setSampleTypes] = useState([]);
  const [qaEvent, setQaEvent] = useState([]);

  const [formData, setFormData] = useState({
    id: "",
    ncNumber: "",
    reportDate: "",
    siteProvenance: "",
    sampleType: "",
    rejectionReason: "",
    comment: "",
    reporterName: "",
    labNumber: "",
    correctiveAction: "",
  });

  const [filters, setFilters] = useState({
    siteProvenance: "",
    sampleType: "",
    rejectionReason: "",
    startDate: "",
    endDate: "",
  });

  const [fieldErrors, setFieldErrors] = useState({
    reportDate: "",
    siteProvenance: "",
    sampleType: "",
    rejectionReason: "",
    reporterName: "",
  });

  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const pageSizes = [10, 20, 30, 50];

  useEffect(() => {
    loadNonConformities();
  }, []);

  
    const getSiteList = (response) => {
      if (componentMounted.current) {
        setSiteNames(response);
      }
    };
  
    useEffect(() => {
      componentMounted.current = true;
      getFromOpenElisServer("/rest/site-names", getSiteList);
      getFromOpenElisServer("/rest/user-sample-types", fetchSamplesTypes);
      getFromOpenElisServer("/rest/qaevents-dictionnary", fetchQaEvents);
      return () => {
        componentMounted.current = false;
      };
    }, []);

  const fetchSamplesTypes = (res) => {
    if (componentMounted.current) {
      setSampleTypes(res);
      setLoading(false);
    }
  };

    const fetchQaEvents = (res) => {
    if (componentMounted.current) {
      setQaEvent(res);
      setLoading(false);
    }
  };
  const loadNonConformities = () => {
    setLoading(true);
    getFromOpenElisServer("/rest/nonconformities", (data) => {
      const dataArray = Array.isArray(data) ? data : [];
      setNonConformities(dataArray);
      setFilteredData(dataArray);
      setLoading(false);
    });
  };

  const handleSearch = () => {
    setLoading(true);
    const params = new URLSearchParams();

    if (filters.siteProvenance) params.append("siteProvenance", filters.siteProvenance);
    if (filters.sampleType) params.append("sampleType", filters.sampleType);
    if (filters.rejectionReason) params.append("rejectionReason", filters.rejectionReason);
    if (filters.startDate) params.append("startDate", filters.startDate);
    if (filters.endDate) params.append("endDate", filters.endDate);

    getFromOpenElisServer(
      `/rest/nonconformities/search?${params.toString()}`,
      (data) => {
        const dataArray = Array.isArray(data) ? data : [];
        setFilteredData(dataArray);
        setLoading(false);
      }
    );
  };

  const handleExportCSV = () => {
    try {
      const escapeCsv = (value) => {
        if (value === null || value === undefined) {
          return "";
        }
        const stringValue = String(value);
        if (stringValue.includes(",") || stringValue.includes('"') || stringValue.includes("\n")) {
          return `"${stringValue.replace(/"/g, '""')}"`;
        }
        return stringValue;
      };

      let csvContent = "\ufeff"; 

      csvContent += "Numéro NC,Date de Signalement,Site de Provenance,Type d'Échantillon,";
      csvContent += "Raison du Rejet,Commentaire,Rapporteur,Numéro Laboratoire,";
      csvContent += "Action Corrective\n";

      filteredData.forEach((nc) => {
        csvContent += escapeCsv(nc.ncNumber) + ",";
        csvContent += escapeCsv(nc.reportDate) + ",";
        csvContent += escapeCsv(getSiteNameById(nc.siteProvenance)) + ",";
        csvContent += escapeCsv(getSampleNameById(nc.sampleType)) + ",";
        csvContent += escapeCsv(getRejectionReasonById(nc.rejectionReason)) + ",";
        csvContent += escapeCsv(nc.comment) + ",";
        csvContent += escapeCsv(nc.reporterName) + ",";
        csvContent += escapeCsv(nc.labNumber) + ",";
        csvContent += escapeCsv(nc.correctiveAction) + "\n";
      });

      const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `nonconformites_${new Date().toISOString().split('T')[0]}.csv`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
    } catch (error) {
      console.error('Erreur lors de l\'exportation CSV:', error);
      alert('Erreur lors de l\'exportation du fichier CSV: ' + error.message);
    }
  };

  const handleOpenModal = (nc = null) => {
    if (nc) {
      setIsEditing(true);
      setFormData({
        id: nc.id,
        ncNumber: nc.ncNumber,
        reportDate: nc.reportDate || "",
        siteProvenance: nc.siteProvenance || "",
        sampleType: nc.sampleType || "",
        rejectionReason: nc.rejectionReason || "",
        comment: nc.comment || "",
        reporterName: nc.reporterName || "",
        labNumber: nc.labNumber || "",
        correctiveAction: nc.correctiveAction || "",
      });
    } else {
      setIsEditing(false);
      setFormData({
        id: "",
        ncNumber: "",
        reportDate: new Date().toISOString().split("T")[0],
        siteProvenance: "",
        sampleType: "",
        rejectionReason: "",
        comment: "",
        reporterName: "",
        labNumber: "",
        correctiveAction: "",
      });
    }
    setShowModal(true);
  };

  const handleCloseModal = () => {
    setShowModal(false);
    setIsEditing(false);
    setFieldErrors({
      reportDate: "",
      siteProvenance: "",
      sampleType: "",
      rejectionReason: "",
      reporterName: "",
    });
  };

  const handleInputChange = (field, value) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
    if (fieldErrors[field]) {
      setFieldErrors((prev) => ({ ...prev, [field]: "" }));
    }
  };

  const handleFilterChange = (field, value) => {
    setFilters((prev) => ({ ...prev, [field]: value }));
  };

  function handleAutoCompleteSiteName(siteId) {
    console.log({ siteId });
    handleInputChange("siteProvenance", siteId);
  }

  const handleSubmit = () => {
    console.log("handleSubmit called", formData);

    // Validation des champs obligatoires
    const errors = {
      reportDate: "",
      siteProvenance: "",
      sampleType: "",
      rejectionReason: "",
      reporterName: "",
    };

    let hasError = false;

    if (!formData.reportDate) {
      errors.reportDate = "Ce champ est obligatoire";
      hasError = true;
    }
    if (!formData.siteProvenance) {
      errors.siteProvenance = "Ce champ est obligatoire";
      hasError = true;
    }
    if (!formData.sampleType) {
      errors.sampleType = "Ce champ est obligatoire";
      hasError = true;
    }
    if (!formData.rejectionReason) {
      errors.rejectionReason = "Ce champ est obligatoire";
      hasError = true;
    }
    if (!formData.reporterName) {
      errors.reporterName = "Ce champ est obligatoire";
      hasError = true;
    }

    if (hasError) {
      setFieldErrors(errors);
      return;
    }

    console.log("Sending data to server:", formData);

    try {
      postToOpenElisServerJsonResponse(
        "/rest/nonconformity",
        JSON.stringify(formData),
        (response) => {
          console.log("Server response:", response);
          if (response && response.success) {
            handleCloseModal();
            loadNonConformities();
          } else {
            console.error("Error from server:", response?.message);
          }
        },
        (error) => {
          console.error("Error calling server:", error);
        }
      );
    } catch (error) {
      console.error("Exception in handleSubmit:", error);
    }
  };

  const headers = [
    { key: "ncNumber", header: "Numéro NC" },
    { key: "reportDate", header: "Date" },
    { key: "siteProvenance", header: "Site de Provenance" },
    { key: "sampleType", header: "Type d'Échantillon" },
    { key: "rejectionReason", header: "Raison du Rejet" },
    { key: "reporterName", header: "Rapporteur" },
    { key: "labNumber", header: "N° Labo" },
    { key: "actions", header: "Actions" },
  ];


  const getSiteNameById = (siteId) => {
    if (!siteId) return "-";
    const site = siteNames.find((s) => s.id === siteId);
    return site ? site.value : siteId;
  };

   const getSampleNameById = (sampleId) => {
    if (!sampleId) return "-";
    const sample = sampleTypes.find((s) => s.id === sampleId);
    return sample ? sample.value : sampleId;
  };

  const getRejectionReasonById = (reasonId) => {
    if (!reasonId) return "-";
    const reason = qaEvent.find((s) => s.id === reasonId);
    return reason ? reason.value : reasonId;
  };

  const allRows = Array.isArray(filteredData) ? filteredData.map((nc) => ({
    id: nc.id,
    ncNumber: nc.ncNumber,
    reportDate: nc.reportDate,
    siteProvenance: getSiteNameById(nc.siteProvenance),
    sampleType: getSampleNameById(nc.sampleType),
    rejectionReason: getRejectionReasonById(nc.rejectionReason),
    reporterName: nc.reporterName,
    labNumber: nc.labNumber || "-",
    actions: (
      <Button size="sm" onClick={() => handleOpenModal(nc)}>
        Modifier
      </Button>
    ),
  })) : [];

  const startIndex = (currentPage - 1) * pageSize;
  const endIndex = startIndex + pageSize;
  const rows = allRows.slice(startIndex, endIndex);
  const totalItems = allRows.length;

  return (
    <>
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <Section>
            <h2>Registre des Non-Conformités</h2>
          </Section>
        </Column>

        {/* Section des filtres */}
        <Column lg={16} md={8} sm={4}>
          {/* <Section>
            <h4>Filtres de Recherche</h4>
            <Grid>
             <Column lg={4} md={4} sm={4}>
                  <TextInput
                  id="filter-site"
                  labelText="Site de Provenance"
                  value={filters.siteProvenance}
                  onChange={(e) => handleFilterChange("siteProvenance", e.target.value)}
                /> 
           <AutoComplete
            id="filter-site"
            labelText="Site de Provenance"
                value={filters.siteProvenance}
              allowFreeText={false}
              onSelect={(id) => handleFilterChange("siteProvenance", id)}
              onChange={(e) => handleFilterChange("siteProvenance", e.target.value)}
              label={
                <>
                  <FormattedMessage id="order.search.site.name" />{" "}
                </>
              }
              style={{ width: "!important 100%" }}
              suggestions={siteNames.length > 0 ? siteNames : []}
            />
              </Column>
              <Column lg={4} md={4} sm={4}>
                <Select
                  id="filter-sample-type"
                  labelText="Type d'Échantillon"
                  value={filters.sampleType}
                  onChange={(e) => handleFilterChange("sampleType", e.target.value)}
                >
                  <SelectItem value="" text="Tous" />
                  <SelectItem value="Sang" text="Sang" />
                  <SelectItem value="Urine" text="Urine" />
                  <SelectItem value="Selles" text="Selles" />
                  <SelectItem value="Salive" text="Salive" />
                  <SelectItem value="Expectorations" text="Expectorations" />
                  <SelectItem value="LCR" text="LCR (Liquide Céphalo-Rachidien)" />
                  <SelectItem value="Autre" text="Autre" />
                </Select>
              </Column>
              <Column lg={4} md={4} sm={4}>
                <Select
                  id="filter-rejection-reason"
                  labelText="Raison du Rejet"
                  value={filters.rejectionReason}
                  onChange={(e) => handleFilterChange("rejectionReason", e.target.value)}
                >
                  <SelectItem value="" text="Toutes" />
                  <SelectItem value="Tube inadéquat" text="Tube inadéquat" />
                  <SelectItem value="Échantillon hémolysé" text="Échantillon hémolysé" />
                  <SelectItem value="Quantité insuffisante" text="Quantité insuffisante" />
                  <SelectItem value="Étiquetage incorrect" text="Étiquetage incorrect" />
                  <SelectItem value="Échantillon contaminé" text="Échantillon contaminé" />
                  <SelectItem value="Délai dépassé" text="Délai dépassé" />
                  <SelectItem value="Autre" text="Autre" />
                </Select>
              </Column>
              <Column lg={4} md={4} sm={4}>
                <DatePicker
                  datePickerType="single"
                  dateFormat="Y-m-d"
                  onChange={(dates) => {
                    if (dates[0]) {
                      const date = dates[0].toISOString().split("T")[0];
                      handleFilterChange("startDate", date);
                    }
                  }}
                >
                  <DatePickerInput
                    id="filter-start-date"
                    labelText="Date de Début"
                    placeholder="YYYY-MM-DD"
                  />
                </DatePicker>
              </Column>
              <Column lg={4} md={4} sm={4}>
                <DatePicker
                  datePickerType="single"
                  dateFormat="Y-m-d"
                  onChange={(dates) => {
                    if (dates[0]) {
                      const date = dates[0].toISOString().split("T")[0];
                      handleFilterChange("endDate", date);
                    }
                  }}
                >
                  <DatePickerInput
                    id="filter-end-date"
                    labelText="Date de Fin"
                    placeholder="YYYY-MM-DD"
                  />
                </DatePicker>
              </Column>
              <Column lg={16} md={8} sm={4}>
                <Button onClick={handleSearch} style={{ marginRight: "1rem" }}>
                  Rechercher
                </Button>
                <Button kind="secondary" onClick={() => {
                  setFilters({
                    siteProvenance: "",
                    sampleType: "",
                    rejectionReason: "",
                    startDate: "",
                    endDate: "",
                  });
                  loadNonConformities();
                }}>
                  Réinitialiser
                </Button>
              </Column>
            </Grid>
          </Section>*/}
        </Column>

        {/* Tableau des non-conformités */}
        <Column lg={16} md={8} sm={4}>
          <Section>
            {loading ? (
              <Loading />
            ) : (
              <DataTable rows={rows} headers={headers}>
                {({
                  rows,
                  headers,
                  getTableProps,
                  getHeaderProps,
                  getRowProps,
                }) => (
                  <TableContainer>
                    <TableToolbar>
                      <TableToolbarContent>
                        <Button
                          kind="primary"
                          renderIcon={Add}
                          onClick={() => handleOpenModal()}
                        >
                          Nouvelle Non-Conformité
                        </Button>
                        <Button
                          kind="secondary"
                          renderIcon={Download}
                          onClick={handleExportCSV}
                        >
                          Exporter CSV
                        </Button>
                      </TableToolbarContent>
                    </TableToolbar>
                    <Table {...getTableProps()}>
                      <TableHead>
                        <TableRow>
                          {headers.map((header) => (
                            <TableHeader key={header.key} {...getHeaderProps({ header })}>
                              {header.header}
                            </TableHeader>
                          ))}
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        {rows.map((row) => (
                          <TableRow key={row.id} {...getRowProps({ row })}>
                            {row.cells.map((cell) => (
                              <TableCell key={cell.id}>{cell.value}</TableCell>
                            ))}
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  </TableContainer>
                )}
              </DataTable>
            )}
            {!loading && totalItems > 0 && (
              <Pagination
                backwardText="Page précédente"
                forwardText="Page suivante"
                itemsPerPageText="Éléments par page:"
                page={currentPage}
                pageNumberText="Numéro de page"
                pageSize={pageSize}
                pageSizes={pageSizes}
                totalItems={totalItems}
                onChange={({ page, pageSize: newPageSize }) => {
                  setCurrentPage(page);
                  setPageSize(newPageSize);
                }}
              />
            )}
          </Section>
        </Column>
      </Grid>

      {/* Modal pour le formulaire */}
      <Modal
        open={showModal}
        onRequestClose={handleCloseModal}
        modalHeading={isEditing ? "Modifier la Non-Conformité" : "Nouvelle Non-Conformité"}
        primaryButtonText="Enregistrer"
        secondaryButtonText="Annuler"
        onRequestSubmit={handleSubmit}
        size="lg"
      >
        <Grid>
          {isEditing && (
            <Column lg={16}>
              <TextInput
                id="nc-number"
                labelText="Numéro NC"
                value={formData.ncNumber}
                readOnly
              />
            </Column>
          )}
          <Column lg={8}>
            <TextInput
              id="report-date"
              type="date"
              labelText="Date de Signalement *"
              value={formData.reportDate}
              onChange={(e) => handleInputChange("reportDate", e.target.value)}
              invalid={!!fieldErrors.reportDate}
              invalidText={fieldErrors.reportDate}
              required
            />
          </Column>
          <Column lg={8}>
            <TextInput
              id="lab-number"
              labelText="Numéro Laboratoire"
              value={formData.labNumber}
              onChange={(e) => handleInputChange("labNumber", e.target.value)}
            />
          </Column>
          <Column lg={8}>
            <div>
              <AutoComplete
                id="site-provenance"
                labelText="Site de Provenance *"
                value={formData.siteProvenance}
                allowFreeText={false}
                onSelect={handleAutoCompleteSiteName}
                onChange={(e) => handleInputChange("siteProvenance", e.target.value)}
                label={
                    <>
                      <FormattedMessage id="order.search.site.name" />{" "}
                    </>
                  }
                  style={{ width: "!important 100%" }}
                  suggestions={siteNames.length > 0 ? siteNames : []}
              />
              {fieldErrors.siteProvenance && (
                <div style={{ color: "red", fontSize: "0.75rem", marginTop: "0.25rem" }}>
                  {fieldErrors.siteProvenance}
                </div>
              )}
            </div>
          </Column>
          <Column lg={8}>
            <Select
              id="sample-type"
              labelText="Type d'Échantillon *"
              value={formData.sampleType}
              onChange={(e) => handleInputChange("sampleType", e.target.value)}
              invalid={!!fieldErrors.sampleType}
              invalidText={fieldErrors.sampleType}
              required
            >
              <SelectItem text="Select sample type" value="" />
              {sampleTypes?.map((sampleType, i) => (
                <SelectItem text={sampleType.value} value={sampleType.id} key={i} />
              ))}
            </Select>
          </Column>
          <Column lg={16}>
            <Select
              id="rejection-reason"
              labelText="Raison du Rejet *"
              value={formData.rejectionReason}
              onChange={(e) => handleInputChange("rejectionReason", e.target.value)}
              invalid={!!fieldErrors.rejectionReason}
              invalidText={fieldErrors.rejectionReason}
              required
            >
              <SelectItem text="Raison du Rejet" value="" />
              {qaEvent?.map((rejection, i) => (
                <SelectItem text={rejection.value} value={rejection.id} key={i} />
              ))}
            </Select>
          </Column>
          <Column lg={16}>
            <TextInput
              id="reporter-name"
              labelText="Personne ayant constaté la NC (Rapporteur) *"
              value={formData.reporterName}
              onChange={(e) => handleInputChange("reporterName", e.target.value.toUpperCase())}
              invalid={!!fieldErrors.reporterName}
              invalidText={fieldErrors.reporterName}
              required
            />
          </Column>
          <Column lg={16}>
            <TextArea
              id="comment"
              labelText="Commentaire"
              value={formData.comment}
              onChange={(e) => handleInputChange("comment", e.target.value.toUpperCase())}
              rows={3}
            />
          </Column>
          <Column lg={16}>
            <TextArea
              id="corrective-action"
              labelText="Action Corrective"
              value={formData.correctiveAction}
              onChange={(e) => handleInputChange("correctiveAction", e.target.value.toUpperCase())}
              rows={3}
            />
          </Column>
        </Grid>
      </Modal>
    </>
  );
};

export default NonConformityRegistry;

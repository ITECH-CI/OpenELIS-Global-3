/**
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations under
 * the License.
 *
 * The Original Code is OpenELIS code.
 *
 * Copyright (C) ITECH, University of Washington, Seattle WA.  All Rights Reserved.
 *
 */
package org.openelisglobal.reports.action.implementation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.bacteriology.service.BacteriologyAntibiogramService;
import org.openelisglobal.bacteriology.service.BacteriologyOrganismService;
import org.openelisglobal.bacteriology.valueholder.BacteriologyAntibiogram;
import org.openelisglobal.bacteriology.valueholder.BacteriologyOrganism;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.reports.action.implementation.reportBeans.ClinicalPatientData;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sample.util.AccessionNumberUtil;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestServiceImpl;
import org.openelisglobal.test.valueholder.Test;

import liquibase.repackaged.org.apache.commons.lang3.ObjectUtils;

/**
 * Bacteriology Patient Report Organizes results by test type: Macroscopy,
 * Microscopy, and Culture
 */
public class BacterioPatientReport extends PatientReport implements IReportCreator, IReportParameterSetter {

    private static Set<Integer> analysisStatusIds;
    protected List<ClinicalPatientData> clinicalReportItems;
    private java.util.Date maxCompletionDate;

    // Test type identifiers for bacteriology (based on test names, not sections)
    private static final int TYPE_ORDER_MACROSCOPY = 1;
    private static final int TYPE_ORDER_MICROSCOPY = 2;
    private static final int TYPE_ORDER_CULTURE = 3;
    private static final int TYPE_ORDER_OTHER = 99;

    static {
        analysisStatusIds = new HashSet<>();
        analysisStatusIds.add(Integer
                .parseInt(SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.BiologistRejected)));
        analysisStatusIds.add(
                Integer.parseInt(SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.Finalized)));
        analysisStatusIds.add(Integer.parseInt(
                SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.NonConforming_depricated)));
        analysisStatusIds.add(
                Integer.parseInt(SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.NotStarted)));
        analysisStatusIds.add(Integer
                .parseInt(SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.TechnicalAcceptance)));
        analysisStatusIds.add(
                Integer.parseInt(SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.Canceled)));
        analysisStatusIds.add(Integer
                .parseInt(SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.TechnicalRejected)));
    }

    static final String configName = ConfigurationProperties.getInstance().getPropertyValue(Property.configurationName);

    /**
     * Inner class to hold parsed test name components
     */
    private static class TestNameParts {
        String mainSection; // e.g., "Examen macroscopique"
        String subSection; // e.g., "Muqueuse vaginale" (null if no subsection)
        String testName; // e.g., "Abondance"

        TestNameParts(String mainSection, String subSection, String testName) {
            this.mainSection = mainSection;
            this.subSection = subSection;
            this.testName = testName;
        }
    }

    public BacterioPatientReport() {
        super();
    }

    @Override
    protected String reportFileName() {
        return "BacterioPatientReport";
    }

    @Override
    protected void createReportParameters() {
        super.createReportParameters();
        reportParameters.put("billingNumberLabel",
                SpringContext.getBean(LocalizationService.class).getLocalizedValueById(ConfigurationProperties
                        .getInstance().getPropertyValue(Property.BILLING_REFERENCE_NUMBER_LABEL)));
        reportParameters.put("footerName", getFooterName());
    }

    private Object getFooterName() {
        if (configName.equals("CI IPCI") || configName.equals("CI LNSP")) {
            return "CILNSPFooter.jasper";
        } else {
            return "";
        }
    }

    @Override
    protected String getHeaderName() {
        return "CDIHeader.jasper";
    }

    @Override
    protected void createReportItems() {
        Set<SampleItem> sampleSet = new HashSet<>();
        boolean isConfirmationSample = sampleService.isConfirmationSample(currentSample);
        List<Analysis> analysisList = analysisService
                .getAnalysesBySampleIdAndStatusId(sampleService.getId(currentSample), analysisStatusIds);
        List<Analysis> filteredAnalysisList = userService.filterAnalysesByLabUnitRoles(systemUserId, analysisList,
                Constants.ROLE_REPORTS);
        List<ClinicalPatientData> currentSampleReportItems = new ArrayList<>(filteredAnalysisList.size());
        currentConclusion = null;

        for (Analysis analysis : filteredAnalysisList) {
            if (!analysis.getTest().isInLabOnly()) {
                // Track the most recent completion date across all analyses in the report.
                // Prefer completedDate, fall back to releasedDate when the former is null.
                java.util.Date analysisDate = analysis.getCompletedDate();
                if (analysisDate == null) {
                    analysisDate = analysis.getReleasedDate();
                }
                if (analysisDate != null
                        && (maxCompletionDate == null || analysisDate.after(maxCompletionDate))) {
                    maxCompletionDate = analysisDate;
                }
                boolean hasParentResult = analysis.getParentResult() != null;
                sampleSet.add(analysis.getSampleItem());
                if (analysis.getTest() != null) {
                    currentAnalysis = analysis;
                    ClinicalPatientData resultsData = buildClinicalPatientData(hasParentResult);

                    // Override the parent's labOrderType (which uses the PROGRAM observation,
                    // i.e. "Routine Bacteriology Testing") with the actual order type saved
                    // by the bacterio entry form ("IN" / "OUT", stored as BacterioTypeExamens
                    // observation). Then translate to a human-readable label.
                    String rawOrderType = SpringContext
                            .getBean(org.openelisglobal.observationhistory.service.ObservationHistoryService.class)
                            .getValueForSample(
                                    org.openelisglobal.observationhistory.service.ObservationHistoryServiceImpl.ObservationType.BACTERIO_ORDER_TYPE,
                                    sampleService.getId(currentSample));
                    if (rawOrderType != null && !rawOrderType.trim().isEmpty()) {
                        String labelKey;
                        if ("IN".equalsIgnoreCase(rawOrderType)) {
                            labelKey = "report.bacterio.ordonnancetype.in";
                        } else if ("OUT".equalsIgnoreCase(rawOrderType)) {
                            labelKey = "report.bacterio.ordonnancetype.out";
                        } else {
                            labelKey = null;
                        }
                        resultsData.setLabOrderType(
                                labelKey != null ? MessageUtil.getMessage(labelKey) : rawOrderType);
                    } else {
                        // No bacterio-specific order type — clear the parent's PROGRAM value
                        // so the report doesn't display "Routine Bacteriology Testing".
                        resultsData.setLabOrderType("");
                    }

                    // Get test name to determine grouping
                    Test test = analysis.getTest();
                    String testName = TestServiceImpl.getLocalizedTestNameWithType(test);
                    String reportingName = "";
                    try {
                        Localization localization = test.getLocalizedReportingName();
                        if (localization != null) {
                            reportingName = localization.getLocalizedValue();
                        }
                    } catch (RuntimeException ignored) {
                        // Lazy loading issue - use test name as fallback
                        reportingName = testName;
                    }

                    // Parse test name to extract section, subsection, and test name
                    // Format: "Section - Subsection - TestName" or "Section - TestName"
                    TestNameParts parts = parseTestName(testName, reportingName);

                    // Set the main section (Macroscopie, Microscopie, Culture)
                    resultsData.setTestSection(parts.mainSection);

                    // Set the subsection in the panelName field (e.g., "Muqueuse vaginale")
                    resultsData.setPanelName(parts.subSection);

                    // Set the actual test name (e.g., "Abondance")
                    // If this is a conditional test (triggered by a parent result), indent it
                    String displayTestName = parts.testName;
                    if (hasParentResult) {
                        displayTestName = "    " + displayTestName; // 4 spaces indentation for conditional tests
                    }
                    resultsData.setTestName(displayTestName);
                    
                    resultsData.setIsBacterioParentTest(ObjectUtils.isNotEmpty(test.getParentTriggerValue()));

                    // Look up the referring site via sample_requester (where the UI stores
                    // it), like the parent PatientReport does. sample_organization is
                    // typically empty on bacterio orders, hence the previously blank
                    // "Site référant" cell.
                    Organization referringOrg = sampleService.getOrganizationRequester(currentSample,
                            org.openelisglobal.common.services.TableIdService.getInstance().REFERRING_ORG_TYPE_ID);
                    currentSiteInfo = referringOrg == null ? "" : referringOrg.getOrganizationName();
                    resultsData.setSiteInfo(currentSiteInfo);

                    // Set urgence (order priority) - check if sample is urgent
                    if (currentSample.getPriority() != null) {
                        String priorityName = currentSample.getPriority().name();
                        // Set "OUI" for URGENT priority, empty otherwise
                        resultsData.setUrgence(!"ROUTINE".equalsIgnoreCase(priorityName) ? "NON" : "OUI");
                    } else {
                        resultsData.setUrgence("");
                    }

                    if (isConfirmationSample) {
                        String alerts = resultsData.getAlerts();
                        if (!GenericValidator.isBlankOrNull(alerts)) {
                            alerts += ", C";
                        } else {
                            alerts = "C";
                        }
                        resultsData.setAlerts(alerts);
                    }
                    resultsData.setBacterioRowType("TEST");

                    // Flora-count tests (e.g. "Nombre de flore") store their value in
                    // bacteriology_flora, not in the result table — so the standard
                    // pipeline left resultsData.result at "En cours". If the analysis is
                    // biologically validated (Finalized), replace it by the actual count
                    // AND inject FLORA_HEADER + FLORA_ROW rows just after it so the JRXML
                    // renders the per-flora details as a small 4-column table.
                    org.openelisglobal.bacteriology.valueholder.BacteriologyFlora finalizedFlora = null;
                    if (Boolean.TRUE.equals(test.getIsFloraCountTest())) {
                        String finalizedStatusId = SpringContext.getBean(IStatusService.class)
                                .getStatusID(AnalysisStatus.Finalized);
                        if (finalizedStatusId != null && finalizedStatusId.equals(analysis.getStatusId())) {
                            try {
                                Integer aid = Integer.valueOf(analysis.getId());
                                Integer tid = Integer.valueOf(test.getId());
                                org.openelisglobal.bacteriology.service.BacteriologyFloraService floraSvc = SpringContext
                                        .getBean(org.openelisglobal.bacteriology.service.BacteriologyFloraService.class);
                                org.openelisglobal.bacteriology.valueholder.BacteriologyFlora flora = floraSvc
                                        .getByAnalysisIdAndTestId(aid, tid);
                                if (flora != null && flora.getFloraCount() != null
                                        && !flora.getFloraCount().trim().isEmpty()) {
                                    resultsData.setResult(flora.getFloraCount());
                                    finalizedFlora = flora;
                                }
                            } catch (NumberFormatException nfe) {
                                // ids non-numeric — keep the in-progress placeholder.
                            }
                        }
                    }

                    reportItems.add(resultsData);
                    currentSampleReportItems.add(resultsData);

                    // Emit FLORA_HEADER + one FLORA_ROW per identified flora, right after
                    // the parent "Nombre de flore" row.
                    if (finalizedFlora != null && finalizedFlora.getDetails() != null
                            && !finalizedFlora.getDetails().isEmpty()) {
                        ClinicalPatientData headerRow = new ClinicalPatientData(resultsData);
                        headerRow.setBacterioRowType("FLORA_HEADER");
                        headerRow.setSeparator(false);
                        reportItems.add(headerRow);
                        currentSampleReportItems.add(headerRow);

                        DictionaryService dictSvc = SpringContext.getBean(DictionaryService.class);
                        List<org.openelisglobal.bacteriology.valueholder.BacteriologyFloraDetail> sorted = new ArrayList<>(
                                finalizedFlora.getDetails());
                        sorted.sort(Comparator.comparing(
                                d -> d.getFloraNumber() != null ? d.getFloraNumber() : Integer.MAX_VALUE));
                        for (org.openelisglobal.bacteriology.valueholder.BacteriologyFloraDetail detail : sorted) {
                            ClinicalPatientData floraRow = new ClinicalPatientData(resultsData);
                            floraRow.setBacterioRowType("FLORA_ROW");
                            floraRow.setSeparator(false);
                            floraRow.setFloraNumber(detail.getFloraNumber() != null
                                    ? String.valueOf(detail.getFloraNumber())
                                    : "");
                            floraRow.setFloraGramType(resolveDictLabel(dictSvc, detail.getGramTypeDictId()));
                            floraRow.setFloraGroupingMode(resolveDictLabel(dictSvc, detail.getGroupingModeDictId()));
                            floraRow.setFloraOtherCharacteristic(
                                    resolveDictLabel(dictSvc, detail.getOtherCharacteristicDictId()));
                            reportItems.add(floraRow);
                            currentSampleReportItems.add(floraRow);
                        }
                    }

                    // For culture tests, add organisms and antibiograms
                    if (parts.mainSection != null && parts.mainSection.toUpperCase().contains("CULTURE")) {
                        addOrganismsAndAntibiograms(analysis, parts.mainSection, resultsData, reportItems,
                                currentSampleReportItems);
                    }
                }
            }
        }
        setCollectionTime(sampleSet, currentSampleReportItems, true);
    }

    /**
     * Add organisms and antibiograms for culture tests
     *
     * @param analysis The culture analysis
     * @param mainSection The main section name (CULTURE)
     * @param patientTemplate A test row already populated with patient/order identity fields,
     *                       used to propagate those onto organism/antibiogram rows
     * @param reportItems The global report items list
     * @param currentSampleReportItems The current sample report items list
     */
    private void addOrganismsAndAntibiograms(Analysis analysis, String mainSection,
            ClinicalPatientData patientTemplate,
            List<ClinicalPatientData> reportItems, List<ClinicalPatientData> currentSampleReportItems) {

        // Suppress organisms and antibiogram rendering until the culture analysis has
        // been biologically validated. While the culture is still "En cours" we don't
        // surface preliminary identifications on the printed report. The "Nombre de
        // germes" row already handles the parent-row label separately.
        String finalizedStatusId = SpringContext.getBean(IStatusService.class)
                .getStatusID(AnalysisStatus.Finalized);
        if (finalizedStatusId != null && !finalizedStatusId.equals(analysis.getStatusId())) {
            return;
        }

        // Get accession number from analysis
        String accessionNumber = "";
        if (analysis.getSampleItem() != null && analysis.getSampleItem().getSample() != null) {
            accessionNumber = analysis.getSampleItem().getSample().getAccessionNumber();
        }
        BacteriologyOrganismService organismService = SpringContext.getBean(BacteriologyOrganismService.class);
        BacteriologyAntibiogramService antibiogramService = SpringContext.getBean(BacteriologyAntibiogramService.class);
        DictionaryService dictionaryService = SpringContext.getBean(DictionaryService.class);

        // Get organisms for this analysis
        Integer analysisIdInt = Integer.parseInt(analysis.getId());

        List<BacteriologyOrganism> organisms = organismService.getOrganismsByAnalysisId(analysisIdInt);


        if (organisms == null || organisms.isEmpty()) {
            return; // No organisms to display
        }

        // Sort organisms by organism number
        organisms.sort(Comparator.comparing(BacteriologyOrganism::getOrganismNumber));

        for (BacteriologyOrganism organism : organisms) {

            if (!organism.getIsActive()) {
                continue; // Skip inactive organisms
            }

            // Create organism display item, copying patient/order identity from the culture test row
            ClinicalPatientData organismData = new ClinicalPatientData(patientTemplate);
            organismData.setTestSection(mainSection);
            organismData.setAccessionNumber(accessionNumber);
            organismData.setSampleType(analysis.getSampleTypeName());
            organismData.setSampleId(analysis.getSampleItem() != null ? analysis.getSampleItem().getId() : null);
            organismData.setPanelName(null);
            organismData.setBacterioRowType("ORGANISM");

            // Resolve organism name
            String organismName = organism.getOrganismNameText();
            if (organismName == null || organismName.isEmpty()) {
                if (organism.getOrganismNameDictId() != null) {
                    Dictionary dict = dictionaryService.getDataForId(organism.getOrganismNameDictId().toString());
                    organismName = (dict != null) ? dict.getLocalizedName() : "Organisme inconnu";
                } else {
                    organismName = "Organisme inconnu";
                }
            }

            // Build organism details
            StringBuilder organismDetails = new StringBuilder();
            if (organism.getGramType() != null && !organism.getGramType().isEmpty()) {
                organismDetails.append(organism.getGramType());
            }
            if (organism.getGroupingMode() != null && !organism.getGroupingMode().isEmpty()) {
                if (organismDetails.length() > 0) {
                    organismDetails.append(", ");
                }
                organismDetails.append(organism.getGroupingMode());
            }
            if (organism.getCapsulePresence() != null && organism.getCapsulePresence()) {
                if (organismDetails.length() > 0) {
                    organismDetails.append(", ");
                }
                organismDetails.append("Capsule présente");
            }

            organismData.setTestName("Organisme " + organism.getOrganismNumber() + " : " + organismName);
            organismData.setResult(organismDetails.length() > 0 ? organismDetails.toString() : "");
            organismData.setParentMarker(false);

            reportItems.add(organismData);
            currentSampleReportItems.add(organismData);

            // Get antibiograms for this organism
            List<BacteriologyAntibiogram> antibiograms = antibiogramService.getAntibiogramsByOrganismId(organism.getId());

            if (antibiograms != null && !antibiograms.isEmpty()) {
                // Sort antibiograms alphabetically by antibiotic name
                antibiograms.sort((a1, a2) -> {
                    String name1 = a1.getAntibioticNameText();
                    String name2 = a2.getAntibioticNameText();
                    if (name1 == null) name1 = "";
                    if (name2 == null) name2 = "";
                    return name1.compareToIgnoreCase(name2);
                });

                // Add antibiogram table header (one row, will render as table column titles in JRXML)
                ClinicalPatientData antibiogramHeader = new ClinicalPatientData(patientTemplate);
                antibiogramHeader.setTestSection(mainSection);
                antibiogramHeader.setPanelName(null);
                antibiogramHeader.setTestName("");
                antibiogramHeader.setResult("");
                antibiogramHeader.setParentMarker(false);
                antibiogramHeader.setAccessionNumber(accessionNumber);
                antibiogramHeader.setSampleType(analysis.getSampleTypeName());
                antibiogramHeader.setSampleId(analysis.getSampleItem() != null ? analysis.getSampleItem().getId() : null);
                antibiogramHeader.setBacterioRowType("ANTIBIOGRAM_HEADER");

                reportItems.add(antibiogramHeader);
                currentSampleReportItems.add(antibiogramHeader);

                // Add each antibiogram result
                for (BacteriologyAntibiogram abg : antibiograms) {
                    if (!abg.getIsActive()) {
                        continue;
                    }

                    ClinicalPatientData abgData = new ClinicalPatientData(patientTemplate);
                    abgData.setTestSection(mainSection);
                    abgData.setPanelName(null);
                    abgData.setAccessionNumber(accessionNumber);
                    abgData.setSampleType(analysis.getSampleTypeName());
                    abgData.setSampleId(analysis.getSampleItem() != null ? analysis.getSampleItem().getId() : null);
                    abgData.setBacterioRowType("ANTIBIOGRAM_ROW");

                    // Resolve antibiotic name
                    String antibioticName = abg.getAntibioticNameText();
                    if (antibioticName == null || antibioticName.isEmpty()) {
                        if (abg.getAntibioticDictId() != null) {
                            Dictionary dict = dictionaryService.getDataForId(abg.getAntibioticDictId().toString());
                            antibioticName = (dict != null) ? dict.getLocalizedName() : "Antibiotique inconnu";
                        } else {
                            antibioticName = "Antibiotique inconnu";
                        }
                    }

                    abgData.setTestName(antibioticName);

                    // Split into separate fields so JRXML can render them in table columns
                    String interpretation = "";
                    if (abg.getInterpretationText() != null && !abg.getInterpretationText().isEmpty()) {
                        interpretation = abg.getInterpretationText();
                    } else if (abg.getResult() != null) {
                        interpretation = abg.getResult();
                    }
                    abgData.setAbgInterpretation(interpretation);

                    abgData.setAbgDiameter(abg.getDiameterMm() != null ? abg.getDiameterMm() + " mm" : "");
                    abgData.setAbgMic(abg.getMicValue() != null ? abg.getMicValue() : "");

                    // Keep result populated for legacy/fallback use
                    abgData.setResult(interpretation);
                    abgData.setParentMarker(false);

                    reportItems.add(abgData);
                    currentSampleReportItems.add(abgData);
                }
            }
        }
    }

    /**
     * Parse test name to extract main section, subsection, and test name Format
     * examples: - "Macroscopie - Muqueuse vaginale - Abondance" -> section="Examen
     * macroscopique", subsection="Muqueuse vaginale", test="Abondance" -
     * "Macroscopie - Turbidité" -> section="Examen macroscopique", subsection=null,
     * test="Turbidité" - "Microscopie - Frottis Coloration Gram - Bactéries" ->
     * section="Examen microscopique", subsection="Frottis Coloration Gram",
     * test="Bactéries" - "Culture - Identification" -> section="Culture",
     * subsection=null, test="Identification"
     *
     * @param testName      The full test name
     * @param reportingName The reporting name (fallback)
     * @return TestNameParts containing parsed components
     */
    private TestNameParts parseTestName(String testName, String reportingName) {
        // Use reporting name if available, otherwise use test name
        String fullName = (reportingName != null && !reportingName.isEmpty()) ? reportingName : testName;

        if (fullName == null || fullName.isEmpty()) {
            return new TestNameParts(MessageUtil.getMessage("bacteriology.section.other"), null, "Unknown");
        }

        // Split by " - " to get parts
        String[] parts = fullName.split("\\s*-\\s*");

        String mainSection;
        String subSection = null;
        String actualTestName;

        if (parts.length >= 3) {
            // Format: "Section - Subsection - TestName"
            // The first part is the section keyword
            String sectionKeyword = parts[0].trim();
            mainSection = mapToMainSection(sectionKeyword);

            // Check if the middle part is a subsection or if it's part of the test name
            // If we have exactly 3 parts, treat middle as subsection
            if (parts.length == 3) {
                subSection = parts[1].trim();
                actualTestName = parts[2].trim();
            } else {
                // More than 3 parts - combine all except first as subsection-testname
                // e.g., "Macroscopie - Muqueuse vaginale - Flore lactobacillaire - Abondance"
                // -> subsection="Muqueuse vaginale - Flore lactobacillaire", test="Abondance"
                StringBuilder subSectionBuilder = new StringBuilder();
                for (int i = 1; i < parts.length - 1; i++) {
                    if (i > 1)
                        subSectionBuilder.append(" - ");
                    subSectionBuilder.append(parts[i].trim());
                }
                subSection = subSectionBuilder.toString();
                actualTestName = parts[parts.length - 1].trim();
            }
        } else if (parts.length == 2) {
            // Format: "Section - TestName" (no subsection)
            String sectionKeyword = parts[0].trim();
            mainSection = mapToMainSection(sectionKeyword);
            actualTestName = parts[1].trim();
            subSection = null; // No subsection
        } else {
            // No separator found - try to infer from keywords
            mainSection = getTestTypeGroup(fullName, fullName);
            actualTestName = fullName;
            subSection = null;
        }

        return new TestNameParts(mainSection, subSection, actualTestName);
    }

    /**
     * Map section keyword to localized main section name
     * 
     * @param sectionKeyword The first part of test name (e.g., "Macroscopie",
     *                       "Microscopie", "Culture")
     * @return Localized section name
     */
    private String mapToMainSection(String sectionKeyword) {
        String keyword = sectionKeyword.toLowerCase();

        if (keyword.contains("macroscop") || keyword.contains("aspect") || keyword.contains("appearance")) {
            return MessageUtil.getMessage("bacteriology.section.macroscopy");
        }

        if (keyword.contains("microscop") || keyword.contains("examen direct") || keyword.contains("direct examination")
                || keyword.contains("gram") || keyword.contains("frottis")) {
            return MessageUtil.getMessage("bacteriology.section.microscopy");
        }

        if (keyword.contains("culture") || keyword.contains("identification") || keyword.contains("antibiogram")
                || keyword.contains("organism") || keyword.contains("organisme") || keyword.contains("sensibilit")) {
            return MessageUtil.getMessage("bacteriology.section.culture");
        }

        return MessageUtil.getMessage("bacteriology.section.other");
    }

    /**
     * Determine test type group based on test name Returns localized section name
     * for grouping in report
     */
    private String getTestTypeGroup(String testName, String reportingName) {
        if (testName == null && reportingName == null) {
            return MessageUtil.getMessage("bacteriology.section.other");
        }

        String combinedName = ((testName != null ? testName : "") + " " + (reportingName != null ? reportingName : ""))
                .toLowerCase();

        // Check for Macroscopy keywords
        if (combinedName.contains("macroscop") || combinedName.contains("aspect")
                || combinedName.contains("appearance")) {
            return MessageUtil.getMessage("bacteriology.section.macroscopy");
        }

        // Check for Microscopy keywords
        if (combinedName.contains("microscop") || combinedName.contains("examen direct")
                || combinedName.contains("direct examination") || combinedName.contains("gram")) {
            return MessageUtil.getMessage("bacteriology.section.microscopy");
        }

        // Check for Culture keywords
        if (combinedName.contains("culture") || combinedName.contains("identification")
                || combinedName.contains("antibiogram") || combinedName.contains("organism")
                || combinedName.contains("organisme") || combinedName.contains("sensibilit")) {
            return MessageUtil.getMessage("bacteriology.section.culture");
        }

        return MessageUtil.getMessage("bacteriology.section.other");
    }

    /**
     * Get test type sort order for organizing report sections
     */
    private int getTestTypeOrder(String testSection) {
        if (testSection == null) {
            return TYPE_ORDER_OTHER;
        }

        // Use message keys to avoid language dependency
        String macroscopy = MessageUtil.getMessage("bacteriology.section.macroscopy");
        String microscopy = MessageUtil.getMessage("bacteriology.section.microscopy");
        String culture = MessageUtil.getMessage("bacteriology.section.culture");

        if (testSection.equals(macroscopy)) {
            return TYPE_ORDER_MACROSCOPY;
        } else if (testSection.equals(microscopy)) {
            return TYPE_ORDER_MICROSCOPY;
        } else if (testSection.equals(culture)) {
            return TYPE_ORDER_CULTURE;
        }

        return TYPE_ORDER_OTHER;
    }

    @Override
    protected void setEmptyResult(ClinicalPatientData data) {
        // Set "In progress" message in both result and analysisStatus fields
        // This ensures non-finalized tests appear in the report (result != null)
        String inProgressMessage = MessageUtil.getMessage("report.test.status.inProgress");
        data.setResult(inProgressMessage);
        data.setAnalysisStatus(inProgressMessage);
    }

    @Override
    protected void setReferredOutResult(ClinicalPatientData data) {
        data.setAlerts("R");
        data.setAnalysisStatus(MessageUtil.getMessage("report.test.status.inProgress"));
    }

    @Override
    protected void postSampleBuild() {
        if (reportItems.isEmpty()) {
            ClinicalPatientData reportItem = buildClinicalPatientData(false);
            reportItem.setTestSection(MessageUtil.getMessage("report.no.results"));
            clinicalReportItems.add(reportItem);
        } else {
            buildReport();
        }
    }

    private void buildReport() {
        // Sort by test type order: Macroscopy -> Microscopy -> Culture
        Collections.sort(reportItems, new Comparator<ClinicalPatientData>() {
            @Override
            public int compare(ClinicalPatientData o1, ClinicalPatientData o2) {
                String o1AccessionNumber = AccessionNumberUtil
                        .getAccessionNumberFromSampleItemAccessionNumber(o1.getAccessionNumber());
                String o2AccessionNumber = AccessionNumberUtil
                        .getAccessionNumberFromSampleItemAccessionNumber(o2.getAccessionNumber());
                int accessionSort = o1AccessionNumber.compareTo(o2AccessionNumber);

                if (accessionSort != 0) {
                    return accessionSort;
                }

                // Sort by test type: Macroscopy < Microscopy < Culture
                int typeSort = getTestTypeOrder(o1.getTestSection()) - getTestTypeOrder(o2.getTestSection());

                if (typeSort != 0) {
                    return typeSort;
                }

                // Within same section, sort by subsection (panelName)
                // Tests without subsection come first (null < non-null)
                String o1SubSection = o1.getPanelName();
                String o2SubSection = o2.getPanelName();

                if (o1SubSection == null && o2SubSection != null) {
                    return -1; // o1 (no subsection) comes before o2 (has subsection)
                } else if (o1SubSection != null && o2SubSection == null) {
                    return 1; // o2 (no subsection) comes before o1 (has subsection)
                } else if (o1SubSection != null && o2SubSection != null) {
                    int subSectionSort = o1SubSection.compareTo(o2SubSection);
                    if (subSectionSort != 0) {
                        return subSectionSort;
                    }
                }

                // Within same type and subsection, sort by sample type (null-safe)
                String o1SampleType = o1.getSampleType();
                String o2SampleType = o2.getSampleType();
                if (o1SampleType != null && o2SampleType != null) {
                    int sampleTypeSort = o1SampleType.compareTo(o2SampleType);
                    if (sampleTypeSort != 0) {
                        return sampleTypeSort;
                    }
                } else if (o1SampleType == null && o2SampleType != null) {
                    return 1; // null comes after non-null
                } else if (o1SampleType != null && o2SampleType == null) {
                    return -1; // non-null comes before null
                }

                // Sort by sample ID (null-safe)
                String o1SampleId = o1.getSampleId();
                String o2SampleId = o2.getSampleId();
                if (o1SampleId != null && o2SampleId != null) {
                    int sampleIdSort = o1SampleId.compareTo(o2SampleId);
                    if (sampleIdSort != 0) {
                        return sampleIdSort;
                    }
                } else if (o1SampleId == null && o2SampleId != null) {
                    return 1; // null comes after non-null
                } else if (o1SampleId != null && o2SampleId == null) {
                    return -1; // non-null comes before null
                }

                if (o1.getParentResult() != null && o2.getParentResult() != null) {
                    int parentSort = Integer.parseInt(o1.getParentResult().getId())
                            - Integer.parseInt(o2.getParentResult().getId());
                    if (parentSort != 0) {
                        return parentSort;
                    }
                }
                return o1.getTestSortOrder() - o2.getTestSortOrder();
            }
        });

        ArrayList<ClinicalPatientData> augmentedList = new ArrayList<>(reportItems.size());
        HashSet<String> parentResults = new HashSet<>();

        for (ClinicalPatientData data : reportItems) {
            if (data.getParentResult() != null && !parentResults.contains(data.getParentResult().getId())) {
                parentResults.add(data.getParentResult().getId());
                ClinicalPatientData marker = new ClinicalPatientData(data);
                ResultService resultResultService = SpringContext.getBean(ResultService.class);
                Result result = data.getParentResult();
                marker.setTestName(resultResultService.getSimpleResultValue(result));
                marker.setResult(null);
                marker.setTestRefRange(null);
                marker.setParentMarker(true);
                augmentedList.add(marker);
            }

            augmentedList.add(data);
        }

        reportItems = augmentedList;

        String currentSubSection = null;
        String currentTestType = null;

        for (ClinicalPatientData reportItem : reportItems) {
            // Mark test type separators (main sections: Macroscopie, Microscopie, Culture)
            if (!reportItem.getTestSection().equals(currentTestType)) {
                currentTestType = reportItem.getTestSection();
                currentSubSection = null; // Reset subsection when section changes
                reportItem.setSeparator(true);
            }
            // Mark subsection separators
            else if (reportItem.getPanelName() != null && !reportItem.getPanelName().equals(currentSubSection)) {
                currentSubSection = reportItem.getPanelName();
                reportItem.setSeparator(true);
            }
            // Mark transition from subsection to no subsection
            else if (reportItem.getPanelName() == null && currentSubSection != null) {
                currentSubSection = null;
                reportItem.setSeparator(true);
            }

            // Extract base accession number (remove suffix after last '-' if present)
            int dividerIndex = reportItem.getAccessionNumber().lastIndexOf("-");
            String accessionNumber;
            if (dividerIndex > 0) {
                accessionNumber = reportItem.getAccessionNumber().substring(0, dividerIndex);
            } else {
                // No divider found - use the full accession number
                accessionNumber = reportItem.getAccessionNumber();
            }
            reportItem.setAccessionNumber(accessionNumber);

            // Set labNo (ordonnance number) - same as accession number for bacteriology
            reportItem.setLabNo(accessionNumber);

            // Set status based on completion
            boolean isComplete = sampleCompleteMap.get(accessionNumber);
            reportItem
                    .setStatus(MessageUtil.getMessage(isComplete ? "report.status.complete" : "report.status.partial"));
            reportItem.setCompleteFlag(reportItem.getStatus());

            // Set urgence - default to empty if not set
            if (reportItem.getUrgence() == null) {
                reportItem.setUrgence("");
            }
            if (reportItem.isCorrectedResult()) {
                if (reportItem.getNote() != null && reportItem.getNote().length() > 0) {
                    reportItem.setNote(MessageUtil.getMessage("result.corrected") + "<br/>" + reportItem.getNote());
                } else {
                    reportItem.setNote(MessageUtil.getMessage("result.corrected"));
                }
            }

            reportItem
                    .setCorrectedResult(sampleCorrectedMap.get(reportItem.getAccessionNumber().split("_")[0]) != null);
        }

        // Re-order culture section: root TEST rows -> "Nombre de germes" -> ORGANISM+ANTIBIOGRAM_* grouped
        // Also appends a LEGEND row at the end so JRXML can render the S/I/R legend.
        reportItems = reorderCultureSection(reportItems);

        // Propagate the most recent test completion date onto every row (max across all rows).
        // The JRXML reads testCompletedDate to display "Date de réalisation".
        propagateMaxCompletionDate(reportItems);

        // Final step: pair up consecutive TEST rows in Macroscopy/Microscopy to render two tests per line
        reportItems = pairMacroMicroTests(reportItems);
    }

    /**
     * Find the most recent testCompletedDate among all rows and assign it back to every row
     * so the JRXML can show a single "Date de réalisation" value. Dates are compared as the
     * String produced by DateUtil.convertSqlDateToStringDate(), which uses the configured
     * locale format. Since all rows share the same format, lexicographic compare on dd/MM/yyyy
     * would be wrong, so we parse and pick the latest by epoch order.
     */
    private void propagateMaxCompletionDate(List<ClinicalPatientData> items) {
        if (maxCompletionDate == null) {
            return;
        }
        String maxDate = new java.text.SimpleDateFormat(
                org.openelisglobal.common.util.DateUtil.getDateFormat())
                .format(maxCompletionDate);
        for (ClinicalPatientData item : items) {
            item.setTestCompletedDate(maxDate);
        }
    }

    /**
     * For the Culture section, ensure that simple TEST rows (the root culture tests like
     * "Sécrétions vaginales : Positive") appear BEFORE any ORGANISM / ANTIBIOGRAM rows,
     * and inject a synthetic "Nombre de germes : N" row right after the last culture TEST row.
     * Other sections (Macroscopy/Microscopy) are untouched.
     */
    private List<ClinicalPatientData> reorderCultureSection(List<ClinicalPatientData> items) {
        String culture = MessageUtil.getMessage("bacteriology.section.culture");

        List<ClinicalPatientData> nonCulture = new ArrayList<>();
        List<ClinicalPatientData> cultureTests = new ArrayList<>();
        List<ClinicalPatientData> cultureOrganisms = new ArrayList<>();
        ClinicalPatientData firstCultureTemplate = null;

        for (ClinicalPatientData item : items) {
            if (culture.equals(item.getTestSection())) {
                String type = item.getBacterioRowType();
                if ("TEST".equals(type) || type == null) {
                    cultureTests.add(item);
                    if (firstCultureTemplate == null) {
                        firstCultureTemplate = item;
                    }
                } else {
                    cultureOrganisms.add(item);
                }
            } else {
                nonCulture.add(item);
            }
        }

        if (cultureTests.isEmpty() && cultureOrganisms.isEmpty()) {
            return items;
        }

        int organismCount = 0;
        for (ClinicalPatientData item : cultureOrganisms) {
            if ("ORGANISM".equals(item.getBacterioRowType())) {
                organismCount++;
            }
        }

        if (firstCultureTemplate != null) {
            ClinicalPatientData countRow = new ClinicalPatientData(firstCultureTemplate);
            countRow.setTestSection(culture);
            countRow.setPanelName(null);
            countRow.setBacterioRowType("TEST");
            countRow.setIsBacterioParentTest(false);
            countRow.setParentMarker(false);
            countRow.setTestName("Nombre de germes");
            // If the culture root test is not biologically validated yet (its result is the
            // "in progress" placeholder set by setEmptyResult), the germ count is not
            // meaningful — display "En cours" instead of the raw number.
            String inProgressMsg = MessageUtil.getMessage("report.test.status.inProgress");
            boolean cultureInProgress = inProgressMsg != null
                    && inProgressMsg.equals(firstCultureTemplate.getResult());
            countRow.setResult(cultureInProgress ? inProgressMsg : String.valueOf(organismCount));
            countRow.setSeparator(false);
            cultureTests.add(countRow);
        }

        // Append a LEGEND row at the very end of the culture section so the JRXML can
        // render the S/I/R legend once after all antibiograms.
        if (!cultureOrganisms.isEmpty() && firstCultureTemplate != null) {
            ClinicalPatientData legendRow = new ClinicalPatientData(firstCultureTemplate);
            legendRow.setTestSection(culture);
            legendRow.setPanelName(null);
            legendRow.setBacterioRowType("LEGEND");
            legendRow.setIsBacterioParentTest(false);
            legendRow.setParentMarker(false);
            legendRow.setTestName("");
            legendRow.setResult("");
            legendRow.setSeparator(false);
            cultureOrganisms.add(legendRow);
        }

        List<ClinicalPatientData> result = new ArrayList<>(items.size() + 2);
        result.addAll(nonCulture);
        result.addAll(cultureTests);
        result.addAll(cultureOrganisms);
        return result;
    }

    /**
     * Pair consecutive simple TEST rows (Macroscopy / Microscopy only) sharing the same
     * section, subsection and accession into a single TEST_PAIR row, so the JRXML can
     * render two tests per line. Tests with parent triggers, parent markers, or in the
     * Culture section stay as single rows.
     */
    private List<ClinicalPatientData> pairMacroMicroTests(List<ClinicalPatientData> items) {
        List<ClinicalPatientData> paired = new ArrayList<>(items.size());
        String macroscopy = MessageUtil.getMessage("bacteriology.section.macroscopy");
        String microscopy = MessageUtil.getMessage("bacteriology.section.microscopy");

        int i = 0;
        while (i < items.size()) {
            ClinicalPatientData current = items.get(i);
            // Pairable = simple TEST in Macro/Micro, not a parent (has subtests), not a parent marker,
            // not a conditional subtest (those have parentResult != null and must stay aligned).
            // separator flag is ignored: first item of a subsection is still pairable.
            boolean isPairable = "TEST".equals(current.getBacterioRowType())
                    && (macroscopy.equals(current.getTestSection()) || microscopy.equals(current.getTestSection()))
                    && !current.getIsBacterioParentTest()
                    && !current.getParentMarker()
                    && current.getParentResult() == null;

            if (!isPairable) {
                paired.add(current);
                i++;
                continue;
            }

            // Look ahead for a partner with matching section/subsection/accession.
            // The partner must also not be a separator (start of a new subsection) — that would
            // visually break the layout. So here separator IS respected for the partner.
            ClinicalPatientData partner = null;
            int j = i + 1;
            if (j < items.size()) {
                ClinicalPatientData candidate = items.get(j);
                boolean candidatePairable = "TEST".equals(candidate.getBacterioRowType())
                        && current.getTestSection().equals(candidate.getTestSection())
                        && java.util.Objects.equals(current.getPanelName(), candidate.getPanelName())
                        && current.getAccessionNumber().equals(candidate.getAccessionNumber())
                        && !candidate.getIsBacterioParentTest()
                        && !candidate.getParentMarker()
                        && candidate.getParentResult() == null
                        && !Boolean.TRUE.equals(candidate.getSeparator());
                if (candidatePairable) {
                    partner = candidate;
                }
            }

            current.setBacterioRowType("TEST_PAIR");
            current.setTestNameLeft(current.getTestName());
            current.setResultLeft(current.getResult());
            if (partner != null) {
                current.setTestNameRight(partner.getTestName());
                current.setResultRight(partner.getResult());
                paired.add(current);
                i += 2;
            } else {
                current.setTestNameRight(null);
                current.setResultRight(null);
                paired.add(current);
                i++;
            }
        }
        return paired;
    }

    @Override
    protected String getReportNameForParameterPage() {
        return MessageUtil.getMessage("openreports.patientTestStatus");
    }

    @Override
    public JRDataSource getReportDataSource() throws IllegalStateException {
        if (!initialized) {
            throw new IllegalStateException("initializeReport not called first");
        }

        return errorFound ? new JRBeanCollectionDataSource(errorMsgs) : new JRBeanCollectionDataSource(reportItems);
    }

    @Override
    protected void initializeReportItems() {
        super.initializeReportItems();
        clinicalReportItems = new ArrayList<>();
    }

    @Override
    protected void setReferredResult(ClinicalPatientData data, Result result) {
        data.setResult(data.getResult());
        data.setAlerts(getResultFlag(result, null));
    }

    @Override
    protected boolean appendUOMToRange() {
        return false;
    }

    @Override
    protected boolean augmentResultWithFlag() {
        return false;
    }

    @Override
    protected boolean useReportingDescription() {
        return true;
    }

    /**
     * Resolve a dictionary id to its localized label, returning an empty string
     * when the id is missing or the entry can't be loaded. Used to flatten the
     * dictionary FKs on each BacteriologyFloraDetail into display strings.
     */
    private String resolveDictLabel(DictionaryService dictSvc, Integer dictId) {
        if (dictId == null || dictSvc == null) {
            return "";
        }
        try {
            Dictionary dict = dictSvc.getDataForId(String.valueOf(dictId));
            if (dict == null) {
                return "";
            }
            String label = dict.getLocalizedName();
            if (label == null || label.trim().isEmpty()) {
                label = dict.getDictEntry();
            }
            return label == null ? "" : label;
        } catch (Exception e) {
            return "";
        }
    }
}

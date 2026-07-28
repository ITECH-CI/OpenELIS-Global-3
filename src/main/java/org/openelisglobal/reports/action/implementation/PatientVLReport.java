package org.openelisglobal.reports.action.implementation;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.provider.validation.AccessionNumberValidatorFactory.AccessionFormat;
import org.openelisglobal.common.provider.validation.AlphanumAccessionValidator;
import org.openelisglobal.common.services.IReportTrackingService;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.ReportTrackingService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.observationhistory.service.ObservationHistoryService;
import org.openelisglobal.observationhistory.service.ObservationHistoryServiceImpl.ObservationType;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.reports.action.implementation.reportBeans.VLReportData;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sampleorganization.service.SampleOrganizationService;
import org.openelisglobal.sampleorganization.valueholder.SampleOrganization;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestServiceImpl;

public abstract class PatientVLReport extends RetroCIPatientReport {

    protected static final long YEAR = 1000L * 60L * 60L * 24L * 365L;
    protected static final long THREE_YEARS = YEAR * 3L;
    protected static final long WEEK = YEAR / 52L;
    protected static final long MONTH = YEAR / 12L;

    private AnalysisService analysisService = SpringContext.getBean(AnalysisService.class);
    private ResultService resultService = SpringContext.getBean(ResultService.class);
    private SampleOrganizationService orgService = SpringContext.getBean(SampleOrganizationService.class);
    private OrganizationService oService = SpringContext.getBean(OrganizationService.class);
    private ObservationHistoryService ohService = SpringContext.getBean(ObservationHistoryService.class);
    private org.openelisglobal.dictionary.service.DictionaryService dictionaryService = SpringContext
            .getBean(org.openelisglobal.dictionary.service.DictionaryService.class);
    private org.openelisglobal.siteinformation.service.SiteInformationService siteInformationService = SpringContext
            .getBean(org.openelisglobal.siteinformation.service.SiteInformationService.class);

    /**
     * Préfixe des clés de config (domain site_information « viralLoadReportConfig
     * ») selon le type VIH testé. HIV-2 pur → « vl.hiv2 » ; tout le reste (HIV-1,
     * HIV-1+2, repli) → « vl.hiv1 ».
     */
    private String vlConfigPrefixForKey(String hivTestedTypeKey) {
        if ("HIVStatus.HIV_2".equals(hivTestedTypeKey)) {
            return "vl.hiv2";
        }
        return "vl.hiv1";
    }

    /**
     * Valeur d'une clé de config du rapport de charge virale, ou null si absente ou
     * vide (le JRXML applique alors sa valeur par défaut).
     */
    private String getVlConfigValue(String name) {
        org.openelisglobal.siteinformation.valueholder.SiteInformation info = siteInformationService
                .getSiteInformationByName(name);
        if (info == null || GenericValidator.isBlankOrNull(info.getValue())) {
            return null;
        }
        return info.getValue();
    }

    /**
     * Résout les contenus paramétrables (trousse, automate, PCR, seuils,
     * interprétation, texte « indétectable ») selon le type VIH testé et les pose
     * dans le bean. Les seuils sont stockés « support:valeur;support:valeur » et
     * éclatés en une ligne « support : valeur » par élément (dépendants de la
     * trousse : COBAS VIH-1 en porte 3).
     */
    private void setReportConfig(VLReportData data, String hivTestedTypeKey) {
        String prefix = vlConfigPrefixForKey(hivTestedTypeKey);
        data.setKitLabel(getVlConfigValue(prefix + ".kitLabel"));
        data.setAutomateLabel(getVlConfigValue(prefix + ".automateLabel"));
        data.setPcrLabel(getVlConfigValue(prefix + ".pcrLabel"));
        data.setInterpretation(getVlConfigValue(prefix + ".interpretation"));
        data.setUndetectableText(getVlConfigValue(prefix + ".undetectable"));

        List<String> thresholds = new ArrayList<>();
        String raw = getVlConfigValue(prefix + ".thresholds");
        if (!GenericValidator.isBlankOrNull(raw)) {
            for (String entry : raw.split(";")) {
                String trimmed = entry.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                int sep = trimmed.indexOf(':');
                if (sep > 0) {
                    thresholds.add("Seuil de détection de la technique " + trimmed.substring(0, sep).trim() + " : "
                            + trimmed.substring(sep + 1).trim());
                } else {
                    thresholds.add(trimmed);
                }
            }
        }
        data.setThresholdsList(thresholds);
        data.setThresholdsText(thresholds.isEmpty() ? null : String.join("\n", thresholds));

        // Accréditation (indépendante du type VIH testé) : toggle + note. Le logo
        // est injecté en paramètre Jasper dans createReportParameters().
        data.setAccreditationEnabled(Boolean.parseBoolean(getVlConfigValue("vl.accreditation.enabled")));
        data.setAccreditationNote(getVlConfigValue("vl.accreditation.note"));
    }

    private org.openelisglobal.image.service.ImageService imageService = SpringContext
            .getBean(org.openelisglobal.image.service.ImageService.class);

    protected List<VLReportData> reportItems;
    private String invalidValue = MessageUtil.getMessage("report.test.status.inProgress");

    /**
     * Injecte le logo d'accréditation en paramètre Jasper (InputStream), seulement
     * si le labo est accrédité et qu'un logo a été uploadé. Même mécanisme que les
     * logos d'en-tête (Report.createReportParameters). Le JRXML l'affiche via
     * $P{accreditationLogo} en onErrorType="Blank" : paramètre absent = rien.
     */
    @Override
    protected void createReportParameters() {
        super.createReportParameters();
        if (Boolean.parseBoolean(getVlConfigValue("vl.accreditation.enabled"))) {
            java.util.Optional<org.openelisglobal.image.valueholder.Image> logo = imageService
                    .getImageBySiteInfoName("vl.accreditation.logo");
            if (logo.isPresent() && logo.get().getImage() != null) {
                reportParameters.put("accreditationLogo", new java.io.ByteArrayInputStream(logo.get().getImage()));
            }
        }
    }

    @Override
    protected void initializeReportItems() {
        reportItems = new ArrayList<>();
    }

    @Override
    protected String getReportNameForReport() {
        return MessageUtil.getMessage("reports.label.patient.VL");
    }

    @Override
    public JRDataSource getReportDataSource() throws IllegalStateException {
        if (!initialized) {
            throw new IllegalStateException("initializeReport not called first");
        }

        return errorFound ? new JRBeanCollectionDataSource(errorMsgs) : new JRBeanCollectionDataSource(reportItems);
    }

    @Override
    protected void createReportItems() {
        VLReportData data = new VLReportData();

        setPatientInfo(data);
        setTestInfo(data);
        reportItems.add(data);
    }

    protected void setTestInfo(VLReportData data) {
        boolean atLeastOneAnalysisNotValidated = false;
        List<Analysis> analysisList = analysisService.getAnalysesBySampleId(reportSample.getId());
        Timestamp lastReport = SpringContext.getBean(IReportTrackingService.class)
                .getTimeOfLastNamedReport(reportSample, ReportTrackingService.ReportType.PATIENT, requestedReport);
        Boolean mayBeDuplicate = lastReport != null;

        Date maxCompleationDate = null;
        long maxCompleationTime = 0L;
        Date maxReleasedDate = null;
        long maxReleasedTime = 0L;
        // String invalidValue =
        // MessageUtil.getMessage("report.test.status.inProgress");

        for (Analysis analysis : analysisList) {

            data.setSampleTypeName(analysis.getSampleTypeName());

            if (analysis.getCompletedDate() != null) {
                if (analysis.getCompletedDate().getTime() > maxCompleationTime) {
                    maxCompleationDate = analysis.getCompletedDate();
                    maxCompleationTime = maxCompleationDate.getTime();
                }
            }
            if (analysis.getReleasedDate() != null) {
                if (analysis.getReleasedDate().getTime() > maxReleasedTime) {
                    maxReleasedDate = analysis.getReleasedDate();
                    maxReleasedTime = maxReleasedDate.getTime();
                }
            }

            String testName = TestServiceImpl.getUserLocalizedTestName(analysis.getTest());

            List<Result> resultList = resultService.getResultsByAnalysis(analysis);

            boolean valid = ANALYSIS_FINALIZED_STATUS_ID.equals(analysis.getStatusId());
            if (!valid) {
                atLeastOneAnalysisNotValidated = true;
            }

            if (testName.equals("Viral Load")) {
                if (valid) {
                    // data.setShowVirologie(Boolean.TRUE);
                    String resultValue = "";
                    if (resultList.size() > 0) {
                        resultValue = resultList.get(resultList.size() - 1).getValue(false);
                    }

                    String baseValue = resultValue;
                    if (!GenericValidator.isBlankOrNull(resultValue) && resultValue.contains("(")) {
                        String[] splitValue = resultValue.split("\\(");
                        data.setAmpli2(splitValue[0]);
                        baseValue = splitValue[0];
                    } else {
                        data.setAmpli2(resultValue);
                    }
                    if (!GenericValidator.isBlankOrNull(baseValue) && !"0".equals(baseValue)) {
                        try {
                            double viralLoad = Double.parseDouble(baseValue);
                            data.setAmpli2lo(String.format("%.3g%n", Math.log10(viralLoad)));
                        } catch (NumberFormatException e) {
                            data.setAmpli2lo("");
                        }
                    }
                }
            }
            if (mayBeDuplicate
                    && SpringContext.getBean(IStatusService.class).matches(analysis.getStatusId(),
                            AnalysisStatus.Finalized)
                    && lastReport != null && lastReport.before(analysis.getLastupdated())) {
                mayBeDuplicate = false;
            }
        }
        if (maxCompleationDate != null) {
            data.setCompleationdate(DateUtil.convertSqlDateToStringDate(maxCompleationDate));
        }
        if (maxReleasedDate != null) {
            data.setReleasedate(DateUtil.convertSqlDateToStringDate(maxReleasedDate));
        }

        data.setDuplicateReport(mayBeDuplicate);
        data.setStatus(atLeastOneAnalysisNotValidated ? MessageUtil.getMessage("report.status.partial")
                : MessageUtil.getMessage("report.status.complete"));
    }

    protected void setPatientInfo(VLReportData data) {

        data.setVlSuckle(ohService.getMostRecentValueForPatient(ObservationType.VL_SUCKLE, reportPatient.getId()));
        data.setVlPregnancy(
                ohService.getMostRecentValueForPatient(ObservationType.VL_PREGNANCY, reportPatient.getId()));
        data.setvih(ohService.getMostRecentValueForPatient(ObservationType.HIV_STATUS, reportPatient.getId()));
        // Type VIH TESTÉ : distinct du statut patient (vih). On le résout depuis l'ID
        // de dictionnaire brut pour porter DEUX formes dans le bean :
        // - hivTestedType = libellé (affichage) ;
        // - hivTestedTypeKey = display_key STABLE (ex. « HIVStatus.HIV_1 »),
        // discriminant
        // robuste du conditionnel JRXML (indépendant des traductions).
        // Repli : si aucune observation hivTestedType (demandes antérieures), on
        // retombe
        // sur hivStatus (le statut patient), préservant le comportement d'avant.
        String hivTypeDictId = getRawObservationId(ObservationType.HIV_TESTED_TYPE, reportPatient.getId());
        if (GenericValidator.isBlankOrNull(hivTypeDictId)) {
            hivTypeDictId = getRawObservationId(ObservationType.HIV_STATUS, reportPatient.getId());
        }
        if (!GenericValidator.isBlankOrNull(hivTypeDictId)) {
            org.openelisglobal.dictionary.valueholder.Dictionary dict = dictionaryService.getDataForId(hivTypeDictId);
            if (dict != null) {
                data.setHivTestedType(dict.getLocalizedName());
                data.setHivTestedTypeKey(dict.getNameKey());
            }
        }
        // Contenus paramétrables (trousse/automate/seuils/interprétation) selon le
        // type VIH testé. Résolus depuis site_information ; repli JRXML si absents.
        setReportConfig(data, data.getHivTestedTypeKey());
        data.setSubjectno(reportPatient.getNationalId());
        data.setSitesubjectno(reportPatient.getExternalId());
        data.setBirth_date(reportPatient.getBirthDateForDisplay());
        data.setAge(DateUtil.getCurrentAgeForDate(reportPatient.getBirthDate(), reportSample.getCollectionDate()));
        data.setGender(reportPatient.getGender());
        data.setCollectiondate(DateUtil.convertTimestampToStringDateAndTime(reportSample.getCollectionDate()));
        SampleOrganization sampleOrg = new SampleOrganization();
        sampleOrg.setSample(reportSample);
        orgService.getDataBySample(sampleOrg);
        data.setServicename(sampleOrg.getId() == null ? ""
                : oService.get(sampleOrg.getOrganization().getId()).getOrganizationName());
        data.setDoctor(getObservationValues(OBSERVATION_DOCTOR_ID));
        if (AccessionFormat.ALPHANUM.toString()
                .equals(ConfigurationProperties.getInstance().getPropertyValue(Property.AccessionFormat))) {
            data.setAccessionNumber(
                    AlphanumAccessionValidator.convertAlphaNumLabNumForDisplay(reportSample.getAccessionNumber()));
        } else {
            data.setAccessionNumber(reportSample.getAccessionNumber());
        }
        data.setReceptiondate(DateUtil.convertTimestampToStringDateAndTime(reportSample.getReceivedTimestamp()));
        Timestamp collectionDate = reportSample.getCollectionDate();

        if (collectionDate != null) {
            long collectionTime = collectionDate.getTime() - reportPatient.getBirthDate().getTime();

            if (collectionTime < THREE_YEARS) {
                data.setAgeWeek(String.valueOf((int) Math.floor(collectionTime / WEEK)));
            } else {
                data.setAgeMonth(String.valueOf((int) Math.floor(collectionTime / MONTH)));
            }
        }
        data.getSampleQaEventItems(reportSample);
    }

    /**
     * Renvoie la valeur BRUTE (id de dictionnaire) de la dernière observation d'un
     * type pour un patient, sans décodage en libellé (contrairement à
     * getMostRecentValueForPatient). null si absente.
     */
    private String getRawObservationId(ObservationType type, String patientId) {
        org.openelisglobal.observationhistory.valueholder.ObservationHistory obs = ohService
                .getLastObservationForPatient(type, patientId);
        return obs == null ? null : obs.getValue();
    }

    @Override
    protected String getProjectId() {
        return ANTIRETROVIRAL_STUDY_ID + ":" + ANTIRETROVIRAL_FOLLOW_UP_STUDY_ID + ":" + VL_STUDY_ID;
        // return ANTIRETROVIRAL_ID;
    }
}

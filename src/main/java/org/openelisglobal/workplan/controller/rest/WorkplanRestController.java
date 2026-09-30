package org.openelisglobal.workplan.controller.rest;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.formfields.FormFields;
import org.openelisglobal.common.formfields.FormFields.Field;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.observationhistory.service.ObservationHistoryService;
import org.openelisglobal.observationhistory.service.ObservationHistoryServiceImpl.ObservationType;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

@Controller
public class WorkplanRestController extends BaseRestController {

    @Autowired
    protected TestService testService;

    protected static List<Integer> statusList;
    protected static boolean useReceptionTime = FormFields.getInstance().useField(Field.SampleEntryUseReceptionHour);
    protected static List<String> nfsTestIdList;

    @PostConstruct
    private void initialize() {
        if (statusList == null) {
            statusList = new ArrayList<>();
            statusList.add(Integer
                    .parseInt(SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.NotStarted)));
            statusList.add(Integer.parseInt(
                    SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.BiologistRejected)));
            statusList.add(Integer.parseInt(
                    SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.TechnicalRejected)));
            statusList.add(Integer.parseInt(
                    SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.NonConforming_depricated)));
        }

        if (nfsTestIdList == null) {
            nfsTestIdList = new ArrayList<>();
            nfsTestIdList.add(getTestId("GB"));
            nfsTestIdList.add(getTestId("Neut %"));
            nfsTestIdList.add(getTestId("Lymph %"));
            nfsTestIdList.add(getTestId("Mono %"));
            nfsTestIdList.add(getTestId("Eo %"));
            nfsTestIdList.add(getTestId("Baso %"));
            nfsTestIdList.add(getTestId("GR"));
            nfsTestIdList.add(getTestId("Hb"));
            nfsTestIdList.add(getTestId("HCT"));
            nfsTestIdList.add(getTestId("VGM"));
            nfsTestIdList.add(getTestId("TCMH"));
            nfsTestIdList.add(getTestId("CCMH"));
            nfsTestIdList.add(getTestId("PLQ"));
        }
    }

    protected List<IdValuePair> adjustNFSTests(List<IdValuePair> allTestsList) {
        List<IdValuePair> adjustedList = new ArrayList<>(allTestsList.size());
        for (IdValuePair idValuePair : allTestsList) {
            if (!nfsTestIdList.contains(idValuePair.getId())) {
                adjustedList.add(idValuePair);
            }
        }
        // add NFS to the list
        adjustedList.add(new IdValuePair("NFS", "NFS"));
        return adjustedList;
    }

    protected boolean allNFSTestsRequested(List<String> testIdList) {
        return (testIdList.containsAll(nfsTestIdList));
    }

    protected String getTestId(String testName) {
        Test test = testService.getTestByLocalizedName(testName);
        if (test == null) {
            test = new Test();
        }
        return test.getId();
    }

    protected String getSubjectNumber(Analysis analysis) {
        if (ConfigurationProperties.getInstance().isPropertyValueEqual(Property.SUBJECT_ON_WORKPLAN, "true")) {
            PatientService patientService = SpringContext.getBean(PatientService.class);
            SampleHumanService sampleHumanService = SpringContext.getBean(SampleHumanService.class);
            Patient patient = sampleHumanService.getPatientForSample(analysis.getSampleItem().getSample());
            return patientService.getSubjectNumber(patient);
        } else {
            return "";
        }
    }

    protected String getPatientName(Analysis analysis) {
        if (ConfigurationProperties.getInstance().isPropertyValueEqual(Property.configurationName, "Haiti LNSP")) {
            Sample sample = analysis.getSampleItem().getSample();
            PatientService patientService = SpringContext.getBean(PatientService.class);
            SampleHumanService sampleHumanService = SpringContext.getBean(SampleHumanService.class);
            Patient patient = sampleHumanService.getPatientForSample(sample);
            List<String> values = new ArrayList<>();
            values.add(patientService.getLastName(patient) == null ? ""
                    : patientService.getLastName(patient).toUpperCase());
            values.add(patientService.getNationalId(patient));

            String referringPatientId = SpringContext.getBean(ObservationHistoryService.class)
                    .getValueForSample(ObservationType.REFERRERS_PATIENT_ID, sample.getId());
            values.add(referringPatientId == null ? "" : referringPatientId);
            return StringUtil.buildDelimitedStringFromList(values, " / ", true);

        } else {
            return "";
        }
    }

    /**
     * Filtres optionnels communs aux plans de travail : n° labo (contient, sans
     * casse) et période de RÉCEPTION (bornes incluses, format de date de la
     * locale). Appliqués sur les analyses avant construction des lignes, donc avant
     * la pagination serveur ; l'impression reprend la liste affichée.
     */
    protected static class WorkplanFilter {
        private final String labNumber;
        private final LocalDate from;
        private final LocalDate to;

        private WorkplanFilter(String labNumber, LocalDate from, LocalDate to) {
            this.labNumber = labNumber;
            this.from = from;
            this.to = to;
        }

        static WorkplanFilter fromRequest(HttpServletRequest request) {
            String labNumber = request.getParameter("labNumber");
            labNumber = labNumber == null || labNumber.isBlank() ? null : labNumber.trim().toLowerCase();
            return new WorkplanFilter(labNumber, parseDate(request.getParameter("startDate")),
                    parseDate(request.getParameter("endDate")));
        }

        private static LocalDate parseDate(String date) {
            if (date == null || date.isBlank()) {
                return null;
            }
            try {
                return DateUtil.convertStringDateToLocalDate(date.trim());
            } catch (RuntimeException e) {
                return null; // date illisible : filtre ignoré plutôt qu'une erreur
            }
        }

        boolean isEmpty() {
            return labNumber == null && from == null && to == null;
        }

        List<Analysis> apply(List<Analysis> analyses) {
            if (isEmpty() || analyses == null) {
                return analyses;
            }
            List<Analysis> kept = new ArrayList<>();
            for (Analysis analysis : analyses) {
                if (accept(analysis.getSampleItem().getSample())) {
                    kept.add(analysis);
                }
            }
            return kept;
        }

        private boolean accept(Sample sample) {
            if (labNumber != null && (sample.getAccessionNumber() == null
                    || !sample.getAccessionNumber().toLowerCase().contains(labNumber))) {
                return false;
            }
            if (from != null || to != null) {
                if (sample.getReceivedTimestamp() == null) {
                    return false;
                }
                LocalDate received = sample.getReceivedTimestamp().toLocalDateTime().toLocalDate();
                if ((from != null && received.isBefore(from)) || (to != null && received.isAfter(to))) {
                    return false;
                }
            }
            return true;
        }
    }

    protected String getReceivedDateDisplay(Sample sample) {
        String receptionTime = useReceptionTime ? " " + sample.getReceivedTimeForDisplay() : "";
        return sample.getReceivedDateForDisplay() + receptionTime;
    }

    class ValueComparator implements Comparator<IdValuePair> {

        @Override
        public int compare(IdValuePair p1, IdValuePair p2) {
            return p1.getValue().toUpperCase().compareTo(p2.getValue().toUpperCase());
        }
    }
}

package org.openelisglobal.dataexchange.service.order;

import java.util.ArrayList;
import java.util.List;

/**
 * Une demande reçue (une Task de la plateforme, ou une demande saisie
 * localement) telle que la montre la fenêtre « Demandes reçues ». Lue sur
 * electronic_order (colonnes et data), sans appel au serveur FHIR.
 */
public class ReceivedOrderRow {

    public enum Etape {
        A_TRAITER, PRISE_EN_CHARGE, REFUSEE, ANNULEE, ANNULEE_DEMANDEUR
    }

    /** Une demande électronique de la demande (un examen). */
    public static class Exam {
        private String electronicOrderId;
        private String externalOrderId;
        private String testName;

        public Exam(String electronicOrderId, String externalOrderId, String testName) {
            this.electronicOrderId = electronicOrderId;
            this.externalOrderId = externalOrderId;
            this.testName = testName;
        }

        public String getElectronicOrderId() {
            return electronicOrderId;
        }

        public String getExternalOrderId() {
            return externalOrderId;
        }

        public String getTestName() {
            return testName;
        }
    }

    private String id; // première demande électronique de la demande
    private String taskId;
    private String requestNumber;
    private String requestDate;
    private long receivedAtMillis;
    private long ageMinutes;
    private String priority;
    private String patientFirstName;
    private String patientLastName;
    private String cmuNumber;
    private boolean provisionalIdentity;
    private String requestingFacility;
    private String prescriber;
    private String origine;
    private Etape etape;
    private boolean cancelledByRequester;
    private String labNumber;
    private boolean readOnly;
    private List<Exam> exams = new ArrayList<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getRequestNumber() {
        return requestNumber;
    }

    public void setRequestNumber(String requestNumber) {
        this.requestNumber = requestNumber;
    }

    public String getRequestDate() {
        return requestDate;
    }

    public void setRequestDate(String requestDate) {
        this.requestDate = requestDate;
    }

    public long getReceivedAtMillis() {
        return receivedAtMillis;
    }

    public void setReceivedAtMillis(long receivedAtMillis) {
        this.receivedAtMillis = receivedAtMillis;
    }

    public long getAgeMinutes() {
        return ageMinutes;
    }

    public void setAgeMinutes(long ageMinutes) {
        this.ageMinutes = ageMinutes;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getPatientFirstName() {
        return patientFirstName;
    }

    public void setPatientFirstName(String patientFirstName) {
        this.patientFirstName = patientFirstName;
    }

    public String getPatientLastName() {
        return patientLastName;
    }

    public void setPatientLastName(String patientLastName) {
        this.patientLastName = patientLastName;
    }

    public String getCmuNumber() {
        return cmuNumber;
    }

    public void setCmuNumber(String cmuNumber) {
        this.cmuNumber = cmuNumber;
    }

    public boolean isProvisionalIdentity() {
        return provisionalIdentity;
    }

    public void setProvisionalIdentity(boolean provisionalIdentity) {
        this.provisionalIdentity = provisionalIdentity;
    }

    public String getRequestingFacility() {
        return requestingFacility;
    }

    public void setRequestingFacility(String requestingFacility) {
        this.requestingFacility = requestingFacility;
    }

    public String getPrescriber() {
        return prescriber;
    }

    public void setPrescriber(String prescriber) {
        this.prescriber = prescriber;
    }

    public String getOrigine() {
        return origine;
    }

    public void setOrigine(String origine) {
        this.origine = origine;
    }

    public Etape getEtape() {
        return etape;
    }

    public void setEtape(Etape etape) {
        this.etape = etape;
    }

    public boolean isCancelledByRequester() {
        return cancelledByRequester;
    }

    public void setCancelledByRequester(boolean cancelledByRequester) {
        this.cancelledByRequester = cancelledByRequester;
    }

    public String getLabNumber() {
        return labNumber;
    }

    public void setLabNumber(String labNumber) {
        this.labNumber = labNumber;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public List<Exam> getExams() {
        return exams;
    }
}

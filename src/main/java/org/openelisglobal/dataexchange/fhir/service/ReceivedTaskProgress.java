package org.openelisglobal.dataexchange.fhir.service;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.TaskStatus;

/**
 * Étapes d'une demande électronique reçue, reportées sur sa Task (status et
 * businessStatus) pour que le demandeur suive la demande jusqu'au résultat.
 *
 * <p>
 * Un état ne recule jamais et un état terminal (résultat disponible, refusée,
 * annulée) n'est jamais remplacé.
 */
public final class ReceivedTaskProgress {

    public static final String BUSINESS_STATUS_SYSTEM = "https://hie.ci/fhir/CodeSystem/etape-demande-examen";

    public enum Etape {
        ACCEPTEE("acceptee", "Acceptée", TaskStatus.ACCEPTED, 1),
        ECHANTILLON_RECU("echantillon-recu", "Échantillon reçu", TaskStatus.INPROGRESS, 2),
        EN_ANALYSE("en-analyse", "En analyse", TaskStatus.INPROGRESS, 3),
        RESULTAT_DISPONIBLE("resultat-disponible", "Résultat disponible", TaskStatus.COMPLETED, TERMINAL_RANK),
        REFUSEE("refusee", "Refusée", TaskStatus.REJECTED, TERMINAL_RANK),
        ANNULEE("annulee", "Annulée", TaskStatus.CANCELLED, TERMINAL_RANK);

        private final String code;
        private final String display;
        private final TaskStatus status;
        private final int rank;

        Etape(String code, String display, TaskStatus status, int rank) {
            this.code = code;
            this.display = display;
            this.status = status;
            this.rank = rank;
        }

        public String getCode() {
            return code;
        }

        public TaskStatus getStatus() {
            return status;
        }

        public boolean isTerminal() {
            return rank == TERMINAL_RANK;
        }

        static Etape fromCode(String code) {
            for (Etape etape : values()) {
                if (etape.code.equals(code)) {
                    return etape;
                }
            }
            return null;
        }
    }

    private static final int TERMINAL_RANK = 4;

    private ReceivedTaskProgress() {
    }

    /** businessStatus codé de l'étape. */
    public static CodeableConcept businessStatus(Etape etape) {
        return new CodeableConcept().addCoding(new Coding(BUSINESS_STATUS_SYSTEM, etape.code, etape.display))
                .setText(etape.display);
    }

    /**
     * Étape correspondant à la Task du bon d'OpenELIS (transformToTask), ou
     * {@code null} si son état ne correspond à aucune étape.
     */
    public static Etape fromOrderTask(Task orderTask) {
        if (orderTask == null || orderTask.getStatus() == null) {
            return null;
        }
        switch (orderTask.getStatus()) {
        case READY:
            return Etape.ECHANTILLON_RECU;
        case INPROGRESS:
            return Etape.EN_ANALYSE;
        case COMPLETED:
            return Etape.RESULTAT_DISPONIBLE;
        case REJECTED:
        case FAILED:
            return Etape.REFUSEE;
        case CANCELLED:
            return Etape.ANNULEE;
        default:
            return null;
        }
    }

    /** Vrai si la Task reçue est dans un état terminal. */
    public static boolean isTerminal(Task received) {
        TaskStatus status = received.getStatus();
        return status == TaskStatus.COMPLETED || status == TaskStatus.REJECTED || status == TaskStatus.CANCELLED
                || status == TaskStatus.FAILED || status == TaskStatus.ENTEREDINERROR;
    }

    static int currentRank(Task received) {
        if (isTerminal(received)) {
            return TERMINAL_RANK;
        }
        if (received.hasBusinessStatus()) {
            for (Coding coding : received.getBusinessStatus().getCoding()) {
                if (BUSINESS_STATUS_SYSTEM.equals(coding.getSystem())) {
                    Etape etape = Etape.fromCode(coding.getCode());
                    if (etape != null) {
                        return etape.rank;
                    }
                }
            }
        }
        if (received.getStatus() == TaskStatus.ACCEPTED) {
            return Etape.ACCEPTEE.rank;
        }
        if (received.getStatus() == TaskStatus.INPROGRESS) {
            return Etape.ECHANTILLON_RECU.rank;
        }
        return 0;
    }

    /**
     * Reporte sur la Task reçue l'étape de la Task du bon d'OpenELIS : compte rendu
     * en output quand le résultat est disponible, motif en cas de refus ou
     * d'annulation. Renvoie vrai si la Task reçue a changé.
     */
    public static boolean applyOrderTask(Task received, Task orderTask) {
        Etape etape = fromOrderTask(orderTask);
        if (!advance(received, etape)) {
            return false;
        }
        if (etape == Etape.RESULTAT_DISPONIBLE) {
            orderTask.getOutput().forEach(received::addOutput);
        }
        if ((etape == Etape.REFUSEE || etape == Etape.ANNULEE) && orderTask.hasStatusReason()) {
            received.setStatusReason(orderTask.getStatusReason().copy());
        }
        return true;
    }

    /**
     * Fait avancer la Task reçue jusqu'à l'étape si elle est plus avancée que son
     * état actuel. Renvoie vrai si la Task a changé.
     */
    public static boolean advance(Task received, Etape etape) {
        if (received == null || etape == null || isTerminal(received) || etape.rank <= currentRank(received)) {
            return false;
        }
        received.setStatus(etape.status);
        received.setBusinessStatus(businessStatus(etape));
        return true;
    }
}

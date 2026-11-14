package org.openelisglobal.dataexchange.fhir.service;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.validator.GenericValidator;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Task;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.ExternalOrderStatus;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrder;
import org.openelisglobal.dataexchange.service.order.ElectronicOrderService;
import org.openelisglobal.note.service.NoteService;
import org.openelisglobal.note.service.NoteServiceImpl.NoteType;
import org.openelisglobal.note.valueholder.Note;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RequesterCancellationServiceImpl implements RequesterCancellationService {

    static final String NOTE_SUBJECT = "Demande électronique";
    static final String NOTE_TEXT = "Demande annulée par le demandeur";

    @Autowired
    private ElectronicOrderService electronicOrderService;
    @Autowired
    private SampleService sampleService;
    @Autowired
    private IStatusService statusService;
    @Autowired
    private NoteService noteService;

    @Override
    @Transactional
    public Outcome applyRequesterCancellation(Task cancelledTask, Collection<String> orderNumbers) {
        if (isLaboratoryCancellation(cancelledTask)) {
            return Outcome.LABORATORY_CANCELLATION;
        }
        Outcome outcome = Outcome.NO_EFFECT;
        Set<String> terminal = terminalStatusIds();
        for (String orderNumber : orderNumbers) {
            if (GenericValidator.isBlankOrNull(orderNumber)) {
                continue;
            }
            List<ElectronicOrder> eOrders = electronicOrderService.getElectronicOrdersByExternalId(orderNumber);
            if (eOrders == null) {
                continue;
            }
            for (ElectronicOrder eOrder : eOrders) {
                if (eOrder.getRequesterCancelledAt() != null) {
                    continue; // déjà relevée
                }
                Sample sample = sampleService.getSampleByReferringId(orderNumber);
                Timestamp now = new Timestamp(System.currentTimeMillis());
                if (sample == null && !terminal.contains(eOrder.getStatusId())) {
                    eOrder.setStatusId(statusService.getStatusID(ExternalOrderStatus.Cancelled));
                    eOrder.setRequesterCancelledAt(now);
                    eOrder.setSysUserId("1");
                    electronicOrderService.update(eOrder);
                    outcome = Outcome.CANCELLED;
                } else if (sample != null) {
                    // Concurrence : le bon existe déjà. Rien n'est annulé ; la demande et le
                    // bon portent la mention, le laboratoire décide.
                    eOrder.setRequesterCancelledAt(now);
                    eOrder.setSysUserId("1");
                    electronicOrderService.update(eOrder);
                    addSampleNote(sample);
                    if (outcome != Outcome.CANCELLED) {
                        outcome = Outcome.FLAGGED_TAKEN;
                    }
                }
            }
        }
        return outcome;
    }

    // Annulation posée par OpenELIS lui-même (refus ou annulation du laboratoire,
    // bon annulé) : la Task porte alors l'étape « annulee » d'OpenELIS.
    static boolean isLaboratoryCancellation(Task task) {
        if (task == null || !task.hasBusinessStatus()) {
            return false;
        }
        for (Coding coding : task.getBusinessStatus().getCoding()) {
            if (ReceivedTaskProgress.BUSINESS_STATUS_SYSTEM.equals(coding.getSystem())
                    && ReceivedTaskProgress.Etape.ANNULEE.getCode().equals(coding.getCode())) {
                return true;
            }
        }
        return false;
    }

    private Set<String> terminalStatusIds() {
        Set<String> ids = new HashSet<>();
        for (ExternalOrderStatus status : new ExternalOrderStatus[] { ExternalOrderStatus.Cancelled,
                ExternalOrderStatus.Completed, ExternalOrderStatus.NonConforming }) {
            String id = statusService.getStatusID(status);
            if (!GenericValidator.isBlankOrNull(id)) {
                ids.add(id);
            }
        }
        return ids;
    }

    private void addSampleNote(Sample sample) {
        try {
            Note note = noteService.createSavableNote(sample, NoteType.INTERNAL, NOTE_TEXT, NOTE_SUBJECT, "1");
            noteService.insert(note);
        } catch (RuntimeException e) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "addSampleNote",
                    "note d'annulation non ajoutée au bon " + sample.getId() + " : " + e.getMessage());
        }
    }
}

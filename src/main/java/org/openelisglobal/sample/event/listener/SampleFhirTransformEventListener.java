package org.openelisglobal.sample.event.listener;

import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.dataexchange.fhir.FhirSyncConstants;
import org.openelisglobal.dataexchange.fhir.exception.FhirPersistanceException;
import org.openelisglobal.dataexchange.fhir.exception.FhirTransformationException;
import org.openelisglobal.dataexchange.fhir.service.FhirSyncStatusService;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.patient.action.bean.PatientManagementInfo;
import org.openelisglobal.sample.action.util.SamplePatientUpdateData;
import org.openelisglobal.sample.event.SamplePatientUpdateDataCreatedEvent;
import org.openelisglobal.sample.form.SamplePatientEntryForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class SampleFhirTransformEventListener {

    @Autowired
    private FhirTransformService fhirTransformService;

    @Autowired
    private FhirSyncStatusService fhirSyncStatusService;

    @Async
    @EventListener
    public void handleSamplePatientUpdateDataCreatedEvent(SamplePatientUpdateDataCreatedEvent event) {
        SamplePatientUpdateData updateData = event.getUpdateData();
        String sampleId = updateData.getSample() != null ? updateData.getSample().getId() : null;
        String targetId = sampleId != null ? sampleId : updateData.getAccessionNumber();

        // Trace l'événement (visible + rejouable). Le traçage est non bloquant :
        // il vit dans sa propre transaction et avale ses propres erreurs.
        String syncId = fhirSyncStatusService.recordPending(FhirSyncConstants.TRIGGER_ORDER_ENTRY,
                FhirSyncConstants.TARGET_SAMPLE, targetId);
        try {
            PatientManagementInfo patientInfo = event.getPatientInfo();
            SamplePatientEntryForm form = event.getForm();

            fhirTransformService.transformPersistOrderEntryFhirObjects(updateData, patientInfo, form.getUseReferral(),
                    form.getReferralItems());

            // Persistance OK : qualifie la complétude des ressources produites
            // (SUCCESS ou SUCCESS_INCOMPLETE) quand on a l'id d'échantillon.
            if (sampleId != null) {
                fhirSyncStatusService.markSuccessWithIssues(syncId,
                        fhirTransformService.collectCompletenessIssuesForSample(sampleId));
            } else {
                fhirSyncStatusService.markSuccess(syncId);
            }
            LogEvent.logInfo(this.getClass().getSimpleName(), "handleSamplePatientUpdateDataCreatedEvent",
                    String.format("FHIR transformation completed for sample with accession number: %s",
                            updateData.getAccessionNumber()));

        } catch (FhirTransformationException | FhirPersistanceException e) {
            fhirSyncStatusService.markFailed(syncId, e.toString());
            LogEvent.logError(this.getClass().getSimpleName(), "handleSamplePatientUpdateDataCreatedEvent",
                    "Error during FHIR transformation: " + e.getMessage());
        } catch (Exception e) {
            fhirSyncStatusService.markFailed(syncId, e.toString());
            LogEvent.logError(this.getClass().getSimpleName(), "handleSamplePatientUpdateDataCreatedEvent",
                    "Unexpected error during FHIR transformation");
            LogEvent.logError(e);
        }
    }
}

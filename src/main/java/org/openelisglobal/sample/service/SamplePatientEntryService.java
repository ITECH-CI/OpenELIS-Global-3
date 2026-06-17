package org.openelisglobal.sample.service;

import jakarta.servlet.http.HttpServletRequest;
import org.openelisglobal.patient.action.bean.PatientManagementInfo;
import org.openelisglobal.sample.action.util.SamplePatientUpdateData;
import org.openelisglobal.sample.form.SamplePatientEntryForm;
import org.openelisglobal.patient.action.bean.PatientRoutineBacterioInfo;
import org.openelisglobal.patient.action.bean.PatientTbInfo;
import org.openelisglobal.sample.bean.SampleOrderItem;

public interface SamplePatientEntryService {

    void persistData(SamplePatientUpdateData updateData, PatientManagementUpdate patientUpdate,
            PatientManagementInfo patientInfo, SamplePatientEntryForm form, HttpServletRequest request);

    /**
     * Remplace l'ensemble des observations bactériologie d'un échantillon par
     * celles décrites dans bacterioInfo / patientInfo / sampleOrderItems. Utilisé
     * par la modification d'ordonnance pour éviter les doublons (chaque sauvegarde
     * écrase intégralement le bloc, sans cumul). Pour la création, appeler
     * directement persistData ; cette méthode est conçue pour le flow d'édition.
     */
    void replaceBacterioObservations(PatientRoutineBacterioInfo bacterioInfo, PatientManagementInfo patientInfo,
            SampleOrderItem sampleOrderItems, String sampleId, String patientId, String sysUserId);

    /**
     * Idem pour les observations TB.
     */
    void replaceTbObservations(PatientTbInfo tbInfo, String sampleId, String patientId, String sysUserId);
}

package org.openelisglobal.patient.saving;

import org.apache.commons.validator.GenericValidator;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/**
 * Handles updating an existing sample entry (modify flow).
 * Unlike SampleSecondEntry, this preserves the existing record status.
 */
@Service
@Scope("prototype")
public class SampleEntryUpdate extends SampleEntry implements ISampleEntryUpdate {

    public SampleEntryUpdate() {
        super();
        newPatientStatus = null;
        newSampleStatus = null;
    }

    @Override
    public boolean canAccession() {
        return !GenericValidator.isBlankOrNull(projectFormMapper.getSampleId())
                && statusSet.getSampleRecordStatus() != null;
    }
}

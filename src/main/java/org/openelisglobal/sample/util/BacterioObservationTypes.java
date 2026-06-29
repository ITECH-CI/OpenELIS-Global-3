package org.openelisglobal.sample.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Single source of truth for the observation_history type names persisted by
 * the bacteriology and TB order flows.
 *
 * <p>
 * These lists drive both persistence (which types are written) and cleanup
 * (which types are deleted/replaced on edit) as well as the "has any bacterio
 * observation" gate in the edit controller. Keeping them in one place avoids the
 * silent duplicate-row bugs that happened when the same arrays were copied into
 * SamplePatientEntryServiceImpl and SampleEditRestController and drifted out of
 * sync.
 *
 * <p>
 * When a new observation type is added to persistBacterioObservations() or
 * persistTbObservations(), add it here once.
 */
public final class BacterioObservationTypes {

    private BacterioObservationTypes() {
    }

    /** Bacteriology order/clinical observation type names. */
    public static final List<String> BACTERIO = Collections.unmodifiableList(Arrays.asList("Pregnancy",
            "BacterioTypeExamens", "EPIDEMIO_WEEK", "currentHospitalization", "roomNumber", "CLINICAL_INFOS",
            "CLINICAL_INFOS_OTHER", "PREV3M_ATB", "PREV3M_ATB_LIST", "CURR_ATB", "CURR_ATB_LIST", "CURR_ATB_DUR",
            "HOSP_3M", "HOSP_3M_COUNT", "INVASIVE_GESTURE", "INDWELLING_DEVICES"));

    /** TB order observation type names. */
    public static final List<String> TB = Collections.unmodifiableList(
            Arrays.asList("TbOrderReason", "TbDiagnosticReason", "TbFollowupReason", "TbSampleAspects",
                    "TbFollowupReasonPeriodLine1", "TbFollowupReasonPeriodLine2", "TbSpecimenNature", "TbAnalysisMethod"));

    public static String[] bacterioArray() {
        return BACTERIO.toArray(new String[0]);
    }

    public static String[] tbArray() {
        return TB.toArray(new String[0]);
    }
}

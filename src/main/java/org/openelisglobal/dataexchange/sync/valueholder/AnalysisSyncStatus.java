/**
 * The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.mozilla.org/MPL/
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>The Original Code is OpenELIS code.
 *
 * <p>Copyright (C) ITECH-CI. All Rights Reserved.
 */
package org.openelisglobal.dataexchange.sync.valueholder;

import java.sql.Timestamp;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * Ligne de la file CDC {@code analysis_sync_status} (module d'échange unifié,
 * incrément 4b) : une analyse à remonter vers le serveur consolidé. Alimentée
 * par les triggers PostgreSQL (voir {@code analysis_sync_cdc_triggers.sql}) ;
 * drainée par le service DataSync.
 *
 * <p>
 * {@code uploadFlag} : 1=TO_INSERT, 2=TO_UPDATE, 3=UP_TO_DATE, 4=IN_PROGRESS
 * (cf. {@link AnalysisSyncFlag}). Pas de colonne de version optimiste : les
 * transitions d'état se font par UPDATE ciblé (DAO), pas par load-modify-save.
 */
public class AnalysisSyncStatus extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    /** Valeurs de {@code upload_flag} (miroir de la sémantique uploader). */
    public static final class AnalysisSyncFlag {
        public static final short TO_INSERT = 1;
        public static final short TO_UPDATE = 2;
        public static final short UP_TO_DATE = 3;
        public static final short IN_PROGRESS = 4;

        private AnalysisSyncFlag() {
        }
    }

    private String id;
    private String sampleId;
    private String analysisId;
    private Short uploadFlag;
    private Timestamp lastUpdated;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getSampleId() {
        return sampleId;
    }

    public void setSampleId(String sampleId) {
        this.sampleId = sampleId;
    }

    public String getAnalysisId() {
        return analysisId;
    }

    public void setAnalysisId(String analysisId) {
        this.analysisId = analysisId;
    }

    public Short getUploadFlag() {
        return uploadFlag;
    }

    public void setUploadFlag(Short uploadFlag) {
        this.uploadFlag = uploadFlag;
    }

    public Timestamp getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Timestamp lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}

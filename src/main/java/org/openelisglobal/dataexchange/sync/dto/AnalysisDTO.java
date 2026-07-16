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
package org.openelisglobal.dataexchange.sync.dto;

import java.time.LocalDateTime;

/**
 * Analyse + résultat remonté au serveur consolidé. Noms JSON alignés sur le
 * contrat SC (ci.itech.oedatarepo.dto.AnalysisDTO). Le champ {@code
 * analysisMetadata} du contrat d'origine est volontairement ABSENT : le SC ne
 * le lit jamais (seule la liste top-level {@code orderMetadataDTOs} alimente
 * les métadonnées). Cf. DESIGN_INTEROP_MODULE_CIV.md §6.2.
 */
public class AnalysisDTO {

    private Long analysisLocalId;
    private Long sampleItemLocalId;
    private Long sampleLocalId;
    private String analysisUuid;
    private String analysisStatus;
    private LocalDateTime analysisStartedDate;
    private LocalDateTime analysisCompletedDate;
    private LocalDateTime analysisReleasedDate;
    private String analysisStatusId;
    private String sampleTypeName;
    private String testSection;
    private Integer sampleItemSortOrder;
    private Long resultLocalId;
    private String resultSortOrder;
    private String test;
    private String testLoinc;
    private String resultType;
    private String resultValue;
    private String resultMinLimit;
    private String resultMaxLimit;
    private LocalDateTime analysisLastupdated;

    public Long getAnalysisLocalId() {
        return analysisLocalId;
    }

    public void setAnalysisLocalId(Long analysisLocalId) {
        this.analysisLocalId = analysisLocalId;
    }

    public Long getSampleItemLocalId() {
        return sampleItemLocalId;
    }

    public void setSampleItemLocalId(Long sampleItemLocalId) {
        this.sampleItemLocalId = sampleItemLocalId;
    }

    public Long getSampleLocalId() {
        return sampleLocalId;
    }

    public void setSampleLocalId(Long sampleLocalId) {
        this.sampleLocalId = sampleLocalId;
    }

    public String getAnalysisUuid() {
        return analysisUuid;
    }

    public void setAnalysisUuid(String analysisUuid) {
        this.analysisUuid = analysisUuid;
    }

    public String getAnalysisStatus() {
        return analysisStatus;
    }

    public void setAnalysisStatus(String analysisStatus) {
        this.analysisStatus = analysisStatus;
    }

    public LocalDateTime getAnalysisStartedDate() {
        return analysisStartedDate;
    }

    public void setAnalysisStartedDate(LocalDateTime analysisStartedDate) {
        this.analysisStartedDate = analysisStartedDate;
    }

    public LocalDateTime getAnalysisCompletedDate() {
        return analysisCompletedDate;
    }

    public void setAnalysisCompletedDate(LocalDateTime analysisCompletedDate) {
        this.analysisCompletedDate = analysisCompletedDate;
    }

    public LocalDateTime getAnalysisReleasedDate() {
        return analysisReleasedDate;
    }

    public void setAnalysisReleasedDate(LocalDateTime analysisReleasedDate) {
        this.analysisReleasedDate = analysisReleasedDate;
    }

    public String getAnalysisStatusId() {
        return analysisStatusId;
    }

    public void setAnalysisStatusId(String analysisStatusId) {
        this.analysisStatusId = analysisStatusId;
    }

    public String getSampleTypeName() {
        return sampleTypeName;
    }

    public void setSampleTypeName(String sampleTypeName) {
        this.sampleTypeName = sampleTypeName;
    }

    public String getTestSection() {
        return testSection;
    }

    public void setTestSection(String testSection) {
        this.testSection = testSection;
    }

    public Integer getSampleItemSortOrder() {
        return sampleItemSortOrder;
    }

    public void setSampleItemSortOrder(Integer sampleItemSortOrder) {
        this.sampleItemSortOrder = sampleItemSortOrder;
    }

    public Long getResultLocalId() {
        return resultLocalId;
    }

    public void setResultLocalId(Long resultLocalId) {
        this.resultLocalId = resultLocalId;
    }

    public String getResultSortOrder() {
        return resultSortOrder;
    }

    public void setResultSortOrder(String resultSortOrder) {
        this.resultSortOrder = resultSortOrder;
    }

    public String getTest() {
        return test;
    }

    public void setTest(String test) {
        this.test = test;
    }

    public String getTestLoinc() {
        return testLoinc;
    }

    public void setTestLoinc(String testLoinc) {
        this.testLoinc = testLoinc;
    }

    public String getResultType() {
        return resultType;
    }

    public void setResultType(String resultType) {
        this.resultType = resultType;
    }

    public String getResultValue() {
        return resultValue;
    }

    public void setResultValue(String resultValue) {
        this.resultValue = resultValue;
    }

    public String getResultMinLimit() {
        return resultMinLimit;
    }

    public void setResultMinLimit(String resultMinLimit) {
        this.resultMinLimit = resultMinLimit;
    }

    public String getResultMaxLimit() {
        return resultMaxLimit;
    }

    public void setResultMaxLimit(String resultMaxLimit) {
        this.resultMaxLimit = resultMaxLimit;
    }

    public LocalDateTime getAnalysisLastupdated() {
        return analysisLastupdated;
    }

    public void setAnalysisLastupdated(LocalDateTime analysisLastupdated) {
        this.analysisLastupdated = analysisLastupdated;
    }
}

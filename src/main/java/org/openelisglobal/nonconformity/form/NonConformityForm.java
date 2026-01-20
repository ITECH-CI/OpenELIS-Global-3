package org.openelisglobal.nonconformity.form;

import org.openelisglobal.common.form.BaseForm;

public class NonConformityForm extends BaseForm {

    private String id;
    private String ncNumber;
    private String reportDate;
    private String siteProvenance;
    private String sampleType;
    private String rejectionReason;
    private String comment;
    private String reporterName;
    private String labNumber;
    private String correctiveAction;
    private String status;

    private String searchSiteProvenance;
    private String searchSampleType;
    private String searchRejectionReason;
    private String searchStartDate;
    private String searchEndDate;
    private String searchStatus;

    public NonConformityForm() {
        setFormName("NonConformityForm");
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNcNumber() {
        return ncNumber;
    }

    public void setNcNumber(String ncNumber) {
        this.ncNumber = ncNumber;
    }

    public String getReportDate() {
        return reportDate;
    }

    public void setReportDate(String reportDate) {
        this.reportDate = reportDate;
    }

    public String getSiteProvenance() {
        return siteProvenance;
    }

    public void setSiteProvenance(String siteProvenance) {
        this.siteProvenance = siteProvenance;
    }

    public String getSampleType() {
        return sampleType;
    }

    public void setSampleType(String sampleType) {
        this.sampleType = sampleType;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getReporterName() {
        return reporterName;
    }

    public void setReporterName(String reporterName) {
        this.reporterName = reporterName;
    }

    public String getLabNumber() {
        return labNumber;
    }

    public void setLabNumber(String labNumber) {
        this.labNumber = labNumber;
    }

    public String getCorrectiveAction() {
        return correctiveAction;
    }

    public void setCorrectiveAction(String correctiveAction) {
        this.correctiveAction = correctiveAction;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSearchSiteProvenance() {
        return searchSiteProvenance;
    }

    public void setSearchSiteProvenance(String searchSiteProvenance) {
        this.searchSiteProvenance = searchSiteProvenance;
    }

    public String getSearchSampleType() {
        return searchSampleType;
    }

    public void setSearchSampleType(String searchSampleType) {
        this.searchSampleType = searchSampleType;
    }

    public String getSearchRejectionReason() {
        return searchRejectionReason;
    }

    public void setSearchRejectionReason(String searchRejectionReason) {
        this.searchRejectionReason = searchRejectionReason;
    }

    public String getSearchStartDate() {
        return searchStartDate;
    }

    public void setSearchStartDate(String searchStartDate) {
        this.searchStartDate = searchStartDate;
    }

    public String getSearchEndDate() {
        return searchEndDate;
    }

    public void setSearchEndDate(String searchEndDate) {
        this.searchEndDate = searchEndDate;
    }

    public String getSearchStatus() {
        return searchStatus;
    }

    public void setSearchStatus(String searchStatus) {
        this.searchStatus = searchStatus;
    }
}

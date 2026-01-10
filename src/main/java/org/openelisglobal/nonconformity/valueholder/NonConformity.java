package org.openelisglobal.nonconformity.valueholder;

import java.sql.Date;

import org.openelisglobal.common.valueholder.BaseObject;

public class NonConformity extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    private String id;

    private String ncNumber;

    private Date reportDate;

    private String siteProvenance;

    private String sampleType;

    private String rejectionReason;

    private String comment;

    private String reporterName;

    private String labNumber;

    private String correctiveAction;

    private String status;

    private String createdBy;

    private Date createdDate;

    private String lastUpdatedBy;

    public NonConformity() {
        super();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getNcNumber() {
        return ncNumber;
    }

    public void setNcNumber(String ncNumber) {
        this.ncNumber = ncNumber;
    }

    public Date getReportDate() {
        return reportDate;
    }

    public void setReportDate(Date reportDate) {
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

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public String getLastUpdatedBy() {
        return lastUpdatedBy;
    }

    public void setLastUpdatedBy(String lastUpdatedBy) {
        this.lastUpdatedBy = lastUpdatedBy;
    }


    @Override
    public String toString() {
        return "NonConformity [id=" + id + ", ncNumber=" + ncNumber + ", reportDate=" + reportDate
                + ", siteProvenance=" + siteProvenance + ", sampleType=" + sampleType + ", rejectionReason="
                + rejectionReason + ", comment=" + comment + ", reporterName=" + reporterName + ", labNumber="
                + labNumber + ", correctiveAction=" + correctiveAction + ", status=" + status + ", createdBy="
                + createdBy + ", createdDate=" + createdDate + ", lastUpdatedBy=" + lastUpdatedBy + "]";
    }
}

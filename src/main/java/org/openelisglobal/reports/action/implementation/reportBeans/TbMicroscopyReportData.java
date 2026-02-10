package org.openelisglobal.reports.action.implementation.reportBeans;

public class TbMicroscopyReportData {
    private String reportItem = "";
    private Integer positivePlusResult = 0;
    private Integer positiveRBResult = 0;
    private Integer negativeResult = 0;
    private Integer totalResult = 0;

    public String getReportItem() {
        return reportItem;
    }

    public void setReportItem(String reportItem) {
        this.reportItem = reportItem;
    }

    public Integer getPositivePlusResult() {
        return positivePlusResult;
    }

    public void setPositivePlusResult(Integer positivePlusResult) {
        this.positivePlusResult = positivePlusResult;
    }

    public Integer getPositiveRBResult() {
        return positiveRBResult;
    }

    public void setPositiveRBResult(Integer positiveRBResult) {
        this.positiveRBResult = positiveRBResult;
    }

    public Integer getNegativeResult() {
        return negativeResult;
    }

    public void setNegativeResult(Integer negativeResult) {
        this.negativeResult = negativeResult;
    }

    public Integer getTotalResult() {
        return totalResult;
    }

    public void setTotalResult(Integer totalResult) {
        this.totalResult = totalResult;
    }

}

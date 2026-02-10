package org.openelisglobal.reports.action.implementation.reportBeans;

public class TbTestReportData {
    private String resultLabel = "";
    private Integer testInMonth1 = 0;
    private Integer testInMonth2 = 0;
    private Integer testInMonth3 = 0;
    private Integer totalTest = 0;

    public String getResultLabel() {
        return resultLabel;
    }

    public void setResultLabel(String resultLabel) {
        this.resultLabel = resultLabel;
    }

    public Integer getTestInMonth1() {
        return testInMonth1;
    }

    public void setTestInMonth1(Integer testInMonth1) {
        this.testInMonth1 = testInMonth1;
    }

    public Integer getTestInMonth2() {
        return testInMonth2;
    }

    public void setTestInMonth2(Integer testInMonth2) {
        this.testInMonth2 = testInMonth2;
    }

    public Integer getTestInMonth3() {
        return testInMonth3;
    }

    public void setTestInMonth3(Integer testInMonth3) {
        this.testInMonth3 = testInMonth3;
    }

    public Integer getTotalTest() {
        return totalTest;
    }

    public void setTotalTest(Integer totalTest) {
        this.totalTest = totalTest;
    }
}

package org.openelisglobal.reports.action.implementation.reportBeans;

public class TbCategoryReportData {
    private String categoryName = "";
    private Integer testResultResistant = 0;
    private Integer testResultSensitive = 0;
    private Integer testResultIndeterminate = 0;
    private Integer testResultMTBNonDetected = 0;
    private Integer totalTest = 0;

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public Integer getTestResultResistant() {
        return testResultResistant;
    }

    public void setTestResultResistant(Integer testResultResistant) {
        this.testResultResistant = testResultResistant;
    }

    public Integer getTestResultSensitive() {
        return testResultSensitive;
    }

    public void setTestResultSensitive(Integer testResultSensitive) {
        this.testResultSensitive = testResultSensitive;
    }

    public Integer getTestResultIndeterminate() {
        return testResultIndeterminate;
    }

    public void setTestResultIndeterminate(Integer testResultIndeterminate) {
        this.testResultIndeterminate = testResultIndeterminate;
    }

    public Integer getTestResultMTBNonDetected() {
        return testResultMTBNonDetected;
    }

    public void setTestResultMTBNonDetected(Integer testResultMTBNonDetected) {
        this.testResultMTBNonDetected = testResultMTBNonDetected;
    }

    public Integer getTotalTest() {
        return totalTest;
    }

    public void setTotalTest(Integer totalTest) {
        this.totalTest = totalTest;
    }
}

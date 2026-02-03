package org.openelisglobal.patient.action.bean;

import jakarta.validation.constraints.Pattern;
import java.util.List;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.common.validator.ValidationHelper;
import org.openelisglobal.validation.annotations.SafeHtml;

public class PatientTbInfo {

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String tbOrderReason;

    private List<IdValuePair> tbOrderReasons;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String tbDiagnosticReason;

    private List<IdValuePair> tbDiagnosticReasons;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String tbFollowupReason;

    private List<IdValuePair> tbFollowupReasons;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String tbFollowupPeriodLine1;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String tbFollowupPeriodLine2;

    private List<IdValuePair> tbFollowupPeriodsLine1;

    private List<IdValuePair> tbFollowupPeriodsLine2;

    private List<IdValuePair> tbDiagnosticMethods;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String tbAspect;

    private List<IdValuePair> tbAspects;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String tbSpecimenNature;

    private List<IdValuePair> tbSpecimenNatures;

    @Pattern(regexp = ValidationHelper.PATIENT_ID_REGEX)
    private String tbSubjectNumber;

    @Pattern(regexp = ValidationHelper.PATIENT_ID_REGEX)
    private String tbSubjectNumberRes;

    private String selectedTbMethod;

    private String selectedMethodToRemove;

    // Getters and Setters

    public String getTbOrderReason() {
        return tbOrderReason;
    }

    public void setTbOrderReason(String tbOrderReason) {
        this.tbOrderReason = tbOrderReason;
    }

    public List<IdValuePair> getTbOrderReasons() {
        return tbOrderReasons;
    }

    public void setTbOrderReasons(List<IdValuePair> tbOrderReasons) {
        this.tbOrderReasons = tbOrderReasons;
    }

    public String getTbDiagnosticReason() {
        return tbDiagnosticReason;
    }

    public void setTbDiagnosticReason(String tbDiagnosticReason) {
        this.tbDiagnosticReason = tbDiagnosticReason;
    }

    public List<IdValuePair> getTbDiagnosticReasons() {
        return tbDiagnosticReasons;
    }

    public void setTbDiagnosticReasons(List<IdValuePair> tbDiagnosticReasons) {
        this.tbDiagnosticReasons = tbDiagnosticReasons;
    }

    public String getTbFollowupReason() {
        return tbFollowupReason;
    }

    public void setTbFollowupReason(String tbFollowupReason) {
        this.tbFollowupReason = tbFollowupReason;
    }

    public List<IdValuePair> getTbFollowupReasons() {
        return tbFollowupReasons;
    }

    public void setTbFollowupReasons(List<IdValuePair> tbFollowupReasons) {
        this.tbFollowupReasons = tbFollowupReasons;
    }

    public List<IdValuePair> getTbDiagnosticMethods() {
        return tbDiagnosticMethods;
    }

    public void setTbDiagnosticMethods(List<IdValuePair> tbDiagnosticMethods) {
        this.tbDiagnosticMethods = tbDiagnosticMethods;
    }

    public String getTbAspect() {
        return tbAspect;
    }

    public void setTbAspect(String tbAspect) {
        this.tbAspect = tbAspect;
    }

    public List<IdValuePair> getTbAspects() {
        return tbAspects;
    }

    public void setTbAspects(List<IdValuePair> tbAspects) {
        this.tbAspects = tbAspects;
    }

    public String getTbFollowupPeriodLine1() {
        return tbFollowupPeriodLine1;
    }

    public void setTbFollowupPeriodLine1(String tbFollowupPeriodLine1) {
        this.tbFollowupPeriodLine1 = tbFollowupPeriodLine1;
    }

    public String getTbFollowupPeriodLine2() {
        return tbFollowupPeriodLine2;
    }

    public void setTbFollowupPeriodLine2(String tbFollowupPeriodLine2) {
        this.tbFollowupPeriodLine2 = tbFollowupPeriodLine2;
    }

    public List<IdValuePair> getTbFollowupPeriodsLine1() {
        return tbFollowupPeriodsLine1;
    }

    public void setTbFollowupPeriodsLine1(List<IdValuePair> tbFollowupPeriodsLine1) {
        this.tbFollowupPeriodsLine1 = tbFollowupPeriodsLine1;
    }

    public List<IdValuePair> getTbFollowupPeriodsLine2() {
        return tbFollowupPeriodsLine2;
    }

    public void setTbFollowupPeriodsLine2(List<IdValuePair> tbFollowupPeriodsLine2) {
        this.tbFollowupPeriodsLine2 = tbFollowupPeriodsLine2;
    }

    public String getTbSpecimenNature() {
        return tbSpecimenNature;
    }

    public void setTbSpecimenNature(String tbSpecimenNature) {
        this.tbSpecimenNature = tbSpecimenNature;
    }

    public List<IdValuePair> getTbSpecimenNatures() {
        return tbSpecimenNatures;
    }

    public void setTbSpecimenNatures(List<IdValuePair> tbSpecimenNatures) {
        this.tbSpecimenNatures = tbSpecimenNatures;
    }

    public String getTbSubjectNumber() {
        return tbSubjectNumber;
    }

    public void setTbSubjectNumber(String tbSubjectNumber) {
        this.tbSubjectNumber = tbSubjectNumber;
    }

    public String getTbSubjectNumberRes() {
        return tbSubjectNumberRes;
    }

    public void setTbSubjectNumberRes(String tbSubjectNumberRes) {
        this.tbSubjectNumberRes = tbSubjectNumberRes;
    }

    public String getSelectedTbMethod() {
        return selectedTbMethod;
    }

    public void setSelectedTbMethod(String selectedTbMethod) {
        this.selectedTbMethod = selectedTbMethod;
    }

    public String getSelectedMethodToRemove() {
        return selectedMethodToRemove;
    }

    public void setSelectedMethodToRemove(String selectedMethodToRemove) {
        this.selectedMethodToRemove = selectedMethodToRemove;
    }
}

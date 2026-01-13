package org.openelisglobal.bacteriology.action.bean;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Form bean for complete bacteriology result submission
 */
public class BacteriologyResultForm {

    @NotNull
    private Integer analysisId;

    @NotNull
    private String sysUserId;

    // Macroscopy results - map of test ID to result value (dictionary ID or text)
    private Map<String, String> macroscopyResults = new HashMap<>();

    // Microscopy results - map of test ID to result value (dictionary ID or text)
    private Map<String, String> microscopyResults = new HashMap<>();

    // Culture result - positive or negative (dictionary ID)
    private String cultureResult;

    // Organisms identified (1-3)
    @Valid
    private List<BacteriologyOrganismBean> organisms = new ArrayList<>();

    public BacteriologyResultForm() {
    }

    public Integer getAnalysisId() {
        return analysisId;
    }

    public void setAnalysisId(Integer analysisId) {
        this.analysisId = analysisId;
    }

    public String getSysUserId() {
        return sysUserId;
    }

    public void setSysUserId(String sysUserId) {
        this.sysUserId = sysUserId;
    }

    public Map<String, String> getMacroscopyResults() {
        return macroscopyResults;
    }

    public void setMacroscopyResults(Map<String, String> macroscopyResults) {
        this.macroscopyResults = macroscopyResults;
    }

    public Map<String, String> getMicroscopyResults() {
        return microscopyResults;
    }

    public void setMicroscopyResults(Map<String, String> microscopyResults) {
        this.microscopyResults = microscopyResults;
    }

    public String getCultureResult() {
        return cultureResult;
    }

    public void setCultureResult(String cultureResult) {
        this.cultureResult = cultureResult;
    }

    public List<BacteriologyOrganismBean> getOrganisms() {
        return organisms;
    }

    public void setOrganisms(List<BacteriologyOrganismBean> organisms) {
        this.organisms = organisms;
    }
}

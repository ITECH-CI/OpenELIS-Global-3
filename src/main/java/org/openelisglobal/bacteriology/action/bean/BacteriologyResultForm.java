package org.openelisglobal.bacteriology.action.bean;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Form bean for complete bacteriology result submission
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class BacteriologyResultForm {

    @NotNull
    private Integer analysisId;

    @NotNull
    private String sysUserId;

    // Macroscopy results - map of test ID to result value (dictionary ID or text)
    private Map<String, String> macroscopyResults = new HashMap<>();

    // Microscopy results - map of test ID to result value (dictionary ID or text)
    private Map<String, String> microscopyResults = new HashMap<>();

    // Chemistry results (Glucose, Protéine) - map of test ID to result value.
    // These bacteriology tests are entered in a dedicated section after culture.
    private Map<String, String> chemistryResults = new HashMap<>();

    // Optional per-result UoM override for microscopy tests with a selectable
    // unit (e.g. "Etat frais Quantitatif"). Key = testId, value = uom_id. When
    // a test is absent from the map, the test's default UoM applies.
    private Map<String, String> microscopyUoms = new HashMap<>();

    // Culture result - positive or negative (dictionary ID) - DEPRECATED, use
    // cultures instead
    private String cultureResult;

    // Cultures - map of test ID to culture data (cultureResult + organisms)
    private Map<String, CultureData> cultures = new HashMap<>();

    // Flora data - map of test ID to flora information (count and details)
    private Map<String, Object> floraData = new HashMap<>();

    // Organisms identified (1-3) - DEPRECATED, use cultures instead
    @Valid
    private List<BacteriologyOrganismBean> organisms = new ArrayList<>();

    public BacteriologyResultForm() {
    }

    /**
     * Inner class to hold culture data for a specific test
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CultureData {
        private String cultureResult;
        @Valid
        private List<BacteriologyOrganismBean> organisms = new ArrayList<>();

        public CultureData() {
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

    public Map<String, String> getChemistryResults() {
        return chemistryResults;
    }

    public void setChemistryResults(Map<String, String> chemistryResults) {
        this.chemistryResults = chemistryResults;
    }

    public Map<String, String> getMicroscopyUoms() {
        return microscopyUoms;
    }

    public void setMicroscopyUoms(Map<String, String> microscopyUoms) {
        this.microscopyUoms = microscopyUoms;
    }

    public String getCultureResult() {
        return cultureResult;
    }

    public void setCultureResult(String cultureResult) {
        this.cultureResult = cultureResult;
    }

    public Map<String, Object> getFloraData() {
        return floraData;
    }

    public void setFloraData(Map<String, Object> floraData) {
        this.floraData = floraData;
    }

    public List<BacteriologyOrganismBean> getOrganisms() {
        return organisms;
    }

    public void setOrganisms(List<BacteriologyOrganismBean> organisms) {
        this.organisms = organisms;
    }

    public Map<String, CultureData> getCultures() {
        return cultures;
    }

    public void setCultures(Map<String, CultureData> cultures) {
        this.cultures = cultures;
    }
}

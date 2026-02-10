package org.openelisglobal.bacteriology.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.bacteriology.valueholder.BacteriologyAntibiogram;
import org.openelisglobal.bacteriology.valueholder.BacteriologyOrganism;
import org.openelisglobal.bacteriology.valueholder.BacteriologyResultGroup;

/**
 * Orchestrates bacteriology workflow operations including: - Creating and
 * managing result groups hierarchy - Handling organism identification -
 * Managing antibiogram results - Coordinating save operations across multiple
 * entities
 */
public interface BacteriologyWorkflowService {

    /**
     * Single bacteriology test result with full details
     */
    static class BacteriologyTestResultBean {
        private String analysisId;
        private String testId;
        private String testName;
        private String testDescription;
        private String value;
        private String displayValue;
        private String unitOfMeasure;
        private String resultType;

        public String getAnalysisId() {
            return analysisId;
        }

        public void setAnalysisId(String analysisId) {
            this.analysisId = analysisId;
        }

        public String getTestId() {
            return testId;
        }

        public void setTestId(String testId) {
            this.testId = testId;
        }

        public String getTestName() {
            return testName;
        }

        public void setTestName(String testName) {
            this.testName = testName;
        }

        public String getTestDescription() {
            return testDescription;
        }

        public void setTestDescription(String testDescription) {
            this.testDescription = testDescription;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getDisplayValue() {
            return displayValue;
        }

        public void setDisplayValue(String displayValue) {
            this.displayValue = displayValue;
        }

        public String getUnitOfMeasure() {
            return unitOfMeasure;
        }

        public void setUnitOfMeasure(String unitOfMeasure) {
            this.unitOfMeasure = unitOfMeasure;
        }

        public String getResultType() {
            return resultType;
        }

        public void setResultType(String resultType) {
            this.resultType = resultType;
        }
    }

    /**
     * Complete bacteriology result data including all groups, organisms, and
     * antibiograms
     */
    static class BacteriologyResultData {
        private List<BacteriologyResultGroup> macroscopyGroups;
        private List<BacteriologyResultGroup> microscopyGroups;
        private BacteriologyResultGroup cultureGroup;
        private List<OrganismWithAntibiogram> organisms;
        private String sampleTypeName; // Type of sample (e.g., "Sécrétions vaginales")

        // Test results lists with full details (test name, value, unit, etc.)
        private List<BacteriologyTestResultBean> macroscopyResults = new ArrayList<>();
        private List<BacteriologyTestResultBean> microscopyResults = new ArrayList<>();
        private List<BacteriologyTestResultBean> cultureResults = new ArrayList<>();

        // Test results as maps (testId -> value) for easy lookup and automatic
        // deduplication
        private Map<String, String> macroscopyResultsMap = new HashMap<>();
        private Map<String, String> microscopyResultsMap = new HashMap<>();
        private Map<String, String> cultureResultsMap = new HashMap<>();

        public List<BacteriologyResultGroup> getMacroscopyGroups() {
            return macroscopyGroups;
        }

        public void setMacroscopyGroups(List<BacteriologyResultGroup> macroscopyGroups) {
            this.macroscopyGroups = macroscopyGroups;
        }

        public List<BacteriologyResultGroup> getMicroscopyGroups() {
            return microscopyGroups;
        }

        public void setMicroscopyGroups(List<BacteriologyResultGroup> microscopyGroups) {
            this.microscopyGroups = microscopyGroups;
        }

        public BacteriologyResultGroup getCultureGroup() {
            return cultureGroup;
        }

        public void setCultureGroup(BacteriologyResultGroup cultureGroup) {
            this.cultureGroup = cultureGroup;
        }

        public List<OrganismWithAntibiogram> getOrganisms() {
            return organisms;
        }

        public void setOrganisms(List<OrganismWithAntibiogram> organisms) {
            this.organisms = organisms;
        }

        public List<BacteriologyTestResultBean> getMacroscopyResults() {
            return macroscopyResults;
        }

        public void setMacroscopyResults(List<BacteriologyTestResultBean> macroscopyResults) {
            this.macroscopyResults = macroscopyResults;
        }

        public List<BacteriologyTestResultBean> getMicroscopyResults() {
            return microscopyResults;
        }

        public void setMicroscopyResults(List<BacteriologyTestResultBean> microscopyResults) {
            this.microscopyResults = microscopyResults;
        }

        public List<BacteriologyTestResultBean> getCultureResults() {
            return cultureResults;
        }

        public void setCultureResults(List<BacteriologyTestResultBean> cultureResults) {
            this.cultureResults = cultureResults;
        }

        public String getSampleTypeName() {
            return sampleTypeName;
        }

        public void setSampleTypeName(String sampleTypeName) {
            this.sampleTypeName = sampleTypeName;
        }

        public Map<String, String> getMacroscopyResultsMap() {
            return macroscopyResultsMap;
        }

        public void setMacroscopyResultsMap(Map<String, String> macroscopyResultsMap) {
            this.macroscopyResultsMap = macroscopyResultsMap;
        }

        public Map<String, String> getMicroscopyResultsMap() {
            return microscopyResultsMap;
        }

        public void setMicroscopyResultsMap(Map<String, String> microscopyResultsMap) {
            this.microscopyResultsMap = microscopyResultsMap;
        }

        public Map<String, String> getCultureResultsMap() {
            return cultureResultsMap;
        }

        public void setCultureResultsMap(Map<String, String> cultureResultsMap) {
            this.cultureResultsMap = cultureResultsMap;
        }
    }

    /**
     * Organism with its antibiogram results
     */
    static class OrganismWithAntibiogram {
        private BacteriologyOrganism organism;
        private BacteriologyResultGroup organismGroup;
        private List<BacteriologyAntibiogram> antibiograms;

        public BacteriologyOrganism getOrganism() {
            return organism;
        }

        public void setOrganism(BacteriologyOrganism organism) {
            this.organism = organism;
        }

        public BacteriologyResultGroup getOrganismGroup() {
            return organismGroup;
        }

        public void setOrganismGroup(BacteriologyResultGroup organismGroup) {
            this.organismGroup = organismGroup;
        }

        public List<BacteriologyAntibiogram> getAntibiograms() {
            return antibiograms;
        }

        public void setAntibiograms(List<BacteriologyAntibiogram> antibiograms) {
            this.antibiograms = antibiograms;
        }
    }

    /**
     * Save complete bacteriology results for an analysis Handles all entities in
     * proper order with transaction management
     *
     * @param analysisId The analysis ID
     * @param resultData Complete result data
     * @param sysUserId  User performing the operation
     */
    void saveBacteriologyResults(Integer analysisId, BacteriologyResultData resultData, String sysUserId);

    /**
     * Get complete bacteriology results for an analysis
     *
     * @param analysisId The analysis ID
     * @return Complete result data structure
     */
    BacteriologyResultData getBacteriologyResults(Integer analysisId);

    /**
     * Create a new organism group with organism and antibiogram placeholder
     *
     * @param cultureGroupId The parent culture group ID
     * @param organismNumber The organism number (1, 2, or 3)
     * @param analysisId     The analysis ID
     * @param sysUserId      User performing the operation
     * @return The created organism group
     */
    BacteriologyResultGroup createOrganismGroup(Integer cultureGroupId, Integer organismNumber, Integer analysisId,
            String sysUserId);

    /**
     * Save organism identification for a group
     *
     * @param organismGroupId The organism group ID
     * @param organism        The organism data
     * @param sysUserId       User performing the operation
     * @return The saved organism
     */
    BacteriologyOrganism saveOrganismIdentification(Integer organismGroupId, BacteriologyOrganism organism,
            String sysUserId);

    /**
     * Save antibiogram results for an organism Creates antibiogram group if needed
     *
     * @param organismId   The organism ID
     * @param antibiograms List of antibiogram results
     * @param sysUserId    User performing the operation
     */
    void saveAntibiogramResults(Integer organismId, List<BacteriologyAntibiogram> antibiograms, String sysUserId);

    /**
     * Delete an organism and all its antibiograms (soft delete)
     *
     * @param organismGroupId The organism group ID
     * @param sysUserId       User performing the operation
     */
    void deleteOrganism(Integer organismGroupId, String sysUserId);

    /**
     * Clear all bacteriology results for an analysis (soft delete)
     *
     * @param analysisId The analysis ID
     * @param sysUserId  User performing the operation
     */
    void clearBacteriologyResults(Integer analysisId, String sysUserId);

    /**
     * Check if bacteriology results exist for an analysis
     *
     * @param analysisId The analysis ID
     * @return true if results exist
     */
    boolean hasBacteriologyResults(Integer analysisId);

    /**
     * Validate bacteriology workflow rules: - Culture group must exist before
     * organisms - Organism must exist before antibiogram - Max 3 organisms per
     * culture - Antibiogram only for bacteria type organisms
     *
     * @param resultData The result data to validate
     * @throws IllegalArgumentException if validation fails
     */
    void validateWorkflowRules(BacteriologyResultData resultData);
}

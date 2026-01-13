package org.openelisglobal.bacteriology.service;

import java.util.List;
import org.openelisglobal.bacteriology.valueholder.BacteriologyAntibiogram;
import org.openelisglobal.bacteriology.valueholder.BacteriologyOrganism;
import org.openelisglobal.bacteriology.valueholder.BacteriologyResultGroup;

/**
 * Orchestrates bacteriology workflow operations including: - Creating and
 * managing result groups hierarchy - Handling organism identification - Managing
 * antibiogram results - Coordinating save operations across multiple entities
 */
public interface BacteriologyWorkflowService {

    /**
     * Complete bacteriology result data including all groups, organisms, and
     * antibiograms
     */
    class BacteriologyResultData {
        private List<BacteriologyResultGroup> macroscopyGroups;
        private List<BacteriologyResultGroup> microscopyGroups;
        private BacteriologyResultGroup cultureGroup;
        private List<OrganismWithAntibiogram> organisms;

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
    }

    /**
     * Organism with its antibiogram results
     */
    class OrganismWithAntibiogram {
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

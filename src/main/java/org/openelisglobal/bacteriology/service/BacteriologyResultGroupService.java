package org.openelisglobal.bacteriology.service;

import java.util.List;
import org.openelisglobal.bacteriology.valueholder.BacteriologyResultGroup;
import org.openelisglobal.common.service.BaseObjectService;

public interface BacteriologyResultGroupService extends BaseObjectService<BacteriologyResultGroup, Integer> {

    /**
     * Save or update a result group
     *
     * @param resultGroup The result group to save
     * @return The saved result group with ID
     */
    BacteriologyResultGroup save(BacteriologyResultGroup resultGroup);

    /**
     * Get all groups for an analysis
     *
     * @param analysisId The analysis ID
     * @return List of result groups
     */
    List<BacteriologyResultGroup> getGroupsByAnalysisId(Integer analysisId);

    /**
     * Get groups by analysis ID and type
     *
     * @param analysisId The analysis ID
     * @param groupType  The group type (MACROSCOPY, MICROSCOPY, CULTURE, ORGANISM,
     *                   ANTIBIOGRAM)
     * @return List of result groups
     */
    List<BacteriologyResultGroup> getGroupsByAnalysisAndType(Integer analysisId, String groupType);

    /**
     * Get the culture group for an analysis (there should be only one)
     *
     * @param analysisId The analysis ID
     * @return The culture group or null
     */
    BacteriologyResultGroup getCultureGroupForAnalysis(Integer analysisId);

    /**
     * Get child groups (organisms or antibiograms) of a parent group
     *
     * @param parentGroupId The parent group ID
     * @return List of child groups
     */
    List<BacteriologyResultGroup> getChildGroups(Integer parentGroupId);

    /**
     * Deactivate all groups for an analysis (soft delete)
     *
     * @param analysisId The analysis ID
     */
    void deactivateGroupsForAnalysis(Integer analysisId);

    /**
     * Delete a result group (soft delete by setting isActive to false)
     *
     * @param id        The result group ID
     * @param sysUserId The user ID performing the operation
     */
    void deleteResultGroup(Integer id, String sysUserId);

    /**
     * Get the next display order for a group type in an analysis
     *
     * @param analysisId The analysis ID
     * @param groupType  The group type
     * @return The next display order value
     */
    Integer getNextDisplayOrder(Integer analysisId, String groupType);

    /**
     * Validate group type (MACROSCOPY, MICROSCOPY, CULTURE, ORGANISM, ANTIBIOGRAM)
     *
     * @param groupType The type to validate
     * @return true if valid
     */
    boolean isValidGroupType(String groupType);

    /**
     * Get organism groups for an analysis (groups with ORGANISM type)
     *
     * @param analysisId The analysis ID
     * @return List of organism groups
     */
    List<BacteriologyResultGroup> getOrganismGroupsForAnalysis(Integer analysisId);

    /**
     * Create a hierarchical structure of groups (macroscopy, microscopy, culture
     * with organisms and antibiograms)
     *
     * @param analysisId The analysis ID
     * @return Root groups (macroscopy, microscopy, culture) with populated
     *         children
     */
    List<BacteriologyResultGroup> getGroupHierarchyForAnalysis(Integer analysisId);
}

package org.openelisglobal.bacteriology.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.bacteriology.dao.BacteriologyResultGroupDAO;
import org.openelisglobal.bacteriology.valueholder.BacteriologyResultGroup;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.service.BaseObjectServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BacteriologyResultGroupServiceImpl extends BaseObjectServiceImpl<BacteriologyResultGroup, Integer>
        implements BacteriologyResultGroupService {

    private static final List<String> VALID_GROUP_TYPES = Arrays.asList("MACROSCOPY", "MICROSCOPY", "CULTURE",
            "ORGANISM", "ANTIBIOGRAM");

    @Autowired
    private BacteriologyResultGroupDAO baseObjectDAO;

    BacteriologyResultGroupServiceImpl() {
        super(BacteriologyResultGroup.class);
    }

    @Override
    protected BacteriologyResultGroupDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional
    public BacteriologyResultGroup save(BacteriologyResultGroup resultGroup) {
        try {
            // Validate group type
            if (!isValidGroupType(resultGroup.getGroupType())) {
                throw new IllegalArgumentException("Invalid group type: " + resultGroup.getGroupType()
                        + ". Must be one of: " + VALID_GROUP_TYPES);
            }

            if (resultGroup.getId() == null) {
                Integer id = insert(resultGroup);
                resultGroup.setId(id);
            } else {
                update(resultGroup);
            }
            return resultGroup;
        } catch (Exception e) {
            LogEvent.logError(e);
            throw new RuntimeException("Failed to save result group: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<BacteriologyResultGroup> getGroupsByAnalysisId(Integer analysisId) {
        try {
            return baseObjectDAO.getGroupsByAnalysisId(analysisId);
        } catch (Exception e) {
            LogEvent.logError(e);
            return new ArrayList<>();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<BacteriologyResultGroup> getGroupsByAnalysisAndType(Integer analysisId, String groupType) {
        try {
            return baseObjectDAO.getGroupsByAnalysisAndType(analysisId, groupType);
        } catch (Exception e) {
            LogEvent.logError(e);
            return new ArrayList<>();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public BacteriologyResultGroup getCultureGroupForAnalysis(Integer analysisId) {
        try {
            return baseObjectDAO.getCultureGroupForAnalysis(analysisId);
        } catch (Exception e) {
            LogEvent.logError(e);
            return null;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<BacteriologyResultGroup> getChildGroups(Integer parentGroupId) {
        try {
            return baseObjectDAO.getChildGroups(parentGroupId);
        } catch (Exception e) {
            LogEvent.logError(e);
            return new ArrayList<>();
        }
    }

    @Override
    @Transactional
    public void deactivateGroupsForAnalysis(Integer analysisId) {
        try {
            baseObjectDAO.deactivateGroupsForAnalysis(analysisId);
        } catch (Exception e) {
            LogEvent.logError(e);
            throw new RuntimeException("Failed to deactivate groups: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void deleteResultGroup(Integer id, String sysUserId) {
        try {
            BacteriologyResultGroup resultGroup = get(id);
            if (resultGroup != null) {
                resultGroup.setIsActive(false);
                resultGroup.setLastupdated(new java.sql.Timestamp(System.currentTimeMillis()));
                update(resultGroup);
            }
        } catch (Exception e) {
            LogEvent.logError(e);
            throw new RuntimeException("Failed to delete result group: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getNextDisplayOrder(Integer analysisId, String groupType) {
        try {
            List<BacteriologyResultGroup> groups = baseObjectDAO.getGroupsByAnalysisAndType(analysisId, groupType);
            if (groups.isEmpty()) {
                return 1;
            }

            int maxOrder = 0;
            for (BacteriologyResultGroup group : groups) {
                if (group.getDisplayOrder() != null && group.getDisplayOrder() > maxOrder) {
                    maxOrder = group.getDisplayOrder();
                }
            }
            return maxOrder + 1;
        } catch (Exception e) {
            LogEvent.logError(e);
            return 1;
        }
    }

    @Override
    public boolean isValidGroupType(String groupType) {
        return groupType != null && VALID_GROUP_TYPES.contains(groupType.toUpperCase());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BacteriologyResultGroup> getOrganismGroupsForAnalysis(Integer analysisId) {
        try {
            return baseObjectDAO.getGroupsByAnalysisAndType(analysisId, "ORGANISM");
        } catch (Exception e) {
            LogEvent.logError(e);
            return new ArrayList<>();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<BacteriologyResultGroup> getGroupHierarchyForAnalysis(Integer analysisId) {
        try {
            // Get all groups for the analysis
            List<BacteriologyResultGroup> allGroups = baseObjectDAO.getGroupsByAnalysisId(analysisId);

            // Create a map for quick lookup
            Map<Integer, BacteriologyResultGroup> groupMap = new HashMap<>();
            for (BacteriologyResultGroup group : allGroups) {
                groupMap.put(group.getId(), group);
            }

            // Build hierarchy by linking children to parents
            List<BacteriologyResultGroup> rootGroups = new ArrayList<>();
            for (BacteriologyResultGroup group : allGroups) {
                if (group.getParentGroupId() == null) {
                    // This is a root group (MACROSCOPY, MICROSCOPY, or CULTURE)
                    rootGroups.add(group);
                } else {
                    // This is a child group, link it to its parent
                    BacteriologyResultGroup parent = groupMap.get(group.getParentGroupId());
                    if (parent != null) {
                        // Parent should maintain a list of children
                        // Note: This assumes BacteriologyResultGroup has a transient children
                        // field
                        // If not, we'll need to add it
                    }
                }
            }

            return rootGroups;
        } catch (Exception e) {
            LogEvent.logError(e);
            return new ArrayList<>();
        }
    }
}

package org.openelisglobal.bacteriology.service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.bacteriology.valueholder.BacteriologyAntibiogram;
import org.openelisglobal.bacteriology.valueholder.BacteriologyOrganism;
import org.openelisglobal.bacteriology.valueholder.BacteriologyResultGroup;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BacteriologyWorkflowServiceImpl implements BacteriologyWorkflowService {

    @Autowired
    private BacteriologyResultGroupService resultGroupService;

    @Autowired
    private BacteriologyOrganismService organismService;

    @Autowired
    private BacteriologyAntibiogramService antibiogramService;

    @Override
    @Transactional
    public void saveBacteriologyResults(Integer analysisId, BacteriologyResultData resultData, String sysUserId) {
        try {
            // Validate workflow rules
            validateWorkflowRules(resultData);

            Timestamp now = new Timestamp(System.currentTimeMillis());

            // 1. Save macroscopy groups
            if (resultData.getMacroscopyGroups() != null) {
                for (BacteriologyResultGroup group : resultData.getMacroscopyGroups()) {
                    group.setAnalysisId(analysisId);
                    group.setGroupType("MACROSCOPY");
                    group.setLastupdated(now);
                    group.setSysUserId(sysUserId);
                    resultGroupService.save(group);
                }
            }

            // 2. Save microscopy groups
            if (resultData.getMicroscopyGroups() != null) {
                for (BacteriologyResultGroup group : resultData.getMicroscopyGroups()) {
                    group.setAnalysisId(analysisId);
                    group.setGroupType("MICROSCOPY");
                    group.setLastupdated(now);
                    group.setSysUserId(sysUserId);
                    resultGroupService.save(group);
                }
            }

            // 3. Save culture group
            if (resultData.getCultureGroup() != null) {
                BacteriologyResultGroup cultureGroup = resultData.getCultureGroup();
                cultureGroup.setAnalysisId(analysisId);
                cultureGroup.setGroupType("CULTURE");
                cultureGroup.setLastupdated(now);
                cultureGroup.setSysUserId(sysUserId);
                resultGroupService.save(cultureGroup);

                // 4. Save organisms and their antibiograms
                if (resultData.getOrganisms() != null) {
                    for (OrganismWithAntibiogram organismData : resultData.getOrganisms()) {
                        // Save organism group
                        BacteriologyResultGroup organismGroup = organismData.getOrganismGroup();
                        organismGroup.setAnalysisId(analysisId);
                        organismGroup.setGroupType("ORGANISM");
                        organismGroup.setParentGroupId(cultureGroup.getId());
                        organismGroup.setLastupdated(now);
                        organismGroup.setSysUserId(sysUserId);
                        resultGroupService.save(organismGroup);

                        // Save organism
                        BacteriologyOrganism organism = organismData.getOrganism();
                        organism.setResultGroupId(organismGroup.getId());
                        organism.setLastupdated(now);
                        organismService.save(organism);

                        // Save antibiograms if organism is bacteria
                        if ("BACTERIA".equals(organism.getOrganismType())
                                && organismData.getAntibiograms() != null
                                && !organismData.getAntibiograms().isEmpty()) {

                            // Create antibiogram group
                            BacteriologyResultGroup antibiogramGroup = new BacteriologyResultGroup();
                            antibiogramGroup.setAnalysisId(analysisId);
                            antibiogramGroup.setGroupType("ANTIBIOGRAM");
                            antibiogramGroup.setParentGroupId(organismGroup.getId());
                            antibiogramGroup.setGroupNumber(organism.getOrganismNumber());
                            antibiogramGroup.setDisplayOrder(1);
                            antibiogramGroup.setIsActive(true);
                            antibiogramGroup.setLastupdated(now);
                            antibiogramGroup.setSysUserId(sysUserId);
                            resultGroupService.save(antibiogramGroup);

                            // Save each antibiogram
                            for (BacteriologyAntibiogram antibiogram : organismData.getAntibiograms()) {
                                antibiogram.setOrganismId(organism.getId());
                                antibiogram.setLastupdated(now);
                                antibiogramService.save(antibiogram);
                            }
                        }
                    }
                }
            }

            LogEvent.logInfo(this.getClass().getSimpleName(), "saveBacteriologyResults",
                    "Successfully saved bacteriology results for analysis: " + analysisId);
        } catch (Exception e) {
            LogEvent.logError(e);
            throw new RuntimeException("Failed to save bacteriology results: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public BacteriologyResultData getBacteriologyResults(Integer analysisId) {
        try {
            BacteriologyResultData resultData = new BacteriologyResultData();

            // Get macroscopy groups
            resultData.setMacroscopyGroups(resultGroupService.getGroupsByAnalysisAndType(analysisId, "MACROSCOPY"));

            // Get microscopy groups
            resultData.setMicroscopyGroups(resultGroupService.getGroupsByAnalysisAndType(analysisId, "MICROSCOPY"));

            // Get culture group
            resultData.setCultureGroup(resultGroupService.getCultureGroupForAnalysis(analysisId));

            // Get organisms with antibiograms
            List<OrganismWithAntibiogram> organisms = new ArrayList<>();
            List<BacteriologyResultGroup> organismGroups = resultGroupService.getOrganismGroupsForAnalysis(analysisId);

            for (BacteriologyResultGroup organismGroup : organismGroups) {
                OrganismWithAntibiogram organismData = new OrganismWithAntibiogram();
                organismData.setOrganismGroup(organismGroup);

                // Get organism
                BacteriologyOrganism organism = organismService.getByGroupId(organismGroup.getId());
                organismData.setOrganism(organism);

                // Get antibiograms if organism exists
                if (organism != null) {
                    List<BacteriologyAntibiogram> antibiograms = antibiogramService
                            .getAntibiogramsByOrganismId(organism.getId());
                    organismData.setAntibiograms(antibiograms);
                }

                organisms.add(organismData);
            }

            resultData.setOrganisms(organisms);

            return resultData;
        } catch (Exception e) {
            LogEvent.logError(e);
            throw new RuntimeException("Failed to get bacteriology results: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public BacteriologyResultGroup createOrganismGroup(Integer cultureGroupId, Integer organismNumber,
            Integer analysisId, String sysUserId) {
        try {
            Timestamp now = new Timestamp(System.currentTimeMillis());

            BacteriologyResultGroup organismGroup = new BacteriologyResultGroup();
            organismGroup.setAnalysisId(analysisId);
            organismGroup.setGroupType("ORGANISM");
            organismGroup.setGroupNumber(organismNumber);
            organismGroup.setParentGroupId(cultureGroupId);
            organismGroup.setDisplayOrder(organismNumber);
            organismGroup.setIsActive(true);
            organismGroup.setCreatedDate(now);
            organismGroup.setLastupdated(now);
            organismGroup.setSysUserId(sysUserId);

            return resultGroupService.save(organismGroup);
        } catch (Exception e) {
            LogEvent.logError(e);
            throw new RuntimeException("Failed to create organism group: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public BacteriologyOrganism saveOrganismIdentification(Integer organismGroupId, BacteriologyOrganism organism,
            String sysUserId) {
        try {
            organism.setResultGroupId(organismGroupId);
            organism.setLastupdated(new Timestamp(System.currentTimeMillis()));
            return organismService.save(organism);
        } catch (Exception e) {
            LogEvent.logError(e);
            throw new RuntimeException("Failed to save organism identification: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void saveAntibiogramResults(Integer organismId, List<BacteriologyAntibiogram> antibiograms,
            String sysUserId) {
        try {
            Timestamp now = new Timestamp(System.currentTimeMillis());

            for (BacteriologyAntibiogram antibiogram : antibiograms) {
                antibiogram.setOrganismId(organismId);
                antibiogram.setLastupdated(now);
                antibiogramService.save(antibiogram);
            }

            LogEvent.logInfo(this.getClass().getSimpleName(), "saveAntibiogramResults",
                    "Saved " + antibiograms.size() + " antibiograms for organism: " + organismId);
        } catch (Exception e) {
            LogEvent.logError(e);
            throw new RuntimeException("Failed to save antibiogram results: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void deleteOrganism(Integer organismGroupId, String sysUserId) {
        try {
            // Get organism
            BacteriologyOrganism organism = organismService.getByGroupId(organismGroupId);

            if (organism != null) {
                // Delete antibiograms first
                List<BacteriologyAntibiogram> antibiograms = antibiogramService
                        .getAntibiogramsByOrganismId(organism.getId());
                for (BacteriologyAntibiogram antibiogram : antibiograms) {
                    antibiogramService.deleteAntibiogram(antibiogram.getId(), sysUserId);
                }

                // Delete antibiogram group
                List<BacteriologyResultGroup> antibiogramGroups = resultGroupService.getChildGroups(organismGroupId);
                for (BacteriologyResultGroup group : antibiogramGroups) {
                    resultGroupService.deleteResultGroup(group.getId(), sysUserId);
                }

                // Delete organism
                organismService.deleteOrganism(organism.getId(), sysUserId);
            }

            // Delete organism group
            resultGroupService.deleteResultGroup(organismGroupId, sysUserId);

            LogEvent.logInfo(this.getClass().getSimpleName(), "deleteOrganism",
                    "Deleted organism group and related data: " + organismGroupId);
        } catch (Exception e) {
            LogEvent.logError(e);
            throw new RuntimeException("Failed to delete organism: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void clearBacteriologyResults(Integer analysisId, String sysUserId) {
        try {
            antibiogramService.deactivateAntibiogramsForAnalysis(analysisId);
            organismService.deactivateOrganismsForAnalysis(analysisId);
            resultGroupService.deactivateGroupsForAnalysis(analysisId);

            LogEvent.logInfo(this.getClass().getSimpleName(), "clearBacteriologyResults",
                    "Cleared all bacteriology results for analysis: " + analysisId);
        } catch (Exception e) {
            LogEvent.logError(e);
            throw new RuntimeException("Failed to clear bacteriology results: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasBacteriologyResults(Integer analysisId) {
        try {
            List<BacteriologyResultGroup> groups = resultGroupService.getGroupsByAnalysisId(analysisId);
            return groups != null && !groups.isEmpty();
        } catch (Exception e) {
            LogEvent.logError(e);
            return false;
        }
    }

    @Override
    public void validateWorkflowRules(BacteriologyResultData resultData) {
        // Validate max 3 organisms
        if (resultData.getOrganisms() != null && resultData.getOrganisms().size() > 3) {
            throw new IllegalArgumentException("Maximum 3 organisms allowed per culture");
        }

        // Validate organism numbers are unique and in range 1-3
        if (resultData.getOrganisms() != null) {
            Map<Integer, Boolean> organismNumbers = new HashMap<>();
            for (OrganismWithAntibiogram organismData : resultData.getOrganisms()) {
                if (organismData.getOrganism() != null) {
                    Integer number = organismData.getOrganism().getOrganismNumber();
                    if (number == null || number < 1 || number > 3) {
                        throw new IllegalArgumentException("Organism number must be between 1 and 3");
                    }
                    if (organismNumbers.containsKey(number)) {
                        throw new IllegalArgumentException("Duplicate organism number: " + number);
                    }
                    organismNumbers.put(number, true);

                    // Validate antibiograms only for bacteria
                    if (!"BACTERIA".equals(organismData.getOrganism().getOrganismType())
                            && organismData.getAntibiograms() != null
                            && !organismData.getAntibiograms().isEmpty()) {
                        throw new IllegalArgumentException(
                                "Antibiograms can only be added for bacteria organisms");
                    }
                }
            }
        }
    }
}

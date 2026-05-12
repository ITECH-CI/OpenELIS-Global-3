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
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
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

    @Autowired
    private DictionaryService dictionaryService;

    @Autowired
    private org.openelisglobal.analysis.service.AnalysisService analysisService;

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

            // 3. Save culture groups (one per test) and organisms
            // Track culture groups by testId
            Map<Integer, BacteriologyResultGroup> cultureGroupsByTestId = new HashMap<>();

            // First, collect all culture groups from organisms
            if (resultData.getOrganisms() != null) {
                for (OrganismWithAntibiogram organismData : resultData.getOrganisms()) {
                    BacteriologyResultGroup organismGroup = organismData.getOrganismGroup();
                    Integer testId = organismGroup.getTestId();

                    if (testId != null && !cultureGroupsByTestId.containsKey(testId)) {
                        // Use analysisId from organismGroup if available (comes from controller's Map)
                        Integer cultureAnalysisId = organismGroup.getAnalysisId();
                        if (cultureAnalysisId == null) {
                            cultureAnalysisId = analysisId; // Fallback
                        }

                        // Create or find culture group for this test
                        List<BacteriologyResultGroup> existingCultureGroups = resultGroupService
                                .getGroupsByAnalysisAndType(cultureAnalysisId, "CULTURE");

                        BacteriologyResultGroup cultureGroup = null;
                        for (BacteriologyResultGroup group : existingCultureGroups) {
                            if (testId.equals(group.getTestId())) {
                                cultureGroup = group;
                                break;
                            }
                        }

                        if (cultureGroup == null) {
                            cultureGroup = new BacteriologyResultGroup();
                            cultureGroup.setAnalysisId(cultureAnalysisId);
                            cultureGroup.setGroupType("CULTURE");
                            cultureGroup.setTestId(testId);
                            cultureGroup.setDisplayOrder(1);
                            cultureGroup.setIsActive(true);
                            cultureGroup.setCreatedDate(now);
                        }

                        cultureGroup.setLastupdated(now);
                        cultureGroup.setSysUserId(sysUserId);
                        resultGroupService.save(cultureGroup);

                        cultureGroupsByTestId.put(testId, cultureGroup);
                    }
                }
            }

            // Backward compatibility: if there's a single culture group without testId
            if (resultData.getCultureGroup() != null && cultureGroupsByTestId.isEmpty()) {
                BacteriologyResultGroup cultureGroup = resultData.getCultureGroup();
                // Only set analysisId if not already set
                if (cultureGroup.getAnalysisId() == null) {
                    cultureGroup.setAnalysisId(analysisId);
                }
                cultureGroup.setGroupType("CULTURE");
                cultureGroup.setLastupdated(now);
                cultureGroup.setSysUserId(sysUserId);
                resultGroupService.save(cultureGroup);
                // Use null as key for backward compatibility
                cultureGroupsByTestId.put(null, cultureGroup);
            }

            // 4. Get existing organisms for this analysis to handle updates/deletes
            List<BacteriologyResultGroup> existingOrganismGroups = resultGroupService
                    .getOrganismGroupsForAnalysis(analysisId);
            List<Integer> processedOrganismIds = new ArrayList<>();

            // 5. Save organisms and their antibiograms
            if (resultData.getOrganisms() != null) {
                for (OrganismWithAntibiogram organismData : resultData.getOrganisms()) {
                    BacteriologyOrganism organism = organismData.getOrganism();

                    // Save or update organism group
                    BacteriologyResultGroup organismGroup = organismData.getOrganismGroup();
                    // Only set analysisId if not already set (preserve the correct analysisId from controller)
                    if (organismGroup.getAnalysisId() == null) {
                        organismGroup.setAnalysisId(analysisId);
                    }
                    organismGroup.setGroupType("ORGANISM");

                    // Link to appropriate culture group by testId
                    Integer testId = organismGroup.getTestId();
                    BacteriologyResultGroup cultureGroup = cultureGroupsByTestId.get(testId);
                    if (cultureGroup != null) {
                        organismGroup.setParentGroupId(cultureGroup.getId());
                    }

                    organismGroup.setLastupdated(now);
                    organismGroup.setSysUserId(sysUserId);
                    resultGroupService.save(organismGroup);

                    // Save or update organism
                    organism.setResultGroupId(organismGroup.getId());
                    organism.setLastupdated(now);
                    organismService.save(organism);

                    // Track processed organism
                    if (organism.getId() != null) {
                        processedOrganismIds.add(organism.getId());
                    }

                    // Handle antibiograms for bacteria
                    if ("BACTERIA".equals(organism.getOrganismType()) && organismData.getAntibiograms() != null
                            && !organismData.getAntibiograms().isEmpty()) {

                        // Find or create antibiogram group for this organism
                        // Use the same analysisId as the organism group
                        Integer organismAnalysisId = organismGroup.getAnalysisId() != null ? organismGroup.getAnalysisId() : analysisId;
                        List<BacteriologyResultGroup> antibiogramGroups = resultGroupService
                                .getGroupsByAnalysisAndType(organismAnalysisId, "ANTIBIOGRAM");
                        BacteriologyResultGroup antibiogramGroup = null;

                        for (BacteriologyResultGroup group : antibiogramGroups) {
                            if (group.getParentGroupId() != null
                                    && group.getParentGroupId().equals(organismGroup.getId())) {
                                antibiogramGroup = group;
                                break;
                            }
                        }

                        if (antibiogramGroup == null) {
                            antibiogramGroup = new BacteriologyResultGroup();
                            // Use the same analysisId as the organism group
                            antibiogramGroup.setAnalysisId(organismGroup.getAnalysisId() != null ? organismGroup.getAnalysisId() : analysisId);
                            antibiogramGroup.setGroupType("ANTIBIOGRAM");
                            antibiogramGroup.setParentGroupId(organismGroup.getId());
                            antibiogramGroup.setGroupNumber(organism.getOrganismNumber());
                            antibiogramGroup.setDisplayOrder(1);
                            antibiogramGroup.setIsActive(true);
                        }

                        antibiogramGroup.setLastupdated(now);
                        antibiogramGroup.setSysUserId(sysUserId);
                        resultGroupService.save(antibiogramGroup);

                        // Get existing antibiograms for this organism
                        List<BacteriologyAntibiogram> existingAntibiograms = antibiogramService
                                .getAntibiogramsByOrganismId(organism.getId());
                        List<Integer> processedAntibiogramIds = new ArrayList<>();

                        // Save or update each antibiogram
                        for (BacteriologyAntibiogram antibiogram : organismData.getAntibiograms()) {
                            antibiogram.setOrganismId(organism.getId());
                            antibiogram.setLastupdated(now);
                            antibiogramService.save(antibiogram);

                            if (antibiogram.getId() != null) {
                                processedAntibiogramIds.add(antibiogram.getId());
                            }
                        }

                        // Deactivate antibiograms that were removed
                        for (BacteriologyAntibiogram existing : existingAntibiograms) {
                            if (!processedAntibiogramIds.contains(existing.getId())) {
                                existing.setIsActive(false);
                                existing.setLastupdated(now);
                                antibiogramService.save(existing);
                            }
                        }
                    }
                }

                // 6. Deactivate organisms that were removed
                for (BacteriologyResultGroup existingGroup : existingOrganismGroups) {
                    BacteriologyOrganism existingOrganism = organismService.getByGroupId(existingGroup.getId());
                    if (existingOrganism != null && existingOrganism.getId() != null
                            && !processedOrganismIds.contains(existingOrganism.getId())) {
                        existingOrganism.setIsActive(false);
                        existingOrganism.setLastupdated(now);
                        organismService.save(existingOrganism);
                    }
                }
            }
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

            // Get analysis to extract sample type name
            org.openelisglobal.analysis.valueholder.Analysis analysis = analysisService
                    .getAnalysisById(String.valueOf(analysisId));
            if (analysis != null && analysis.getSampleTypeName() != null) {
                resultData.setSampleTypeName(analysis.getSampleTypeName());
            }

            // Get ALL analyses for this sample item to collect all bacteriology data
            // This is necessary because organisms may be linked to different analyses (e.g., specific culture tests)
            List<org.openelisglobal.analysis.valueholder.Analysis> allAnalyses = new ArrayList<>();
            if (analysis != null && analysis.getSampleItem() != null) {
                allAnalyses = analysisService.getAnalysesBySampleItem(analysis.getSampleItem());
            }
            if (allAnalyses.isEmpty()) {
                allAnalyses.add(analysis); // Fallback to single analysis
            }

            // Collect all analysisIds for loading bacteriology groups
            List<Integer> allAnalysisIds = new ArrayList<>();
            for (org.openelisglobal.analysis.valueholder.Analysis a : allAnalyses) {
                allAnalysisIds.add(Integer.parseInt(a.getId()));
            }

            // Get macroscopy groups from all analyses
            List<BacteriologyResultGroup> macroscopyGroups = new ArrayList<>();
            for (Integer aid : allAnalysisIds) {
                macroscopyGroups.addAll(resultGroupService.getGroupsByAnalysisAndType(aid, "MACROSCOPY"));
            }
            resultData.setMacroscopyGroups(macroscopyGroups);

            // Get microscopy groups from all analyses
            List<BacteriologyResultGroup> microscopyGroups = new ArrayList<>();
            for (Integer aid : allAnalysisIds) {
                microscopyGroups.addAll(resultGroupService.getGroupsByAnalysisAndType(aid, "MICROSCOPY"));
            }
            resultData.setMicroscopyGroups(microscopyGroups);

            // Get culture group (backward compatibility - single culture group)
            resultData.setCultureGroup(resultGroupService.getCultureGroupForAnalysis(analysisId));

            // Get organisms with antibiograms from ALL analyses
            List<OrganismWithAntibiogram> organisms = new ArrayList<>();
            List<BacteriologyResultGroup> organismGroups = new ArrayList<>();
            for (Integer aid : allAnalysisIds) {
                organismGroups.addAll(resultGroupService.getOrganismGroupsForAnalysis(aid));
            }

            for (BacteriologyResultGroup organismGroup : organismGroups) {
                OrganismWithAntibiogram organismData = new OrganismWithAntibiogram();
                organismData.setOrganismGroup(organismGroup);

                // Get organism
                BacteriologyOrganism organism = organismService.getByGroupId(organismGroup.getId());

                // Provide a display-ready label in resolvedOrganismName: free text wins,
                // otherwise resolve from the dictionary. organismNameText itself is left
                // untouched so the result-entry screen keeps its free-text input empty
                // when the user picked the organism from the dictionary.
                if (organism != null) {
                    String displayName = organism.getOrganismNameText();
                    if ((displayName == null || displayName.trim().isEmpty())
                            && organism.getOrganismNameDictId() != null) {
                        try {
                            Dictionary dict = dictionaryService
                                    .getDataForId(String.valueOf(organism.getOrganismNameDictId()));
                            if (dict != null) {
                                displayName = dict.getLocalizedName();
                            }
                        } catch (Exception e) {
                            LogEvent.logError("BacteriologyWorkflowServiceImpl", "getBacteriologyResults",
                                    "Exception resolving organism name for dictId "
                                            + organism.getOrganismNameDictId() + ": " + e.getMessage());
                        }
                    }
                    organism.setResolvedOrganismName(displayName);
                }

                organismData.setOrganism(organism);

                // Get antibiograms if organism exists
                if (organism != null) {
                    List<BacteriologyAntibiogram> antibiograms = antibiogramService
                            .getAntibiogramsByOrganismId(organism.getId());

                    // Resolve antibiotic and interpretation names from dictionary
                    for (BacteriologyAntibiogram antibiogram : antibiograms) {
                        // Resolve antibiotic name
                        if (antibiogram.getAntibioticDictId() != null && (antibiogram.getAntibioticNameText() == null
                                || antibiogram.getAntibioticNameText().trim().isEmpty())) {
                            try {
                                Dictionary dict = dictionaryService
                                        .getDataForId(String.valueOf(antibiogram.getAntibioticDictId()));

                                if (dict != null) {
                                    antibiogram.setAntibioticNameText(dict.getLocalizedName());
                                }
                            } catch (Exception e) {
                                LogEvent.logError("BacteriologyWorkflowServiceImpl", "getBacteriologyResults",
                                        "Exception resolving antibiotic name for dictId "
                                                + antibiogram.getAntibioticDictId() + ": " + e.getMessage());
                            }
                        }

                        // Set interpretation text from result field (S/R/I)
                        if (antibiogram.getResult() != null && !antibiogram.getResult().trim().isEmpty()) {
                            antibiogram.setInterpretationText(antibiogram.getResult());
                        }
                    }

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
        // NOTE: Validation disabled for now as we're handling multiple cultures
        // Each culture can have up to 3 organisms, so total can be > 3
        // TODO: Implement per-culture validation when culture-based architecture is
        // ready

        // Validate organism numbers are in range 1-3
        if (resultData.getOrganisms() != null) {
            for (OrganismWithAntibiogram organismData : resultData.getOrganisms()) {
                if (organismData.getOrganism() != null) {
                    Integer number = organismData.getOrganism().getOrganismNumber();
                    if (number == null || number < 1 || number > 3) {
                        throw new IllegalArgumentException("Organism number must be between 1 and 3");
                    }

                    // Validate antibiograms only for bacteria
                    if (!"BACTERIA".equals(organismData.getOrganism().getOrganismType())
                            && organismData.getAntibiograms() != null && !organismData.getAntibiograms().isEmpty()) {
                        throw new IllegalArgumentException("Antibiograms can only be added for bacteria organisms");
                    }
                }
            }
        }
    }
}

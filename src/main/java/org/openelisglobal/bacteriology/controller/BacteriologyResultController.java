package org.openelisglobal.bacteriology.controller;

import jakarta.validation.Valid;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.openelisglobal.bacteriology.action.bean.AntibioticDTO;
import org.openelisglobal.bacteriology.action.bean.AntibiogramResultBean;
import org.openelisglobal.bacteriology.action.bean.BacteriologyOrganismBean;
import org.openelisglobal.bacteriology.action.bean.BacteriologyResultForm;
import org.openelisglobal.bacteriology.service.BacteriologyAntibiogramService;
import org.openelisglobal.bacteriology.service.BacteriologyOrganismService;
import org.openelisglobal.bacteriology.service.BacteriologyResultGroupService;
import org.openelisglobal.bacteriology.service.BacteriologyWorkflowService;
import org.openelisglobal.bacteriology.service.BacteriologyWorkflowService.BacteriologyResultData;
import org.openelisglobal.bacteriology.service.BacteriologyWorkflowService.OrganismWithAntibiogram;
import org.openelisglobal.bacteriology.valueholder.BacteriologyAntibiogram;
import org.openelisglobal.bacteriology.valueholder.BacteriologyOrganism;
import org.openelisglobal.bacteriology.valueholder.BacteriologyResultGroup;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/bacteriology")
public class BacteriologyResultController {

    @Autowired
    private BacteriologyWorkflowService workflowService;

    @Autowired
    private BacteriologyResultGroupService resultGroupService;

    @Autowired
    private BacteriologyOrganismService organismService;

    @Autowired
    private BacteriologyAntibiogramService antibiogramService;

    /**
     * Get all antibiotics from dictionary
     */
    @GetMapping(value = "/antibiotics", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<AntibioticDTO>> getAllAntibiotics() {
        try {
            List<Dictionary> antibiotics = antibiogramService.getAllAntibioticsSorted();
            List<AntibioticDTO> dtoList = antibiotics.stream()
                    .map(dict -> new AntibioticDTO(Integer.parseInt(dict.getId()), dict.getDictEntry(),
                            dict.getLocalAbbreviation(), dict.getSortOrder()))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(dtoList);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get all organism names from dictionary
     */
    @GetMapping(value = "/organisms", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<AntibioticDTO>> getAllOrganisms() {
        try {
            List<Dictionary> organisms = organismService.getAllOrganismNamesSorted();
            List<AntibioticDTO> dtoList = organisms.stream()
                    .map(dict -> new AntibioticDTO(Integer.parseInt(dict.getId()), dict.getDictEntry(),
                            dict.getLocalAbbreviation(), dict.getSortOrder()))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(dtoList);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get bacteriology results for an analysis
     */
    @GetMapping(value = "/results/{analysisId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BacteriologyResultData> getBacteriologyResults(
            @PathVariable("analysisId") Integer analysisId) {
        try {
            BacteriologyResultData resultData = workflowService.getBacteriologyResults(analysisId);
            return ResponseEntity.ok(resultData);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Check if bacteriology results exist for an analysis
     */
    @GetMapping(value = "/results/{analysisId}/exists", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Boolean> hasBacteriologyResults(@PathVariable("analysisId") Integer analysisId) {
        try {
            boolean exists = workflowService.hasBacteriologyResults(analysisId);
            return ResponseEntity.ok(exists);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Save complete bacteriology results
     */
    @PostMapping(value = "/results", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> saveBacteriologyResults(@Valid @RequestBody BacteriologyResultForm form,
            BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body("Validation errors: " + bindingResult.getAllErrors());
        }

        try {
            // Convert form to BacteriologyResultData
            BacteriologyResultData resultData = convertFormToResultData(form);

            // Save results
            workflowService.saveBacteriologyResults(form.getAnalysisId(), resultData, form.getSysUserId());

            return ResponseEntity.ok("Bacteriology results saved successfully");
        } catch (IllegalArgumentException e) {
            LogEvent.logError(e);
            return ResponseEntity.badRequest().body("Validation error: " + e.getMessage());
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to save bacteriology results: " + e.getMessage());
        }
    }

    /**
     * Create a new organism group
     */
    @PostMapping(value = "/organism/group", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BacteriologyResultGroup> createOrganismGroup(@RequestParam("cultureGroupId") Integer cultureGroupId,
            @RequestParam("organismNumber") Integer organismNumber, @RequestParam("analysisId") Integer analysisId,
            @RequestParam("sysUserId") String sysUserId) {
        try {
            BacteriologyResultGroup organismGroup = workflowService.createOrganismGroup(cultureGroupId, organismNumber,
                    analysisId, sysUserId);
            return ResponseEntity.ok(organismGroup);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Save organism identification
     */
    @PostMapping(value = "/organism", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> saveOrganismIdentification(@Valid @RequestBody BacteriologyOrganismBean organismBean,
            BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body("Validation errors: " + bindingResult.getAllErrors());
        }

        try {
            // Convert bean to entity
            BacteriologyOrganism organism = convertBeanToOrganism(organismBean);

            // Save organism
            workflowService.saveOrganismIdentification(organismBean.getOrganismGroupId(), organism, "SYSTEM");

            return ResponseEntity.ok("Organism identification saved successfully");
        } catch (IllegalArgumentException e) {
            LogEvent.logError(e);
            return ResponseEntity.badRequest().body("Validation error: " + e.getMessage());
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to save organism identification: " + e.getMessage());
        }
    }

    /**
     * Save antibiogram results for an organism
     */
    @PostMapping(value = "/antibiogram/{organismId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> saveAntibiogramResults(@PathVariable("organismId") Integer organismId,
            @RequestBody List<@Valid AntibiogramResultBean> antibiogramBeans, @RequestParam("sysUserId") String sysUserId) {
        try {
            // Convert beans to entities
            List<BacteriologyAntibiogram> antibiograms = antibiogramBeans.stream()
                    .map(this::convertBeanToAntibiogram).collect(Collectors.toList());

            // Save antibiograms
            workflowService.saveAntibiogramResults(organismId, antibiograms, sysUserId);

            return ResponseEntity.ok("Antibiogram results saved successfully");
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to save antibiogram results: " + e.getMessage());
        }
    }

    /**
     * Delete an organism and its antibiograms
     */
    @DeleteMapping(value = "/organism/{organismGroupId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> deleteOrganism(@PathVariable("organismGroupId") Integer organismGroupId,
            @RequestParam("sysUserId") String sysUserId) {
        try {
            workflowService.deleteOrganism(organismGroupId, sysUserId);
            return ResponseEntity.ok("Organism deleted successfully");
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to delete organism: " + e.getMessage());
        }
    }

    /**
     * Clear all bacteriology results for an analysis
     */
    @DeleteMapping(value = "/results/{analysisId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> clearBacteriologyResults(@PathVariable("analysisId") Integer analysisId,
            @RequestParam("sysUserId") String sysUserId) {
        try {
            workflowService.clearBacteriologyResults(analysisId, sysUserId);
            return ResponseEntity.ok("Bacteriology results cleared successfully");
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to clear bacteriology results: " + e.getMessage());
        }
    }

    // Helper methods for conversion

    private BacteriologyResultData convertFormToResultData(BacteriologyResultForm form) {
        BacteriologyResultData resultData = new BacteriologyResultData();
        Timestamp now = new Timestamp(System.currentTimeMillis());

        // Create macroscopy groups if macroscopy results are provided
        if (form.getMacroscopyResults() != null && !form.getMacroscopyResults().isEmpty()) {
            List<BacteriologyResultGroup> macroscopyGroups = new ArrayList<>();
            BacteriologyResultGroup macroscopyGroup = new BacteriologyResultGroup();
            macroscopyGroup.setAnalysisId(form.getAnalysisId());
            macroscopyGroup.setGroupType("MACROSCOPY");
            macroscopyGroup.setDisplayOrder(1);
            macroscopyGroup.setIsActive(true);
            macroscopyGroup.setCreatedDate(now);
            macroscopyGroups.add(macroscopyGroup);
            resultData.setMacroscopyGroups(macroscopyGroups);

            // Note: The actual test result values in macroscopyResults map (testId -> dictionaryId)
            // should be saved to the standard result table via the Result service.
            // This is handled separately from the bacteriology workflow.
        }

        // Create microscopy groups if microscopy results are provided
        if (form.getMicroscopyResults() != null && !form.getMicroscopyResults().isEmpty()) {
            List<BacteriologyResultGroup> microscopyGroups = new ArrayList<>();
            BacteriologyResultGroup microscopyGroup = new BacteriologyResultGroup();
            microscopyGroup.setAnalysisId(form.getAnalysisId());
            microscopyGroup.setGroupType("MICROSCOPY");
            microscopyGroup.setDisplayOrder(1);
            microscopyGroup.setIsActive(true);
            microscopyGroup.setCreatedDate(now);
            microscopyGroups.add(microscopyGroup);
            resultData.setMicroscopyGroups(microscopyGroups);

            // Note: The actual test result values in microscopyResults map (testId -> dictionaryId)
            // should be saved to the standard result table via the Result service.
            // This is handled separately from the bacteriology workflow.
        }

        // Create culture group if culture result is provided
        if (form.getCultureResult() != null && !form.getCultureResult().isEmpty()) {
            BacteriologyResultGroup cultureGroup = new BacteriologyResultGroup();
            cultureGroup.setAnalysisId(form.getAnalysisId());
            cultureGroup.setGroupType("CULTURE");
            cultureGroup.setDisplayOrder(1);
            cultureGroup.setIsActive(true);
            cultureGroup.setCreatedDate(now);
            resultData.setCultureGroup(cultureGroup);
        }

        // Convert organism beans to workflow data
        List<OrganismWithAntibiogram> organisms = form.getOrganisms().stream().map(organismBean -> {
            OrganismWithAntibiogram organismData = new OrganismWithAntibiogram();

            // Create organism group
            BacteriologyResultGroup organismGroup = new BacteriologyResultGroup();
            organismGroup.setAnalysisId(form.getAnalysisId());
            organismGroup.setGroupType("ORGANISM");
            organismGroup.setGroupNumber(organismBean.getOrganismNumber());
            organismGroup.setDisplayOrder(organismBean.getOrganismNumber());
            organismGroup.setIsActive(true);
            organismData.setOrganismGroup(organismGroup);

            // Set organism
            BacteriologyOrganism organism = convertBeanToOrganism(organismBean);
            organismData.setOrganism(organism);

            // Set antibiograms
            List<BacteriologyAntibiogram> antibiograms = organismBean.getAntibiograms().stream()
                    .map(this::convertBeanToAntibiogram).collect(Collectors.toList());
            organismData.setAntibiograms(antibiograms);

            return organismData;
        }).collect(Collectors.toList());

        resultData.setOrganisms(organisms);

        return resultData;
    }

    private BacteriologyOrganism convertBeanToOrganism(BacteriologyOrganismBean bean) {
        BacteriologyOrganism organism = new BacteriologyOrganism();
        organism.setId(bean.getId());
        organism.setOrganismNumber(bean.getOrganismNumber());
        organism.setOrganismType(bean.getOrganismType());
        organism.setOrganismNameDictId(bean.getOrganismNameDictId());
        organism.setOrganismNameText(bean.getOrganismNameText());
        organism.setGramType(bean.getGramType());
        organism.setGroupingMode(bean.getGroupingMode());
        organism.setCapsulePresence(bean.getCapsulePresence());
        organism.setOtherCharacteristics(bean.getOtherCharacteristics());
        organism.setIsActive(true);
        return organism;
    }

    private BacteriologyAntibiogram convertBeanToAntibiogram(AntibiogramResultBean bean) {
        BacteriologyAntibiogram antibiogram = new BacteriologyAntibiogram();
        antibiogram.setId(bean.getId());
        antibiogram.setAntibioticDictId(bean.getAntibioticDictId());
        antibiogram.setResult(bean.getResult());
        antibiogram.setDiameterMm(bean.getDiameterMm());
        antibiogram.setMicValue(bean.getMicValue());
        antibiogram.setInterpretationComment(bean.getInterpretationComment());
        antibiogram.setIsActive(true);
        return antibiogram;
    }
}

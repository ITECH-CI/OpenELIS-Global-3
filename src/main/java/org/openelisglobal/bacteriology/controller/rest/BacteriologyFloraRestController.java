/**
 * The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.mozilla.org/MPL/
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>The Original Code is OpenELIS code.
 *
 * <p>Copyright (C) ITECH-CI. All Rights Reserved.
 */
package org.openelisglobal.bacteriology.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.bacteriology.controller.rest.dto.FloraDataDTO;
import org.openelisglobal.bacteriology.controller.rest.dto.FloraDetailDTO;
import org.openelisglobal.bacteriology.service.BacteriologyFloraService;
import org.openelisglobal.bacteriology.valueholder.BacteriologyFlora;
import org.openelisglobal.bacteriology.valueholder.BacteriologyFloraDetail;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ControllerUtills;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * BacteriologyFloraRestController - REST API for managing bacterial flora data
 */
@RestController
@RequestMapping("/rest/bacteriology/flora")
public class BacteriologyFloraRestController extends ControllerUtills {

    @Autowired
    private BacteriologyFloraService bacteriologyFloraService;

    @Autowired
    private org.openelisglobal.analysis.service.AnalysisService analysisService;

    /**
     * Get all flora data for a specific analysis
     *
     * @param analysisId The analysis ID
     * @return List of flora data DTOs
     */
    @GetMapping("/analysis/{analysisId}")
    public ResponseEntity<List<FloraDataDTO>> getFloraByAnalysisId(@PathVariable Integer analysisId) {
        try {
            // Return flora data for ALL analyses of the same sample item, not only
            // the one matching analysisId. The flora-count test (e.g. id 557) is
            // saved against its own analysisId; the page caller may pass any analysis
            // of the sample (e.g. the primary one), so widen the lookup to the sample
            // item so callers find every saved flora row consistently.
            List<BacteriologyFlora> floraList = new ArrayList<>();
            org.openelisglobal.analysis.valueholder.Analysis primary = analysisService
                    .get(String.valueOf(analysisId));
            if (primary != null && primary.getSampleItem() != null) {
                List<org.openelisglobal.analysis.valueholder.Analysis> siblings = analysisService
                        .getAnalysesBySampleItem(primary.getSampleItem());
                for (org.openelisglobal.analysis.valueholder.Analysis a : siblings) {
                    try {
                        floraList.addAll(bacteriologyFloraService.getByAnalysisId(Integer.parseInt(a.getId())));
                    } catch (NumberFormatException ignored) {
                        // skip non-numeric analysis ids
                    }
                }
            } else {
                floraList.addAll(bacteriologyFloraService.getByAnalysisId(analysisId));
            }
            List<FloraDataDTO> floraDTOs = new ArrayList<>();
            for (BacteriologyFlora flora : floraList) {
                floraDTOs.add(convertToDTO(flora));
            }

            return ResponseEntity.ok(floraDTOs);
        } catch (Exception e) {
            LogEvent.logError("BacteriologyFloraRestController", "getFloraByAnalysisId", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get flora data for a specific test within an analysis
     *
     * @param analysisId The analysis ID
     * @param testId     The flora count test ID
     * @return Flora data DTO
     */
    @GetMapping("/analysis/{analysisId}/test/{testId}")
    public ResponseEntity<FloraDataDTO> getFloraByAnalysisIdAndTestId(@PathVariable Integer analysisId,
            @PathVariable Integer testId) {
        try {
            BacteriologyFlora flora = bacteriologyFloraService.getByAnalysisIdAndTestId(analysisId, testId);

            if (flora == null) {
                return ResponseEntity.notFound().build();
            }

            FloraDataDTO floraDTO = convertToDTO(flora);
            return ResponseEntity.ok(floraDTO);
        } catch (Exception e) {
            LogEvent.logError("BacteriologyFloraRestController", "getFloraByAnalysisIdAndTestId", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Save or update flora data for a specific analysis and test
     *
     * @param analysisId   The analysis ID
     * @param testId       The flora count test ID
     * @param floraDataDTO The flora data to save
     * @param request      HTTP request
     * @return Saved flora data DTO
     */
    @PostMapping("/analysis/{analysisId}/test/{testId}")
    public ResponseEntity<FloraDataDTO> saveFlora(@PathVariable Integer analysisId, @PathVariable Integer testId,
            @RequestBody FloraDataDTO floraDataDTO, HttpServletRequest request) {
        try {
            // Get sys user ID from session
            String sysUserId = getSysUserId(request);

            // Check if flora already exists
            BacteriologyFlora flora = bacteriologyFloraService.getByAnalysisIdAndTestId(analysisId, testId);

            if (flora == null) {
                // Create new flora record
                flora = new BacteriologyFlora();
                flora.setAnalysisId(analysisId);
                flora.setFloraCountTestId(testId);
                flora.setSysUserId(sysUserId);
            } else {
                // Update existing flora record
                flora.setSysUserId(sysUserId);
                // Clear existing details
                flora.getDetails().clear();
            }

            // Set flora count - convert Integer to String
            flora.setFloraCount(floraDataDTO.getCount() != null ? floraDataDTO.getCount().toString() : "0");

            // Add flora details
            if (floraDataDTO.getDetails() != null) {
                for (FloraDetailDTO detailDTO : floraDataDTO.getDetails()) {
                    BacteriologyFloraDetail detail = new BacteriologyFloraDetail();
                    detail.setFloraNumber(detailDTO.getFloraNumber());
                    detail.setGramTypeDictId(detailDTO.getGramTypeDictId());
                    detail.setGroupingModeDictId(detailDTO.getGroupingModeDictId());
                    detail.setOtherCharacteristicDictId(detailDTO.getOtherCharacteristicDictId());
                    detail.setSysUserId(sysUserId);

                    flora.addDetail(detail);
                }
            }

            // Save flora
            BacteriologyFlora savedFlora = bacteriologyFloraService.save(flora);

            // Convert back to DTO
            FloraDataDTO resultDTO = convertToDTO(savedFlora);

            return ResponseEntity.ok(resultDTO);
        } catch (Exception e) {
            LogEvent.logError("BacteriologyFloraRestController", "saveFlora", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Delete all flora data for a specific analysis
     *
     * @param analysisId The analysis ID
     * @return Response indicating success or failure
     */
    @DeleteMapping("/analysis/{analysisId}")
    public ResponseEntity<Void> deleteFloraByAnalysisId(@PathVariable Integer analysisId) {
        try {
            bacteriologyFloraService.deleteByAnalysisId(analysisId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            LogEvent.logError("BacteriologyFloraRestController", "deleteFloraByAnalysisId", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Delete flora data for a specific test within an analysis
     *
     * @param analysisId The analysis ID
     * @param testId     The flora count test ID
     * @return Response indicating success or failure
     */
    @DeleteMapping("/analysis/{analysisId}/test/{testId}")
    public ResponseEntity<Void> deleteFloraByAnalysisIdAndTestId(@PathVariable Integer analysisId,
            @PathVariable Integer testId) {
        try {
            bacteriologyFloraService.deleteByAnalysisIdAndTestId(analysisId, testId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            LogEvent.logError("BacteriologyFloraRestController", "deleteFloraByAnalysisIdAndTestId", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Convert BacteriologyFlora entity to FloraDataDTO
     *
     * @param flora The flora entity
     * @return Flora data DTO
     */
    private FloraDataDTO convertToDTO(BacteriologyFlora flora) {
        List<FloraDetailDTO> detailDTOs = new ArrayList<>();

        if (flora.getDetails() != null) {
            for (BacteriologyFloraDetail detail : flora.getDetails()) {
                FloraDetailDTO detailDTO = new FloraDetailDTO(detail.getFloraNumber(), detail.getGramTypeDictId(),
                        detail.getGroupingModeDictId(), detail.getOtherCharacteristicDictId());
                detailDTOs.add(detailDTO);
            }
        }

        // Convert String floraCount to Integer
        Integer count = null;
        if (flora.getFloraCount() != null) {
            try {
                count = Integer.parseInt(flora.getFloraCount());
            } catch (NumberFormatException e) {
                count = 0;
            }
        }

        return new FloraDataDTO(flora.getFloraCountTestId(), count, detailDTOs);
    }
}

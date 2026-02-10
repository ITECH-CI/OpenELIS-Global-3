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
package org.openelisglobal.bacteriology.service;

import java.util.List;
import org.openelisglobal.bacteriology.valueholder.BacteriologyFlora;

/**
 * Service interface for managing bacterial flora data
 */
public interface BacteriologyFloraService {

    /**
     * Save or update flora data
     * 
     * @param flora The flora to save
     * @return The saved flora
     */
    BacteriologyFlora save(BacteriologyFlora flora);

    /**
     * Get flora by ID
     * 
     * @param id The flora ID
     * @return The flora or null if not found
     */
    BacteriologyFlora get(Long id);

    /**
     * Get all flora for a specific analysis
     * 
     * @param analysisId The analysis ID
     * @return List of flora records
     */
    List<BacteriologyFlora> getByAnalysisId(Integer analysisId);

    /**
     * Get flora for a specific test within an analysis
     * 
     * @param analysisId The analysis ID
     * @param testId     The flora count test ID
     * @return The flora or null if not found
     */
    BacteriologyFlora getByAnalysisIdAndTestId(Integer analysisId, Integer testId);

    /**
     * Delete flora by ID
     * 
     * @param id The flora ID
     */
    void delete(Long id);

    /**
     * Delete all flora for a specific analysis
     * 
     * @param analysisId The analysis ID
     */
    void deleteByAnalysisId(Integer analysisId);

    /**
     * Delete flora for a specific test within an analysis
     * 
     * @param analysisId The analysis ID
     * @param testId     The flora count test ID
     */
    void deleteByAnalysisIdAndTestId(Integer analysisId, Integer testId);
}

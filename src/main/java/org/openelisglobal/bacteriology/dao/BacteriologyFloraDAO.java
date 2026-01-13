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
package org.openelisglobal.bacteriology.dao;

import java.util.List;
import org.openelisglobal.bacteriology.valueholder.BacteriologyFlora;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;

/**
 * BacteriologyFloraDAO - Data Access Object for bacterial flora information
 */
public interface BacteriologyFloraDAO extends BaseDAO<BacteriologyFlora, Long> {

    /**
     * Get all flora records for a specific analysis
     *
     * @param analysisId The analysis ID
     * @return List of flora records
     * @throws LIMSRuntimeException
     */
    List<BacteriologyFlora> getByAnalysisId(Integer analysisId) throws LIMSRuntimeException;

    /**
     * Get flora record for a specific test within an analysis
     *
     * @param analysisId The analysis ID
     * @param floraCountTestId The flora count test ID
     * @return Flora record or null if not found
     * @throws LIMSRuntimeException
     */
    BacteriologyFlora getByAnalysisIdAndTestId(Integer analysisId, Integer floraCountTestId)
            throws LIMSRuntimeException;

    /**
     * Delete all flora records for a specific analysis
     *
     * @param analysisId The analysis ID
     * @throws LIMSRuntimeException
     */
    void deleteByAnalysisId(Integer analysisId) throws LIMSRuntimeException;

    /**
     * Delete flora record for a specific test within an analysis
     *
     * @param analysisId The analysis ID
     * @param floraCountTestId The flora count test ID
     * @throws LIMSRuntimeException
     */
    void deleteByAnalysisIdAndTestId(Integer analysisId, Integer floraCountTestId)
            throws LIMSRuntimeException;
}

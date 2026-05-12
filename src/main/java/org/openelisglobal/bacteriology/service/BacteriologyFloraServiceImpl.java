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
import org.openelisglobal.bacteriology.dao.BacteriologyFloraDAO;
import org.openelisglobal.bacteriology.valueholder.BacteriologyFlora;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for managing bacterial flora data
 */
@Service
public class BacteriologyFloraServiceImpl implements BacteriologyFloraService {

    @Autowired
    private BacteriologyFloraDAO bacteriologyFloraDAO;

    @Override
    @Transactional
    public BacteriologyFlora save(BacteriologyFlora flora) {
        // Do NOT touch lastupdated manually: BaseObject maps it with @Version, so any
        // manual write here breaks Hibernate's optimistic-lock check on the next save
        // ("Row was updated or deleted by another transaction").

        // Insert or update based on whether ID exists
        if (flora.getId() == null) {
            bacteriologyFloraDAO.insert(flora);
        } else {
            flora = bacteriologyFloraDAO.update(flora);
        }
        return flora;
    }

    @Override
    @Transactional(readOnly = true)
    public BacteriologyFlora get(Long id) {
        return bacteriologyFloraDAO.get(id).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BacteriologyFlora> getByAnalysisId(Integer analysisId) {
        return bacteriologyFloraDAO.getByAnalysisId(analysisId);
    }

    @Override
    @Transactional(readOnly = true)
    public BacteriologyFlora getByAnalysisIdAndTestId(Integer analysisId, Integer testId) {
        return bacteriologyFloraDAO.getByAnalysisIdAndTestId(analysisId, testId);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        bacteriologyFloraDAO.get(id).ifPresent(flora -> bacteriologyFloraDAO.delete(flora));
    }

    @Override
    @Transactional
    public void deleteByAnalysisId(Integer analysisId) {
        bacteriologyFloraDAO.deleteByAnalysisId(analysisId);
    }

    @Override
    @Transactional
    public void deleteByAnalysisIdAndTestId(Integer analysisId, Integer testId) {
        bacteriologyFloraDAO.deleteByAnalysisIdAndTestId(analysisId, testId);
    }
}

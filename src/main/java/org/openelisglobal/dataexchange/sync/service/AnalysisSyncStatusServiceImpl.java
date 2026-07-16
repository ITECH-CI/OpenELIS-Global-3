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
package org.openelisglobal.dataexchange.sync.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectServiceImpl;
import org.openelisglobal.dataexchange.sync.dao.AnalysisSyncStatusDAO;
import org.openelisglobal.dataexchange.sync.valueholder.AnalysisSyncStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalysisSyncStatusServiceImpl extends BaseObjectServiceImpl<AnalysisSyncStatus, String>
        implements AnalysisSyncStatusService {

    @Autowired
    protected AnalysisSyncStatusDAO baseObjectDAO;

    AnalysisSyncStatusServiceImpl() {
        super(AnalysisSyncStatus.class);
    }

    @Override
    protected AnalysisSyncStatusDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnalysisSyncStatus> getBatchToSync(int batchSize) {
        return baseObjectDAO.getBatchToSync(batchSize);
    }

    @Override
    @Transactional
    public int markInProgress(List<String> ids) {
        return baseObjectDAO.markInProgress(ids);
    }

    @Override
    @Transactional
    public int markUpToDate(List<String> ids) {
        return baseObjectDAO.markUpToDate(ids);
    }

    @Override
    @Transactional
    public int markToUpdate(List<String> ids) {
        return baseObjectDAO.markToUpdate(ids);
    }

    @Override
    @Transactional
    public int resetStaleInProgress(int timeoutMinutes) {
        return baseObjectDAO.resetStaleInProgress(timeoutMinutes);
    }
}

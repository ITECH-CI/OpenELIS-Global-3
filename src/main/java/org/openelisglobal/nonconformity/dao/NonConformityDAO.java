package org.openelisglobal.nonconformity.dao;

import java.sql.Date;
import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.nonconformity.valueholder.NonConformity;

public interface NonConformityDAO extends BaseDAO<NonConformity, String> {

    List<NonConformity> getAllNonConformities() throws LIMSRuntimeException;

    NonConformity getNonConformityByNumber(String ncNumber) throws LIMSRuntimeException;

    List<NonConformity> getNonConformitiesByLabNumber(String labNumber) throws LIMSRuntimeException;

    List<NonConformity> getNonConformitiesByDateRange(Date startDate, Date endDate) throws LIMSRuntimeException;

    List<NonConformity> getNonConformitiesByStatus(String status) throws LIMSRuntimeException;

    List<NonConformity> searchNonConformities(String siteProvenance, String sampleType, String rejectionReason,
            Date startDate, Date endDate, String status) throws LIMSRuntimeException;
}

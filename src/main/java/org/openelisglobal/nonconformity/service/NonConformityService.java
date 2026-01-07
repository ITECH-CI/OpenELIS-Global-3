package org.openelisglobal.nonconformity.service;

import java.sql.Date;
import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.nonconformity.valueholder.NonConformity;

public interface NonConformityService extends BaseObjectService<NonConformity, String> {

    List<NonConformity> getAllNonConformities();

    NonConformity getNonConformityByNumber(String ncNumber);

    List<NonConformity> getNonConformitiesByLabNumber(String labNumber);

    List<NonConformity> getNonConformitiesByDateRange(Date startDate, Date endDate);

    List<NonConformity> getNonConformitiesByStatus(String status);

    List<NonConformity> searchNonConformities(String siteProvenance, String sampleType, String rejectionReason,
            Date startDate, Date endDate, String status);

    String generateNextNcNumber();

}

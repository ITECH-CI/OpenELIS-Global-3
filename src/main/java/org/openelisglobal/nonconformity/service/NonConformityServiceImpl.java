package org.openelisglobal.nonconformity.service;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.List;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.nonconformity.dao.NonConformityDAO;
import org.openelisglobal.nonconformity.valueholder.NonConformity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NonConformityServiceImpl extends AuditableBaseObjectServiceImpl<NonConformity, String>
        implements NonConformityService {

    @Autowired
    protected NonConformityDAO baseObjectDAO;

    NonConformityServiceImpl() {
        super(NonConformity.class);
    }

    @Override
    protected NonConformityDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<NonConformity> getAllNonConformities() {
        return baseObjectDAO.getAllNonConformities();
    }

    @Override
    @Transactional(readOnly = true)
    public NonConformity getNonConformityByNumber(String ncNumber) {
        // implement NonConformity by its unique ncNumber
        return baseObjectDAO.getNonConformityByNumber(ncNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NonConformity> getNonConformitiesByLabNumber(String labNumber) {
        return baseObjectDAO.getNonConformitiesByLabNumber(labNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NonConformity> getNonConformitiesByDateRange(Date startDate, Date endDate) {
        return baseObjectDAO.getNonConformitiesByDateRange(startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NonConformity> getNonConformitiesByStatus(String status) {
        return baseObjectDAO.getNonConformitiesByStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NonConformity> searchNonConformities(String siteProvenance, String sampleType, String rejectionReason,
            Date startDate, Date endDate, String status) {
        return baseObjectDAO.searchNonConformities(siteProvenance, sampleType, rejectionReason, startDate, endDate,
                status);
    }

    @Override
    @Transactional
    public String generateNextNcNumber() {
        List<NonConformity> allNonConformities = getAllNonConformities();

        SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy");
        String currentYear = yearFormat.format(new java.util.Date());

        int maxNumber = 0;
        String prefix = "NC-" + currentYear + "-";

        for (NonConformity nc : allNonConformities) {
            String ncNumber = nc.getNcNumber();
            if (ncNumber != null && ncNumber.startsWith(prefix)) {
                try {
                    String numberPart = ncNumber.substring(prefix.length());
                    int number = Integer.parseInt(numberPart);
                    if (number > maxNumber) {
                        maxNumber = number;
                    }
                } catch (NumberFormatException e) {
                }
            }
        }

        int nextNumber = maxNumber + 1;
        return prefix + String.format("%04d", nextNumber);
    }

}

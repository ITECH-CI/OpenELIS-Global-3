package org.openelisglobal.nonconformity.daoimpl;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.nonconformity.dao.NonConformityDAO;
import org.openelisglobal.nonconformity.valueholder.NonConformity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class NonConformityDAOImpl extends BaseDAOImpl<NonConformity, String> implements NonConformityDAO {

    public NonConformityDAOImpl() {
        super(NonConformity.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NonConformity> getAllNonConformities() throws LIMSRuntimeException {
        try {
            // String sql = "select from NonConformity order by reportDate DESC, id DESC";
            String sql = "from NonConformity n order by n.reportDate desc, n.id desc";

            Query query = entityManager.unwrap(Session.class).createQuery(sql);
            @SuppressWarnings("unchecked")
            List<NonConformity> list = query.getResultList();
            return list;
        } catch (RuntimeException e) {
            handleException(e, "getAllNonConformities");
            return new ArrayList<>();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public NonConformity getNonConformityByNumber(String ncNumber) throws LIMSRuntimeException {
        try {
            String sql = "FROM NonConformity WHERE ncNumber = :ncNumber";
            Query query = entityManager.unwrap(Session.class).createQuery(sql);
            query.setParameter("ncNumber", ncNumber);
            @SuppressWarnings("unchecked")
            List<NonConformity> list = query.getResultList();
            return list.isEmpty() ? null : list.get(0);
        } catch (RuntimeException e) {
            handleException(e, "getNonConformityByNumber");
            return null;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<NonConformity> getNonConformitiesByLabNumber(String labNumber) throws LIMSRuntimeException {
        try {
            String sql = "FROM NonConformity WHERE labNumber = :labNumber ORDER BY reportDate DESC";
            Query query = entityManager.unwrap(Session.class).createQuery(sql);
            query.setParameter("labNumber", labNumber);
            @SuppressWarnings("unchecked")
            List<NonConformity> list = query.getResultList();
            return list;
        } catch (RuntimeException e) {
            handleException(e, "getNonConformitiesByLabNumber");
            return new ArrayList<>();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<NonConformity> getNonConformitiesByDateRange(Date startDate, Date endDate) throws LIMSRuntimeException {
        try {
            String sql = "FROM NonConformity WHERE reportDate BETWEEN :startDate AND :endDate "
                    + "ORDER BY reportDate DESC";
            Query query = entityManager.unwrap(Session.class).createQuery(sql);
            query.setParameter("startDate", startDate);
            query.setParameter("endDate", endDate);
            @SuppressWarnings("unchecked")
            List<NonConformity> list = query.getResultList();
            return list;
        } catch (RuntimeException e) {
            handleException(e, "getNonConformitiesByDateRange");
            return new ArrayList<>();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<NonConformity> getNonConformitiesByStatus(String status) throws LIMSRuntimeException {
        try {
            String sql = "FROM NonConformity WHERE status = :status ORDER BY reportDate DESC";
            Query query = entityManager.unwrap(Session.class).createQuery(sql);
            query.setParameter("status", status);
            @SuppressWarnings("unchecked")
            List<NonConformity> list = query.getResultList();
            return list;
        } catch (RuntimeException e) {
            handleException(e, "getNonConformitiesByStatus");
            return new ArrayList<>();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<NonConformity> searchNonConformities(String siteProvenance, String sampleType, String rejectionReason,
            Date startDate, Date endDate, String status) throws LIMSRuntimeException {
        try {
            StringBuilder sql = new StringBuilder("FROM NonConformity WHERE 1=1");

            if (siteProvenance != null && !siteProvenance.isEmpty()) {
                sql.append(" AND siteProvenance LIKE :siteProvenance");
            }
            if (sampleType != null && !sampleType.isEmpty()) {
                sql.append(" AND sampleType LIKE :sampleType");
            }
            if (rejectionReason != null && !rejectionReason.isEmpty()) {
                sql.append(" AND rejectionReason LIKE :rejectionReason");
            }
            if (startDate != null && endDate != null) {
                sql.append(" AND reportDate BETWEEN :startDate AND :endDate");
            }
            if (status != null && !status.isEmpty()) {
                sql.append(" AND status = :status");
            }

            sql.append(" ORDER BY reportDate DESC");

            Query query = entityManager.unwrap(Session.class).createQuery(sql.toString());

            if (siteProvenance != null && !siteProvenance.isEmpty()) {
                query.setParameter("siteProvenance", "%" + siteProvenance + "%");
            }
            if (sampleType != null && !sampleType.isEmpty()) {
                query.setParameter("sampleType", "%" + sampleType + "%");
            }
            if (rejectionReason != null && !rejectionReason.isEmpty()) {
                query.setParameter("rejectionReason", "%" + rejectionReason + "%");
            }
            if (startDate != null && endDate != null) {
                query.setParameter("startDate", startDate);
                query.setParameter("endDate", endDate);
            }
            if (status != null && !status.isEmpty()) {
                query.setParameter("status", status);
            }

            @SuppressWarnings("unchecked")
            List<NonConformity> list = query.getResultList();
            return list;
        } catch (RuntimeException e) {
            handleException(e, "searchNonConformities");
            return new ArrayList<>();
        }
    }

    private void handleException(RuntimeException e, String methodName) {
        throw new LIMSRuntimeException("Error in " + methodName, e);
    }
}

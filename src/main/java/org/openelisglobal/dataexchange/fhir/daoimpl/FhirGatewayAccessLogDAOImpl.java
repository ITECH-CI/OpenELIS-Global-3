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
package org.openelisglobal.dataexchange.fhir.daoimpl;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.dataexchange.fhir.dao.FhirGatewayAccessLogDAO;
import org.openelisglobal.dataexchange.fhir.valueholder.FhirGatewayAccessLog;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class FhirGatewayAccessLogDAOImpl extends BaseDAOImpl<FhirGatewayAccessLog, String>
        implements FhirGatewayAccessLogDAO {

    public FhirGatewayAccessLogDAOImpl() {
        super(FhirGatewayAccessLog.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FhirGatewayAccessLog> getRecent(int max) {
        try {
            String hql = "from FhirGatewayAccessLog l order by l.accessedAt desc";
            Query<FhirGatewayAccessLog> query = entityManager.unwrap(Session.class).createQuery(hql,
                    FhirGatewayAccessLog.class);
            query.setMaxResults(max);
            return query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in FhirGatewayAccessLog getRecent()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<FhirGatewayAccessLog> getRecentForClient(String clientId, int max) {
        try {
            String hql = "from FhirGatewayAccessLog l where l.clientId = :clientId order by l.accessedAt desc";
            Query<FhirGatewayAccessLog> query = entityManager.unwrap(Session.class).createQuery(hql,
                    FhirGatewayAccessLog.class);
            query.setParameter("clientId", clientId);
            query.setMaxResults(max);
            return query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in FhirGatewayAccessLog getRecentForClient()", e);
        }
    }
}

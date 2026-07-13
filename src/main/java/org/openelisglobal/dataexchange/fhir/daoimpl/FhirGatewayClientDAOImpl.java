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
import org.openelisglobal.dataexchange.fhir.dao.FhirGatewayClientDAO;
import org.openelisglobal.dataexchange.fhir.valueholder.FhirGatewayClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class FhirGatewayClientDAOImpl extends BaseDAOImpl<FhirGatewayClient, String> implements FhirGatewayClientDAO {

    public FhirGatewayClientDAOImpl() {
        super(FhirGatewayClient.class);
    }

    @Override
    @Transactional(readOnly = true)
    public FhirGatewayClient findByName(String name) {
        try {
            String hql = "from FhirGatewayClient c where c.name = :name";
            Query<FhirGatewayClient> query = entityManager.unwrap(Session.class).createQuery(hql,
                    FhirGatewayClient.class);
            query.setParameter("name", name);
            query.setMaxResults(1);
            List<FhirGatewayClient> list = query.list();
            return list.isEmpty() ? null : list.get(0);
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in FhirGatewayClient findByName()", e);
        }
    }
}

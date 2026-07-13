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
import org.openelisglobal.dataexchange.fhir.dao.FhirGatewayTokenDAO;
import org.openelisglobal.dataexchange.fhir.valueholder.FhirGatewayToken;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class FhirGatewayTokenDAOImpl extends BaseDAOImpl<FhirGatewayToken, String> implements FhirGatewayTokenDAO {

    public FhirGatewayTokenDAOImpl() {
        super(FhirGatewayToken.class);
    }

    @Override
    @Transactional(readOnly = true)
    public FhirGatewayToken findActiveByTokenHash(String tokenHash) {
        try {
            String hql = "from FhirGatewayToken t where t.tokenHash = :hash and t.isActive = 'Y'";
            Query<FhirGatewayToken> query = entityManager.unwrap(Session.class).createQuery(hql,
                    FhirGatewayToken.class);
            query.setParameter("hash", tokenHash);
            query.setMaxResults(1);
            List<FhirGatewayToken> list = query.list();
            return list.isEmpty() ? null : list.get(0);
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in FhirGatewayToken findActiveByTokenHash()", e);
        }
    }
}

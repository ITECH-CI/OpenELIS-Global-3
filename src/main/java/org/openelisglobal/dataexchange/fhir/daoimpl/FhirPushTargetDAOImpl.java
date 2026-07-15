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
import org.openelisglobal.dataexchange.fhir.dao.FhirPushTargetDAO;
import org.openelisglobal.dataexchange.fhir.valueholder.FhirPushTarget;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class FhirPushTargetDAOImpl extends BaseDAOImpl<FhirPushTarget, String> implements FhirPushTargetDAO {

    public FhirPushTargetDAOImpl() {
        super(FhirPushTarget.class);
    }

    @Override
    @Transactional(readOnly = true)
    public FhirPushTarget findByName(String name) {
        return findBySingleProperty("name", name);
    }

    @Override
    @Transactional(readOnly = true)
    public FhirPushTarget findByEndpoint(String endpoint) {
        return findBySingleProperty("endpoint", endpoint);
    }

    private FhirPushTarget findBySingleProperty(String property, String value) {
        try {
            String hql = "from FhirPushTarget t where t." + property + " = :value";
            Query<FhirPushTarget> query = entityManager.unwrap(Session.class).createQuery(hql, FhirPushTarget.class);
            query.setParameter("value", value);
            query.setMaxResults(1);
            List<FhirPushTarget> list = query.list();
            return list.isEmpty() ? null : list.get(0);
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in FhirPushTarget findBy" + property + "()", e);
        }
    }
}

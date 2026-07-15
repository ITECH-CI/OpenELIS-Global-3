package org.openelisglobal.program.dao;

import java.util.UUID;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.program.valueholder.Program;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class ProgramDAOImpl extends BaseDAOImpl<Program, String> implements ProgramDAO {
    ProgramDAOImpl() {
        super(Program.class);
    }

    @Override
    public Program getProgramByQuestionnaireUuid(UUID questionnaireUuid) {
        if (questionnaireUuid == null) {
            return null;
        }
        String sql = "from Program p where p.questionnaireUUID = :questionnaireUuid";
        Query<Program> query = entityManager.unwrap(Session.class).createQuery(sql, Program.class);
        query.setParameter("questionnaireUuid", questionnaireUuid);
        query.setMaxResults(1);
        return query.uniqueResult();
    }
}

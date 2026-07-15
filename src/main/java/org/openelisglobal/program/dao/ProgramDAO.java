package org.openelisglobal.program.dao;

import java.util.UUID;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.program.valueholder.Program;

public interface ProgramDAO extends BaseDAO<Program, String> {

    /**
     * Retrouve le programme dont le Questionnaire FHIR porte l'UUID donné, ou
     * {@code null} si aucun. Utilisé pour associer un ordre reçu (dont le
     * QuestionnaireResponse référence un Questionnaire) au bon programme OE.
     */
    Program getProgramByQuestionnaireUuid(UUID questionnaireUuid);
}

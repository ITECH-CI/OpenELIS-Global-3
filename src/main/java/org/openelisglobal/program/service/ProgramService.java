package org.openelisglobal.program.service;

import java.util.UUID;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.program.valueholder.Program;

public interface ProgramService extends BaseObjectService<Program, String> {

    /**
     * Retrouve le programme dont le Questionnaire FHIR porte l'UUID donné, ou
     * {@code null} si aucun.
     */
    Program getProgramByQuestionnaireUuid(UUID questionnaireUuid);
}

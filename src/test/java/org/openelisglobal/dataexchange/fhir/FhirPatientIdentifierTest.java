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
package org.openelisglobal.dataexchange.fhir;

import static org.junit.Assert.assertEquals;

import org.hl7.fhir.r4.model.Identifier;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Couvre l'exposition FHIR de l'identifiant patient CMU/CNAM avec l'OID
 * national (interop PSNDPE) : l'identifiant CMU doit sortir en
 * {@code use=OFFICIAL} sur le bon system, condition du rapprochement
 * inter-systèmes {@code Patient?identifier=urn:oid:...|<matricule>}.
 */
public class FhirPatientIdentifierTest extends BaseWebContextSensitiveTest {

    @Autowired
    private FhirTransformService fhirTransformService;

    @Autowired
    private FhirConfig fhirConfig;

    @Test
    public void createIdentifier_cmuSystem_shouldBeOfficial() {
        String cmuSystem = fhirConfig.getCmuIdentifierSystem();
        Identifier identifier = fhirTransformService.createIdentifier(cmuSystem, "90909000000");

        assertEquals(cmuSystem, identifier.getSystem());
        assertEquals("90909000000", identifier.getValue());
        assertEquals(Identifier.IdentifierUse.OFFICIAL, identifier.getUse());
    }

    @Test
    public void createIdentifier_nationalIdSystem_shouldBeOfficial() {
        String nationalIdSystem = fhirConfig.getOeFhirSystem() + "/pat_nationalId";
        Identifier identifier = fhirTransformService.createIdentifier(nationalIdSystem, "1234");

        assertEquals(Identifier.IdentifierUse.OFFICIAL, identifier.getUse());
    }

    @Test
    public void createIdentifier_internalSystem_shouldBeUsual() {
        String subjectSystem = fhirConfig.getOeFhirSystem() + "/pat_subjectNumber";
        Identifier identifier = fhirTransformService.createIdentifier(subjectSystem, "P25-001");

        assertEquals(Identifier.IdentifierUse.USUAL, identifier.getUse());
    }

    /**
     * L'OID national CMU par défaut doit être celui attendu par l'interop PSNDPE.
     */
    @Test
    public void cmuIdentifierSystem_defaultShouldBeNationalOid() {
        assertEquals("urn:oid:1.3.6.1.4.1.53864.1.3", fhirConfig.getCmuIdentifierSystem());
    }
}

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
 * <p>Copyright (C) The Minnesota Department of Health. All Rights Reserved.
 *
 * <p>Contributor(s): CIRG, University of Washington, Seattle WA.
 */
package org.openelisglobal.dataexchange.fhir;

/**
 * Constantes partagées pour le suivi de synchronisation FHIR (FhirSyncStatus).
 */
public final class FhirSyncConstants {

    private FhirSyncConstants() {
    }

    // Déclencheurs de transformation.
    public static final String TRIGGER_ORDER_ENTRY = "ORDER_ENTRY";
    public static final String TRIGGER_RESULTS = "RESULTS";
    public static final String TRIGGER_VALIDATION = "VALIDATION";
    public static final String TRIGGER_PATIENT = "PATIENT";
    public static final String TRIGGER_ORGANIZATION = "ORGANIZATION";

    // Types de cible métier (ce qu'on rejoue).
    public static final String TARGET_SAMPLE = "SAMPLE";
    public static final String TARGET_PATIENT = "PATIENT";
    public static final String TARGET_ORGANIZATION = "ORGANIZATION";
}

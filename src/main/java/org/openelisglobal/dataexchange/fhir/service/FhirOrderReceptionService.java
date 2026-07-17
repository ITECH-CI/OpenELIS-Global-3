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
package org.openelisglobal.dataexchange.fhir.service;

/**
 * Réception PUSH d'une demande d'analyse FHIR (mini-HIE). Un tiers POST un
 * Bundle {ServiceRequest, QuestionnaireResponse, Patient, Specimen} SANS Task ;
 * OE stocke ces ressources dans son store FHIR local et crée un ElectronicOrder
 * "en attente" (repris ensuite via l'entrée d'échantillon, où OE construit le
 * Task). Réutilise le moteur d'ordre unique (TaskWorker).
 */
public interface FhirOrderReceptionService {

    /** Résultat de la réception d'un Bundle. */
    class ReceptionResult {
        private final boolean accepted;
        private final String orderNumber;
        private final String message;

        public ReceptionResult(boolean accepted, String orderNumber, String message) {
            this.accepted = accepted;
            this.orderNumber = orderNumber;
            this.message = message;
        }

        public boolean isAccepted() {
            return accepted;
        }

        public String getOrderNumber() {
            return orderNumber;
        }

        public String getMessage() {
            return message;
        }
    }

    /**
     * Reçoit un Bundle FHIR (JSON) et crée l'ElectronicOrder correspondant.
     *
     * @param bundleJson le corps de la requête (Bundle FHIR sérialisé en JSON)
     * @return résultat (accepté / motif de rejet)
     */
    ReceptionResult receiveOrderBundle(String bundleJson);
}

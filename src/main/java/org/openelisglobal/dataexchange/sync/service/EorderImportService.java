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
package org.openelisglobal.dataexchange.sync.service;

import org.openelisglobal.dataexchange.sync.dto.EorderRequestDTO;

/**
 * Importe une demande de charge virale reçue du serveur consolidé (incrément
 * 4c) en un {@code ElectronicOrder} "Entered", via les SERVICES MÉTIER OE (pas
 * de SQL brut, contrairement à l'uploader). Le sample/analyse sont créés PLUS
 * TARD par la saisie OE (workflow CV actuel). Le JSON intégral reçu est
 * conservé dans {@code electronic_order.data} (aucun champ perdu — modèle 4a,
 * pas de table plate).
 */
public interface EorderImportService {

    /** Résultat d'un import (pour piloter l'ACK). */
    class ImportResult {
        private final boolean created;
        private final boolean alreadyPresent;
        private final String electronicOrderId;
        private final String errorMessage;

        private ImportResult(boolean created, boolean alreadyPresent, String electronicOrderId, String errorMessage) {
            this.created = created;
            this.alreadyPresent = alreadyPresent;
            this.electronicOrderId = electronicOrderId;
            this.errorMessage = errorMessage;
        }

        public static ImportResult created(String eoId) {
            return new ImportResult(true, false, eoId, null);
        }

        public static ImportResult alreadyPresent(String eoId) {
            return new ImportResult(false, true, eoId, null);
        }

        public static ImportResult failed(String error) {
            return new ImportResult(false, false, null, error);
        }

        /**
         * true si l'import a réussi OU si la demande était déjà présente (idempotence).
         */
        public boolean isSuccess() {
            return created || alreadyPresent;
        }

        public boolean isCreated() {
            return created;
        }

        public boolean isAlreadyPresent() {
            return alreadyPresent;
        }

        public String getElectronicOrderId() {
            return electronicOrderId;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    /**
     * Importe une demande. Idempotent : si un ElectronicOrder existe déjà pour ce
     * {@code requestUuid} (external_id), ne recrée rien et renvoie
     * {@link ImportResult#alreadyPresent}.
     *
     * @param request le DTO typé (champs mappés)
     * @param rawJson le JSON BRUT de la demande (stocké verbatim dans data)
     */
    ImportResult importRequest(EorderRequestDTO request, String rawJson);
}

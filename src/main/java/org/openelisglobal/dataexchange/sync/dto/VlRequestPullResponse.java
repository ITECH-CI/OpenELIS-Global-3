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
package org.openelisglobal.dataexchange.sync.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Enveloppe de réponse du pull des demandes VL (incrément 4c) :
 * {@code GET /api/v1/vl-requests}. Pagination par curseur composite
 * {@code nextCheckpoint = "<updatedAt>|<id>"} (keyset). {@code count == 0}
 * signale la fin.
 *
 * <p>
 * Les demandes sont conservées en {@link JsonNode} BRUT (pas en DTO typé) : le
 * JSON intégral de chaque demande doit être stocké tel quel dans
 * {@code electronic_order.data} pour ne RIEN perdre (les ~90 champs, dont le
 * clinique CV, cf. modèle 4a). Le mapping typé se fait par-dessus via
 * {@link EorderRequestDTO}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class VlRequestPullResponse {

    private JsonNode requests;
    private String nextCheckpoint;
    private int count;

    public JsonNode getRequests() {
        return requests;
    }

    public void setRequests(JsonNode requests) {
        this.requests = requests;
    }

    public String getNextCheckpoint() {
        return nextCheckpoint;
    }

    public void setNextCheckpoint(String nextCheckpoint) {
        this.nextCheckpoint = nextCheckpoint;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}

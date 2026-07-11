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
package org.openelisglobal.dataexchange.terminology.service;

import java.util.ArrayList;
import java.util.List;

/**
 * Rapport global d'un import terminologique (preview ou apply) : cible, compteurs
 * agrégés et détail ligne à ligne.
 */
public class TerminologyImportReport {

    private TerminologyTarget target;
    private int totalRows;
    private int updated;
    private int wouldUpdate;
    private int notFound;
    private int skipped;
    private int conflict;
    private int noChange;
    private final List<TerminologyImportLine> lines = new ArrayList<>();

    public TerminologyImportReport() {
    }

    public TerminologyImportReport(TerminologyTarget target) {
        this.target = target;
    }

    /**
     * Ajoute une ligne au rapport et incrémente le compteur agrégé correspondant.
     */
    public void addLine(TerminologyImportLine line) {
        lines.add(line);
        totalRows++;
        if (line.getAction() == null) {
            return;
        }
        switch (line.getAction()) {
        case UPDATED:
            updated++;
            break;
        case WOULD_UPDATE:
            wouldUpdate++;
            break;
        case NOT_FOUND:
            notFound++;
            break;
        case CONFLICT:
            conflict++;
            break;
        case NO_CHANGE:
            noChange++;
            break;
        case SKIPPED_PROPOSED:
        case SKIPPED_NO_CODE:
        case AMBIGUOUS:
        case ERROR:
            skipped++;
            break;
        default:
            break;
        }
    }

    public TerminologyTarget getTarget() {
        return target;
    }

    public void setTarget(TerminologyTarget target) {
        this.target = target;
    }

    public int getTotalRows() {
        return totalRows;
    }

    public int getUpdated() {
        return updated;
    }

    public int getWouldUpdate() {
        return wouldUpdate;
    }

    public int getNotFound() {
        return notFound;
    }

    public int getSkipped() {
        return skipped;
    }

    public int getConflict() {
        return conflict;
    }

    public int getNoChange() {
        return noChange;
    }

    public List<TerminologyImportLine> getLines() {
        return lines;
    }
}

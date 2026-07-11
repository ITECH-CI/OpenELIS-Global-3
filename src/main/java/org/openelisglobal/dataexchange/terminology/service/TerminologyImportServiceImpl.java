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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.dataexchange.terminology.service.TerminologyImportLine.Action;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.observationhistorytype.service.ObservationHistoryTypeService;
import org.openelisglobal.observationhistorytype.valueholder.ObservationHistoryType;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TerminologyImportServiceImpl implements TerminologyImportService {

    private static final String SEPARATOR = ";";
    private static final String STATUS_VALIDATED = "validated";

    @Autowired
    private TestService testService;

    @Autowired
    private DictionaryService dictionaryService;

    @Autowired
    private ObservationHistoryTypeService observationHistoryTypeService;

    @Override
    @Transactional(readOnly = true)
    public TerminologyImportReport preview(TerminologyTarget target, String csvContent) {
        // Dry-run : on parcourt les lignes en mode simulation (persist = false).
        return process(target, csvContent, false);
    }

    @Override
    @Transactional
    public TerminologyImportReport apply(TerminologyTarget target, String csvContent) {
        // Application réelle : persist = true.
        return process(target, csvContent, true);
    }

    /**
     * Cœur commun preview/apply : lit l'en-tête pour localiser les colonnes, puis
     * traite chaque ligne dans un try/catch isolé (une ligne en erreur n'arrête pas
     * le lot). {@code persist} distingue dry-run et application.
     */
    private TerminologyImportReport process(TerminologyTarget target, String csvContent, boolean persist) {
        TerminologyImportReport report = new TerminologyImportReport(target);
        if (target == null) {
            return report;
        }

        List<String> rows = splitLines(csvContent);
        Map<String, Integer> columns = null;

        for (String rawLine : rows) {
            // Ignore les lignes vides et les commentaires (#).
            if (rawLine == null) {
                continue;
            }
            String trimmed = rawLine.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }

            // La première ligne non-commentaire/non-vide est l'en-tête.
            if (columns == null) {
                columns = parseHeader(rawLine);
                continue;
            }

            try {
                TerminologyImportLine result = processLine(target, columns, rawLine, persist);
                if (result != null) {
                    report.addLine(result);
                }
            } catch (Exception e) {
                // Défensif : on trace, on remplit un message et on continue le lot.
                LogEvent.logWarn(this.getClass().getSimpleName(), "process",
                        "erreur de traitement d'une ligne d'import terminologique: " + e);
                report.addLine(new TerminologyImportLine(rawLine.trim(), Action.ERROR, null, null, e.getMessage()));
            }
        }
        return report;
    }

    /** Traite une ligne de données : matching par clé naturelle puis application. */
    private TerminologyImportLine processLine(TerminologyTarget target, Map<String, Integer> columns, String rawLine,
            boolean persist) {
        String[] cells = splitCells(rawLine);
        String loinc = value(cells, columns, "loinc");
        String snomed = value(cells, columns, "snomed");
        String status = value(cells, columns, "status");

        switch (target) {
        case TEST:
            return processTest(cells, columns, loinc, snomed, status, persist);
        case DICTIONARY:
            return processDictionary(cells, columns, loinc, snomed, status, persist);
        case OBSERVATION_HISTORY_TYPE:
            return processObservationHistoryType(cells, columns, loinc, snomed, status, persist);
        default:
            return null;
        }
    }

    // ------------------------------------------------------------------
    // Cibles
    // ------------------------------------------------------------------

    /** TEST : clé test_name (=description) + sample_type indicatif. */
    private TerminologyImportLine processTest(String[] cells, Map<String, Integer> columns, String loinc, String snomed,
            String status, boolean persist) {
        String testName = value(cells, columns, "test_name");
        String sampleType = value(cells, columns, "sample_type");
        String key = isBlank(sampleType) ? testName : testName + " / " + sampleType;

        // Court-circuit statut/codes avant même la recherche (comportement homogène).
        TerminologyImportLine guard = guard(key, loinc, snomed, status);
        if (guard != null) {
            return guard;
        }

        Test test = isBlank(testName) ? null : testService.getTestByDescription(testName);
        if (test == null) {
            return new TerminologyImportLine(key, Action.NOT_FOUND, loinc, snomed, "aucun test pour ce test_name");
        }

        // Application des codes (les cellules vides n'écrasent jamais).
        if (!isBlank(loinc)) {
            test.setLoinc(loinc);
        }
        if (!isBlank(snomed)) {
            test.setSnomedCode(snomed);
        }
        if (persist) {
            testService.update(test);
            return new TerminologyImportLine(key, Action.UPDATED, loinc, snomed, null);
        }
        return new TerminologyImportLine(key, Action.WOULD_UPDATE, loinc, snomed, null);
    }

    /** DICTIONARY : clé category + dict_entry. */
    private TerminologyImportLine processDictionary(String[] cells, Map<String, Integer> columns, String loinc,
            String snomed, String status, boolean persist) {
        String category = value(cells, columns, "category");
        String dictEntry = value(cells, columns, "dict_entry");
        String key = category + " / " + dictEntry;

        TerminologyImportLine guard = guard(key, loinc, snomed, status);
        if (guard != null) {
            return guard;
        }

        // Filtre la catégorie sur le dictEntry ; 0 -> NOT_FOUND, >1 -> AMBIGUOUS.
        // Comparaison normalisée sur les espaces : certains dict_entry en base
        // contiennent un saut de ligne interne (ex. "Metronidazole\n10μg-...") que le
        // CSV ne peut pas reproduire tel quel — on aligne en écrasant tout blanc
        // (newline, espaces multiples) en un espace simple des deux côtés.
        List<Dictionary> matches = new ArrayList<>();
        if (!isBlank(category) && !isBlank(dictEntry)) {
            String normalizedEntry = normalizeWhitespace(dictEntry);
            List<Dictionary> inCategory = dictionaryService.getDictionaryEntrysByCategoryNameLocalizedSort(category);
            if (inCategory != null) {
                for (Dictionary d : inCategory) {
                    if (d != null && normalizedEntry.equals(normalizeWhitespace(d.getDictEntry()))) {
                        matches.add(d);
                    }
                }
            }
        }

        if (matches.isEmpty()) {
            return new TerminologyImportLine(key, Action.NOT_FOUND, loinc, snomed,
                    "aucune entrée de dictionnaire pour category+dict_entry");
        }
        if (matches.size() > 1) {
            return new TerminologyImportLine(key, Action.AMBIGUOUS, loinc, snomed,
                    matches.size() + " entrées correspondent à category+dict_entry");
        }

        Dictionary dictionary = matches.get(0);
        if (!isBlank(loinc)) {
            dictionary.setLoincCode(loinc);
        }
        if (!isBlank(snomed)) {
            dictionary.setSnomedCode(snomed);
        }
        if (persist) {
            dictionaryService.update(dictionary);
            return new TerminologyImportLine(key, Action.UPDATED, loinc, snomed, null);
        }
        return new TerminologyImportLine(key, Action.WOULD_UPDATE, loinc, snomed, null);
    }

    /** OBSERVATION_HISTORY_TYPE : clé type_name. */
    private TerminologyImportLine processObservationHistoryType(String[] cells, Map<String, Integer> columns,
            String loinc, String snomed, String status, boolean persist) {
        String typeName = value(cells, columns, "type_name");
        String key = typeName;

        TerminologyImportLine guard = guard(key, loinc, snomed, status);
        if (guard != null) {
            return guard;
        }

        ObservationHistoryType type = isBlank(typeName) ? null : observationHistoryTypeService.getByName(typeName);
        if (type == null) {
            return new TerminologyImportLine(key, Action.NOT_FOUND, loinc, snomed,
                    "aucun type d'observation pour ce type_name");
        }

        if (!isBlank(loinc)) {
            type.setLoincCode(loinc);
        }
        if (!isBlank(snomed)) {
            type.setSnomedCode(snomed);
        }
        if (persist) {
            observationHistoryTypeService.update(type);
            return new TerminologyImportLine(key, Action.UPDATED, loinc, snomed, null);
        }
        return new TerminologyImportLine(key, Action.WOULD_UPDATE, loinc, snomed, null);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * Garde commune : ignore les lignes non validées (SKIPPED_PROPOSED) et celles
     * sans aucun code (SKIPPED_NO_CODE). Renvoie null si la ligne doit être traitée.
     */
    private TerminologyImportLine guard(String key, String loinc, String snomed, String status) {
        if (!STATUS_VALIDATED.equalsIgnoreCase(trimToEmpty(status))) {
            return new TerminologyImportLine(key, Action.SKIPPED_PROPOSED, loinc, snomed,
                    "status != validated (" + trimToEmpty(status) + ")");
        }
        if (isBlank(loinc) && isBlank(snomed)) {
            return new TerminologyImportLine(key, Action.SKIPPED_NO_CODE, loinc, snomed, "aucun code fourni");
        }
        return null;
    }

    /** Découpe le contenu en lignes (gère \r\n, \n, \r). */
    private List<String> splitLines(String csvContent) {
        List<String> lines = new ArrayList<>();
        if (csvContent == null) {
            return lines;
        }
        for (String line : csvContent.split("\r\n|\n|\r", -1)) {
            lines.add(line);
        }
        return lines;
    }

    /** Découpe une ligne sur le séparateur ';' (garde les cellules vides). */
    private String[] splitCells(String line) {
        return line.split(SEPARATOR, -1);
    }

    /** Lit l'en-tête et associe chaque nom de colonne (trim, minuscule) à son index. */
    private Map<String, Integer> parseHeader(String headerLine) {
        Map<String, Integer> columns = new HashMap<>();
        String[] cells = splitCells(headerLine);
        for (int i = 0; i < cells.length; i++) {
            String name = trimToEmpty(cells[i]).toLowerCase();
            if (!name.isEmpty() && !columns.containsKey(name)) {
                columns.put(name, i);
            }
        }
        return columns;
    }

    /**
     * Valeur trimmée d'une colonne pour une ligne donnée, robuste si la colonne est
     * absente ou si la ligne a moins de cellules que l'en-tête.
     */
    private String value(String[] cells, Map<String, Integer> columns, String columnName) {
        Integer index = columns.get(columnName);
        if (index == null || index < 0 || index >= cells.length) {
            return "";
        }
        return trimToEmpty(cells[index]);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    // Écrase toute séquence de blancs (espaces, tabulations, sauts de ligne) en un
    // espace simple, et trim. Permet de matcher un dict_entry du CSV avec sa version
    // en base qui peut contenir un saut de ligne interne.
    private String normalizeWhitespace(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }
}

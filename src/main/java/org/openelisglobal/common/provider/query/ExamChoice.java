package org.openelisglobal.common.provider.query;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.openelisglobal.common.util.XMLUtil;
import org.openelisglobal.panel.valueholder.Panel;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;

/**
 * Examen d'une demande électronique que l'opérateur place lui-même : il admet
 * plusieurs types d'échantillon et aucun prélèvement reçu ne permet de
 * trancher. Chaque option est un couple (test ou panel, type d'échantillon).
 */
public final class ExamChoice {

    public static final class Option {
        final String kind; // test | panel
        final String id;
        final String label;
        final String sampleTypeId;
        final String sampleTypeName;
        final String testIds; // panel : tests du panel pour ce type d'échantillon
        final String testNames;

        Option(String kind, String id, String label, String sampleTypeId, String sampleTypeName, String testIds,
                String testNames) {
            this.kind = kind;
            this.id = id;
            this.label = label;
            this.sampleTypeId = sampleTypeId;
            this.sampleTypeName = sampleTypeName;
            this.testIds = testIds;
            this.testNames = testNames;
        }

        public String getKind() {
            return kind;
        }

        public String getId() {
            return id;
        }

        public String getSampleTypeId() {
            return sampleTypeId;
        }
    }

    private final String requestedName;
    private final List<Option> options = new ArrayList<>();

    private ExamChoice(String requestedName) {
        this.requestedName = requestedName;
    }

    public String getRequestedName() {
        return requestedName;
    }

    public List<Option> getOptions() {
        return options;
    }

    /** Un test (ou plusieurs tests de même code) × ses types d'échantillon. */
    public static ExamChoice forTests(String requestedName, List<Test> tests,
            Function<Test, List<TypeOfSample>> sampleTypesOf) {
        ExamChoice choice = new ExamChoice(requestedName);
        for (Test test : tests) {
            for (TypeOfSample sampleType : sampleTypesOf.apply(test)) {
                choice.options.add(new Option("test", test.getId(), test.getLocalizedName(), sampleType.getId(),
                        sampleType.getLocalizedName(), null, null));
            }
        }
        return choice;
    }

    /** Un panel × ses types d'échantillon, avec les tests du panel pour chacun. */
    public static ExamChoice forPanel(String requestedName, Panel panel, List<TypeOfSample> sampleTypes,
            Function<TypeOfSample, List<Test>> testsFor) {
        ExamChoice choice = new ExamChoice(requestedName);
        for (TypeOfSample sampleType : sampleTypes) {
            List<Test> tests = testsFor.apply(sampleType);
            choice.options.add(new Option("panel", panel.getId(), panel.getLocalizedName(), sampleType.getId(),
                    sampleType.getLocalizedName(), tests.stream().map(Test::getId).collect(Collectors.joining(",")),
                    tests.stream().map(Test::getLocalizedName).collect(Collectors.joining(","))));
        }
        return choice;
    }

    void appendXml(StringBuilder xml) {
        if (options.isEmpty()) {
            return;
        }
        xml.append("<examChoice>");
        XMLUtil.appendKeyValue("name", requestedName, xml);
        for (Option option : options) {
            xml.append("<option>");
            XMLUtil.appendKeyValue("kind", option.kind, xml);
            XMLUtil.appendKeyValue("id", option.id, xml);
            XMLUtil.appendKeyValue("label", option.label, xml);
            XMLUtil.appendKeyValue("sampleTypeId", option.sampleTypeId, xml);
            XMLUtil.appendKeyValue("sampleTypeName", option.sampleTypeName, xml);
            XMLUtil.appendKeyValue("testIds", option.testIds, xml);
            XMLUtil.appendKeyValue("testNames", option.testNames, xml);
            xml.append("</option>");
        }
        xml.append("</examChoice>");
    }
}

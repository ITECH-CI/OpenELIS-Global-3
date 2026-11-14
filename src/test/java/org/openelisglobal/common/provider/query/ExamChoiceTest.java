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
package org.openelisglobal.common.provider.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import org.openelisglobal.panel.valueholder.Panel;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;

/**
 * Examen d'une demande à plusieurs types d'échantillon possibles : une option
 * par type, que l'opérateur choisit.
 */
public class ExamChoiceTest {

    private static TypeOfSample type(String id, String name) {
        TypeOfSample sampleType = mock(TypeOfSample.class);
        when(sampleType.getId()).thenReturn(id);
        when(sampleType.getLocalizedName()).thenReturn(name);
        return sampleType;
    }

    private static Test test(String id, String name) {
        Test test = mock(Test.class);
        when(test.getId()).thenReturn(id);
        when(test.getLocalizedName()).thenReturn(name);
        return test;
    }

    @org.junit.Test
    public void testWithTwoSampleTypes_shouldOfferBoth() {
        Test viralLoad = test("12", "Charge virale VIH");
        ExamChoice choice = ExamChoice.forTests("Charge virale", Arrays.asList(viralLoad),
                t -> Arrays.asList(type("3", "Plasma"), type("5", "DBS")));

        assertEquals(2, choice.getOptions().size());
        assertEquals("3", choice.getOptions().get(0).getSampleTypeId());
        assertEquals("5", choice.getOptions().get(1).getSampleTypeId());

        StringBuilder xml = new StringBuilder();
        choice.appendXml(xml);
        assertTrue(
                xml.toString().contains("<examChoice><name>Charge virale</name><option><kind>test</kind><id>12</id>"));
        assertTrue(xml.toString().contains("<sampleTypeName>DBS</sampleTypeName>"));
    }

    @org.junit.Test
    public void panelWithTwoSampleTypes_shouldCarryItsTests() {
        Panel panel = mock(Panel.class);
        when(panel.getId()).thenReturn("7");
        when(panel.getLocalizedName()).thenReturn("Bilan hépatique");

        ExamChoice choice = ExamChoice.forPanel("Bilan hépatique", panel,
                Arrays.asList(type("4", "Sérum"), type("6", "Plasma")),
                t -> Arrays.asList(test("20", "ASAT"), test("21", "ALAT")));

        assertEquals(2, choice.getOptions().size());
        assertEquals("panel", choice.getOptions().get(0).getKind());
        StringBuilder xml = new StringBuilder();
        choice.appendXml(xml);
        assertTrue(xml.toString().contains("<testIds>20,21</testIds>"));
    }

    @org.junit.Test
    public void noOption_shouldEmitNothing() {
        ExamChoice choice = ExamChoice.forTests("X", Collections.emptyList(), t -> Collections.emptyList());
        StringBuilder xml = new StringBuilder();
        choice.appendXml(xml);
        assertEquals("", xml.toString());
    }
}

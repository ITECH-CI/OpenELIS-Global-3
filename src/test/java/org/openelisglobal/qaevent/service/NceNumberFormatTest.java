package org.openelisglobal.qaevent.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class NceNumberFormatTest {

    @Test
    public void siteYearAndFourDigitSequence() {
        assertEquals("ENC-14001-26-0014", NceNumberFormat.format("14001", "26", 14));
        assertEquals(17, NceNumberFormat.format("14001", "26", 14).length());
    }

    @Test
    public void emptySiteOmitsFieldAndSeparator() {
        assertEquals("ENC-26-0014", NceNumberFormat.format("", "26", 14));
        assertEquals("ENC-26-0014", NceNumberFormat.format(null, "26", 14));
        assertEquals("ENC-26-0014", NceNumberFormat.format("   ", "26", 14));
        assertFalse(NceNumberFormat.format("", "26", 14).contains("--"));
    }

    @Test
    public void siteIsTrimmed() {
        assertEquals("ENC-CHRSP-26-0001", NceNumberFormat.format(" CHRSP ", "26", 1));
    }

    @Test
    public void sequenceAboveFourDigitsWidensWithoutWrapping() {
        assertEquals("ENC-14001-26-9999", NceNumberFormat.format("14001", "26", 9999));
        assertEquals("ENC-14001-26-10000", NceNumberFormat.format("14001", "26", 10000));
        assertTrue(NceNumberFormat.format("14001", "26", 10000).length() <= NceNumberFormat.MAX_LENGTH);
    }

    @Test
    public void counterKeyIsPerSiteAndYear() {
        assertEquals("ENC-14001-26", NceNumberFormat.counterKey("14001", "26"));
        assertEquals("ENC-26", NceNumberFormat.counterKey("", "26"));
        assertNotEquals(NceNumberFormat.counterKey("14001", "26"), NceNumberFormat.counterKey("14001", "27"));
        // distinct du préfixe des numéros de laboratoire (site + année, sans
        // séparateur)
        assertNotEquals("1400126", NceNumberFormat.counterKey("14001", "26"));
    }
}

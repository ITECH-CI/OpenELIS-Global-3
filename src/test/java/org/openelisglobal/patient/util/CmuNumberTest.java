package org.openelisglobal.patient.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Numéros fictifs uniquement (préfixe 9992611). */
public class CmuNumberTest {

    @Test
    public void conforms_thirteenDigits_shouldConform() {
        assertTrue(CmuNumber.conforms("9992611000001", null));
        assertEquals("9992611000001", CmuNumber.conformingOrNull("9992611000001", ""));
    }

    @Test
    public void normalize_spacesDashesDots_shouldBeRemoved() {
        assertEquals("9992611000001", CmuNumber.normalize(" 999 2611-000.001 "));
        assertEquals("9992611000001", CmuNumber.conformingOrNull("999-2611-000-001", null));
    }

    @Test
    public void conforms_twelveDigitsOrLetters_shouldNotConform() {
        assertFalse(CmuNumber.conforms("999261100000", null));
        assertFalse(CmuNumber.conforms("999261100000A", null));
        assertNull(CmuNumber.conformingOrNull("DEMO-CMU", null));
    }

    @Test
    public void conforms_blank_shouldNotConform() {
        assertFalse(CmuNumber.conforms(null, null));
        assertFalse(CmuNumber.conforms(" - ", null));
        assertTrue(CmuNumber.isBlank(" . "));
    }

    @Test
    public void conforms_customFormat_shouldBeHonoured_andInvalidFormatFallsBackToDefault() {
        assertTrue(CmuNumber.conforms("999261100000", "^[0-9]{12}$"));
        assertTrue(CmuNumber.conforms("9992611000001", "[unclosed"));
    }
}

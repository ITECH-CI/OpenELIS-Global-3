package org.openelisglobal.common.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DisplayDateFormatTest {

    @Test
    public void dayFirst_ambiguousDate_keepsDayAndMonth() {
        // 12 mars 1990, saisi en jj/mm/aaaa
        assertEquals("12/03/1990", DisplayDateFormat.reformat("12/03/1990", true));
    }

    @Test
    public void dayFirst_isIdempotent() {
        String once = DisplayDateFormat.reformat("12/05/2001", true);
        assertEquals("12/05/2001", once);
        assertEquals(once, DisplayDateFormat.reformat(once, true));
    }

    @Test
    public void dayFirst_monthDayOnlyInput_isRewritten() {
        // 25 décembre : ne se lit qu'en mm/jj
        assertEquals("25/12/1990", DisplayDateFormat.reformat("12/25/1990", true));
    }

    @Test
    public void monthFirst_ambiguousDate_keepsMonthAndDay() {
        assertEquals("03/12/1990", DisplayDateFormat.reformat("03/12/1990", false));
        assertEquals("12/25/1990", DisplayDateFormat.reformat("25/12/1990", false));
    }

    @Test
    public void unreadableOrMissingDate() {
        assertEquals("Invalid date format: xx/xx/1990", DisplayDateFormat.reformat("xx/xx/1990", true));
        assertEquals("", DisplayDateFormat.reformat(null, true));
    }

    @Test
    public void dayFirst_onlyForFrFr() {
        assertTrue(DisplayDateFormat.isDayFirst("fr-FR"));
        assertFalse(DisplayDateFormat.isDayFirst("en-US"));
        assertFalse(DisplayDateFormat.isDayFirst(null));
    }
}

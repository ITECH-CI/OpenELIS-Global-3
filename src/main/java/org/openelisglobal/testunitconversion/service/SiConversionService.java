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
 */
package org.openelisglobal.testunitconversion.service;

import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.test.valueholder.Test;

/**
 * Service for converting test result values from traditional units to SI units.
 */
public interface SiConversionService {

    /**
     * Convert a single result value to SI units. Updates the result object with
     * valueSi, uomSi, siRule, and siLastupdated fields.
     *
     * @param result The result to convert
     * @return true if conversion was successful, false otherwise
     */
    boolean convertResultToSi(Result result);

    /**
     * Convert reference range (min/max normal) values to SI units.
     *
     * @param test      Test
     * @param minNormal Minimum normal value in traditional units
     * @param maxNormal Maximum normal value in traditional units
     * @return ReferenceRangeConversion object containing SI values
     */
    ReferenceRangeConversion convertReferenceRange(Test test, Double minNormal, Double maxNormal);

    /**
     * Clear the conversion rules cache. Call this when conversion rules are
     * modified.
     */
    void clearCache();

    /**
     * Container class for converted reference range values.
     */
    class ReferenceRangeConversion {
        public final Double minSi;
        public final Double maxSi;

        public ReferenceRangeConversion(Double minSi, Double maxSi) {
            this.minSi = minSi;
            this.maxSi = maxSi;
        }
    }
}

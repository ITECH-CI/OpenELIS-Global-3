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
 * <p>Copyright (C) ITECH, University of Washington, Seattle WA. All Rights Reserved.
 */
package org.openelisglobal.common.services.registration;

import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.common.services.registration.interfaces.IResultUpdate;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.dataexchange.resultreporting.ResultReportingUpdate;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.testunitconversion.service.SiUnitConversionUpdate;

public class ValidationUpdateRegister {
    public static List<IResultUpdate> getRegisteredUpdaters() {
        List<IResultUpdate> updaters = new ArrayList<IResultUpdate>();

        // kluge at this point, should be discoverable
        if (shouldReport(Property.reportResults)) {
            updaters.add(new ResultReportingUpdate());
        }

        // SI Unit Conversion is NOT included in validation updaters because:
        // 1. SI values are already calculated during result entry
        // 2. Results cannot be modified in the validation interface
        // 3. Re-calculating SI values during validation triggers false "modified result" detections
        // SI conversion only happens in ResultUpdateRegister (during result entry)

        /*****
         * BIG WARNING FLASHING LIGHT ********* If another listener is added then the
         * method addResultSets() needs to be modified in ResultValidationSaveAction
         *
         * There is code that only looks at patient reports when deciding what is a new
         * value and what is an old value
         */
        return updaters;
    }

    private static boolean shouldReport(Property property) {
        String reportResults = ConfigurationProperties.getInstance().getPropertyValueLowerCase(property);
        return ("true".equals(reportResults) || "enable".equals(reportResults));
    }
}

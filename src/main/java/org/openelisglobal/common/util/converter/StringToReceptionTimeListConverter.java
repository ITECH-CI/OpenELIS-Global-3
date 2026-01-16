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
package org.openelisglobal.common.util.converter;

import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.reports.form.ReportForm.ReceptionTime;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * Converter to convert String to List of ReceptionTime. Handles empty strings
 * and invalid values gracefully.
 */
@Component
public class StringToReceptionTimeListConverter implements Converter<String, List<ReceptionTime>> {

    @Override
    public List<ReceptionTime> convert(String source) {
        List<ReceptionTime> result = new ArrayList<>();

        // Handle null or empty strings
        if (source == null || source.trim().isEmpty()) {
            return result;
        }

        // Handle common invalid values from browsers
        if (source.equals("u=0") || source.equals("undefined") || source.equals("null")) {
            return result;
        }

        // Split by comma if multiple values
        String[] values = source.split(",");

        for (String value : values) {
            String trimmed = value.trim();
            if (!trimmed.isEmpty()) {
                try {
                    ReceptionTime receptionTime = ReceptionTime.valueOf(trimmed.toUpperCase());
                    result.add(receptionTime);
                } catch (IllegalArgumentException e) {
                    // Ignore invalid enum values
                    continue;
                }
            }
        }

        return result;
    }
}

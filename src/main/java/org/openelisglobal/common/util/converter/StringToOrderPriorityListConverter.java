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
import org.openelisglobal.sample.valueholder.OrderPriority;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * Converter to convert String to List of OrderPriority. Handles empty strings
 * and invalid values gracefully.
 */
@Component
public class StringToOrderPriorityListConverter implements Converter<String, List<OrderPriority>> {

    @Override
    public List<OrderPriority> convert(String source) {
        List<OrderPriority> result = new ArrayList<>();

        // Handle null or empty strings
        if (source == null || source.trim().isEmpty()) {
            return result;
        }

        // Neutralise les valeurs du header HTTP RFC 9218 "Priority: u=N[, i]"
        // que Firefox envoie automatiquement (pas Chrome). Spring matche le
        // nom de header sur l'attribut form "priority" et tente de le convertir
        // ici. Trimmed startsWith couvre "u=0", "u=3, i", "u=7", etc.
        String trimmedSource = source.trim();
        if (trimmedSource.startsWith("u=") || "undefined".equals(trimmedSource)
                || "null".equals(trimmedSource)) {
            return result;
        }

        // Split by comma if multiple values
        String[] values = source.split(",");

        for (String value : values) {
            String trimmed = value.trim();
            if (!trimmed.isEmpty()) {
                try {
                    OrderPriority priority = OrderPriority.valueOf(trimmed.toUpperCase());
                    result.add(priority);
                } catch (IllegalArgumentException e) {
                    // Ignore invalid enum values
                    continue;
                }
            }
        }

        return result;
    }
}

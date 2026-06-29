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

import org.openelisglobal.sample.valueholder.OrderPriority;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * Converter String -> OrderPriority (scalaire). Pendant du
 * StringToOrderPriorityListConverter.
 *
 * <p>
 * Sans ce converter explicite, Spring tente d'instancier l'enum via
 * OrderPriority.valueOf(String) directement. Cela échoue quand le header HTTP
 * "Priority" envoyé par Firefox (RFC 9218 — valeurs "u=0".."u=7") fuit dans le
 * binding de form parce que Spring matche le nom de header "Priority" sur
 * l'attribut form "priority". Sur Chrome ce header n'est pas envoyé, d'où le
 * comportement asymétrique signalé.
 *
 * <p>
 * Ce converter intercepte ces valeurs réseau (et quelques autres artefacts
 * communs — "undefined", "null") et renvoie null, laissant le champ form vide
 * au lieu de lever un typeMismatch et de casser l'impression PDF.
 */
@Component
public class StringToOrderPriorityConverter implements Converter<String, OrderPriority> {

    @Override
    public OrderPriority convert(String source) {
        if (source == null) {
            return null;
        }
        String trimmed = source.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        // Artefacts HTTP / clients : header RFC 9218 "Priority: u=N", valeur
        // JS sérialisée en string, paramètre vide. On les neutralise.
        if (trimmed.startsWith("u=") || "undefined".equals(trimmed) || "null".equals(trimmed)) {
            return null;
        }
        try {
            return OrderPriority.valueOf(trimmed.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}

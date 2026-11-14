package org.openelisglobal.dataexchange.order.controller;

import java.util.List;
import java.util.stream.Collectors;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrder;

/**
 * Chaque écran ne montre que sa catégorie : la routine dans les demandes
 * reçues, le reste (étude charge virale, demandes sans catégorie) dans l'écran
 * d'étude.
 */
public final class ElectronicOrderFilters {

    private ElectronicOrderFilters() {
    }

    public static List<ElectronicOrder> routine(List<ElectronicOrder> orders) {
        return orders == null ? null : orders.stream().filter(ElectronicOrder::isRoutine).collect(Collectors.toList());
    }

    public static List<ElectronicOrder> study(List<ElectronicOrder> orders) {
        return orders == null ? null : orders.stream().filter(order -> !order.isRoutine()).collect(Collectors.toList());
    }
}

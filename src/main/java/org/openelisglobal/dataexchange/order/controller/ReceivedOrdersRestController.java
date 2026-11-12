package org.openelisglobal.dataexchange.order.controller;

import java.util.Map;
import org.openelisglobal.dataexchange.service.order.ReceivedOrdersService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Fenêtre « Demandes reçues » : liste, compteur, détail. Le refus et
 * l'annulation passent par /rest/rejectElectronicOrders et
 * /rest/cancelElectronicOrders.
 */
@RestController
@RequestMapping("/rest/ReceivedOrders")
public class ReceivedOrdersRestController {

    @Autowired
    private ReceivedOrdersService receivedOrdersService;

    @GetMapping
    public ReceivedOrdersService.Page list(@RequestParam(required = false) String etape,
            @RequestParam(required = false) String origine, @RequestParam(required = false) String etablissement,
            @RequestParam(required = false) String dateDebut, @RequestParam(required = false) String dateFin,
            @RequestParam(required = false) String recherche, @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "25") int pageSize) {
        ReceivedOrdersService.Criteria criteria = new ReceivedOrdersService.Criteria();
        criteria.etape = etape;
        criteria.origine = origine;
        criteria.etablissement = etablissement;
        criteria.dateDebut = dateDebut;
        criteria.dateFin = dateFin;
        criteria.recherche = recherche;
        criteria.page = page;
        criteria.pageSize = pageSize;
        return receivedOrdersService.search(criteria);
    }

    @GetMapping("/count")
    public Map<String, Integer> count() {
        return Map.of("toProcess", receivedOrdersService.countToProcess());
    }

    @GetMapping("/{electronicOrderId}/detail")
    public ResponseEntity<Map<String, Object>> detail(@PathVariable String electronicOrderId) {
        Map<String, Object> detail = receivedOrdersService.detail(electronicOrderId);
        return detail.isEmpty() ? ResponseEntity.notFound().build() : ResponseEntity.ok(detail);
    }
}

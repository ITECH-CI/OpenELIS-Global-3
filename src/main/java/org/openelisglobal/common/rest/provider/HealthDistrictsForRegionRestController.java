package org.openelisglobal.common.rest.provider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.common.util.validator.GenericValidator;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.valueholder.Organization;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping(value = "/rest/")
public class HealthDistrictsForRegionRestController {
    @Autowired
    OrganizationService organizationService;

    @GetMapping(value = "health-districts-for-region", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<IdValuePair> getHealthDistrictsForRegion(@RequestParam String regionId) {
        if (GenericValidator.isBlankOrNull(regionId)) {
            return Collections.<IdValuePair>emptyList();
        }

        try {
            List<Organization> districts = null;

            // Check if regionId is numeric before trying to use it as a parent ID
            if (regionId.matches("^\\d+$")) {
                // It's a numeric ID, use it directly
                districts = organizationService.getOrganizationsByParentId(regionId);
            } else {
                // It's a name, find the region by name first
                Organization searchOrg = new Organization();
                searchOrg.setOrganizationName(regionId);
                Organization region = organizationService.getOrganizationByName(searchOrg, true);
                if (region != null && region.getId() != null) {
                    districts = organizationService.getOrganizationsByParentId(region.getId());
                }
            }

            List<IdValuePair> districtIdValues = new ArrayList<>();
            if (districts != null && !districts.isEmpty()) {
                districts.forEach(org -> {
                    IdValuePair district = new IdValuePair(org.getId(), org.getOrganizationName());
                    districtIdValues.add(district);
                });
                return districtIdValues;
            } else {
                return Collections.<IdValuePair>emptyList();
            }
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getName(), "getHealthDistrictsForRegion",
                    "Error retrieving health districts for region: " + regionId);
            LogEvent.logError(e);
            return Collections.<IdValuePair>emptyList();
        }
    }
}

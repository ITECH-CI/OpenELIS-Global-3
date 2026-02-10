import React from "react";
import GlobalSideBar from "../common/GlobalSideBar";
import { FormattedMessage, injectIntl } from "react-intl";
import { IbmWatsonDiscovery, Microscope } from "@carbon/icons-react";
import config from "../../config.json";
import PageBreadCrumb from "../common/PageBreadCrumb";

let breadcrumbs = [{ label: "home.label", link: "/" }];

export const BacteriologyReportsMenu = {
  className: "bacterioSideNav",
  sideNavMenuItems: [
    {
      title: <FormattedMessage id="sidenav.title.bacterio.statusreport" />,
      icon: IbmWatsonDiscovery,
      SideNavMenuItem: [
        {
          link: "/BacteriologyReport?type=patient&report=BacterioPatientReport",
          label: <FormattedMessage id="sidenav.label.bacterio.patientreport" />,
        },
      ],
    },
    {
      title: <FormattedMessage id="sidenav.title.bacterio.export" />,
      icon: Microscope,
      SideNavMenuItem: [
        {
          link: "/BacteriologyReport?type=export&report=bacteriologyCSV",
          label: <FormattedMessage id="sidenav.label.bacterio.csvexport" />,
        },
      ],
    },
  ],
};

function Bacteriology({ intl }) {
  return (
    <>
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <div className="adminPageContent">
        <GlobalSideBar
          globalSideBar={{
            ...BacteriologyReportsMenu,
            title: intl.formatMessage({
              id: "banner.menu.microbiology_classic",
            }),
          }}
        />
      </div>
    </>
  );
}

export default injectIntl(Bacteriology);

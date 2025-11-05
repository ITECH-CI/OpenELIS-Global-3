 import { WarningAltFilled} from "@carbon/icons-react";


 export const priorities = [
  {
    value: "ROUTINE",
    label: "ROUTINE",
    icon :  <></>
  },
  {
    value: "ASAP",
    label: "URGENT",
    icon : <WarningAltFilled size={18} style={{ color: '#f1b605ff' }} />
  },
  {
    value: "STAT",
    label: "IMMEDIAT",
    icon: <WarningAltFilled size={18} style={{ color: '#da1e28' }} />,
  },
 /* {
    value: "Timed",
    label: "Timed",
  },
  {
    value: "FUTURE_STAT",
    label: "Future STAT",
  },*/
];
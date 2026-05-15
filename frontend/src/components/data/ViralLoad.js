export const studyForms = [
  { value: "InitialARV_Id", label: "ARV - Bilan Initial" },
  { value: "FollowUpARV_Id", label: "ARV - Bilan de Suivi" },
  { value: "VL_Id", label: "ARV - Charge Virale" },
  { value: "EID_Id", label: "EID" },
    //{ value: "RTN_Id", label: "RTN" },
 // { value: "Indeterminate_Id", label: "Indeterminate" },
 // { value: "Special_Request_Id", label: "Requête Spéciale" },
  //{ value: "Recency_Id", label: "Recency" },
  { value: "HPV_Id", label: "HPV Testing" },
];

export const LAB_PREFIXES = {
  InitialARV_Id: "sample.entry.project.LART",
  FollowUpARV_Id: "sample.entry.project.LART",
  RTN_Id: "sample.entry.project.LRTN",
  EID_Id: "sample.entry.project.LDBS",
  Indeterminate_Id: "sample.entry.project.LIND",
  Special_Request_Id: "sample.entry.project.LSPE",
  VL_Id: "sample.entry.project.LVL",
  Recency_Id: "sample.entry.project.LRT",
  HPV_Id: "",
};
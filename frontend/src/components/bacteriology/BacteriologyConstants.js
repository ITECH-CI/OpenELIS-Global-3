/**
 * Constants for Bacteriology Classique module
 */

export const ROUTINE_BACTERIOLOGY_TEST_SECTION_NAME = "Routine Bacteriology";

export const ORGANISM_TYPES = {
  BACTERIA: "BACTERIA",
  YEAST: "YEAST",
};

export const GRAM_TYPES = {
  POSITIVE: "POSITIVE",
  NEGATIVE: "NEGATIVE",
};

export const ANTIBIOGRAM_RESULTS = {
  S: "S", // Sensible
  I: "I", // Intermédiaire
  R: "R", // Résistant
};

export const GROUP_TYPES = {
  MACROSCOPY: "MACROSCOPY",
  MICROSCOPY: "MICROSCOPY",
  CULTURE: "CULTURE",
  ORGANISM: "ORGANISM",
  ANTIBIOGRAM: "ANTIBIOGRAM",
};

export const MAX_ORGANISMS = 3;

// Display list categories
export const ANTIBIOTICS_CATEGORY = "BACTERIOLOGY_ANTIBIOTICS";

export const API_ENDPOINTS = {
  ANTIBIOTICS: "/rest/bacteriology/antibiotics",
  ORGANISMS: "/rest/bacteriology/organisms",
  GET_RESULTS: "/rest/bacteriology/results",
  SAVE_RESULTS: "/rest/bacteriology/results",
  VALIDATE_RESULTS: "/rest/bacteriology/validate",
  CHECK_EXISTS: "/rest/bacteriology/results/{analysisId}/exists",
  CREATE_ORGANISM_GROUP: "/rest/bacteriology/organism/group",
  SAVE_ORGANISM: "/rest/bacteriology/organism",
  SAVE_ANTIBIOGRAM: "/rest/bacteriology/antibiogram",
  DELETE_ORGANISM: "/rest/bacteriology/organism",
  CLEAR_RESULTS: "/rest/bacteriology/results",
  FLORA_BY_ANALYSIS: "/rest/bacteriology/flora/analysis",
};

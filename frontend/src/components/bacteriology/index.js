/**
 * Bacteriology module exports
 * This module provides specialized result entry for Routine Bacteriology analyses
 */

export { default as BacteriologyResultEntry } from "./BacteriologyResultEntry";
export { default as BacteriologyResultsContainer } from "./BacteriologyResultsContainer";
export {
  default as useBacteriologyDetection,
  isBacteriologyTestSection,
} from "./hooks/useBacteriologyDetection";
export * from "./BacteriologyConstants";

// Re-export sub-components for flexibility
export { default as MacroscopySection } from "./sections/MacroscopySection";
export { default as MicroscopySection } from "./sections/MicroscopySection";
export { default as CultureSection } from "./sections/CultureSection";
export { default as OrganismList } from "./organisms/OrganismList";
export { default as OrganismIdentification } from "./organisms/OrganismIdentification";
export { default as AntibiogramTable } from "./organisms/AntibiogramTable";
export { default as BacteriologyResultField } from "./common/BacteriologyResultField";

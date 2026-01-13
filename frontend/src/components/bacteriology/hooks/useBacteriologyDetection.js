import { useState, useEffect } from "react";
import { ROUTINE_BACTERIOLOGY_TEST_SECTION_NAME } from "../BacteriologyConstants";

/**
 * Hook to detect if the current analysis is for Routine Bacteriology
 * @param {Object} testSection - The test section object with testSectionName property
 * @returns {boolean} - True if this is a Routine Bacteriology analysis
 */
export const useBacteriologyDetection = (testSection) => {
  const [isBacteriology, setIsBacteriology] = useState(false);

  useEffect(() => {
    if (testSection && testSection.testSectionName) {
      const isRoutineBacteriology =
        testSection.testSectionName === ROUTINE_BACTERIOLOGY_TEST_SECTION_NAME;
      setIsBacteriology(isRoutineBacteriology);
    } else {
      setIsBacteriology(false);
    }
  }, [testSection]);

  return isBacteriology;
};

/**
 * Hook to detect if a test section name is Routine Bacteriology
 * @param {string} testSectionName - The test section name
 * @returns {boolean} - True if this is a Routine Bacteriology test section
 */
export const isBacteriologyTestSection = (testSectionName) => {
  return testSectionName === ROUTINE_BACTERIOLOGY_TEST_SECTION_NAME;
};

export default useBacteriologyDetection;

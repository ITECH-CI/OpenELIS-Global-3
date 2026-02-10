package org.openelisglobal.reports.action.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Data model specifically designed for bacteriology reports.
 * Represents a hierarchical structure: Sections > Subsections > Tests > Results
 */
public class BacteriologyReportData {

    // Patient and sample information (inherited from base report)
    private String accessionNumber;
    private String patientName;
    private String patientId;
    private String age;
    private String gender;
    private String receivedDate;
    private String testDate;
    private String siteInfo;

    // Bacteriology-specific sections
    private List<BacteriologySection> sections = new ArrayList<>();

    public BacteriologyReportData() {
    }

    // Getters and setters
    public String getAccessionNumber() {
        return accessionNumber;
    }

    public void setAccessionNumber(String accessionNumber) {
        this.accessionNumber = accessionNumber;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getAge() {
        return age;
    }

    public void setAge(String age) {
        this.age = age;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getReceivedDate() {
        return receivedDate;
    }

    public void setReceivedDate(String receivedDate) {
        this.receivedDate = receivedDate;
    }

    public String getTestDate() {
        return testDate;
    }

    public void setTestDate(String testDate) {
        this.testDate = testDate;
    }

    public String getSiteInfo() {
        return siteInfo;
    }

    public void setSiteInfo(String siteInfo) {
        this.siteInfo = siteInfo;
    }

    public List<BacteriologySection> getSections() {
        return sections;
    }

    public void setSections(List<BacteriologySection> sections) {
        this.sections = sections;
    }

    public void addSection(BacteriologySection section) {
        this.sections.add(section);
    }

    /**
     * Represents a main section in the bacteriology report (e.g., Macroscopie, Microscopie, Culture)
     */
    public static class BacteriologySection {
        private String sectionName;
        private int sectionOrder;
        private List<BacteriologySubsection> subsections = new ArrayList<>();

        public BacteriologySection() {
        }

        public BacteriologySection(String sectionName, int sectionOrder) {
            this.sectionName = sectionName;
            this.sectionOrder = sectionOrder;
        }

        public String getSectionName() {
            return sectionName;
        }

        public void setSectionName(String sectionName) {
            this.sectionName = sectionName;
        }

        public int getSectionOrder() {
            return sectionOrder;
        }

        public void setSectionOrder(int sectionOrder) {
            this.sectionOrder = sectionOrder;
        }

        public List<BacteriologySubsection> getSubsections() {
            return subsections;
        }

        public void setSubsections(List<BacteriologySubsection> subsections) {
            this.subsections = subsections;
        }

        public void addSubsection(BacteriologySubsection subsection) {
            this.subsections.add(subsection);
        }
    }

    /**
     * Represents a subsection within a section (e.g., Gland, Muqueuse vaginale)
     */
    public static class BacteriologySubsection {
        private String subsectionName;
        private int subsectionOrder;
        private List<BacteriologyTest> tests = new ArrayList<>();

        public BacteriologySubsection() {
        }

        public BacteriologySubsection(String subsectionName, int subsectionOrder) {
            this.subsectionName = subsectionName;
            this.subsectionOrder = subsectionOrder;
        }

        public String getSubsectionName() {
            return subsectionName;
        }

        public void setSubsectionName(String subsectionName) {
            this.subsectionName = subsectionName;
        }

        public int getSubsectionOrder() {
            return subsectionOrder;
        }

        public void setSubsectionOrder(int subsectionOrder) {
            this.subsectionOrder = subsectionOrder;
        }

        public List<BacteriologyTest> getTests() {
            return tests;
        }

        public void setTests(List<BacteriologyTest> tests) {
            this.tests = tests;
        }

        public void addTest(BacteriologyTest test) {
            this.tests.add(test);
        }
    }

    /**
     * Represents an individual test result
     */
    public static class BacteriologyTest {
        private String testName;
        private String result;
        private int testOrder;

        public BacteriologyTest() {
        }

        public BacteriologyTest(String testName, String result, int testOrder) {
            this.testName = testName;
            this.result = result;
            this.testOrder = testOrder;
        }

        public String getTestName() {
            return testName;
        }

        public void setTestName(String testName) {
            this.testName = testName;
        }

        public String getResult() {
            return result;
        }

        public void setResult(String result) {
            this.result = result;
        }

        public int getTestOrder() {
            return testOrder;
        }

        public void setTestOrder(int testOrder) {
            this.testOrder = testOrder;
        }
    }
}

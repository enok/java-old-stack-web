package com.campusconnect.domain.sis;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

/**
 * One row of the nightly extract.
 *
 * The element set is the union of what all three customers send. Fields that
 * only one customer populates are simply null for the others, and which fields
 * matter is decided in the importers by customer code rather than by the file.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class SisStudentRecord {

    @XmlElement(name = "sis-id")
    private String sisId;

    @XmlElement(name = "first-name")
    private String firstName;

    @XmlElement(name = "last-name")
    private String lastName;

    @XmlElement(name = "email")
    private String email;

    @XmlElement(name = "phone")
    private String phone;

    @XmlElement(name = "birth-date")
    private String birthDate;

    @XmlElement(name = "gpa")
    private String gpa;

    @XmlElement(name = "credits")
    private String creditsCompleted;

    @XmlElement(name = "status")
    private String enrollmentStatus;

    @XmlElement(name = "program")
    private String programCode;

    @XmlElement(name = "advisor-staff-id")
    private String advisorStaffId;

    /** RIVERTON only. */
    @XmlElement(name = "athletics-code")
    private String athleticsCode;

    /** RIVERTON only - raw campus code that must be mapped before storage. */
    @XmlElement(name = "campus")
    private String campusCode;

    /** SUMMIT only. */
    @XmlElement(name = "home-campus")
    private String homeCampus;

    public String getSisId() { return sisId; }
    public void setSisId(String sisId) { this.sisId = sisId; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getBirthDate() { return birthDate; }
    public void setBirthDate(String birthDate) { this.birthDate = birthDate; }
    public String getGpa() { return gpa; }
    public void setGpa(String gpa) { this.gpa = gpa; }
    public String getCreditsCompleted() { return creditsCompleted; }
    public void setCreditsCompleted(String creditsCompleted) { this.creditsCompleted = creditsCompleted; }
    public String getEnrollmentStatus() { return enrollmentStatus; }
    public void setEnrollmentStatus(String enrollmentStatus) { this.enrollmentStatus = enrollmentStatus; }
    public String getProgramCode() { return programCode; }
    public void setProgramCode(String programCode) { this.programCode = programCode; }
    public String getAdvisorStaffId() { return advisorStaffId; }
    public void setAdvisorStaffId(String advisorStaffId) { this.advisorStaffId = advisorStaffId; }
    public String getAthleticsCode() { return athleticsCode; }
    public void setAthleticsCode(String athleticsCode) { this.athleticsCode = athleticsCode; }
    public String getCampusCode() { return campusCode; }
    public void setCampusCode(String campusCode) { this.campusCode = campusCode; }
    public String getHomeCampus() { return homeCampus; }
    public void setHomeCampus(String homeCampus) { this.homeCampus = homeCampus; }
}

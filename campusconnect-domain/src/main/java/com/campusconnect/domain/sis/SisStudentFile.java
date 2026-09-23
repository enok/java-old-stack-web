package com.campusconnect.domain.sis;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * JAXB binding for the nightly SIS student extract.
 *
 * On JDK 8 javax.xml.bind ships with the platform, which is exactly why the
 * original team chose it and why the jump to JDK 11+ is a breaking change.
 */
@XmlRootElement(name = "sis-student-file")
@XmlAccessorType(XmlAccessType.FIELD)
public class SisStudentFile {

    @XmlAttribute(name = "customer")
    private String customerCode;

    @XmlAttribute(name = "term")
    private String termCode;

    @XmlAttribute(name = "generated")
    private String generatedOn;

    @XmlElementWrapper(name = "students")
    @XmlElement(name = "student")
    private List<SisStudentRecord> students = new ArrayList<SisStudentRecord>();

    public String getCustomerCode() { return customerCode; }
    public void setCustomerCode(String customerCode) { this.customerCode = customerCode; }
    public String getTermCode() { return termCode; }
    public void setTermCode(String termCode) { this.termCode = termCode; }
    public String getGeneratedOn() { return generatedOn; }
    public void setGeneratedOn(String generatedOn) { this.generatedOn = generatedOn; }
    public List<SisStudentRecord> getStudents() { return students; }
    public void setStudents(List<SisStudentRecord> students) { this.students = students; }
}

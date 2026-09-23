package com.campusconnect.domain;

import java.io.Serializable;
import java.util.Date;

/**
 * NOT a Hibernate entity on purpose.
 *
 * LEGACY SMELL #6: enrollment is read and written by JdbcEnrollmentDao with
 * hand-built SQL strings, so this is a plain transfer object that the JDBC DAO
 * populates from a ResultSet by hand. Two persistence technologies, one schema,
 * no shared transaction.
 */
public class Enrollment implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String customerCode;
    private Long studentId;
    private Long courseId;
    private String courseCode;
    private String courseTitle;
    private String termCode;
    private String grade;
    private Integer credits;
    private String status;
    private Date enrolledAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCustomerCode() { return customerCode; }
    public void setCustomerCode(String customerCode) { this.customerCode = customerCode; }
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }
    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }
    public String getCourseTitle() { return courseTitle; }
    public void setCourseTitle(String courseTitle) { this.courseTitle = courseTitle; }
    public String getTermCode() { return termCode; }
    public void setTermCode(String termCode) { this.termCode = termCode; }
    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }
    public Integer getCredits() { return credits; }
    public void setCredits(Integer credits) { this.credits = credits; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Date getEnrolledAt() { return enrolledAt; }
    public void setEnrolledAt(Date enrolledAt) { this.enrolledAt = enrolledAt; }
}

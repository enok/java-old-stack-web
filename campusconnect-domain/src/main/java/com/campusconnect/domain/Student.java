package com.campusconnect.domain;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * Annotated entity (javax.persistence) mapped by Hibernate 4.3 through
 * LocalSessionFactoryBean. No HBM files - that much, at least, was modern in 2014.
 *
 * XXX: customerCode is a column on every table instead of a schema-per-tenant or
 * a discriminator. Nothing enforces it, so a missing WHERE clause leaks rows
 * across customers. See JdbcEnrollmentDao.
 */
@Entity
@Table(name = "student")
public class Student implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "customer_code", length = 20, nullable = false)
    private String customerCode;

    @Column(name = "sis_id", length = 40, nullable = false)
    private String sisId;

    @Column(name = "first_name", length = 80)
    private String firstName;

    @Column(name = "last_name", length = 80)
    private String lastName;

    @Column(name = "email", length = 160)
    private String email;

    @Column(name = "phone", length = 40)
    private String phone;

    @Column(name = "gpa")
    private Double gpa;

    @Column(name = "credits_completed")
    private Integer creditsCompleted;

    @Column(name = "enrollment_status", length = 20)
    private String enrollmentStatus;

    @Column(name = "program_code", length = 40)
    private String programCode;

    /** Riverton only. Null for every other customer. See docs/FORKS.md. */
    @Column(name = "athletics_code", length = 20)
    private String athleticsCode;

    /** Summit only. Null for every other customer. */
    @Column(name = "home_campus", length = 40)
    private String homeCampus;

    @Temporal(TemporalType.DATE)
    @Column(name = "birth_date")
    private Date birthDate;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at")
    private Date createdAt;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_at")
    private Date updatedAt;

    @ManyToOne
    @JoinColumn(name = "advisor_id")
    private Advisor advisor;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCustomerCode() { return customerCode; }
    public void setCustomerCode(String customerCode) { this.customerCode = customerCode; }
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
    public Double getGpa() { return gpa; }
    public void setGpa(Double gpa) { this.gpa = gpa; }
    public Integer getCreditsCompleted() { return creditsCompleted; }
    public void setCreditsCompleted(Integer creditsCompleted) { this.creditsCompleted = creditsCompleted; }
    public String getEnrollmentStatus() { return enrollmentStatus; }
    public void setEnrollmentStatus(String enrollmentStatus) { this.enrollmentStatus = enrollmentStatus; }
    public String getProgramCode() { return programCode; }
    public void setProgramCode(String programCode) { this.programCode = programCode; }
    public String getAthleticsCode() { return athleticsCode; }
    public void setAthleticsCode(String athleticsCode) { this.athleticsCode = athleticsCode; }
    public String getHomeCampus() { return homeCampus; }
    public void setHomeCampus(String homeCampus) { this.homeCampus = homeCampus; }
    public Date getBirthDate() { return birthDate; }
    public void setBirthDate(Date birthDate) { this.birthDate = birthDate; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
    public Advisor getAdvisor() { return advisor; }
    public void setAdvisor(Advisor advisor) { this.advisor = advisor; }

    /** Presentation logic on an entity. It is called from three JSPs. */
    public String getFullName() {
        return (firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName);
    }
}

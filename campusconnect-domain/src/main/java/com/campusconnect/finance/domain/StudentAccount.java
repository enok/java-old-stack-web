package com.campusconnect.finance.domain;

import java.io.Serializable;
import java.math.BigDecimal;
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

import com.campusconnect.domain.Student;

/**
 * Student Finance was bolted on in 2016 "for one term, as a stopgap, until the
 * bursar's package is replaced". It is still here.
 *
 * It lives in campusconnect-domain next to the advising entities and hangs off
 * the SAME student row by foreign key. There is no billing-side person record
 * and no billing-side customer table: finance reads the advising student and
 * the advising customer_code and that is the whole integration.
 *
 * One account per student per customer is a convention, not a constraint.
 * There is no unique key on (customer_code, student_id) and the nightly
 * reconcile has created a second account row at SUMMIT twice (CC-1412).
 */
@Entity
@Table(name = "student_account")
public class StudentAccount implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_PAST_DUE = "PAST_DUE";
    public static final String STATUS_CLOSED = "CLOSED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "customer_code", length = 20, nullable = false)
    private String customerCode;

    /** The seam. Finance points straight at the advising student row. */
    @ManyToOne
    @JoinColumn(name = "student_id")
    private Student student;

    @Column(name = "term_code", length = 20)
    private String termCode;

    @Column(name = "balance", precision = 12, scale = 2)
    private BigDecimal balance;

    @Column(name = "status", length = 20)
    private String status;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_charge_at")
    private Date lastChargeAt;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_payment_at")
    private Date lastPaymentAt;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "recomputed_at")
    private Date recomputedAt;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at")
    private Date createdAt;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_at")
    private Date updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCustomerCode() { return customerCode; }
    public void setCustomerCode(String customerCode) { this.customerCode = customerCode; }
    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }
    public String getTermCode() { return termCode; }
    public void setTermCode(String termCode) { this.termCode = termCode; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Date getLastChargeAt() { return lastChargeAt; }
    public void setLastChargeAt(Date lastChargeAt) { this.lastChargeAt = lastChargeAt; }
    public Date getLastPaymentAt() { return lastPaymentAt; }
    public void setLastPaymentAt(Date lastPaymentAt) { this.lastPaymentAt = lastPaymentAt; }
    public Date getRecomputedAt() { return recomputedAt; }
    public void setRecomputedAt(Date recomputedAt) { this.recomputedAt = recomputedAt; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }

    /**
     * Presentation logic on an entity again, copied from Student.getFullName().
     * Two JSPs call it and so does StudentService.
     */
    public String getBalanceLabel() {
        if (balance == null) {
            return "0.00";
        }
        return balance.toString();
    }
}

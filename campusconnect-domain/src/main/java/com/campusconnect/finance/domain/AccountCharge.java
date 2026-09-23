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

@Entity
@Table(name = "account_charge")
public class AccountCharge implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String TYPE_TUITION = "TUITION";
    public static final String TYPE_FEE = "FEE";
    public static final String TYPE_LATE_FEE = "LATE_FEE";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "customer_code", length = 20, nullable = false)
    private String customerCode;

    @ManyToOne
    @JoinColumn(name = "account_id")
    private StudentAccount account;

    /**
     * Denormalised copy of account.student.id, added so the nightly report
     * query would stop timing out. Nothing keeps the two in step; when an
     * account is re-pointed by hand at the bursar's request they drift.
     */
    @Column(name = "student_id")
    private Long studentId;

    @Column(name = "charge_type", length = 20)
    private String chargeType;

    @Column(name = "amount", precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "term_code", length = 20)
    private String termCode;

    @Column(name = "description", length = 200)
    private String description;

    @Column(name = "source_ref", length = 60)
    private String sourceRef;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "posted_at")
    private Date postedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCustomerCode() { return customerCode; }
    public void setCustomerCode(String customerCode) { this.customerCode = customerCode; }
    public StudentAccount getAccount() { return account; }
    public void setAccount(StudentAccount account) { this.account = account; }
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    public String getChargeType() { return chargeType; }
    public void setChargeType(String chargeType) { this.chargeType = chargeType; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getTermCode() { return termCode; }
    public void setTermCode(String termCode) { this.termCode = termCode; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getSourceRef() { return sourceRef; }
    public void setSourceRef(String sourceRef) { this.sourceRef = sourceRef; }
    public Date getPostedAt() { return postedAt; }
    public void setPostedAt(Date postedAt) { this.postedAt = postedAt; }
}

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
 * A hold is a finance row that an ADVISING rule reads: AppointmentService asks
 * FinancialHoldService whether a hold blocks scheduling. There is no interface
 * and no event in between, which is the single tightest knot in the codebase.
 *
 * "Released" is modelled as a nullable date rather than a status,
 * so "is this hold active" is spelled `released_at IS NULL` in HQL, in raw SQL
 * and once in a JSP. Three spellings, one rule.
 */
@Entity
@Table(name = "financial_hold")
public class FinancialHold implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String REASON_BALANCE = "BALANCE_OVER_THRESHOLD";
    public static final String REASON_MANUAL = "BURSAR_MANUAL";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "customer_code", length = 20, nullable = false)
    private String customerCode;

    @ManyToOne
    @JoinColumn(name = "account_id")
    private StudentAccount account;

    @ManyToOne
    @JoinColumn(name = "student_id")
    private Student student;

    @Column(name = "reason_code", length = 40)
    private String reasonCode;

    @Column(name = "threshold_amount", precision = 12, scale = 2)
    private BigDecimal thresholdAmount;

    @Column(name = "balance_at_placement", precision = 12, scale = 2)
    private BigDecimal balanceAtPlacement;

    @Column(name = "placed_by", length = 120)
    private String placedBy;

    @Column(name = "notes", length = 4000)
    private String notes;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "placed_at")
    private Date placedAt;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "released_at")
    private Date releasedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCustomerCode() { return customerCode; }
    public void setCustomerCode(String customerCode) { this.customerCode = customerCode; }
    public StudentAccount getAccount() { return account; }
    public void setAccount(StudentAccount account) { this.account = account; }
    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }
    public String getReasonCode() { return reasonCode; }
    public void setReasonCode(String reasonCode) { this.reasonCode = reasonCode; }
    public BigDecimal getThresholdAmount() { return thresholdAmount; }
    public void setThresholdAmount(BigDecimal thresholdAmount) { this.thresholdAmount = thresholdAmount; }
    public BigDecimal getBalanceAtPlacement() { return balanceAtPlacement; }
    public void setBalanceAtPlacement(BigDecimal balanceAtPlacement) { this.balanceAtPlacement = balanceAtPlacement; }
    public String getPlacedBy() { return placedBy; }
    public void setPlacedBy(String placedBy) { this.placedBy = placedBy; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Date getPlacedAt() { return placedAt; }
    public void setPlacedAt(Date placedAt) { this.placedAt = placedAt; }
    public Date getReleasedAt() { return releasedAt; }
    public void setReleasedAt(Date releasedAt) { this.releasedAt = releasedAt; }

    public boolean isActive() {
        return releasedAt == null;
    }
}

package com.campusconnect.finance.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * NOT a Hibernate entity, for the same reason {@link com.campusconnect.domain.Enrollment}
 * is not one: JdbcBillingExposureDao builds it by hand from a ResultSet.
 *
 * Every field on this object comes from a single SELECT that joins the advising
 * tables (appointment, student) to the finance tables (student_account,
 * financial_hold). Nothing in the type system says so.
 */
public class BillingExposureRow implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long studentId;
    private String sisId;
    private String studentName;
    private Long appointmentId;
    private Date appointmentStartsAt;
    private String appointmentStatus;
    private String reasonCode;
    private Long accountId;
    private BigDecimal balance;
    private String termCode;
    private String holdReasonCode;
    private Date holdPlacedAt;

    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    public String getSisId() { return sisId; }
    public void setSisId(String sisId) { this.sisId = sisId; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public Long getAppointmentId() { return appointmentId; }
    public void setAppointmentId(Long appointmentId) { this.appointmentId = appointmentId; }
    public Date getAppointmentStartsAt() { return appointmentStartsAt; }
    public void setAppointmentStartsAt(Date appointmentStartsAt) { this.appointmentStartsAt = appointmentStartsAt; }
    public String getAppointmentStatus() { return appointmentStatus; }
    public void setAppointmentStatus(String appointmentStatus) { this.appointmentStatus = appointmentStatus; }
    public String getReasonCode() { return reasonCode; }
    public void setReasonCode(String reasonCode) { this.reasonCode = reasonCode; }
    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    public String getTermCode() { return termCode; }
    public void setTermCode(String termCode) { this.termCode = termCode; }
    public String getHoldReasonCode() { return holdReasonCode; }
    public void setHoldReasonCode(String holdReasonCode) { this.holdReasonCode = holdReasonCode; }
    public Date getHoldPlacedAt() { return holdPlacedAt; }
    public void setHoldPlacedAt(Date holdPlacedAt) { this.holdPlacedAt = holdPlacedAt; }

    public boolean isOnHold() {
        return holdReasonCode != null;
    }
}

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
@Table(name = "account_payment")
public class AccountPayment implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String METHOD_CASH = "CASH";
    public static final String METHOD_CHECK = "CHECK";
    public static final String METHOD_CARD = "CARD";
    public static final String METHOD_AID = "AID";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "customer_code", length = 20, nullable = false)
    private String customerCode;

    @ManyToOne
    @JoinColumn(name = "account_id")
    private StudentAccount account;

    @Column(name = "amount", precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "payment_method", length = 20)
    private String paymentMethod;

    @Column(name = "reference_no", length = 60)
    private String referenceNo;

    @Column(name = "received_by", length = 120)
    private String receivedBy;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "posted_at")
    private Date postedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCustomerCode() { return customerCode; }
    public void setCustomerCode(String customerCode) { this.customerCode = customerCode; }
    public StudentAccount getAccount() { return account; }
    public void setAccount(StudentAccount account) { this.account = account; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getReferenceNo() { return referenceNo; }
    public void setReferenceNo(String referenceNo) { this.referenceNo = referenceNo; }
    public String getReceivedBy() { return receivedBy; }
    public void setReceivedBy(String receivedBy) { this.receivedBy = receivedBy; }
    public Date getPostedAt() { return postedAt; }
    public void setPostedAt(Date postedAt) { this.postedAt = postedAt; }
}

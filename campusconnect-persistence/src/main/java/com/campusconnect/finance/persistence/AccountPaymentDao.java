package com.campusconnect.finance.persistence;

import java.util.List;

import com.campusconnect.finance.domain.AccountPayment;

public interface AccountPaymentDao {
    AccountPayment findById(Long id);
    List findByAccountId(Long accountId);
    void save(AccountPayment payment);
}

package com.campusconnect.finance.persistence;

import java.util.List;

import com.campusconnect.finance.domain.AccountCharge;

public interface AccountChargeDao {
    AccountCharge findById(Long id);
    List findByAccountId(Long accountId);
    List findByAccountIdAndTerm(Long accountId, String termCode);
    void save(AccountCharge charge);
    void update(AccountCharge charge);
}

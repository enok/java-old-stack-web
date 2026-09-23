package com.campusconnect.finance.persistence;

import java.util.List;

import com.campusconnect.finance.domain.FinancialHold;

public interface FinancialHoldDao {
    FinancialHold findById(Long id);
    FinancialHold findActiveForStudent(Long studentId);
    List findActive();
    void save(FinancialHold hold);
    void update(FinancialHold hold);
}

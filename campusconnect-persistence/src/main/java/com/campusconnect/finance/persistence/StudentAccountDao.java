package com.campusconnect.finance.persistence;

import java.util.List;

import com.campusconnect.finance.domain.StudentAccount;

public interface StudentAccountDao {

    StudentAccount findById(Long id);

    /** One account per student per customer, by convention only. */
    StudentAccount findByStudentId(Long studentId);

    /** Raw List return type, like every other DAO here. Callers cast. */
    List findByCustomer(String customerCode);

    List findWithBalanceOver(double threshold);

    void save(StudentAccount account);

    void update(StudentAccount account);
}

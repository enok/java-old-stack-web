package com.campusconnect.persistence;

import java.util.List;

import com.campusconnect.domain.Student;

public interface StudentDao {

    Student findById(Long id);

    Student findBySisId(String sisId);

    /** Raw List return type. Callers cast element by element. */
    List findByCustomer(String customerCode);

    List search(String lastNameFragment);

    List findByAdvisorId(Long advisorId);

    List findAtRisk(double gpaThreshold);

    void save(Student student);

    void update(Student student);

    void delete(Student student);

    int countByCustomer(String customerCode);
}

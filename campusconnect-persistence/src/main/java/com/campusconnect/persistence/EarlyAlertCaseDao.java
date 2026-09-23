package com.campusconnect.persistence;

import java.util.List;

import com.campusconnect.domain.EarlyAlertCase;

public interface EarlyAlertCaseDao {
    EarlyAlertCase findById(Long id);
    List findOpenCases();
    List findByStudentId(Long studentId);
    void save(EarlyAlertCase alertCase);
    void update(EarlyAlertCase alertCase);
}

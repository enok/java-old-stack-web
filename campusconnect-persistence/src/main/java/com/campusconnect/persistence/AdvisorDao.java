package com.campusconnect.persistence;

import java.util.List;

import com.campusconnect.domain.Advisor;

public interface AdvisorDao {
    Advisor findById(Long id);
    Advisor findByStaffId(String staffId);
    List findActive();
    void save(Advisor advisor);
    void update(Advisor advisor);
}

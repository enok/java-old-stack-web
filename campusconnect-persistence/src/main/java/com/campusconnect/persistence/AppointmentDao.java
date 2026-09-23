package com.campusconnect.persistence;

import java.util.Date;
import java.util.List;

import com.campusconnect.domain.Appointment;

public interface AppointmentDao {
    Appointment findById(Long id);
    List findByStudentId(Long studentId);
    List findByAdvisorAndDay(Long advisorId, Date day);
    List findUpcoming(Date from, Date to);
    void save(Appointment appointment);
    void update(Appointment appointment);
    int countForAdvisorOnDay(Long advisorId, Date day);
}

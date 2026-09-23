package com.campusconnect.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.transaction.annotation.Transactional;

import com.campusconnect.common.CustomerContext;
import com.campusconnect.common.CustomerProperties;
import com.campusconnect.common.DateUtils;
import com.campusconnect.domain.Advisor;
import com.campusconnect.domain.Appointment;
import com.campusconnect.domain.Student;
import com.campusconnect.persistence.AdvisorDao;
import com.campusconnect.persistence.AppointmentDao;
import com.campusconnect.persistence.StudentDao;

/**
 * LEGACY SMELL #1, occurrence 4 of 6 (module: campusconnect-service).
 *
 * The scheduling rules are the single most forked piece of behaviour in the
 * product. Slot length, lead time, walk-ins and the daily cap differ per
 * customer, and the rules are expressed three times: here as an if-chain, in
 * the property files, and again in the JSP that renders the form.
 *
 * This is the class the modernization turns into a SchedulingPolicy Strategy.
 */
public class AppointmentService {

    private static final Logger LOG = Logger.getLogger(AppointmentService.class);

    private AppointmentDao appointmentDao;
    private StudentDao studentDao;
    private AdvisorDao advisorDao;

    public void setAppointmentDao(AppointmentDao appointmentDao) { this.appointmentDao = appointmentDao; }
    public void setStudentDao(StudentDao studentDao) { this.studentDao = studentDao; }
    public void setAdvisorDao(AdvisorDao advisorDao) { this.advisorDao = advisorDao; }

    public int slotMinutes() {
        String customerCode = CustomerContext.get();
        if ("RIVERTON".equals(customerCode)) {
            return 45;
        } else if ("SUMMIT".equals(customerCode)) {
            return 20;
        } else if ("NORTHLAKE".equals(customerCode)) {
            return 30;
        } else {
            return 30;
        }
    }

    public int leadTimeHours() {
        // The property file has this value too. They disagree at Summit and the
        // if-chain wins, which nobody expects.
        String customerCode = CustomerContext.get();
        if ("RIVERTON".equals(customerCode)) {
            return 48;
        } else if ("SUMMIT".equals(customerCode)) {
            return 4;
        } else {
            return 24;
        }
    }

    public boolean walkInsAllowed() {
        String customerCode = CustomerContext.get();
        if ("RIVERTON".equals(customerCode)) {
            return false;
        } else {
            return true;
        }
    }

    @Transactional(readOnly = true)
    public List listForStudent(Long studentId) {
        return appointmentDao.findByStudentId(studentId);
    }

    @Transactional(readOnly = true)
    public List listForAdvisorDay(Long advisorId, Date day) {
        return appointmentDao.findByAdvisorAndDay(advisorId, day);
    }

    /** Returns a raw List of validation messages; empty means booked. */
    @Transactional
    public List schedule(Long studentId, Long advisorId, Date startsAt, String reasonCode, boolean walkIn) {
        List errors = new ArrayList();

        Student student = studentDao.findById(studentId);
        Advisor advisor = advisorDao.findById(advisorId);
        if (student == null) {
            errors.add("Unknown student.");
        }
        if (advisor == null) {
            errors.add("Unknown advisor.");
        }
        if (startsAt == null) {
            errors.add("Start time is required.");
        }
        if (!errors.isEmpty()) {
            return errors;
        }

        if (walkIn && !walkInsAllowed()) {
            errors.add("Walk-in appointments are not permitted at this institution.");
            return errors;
        }

        long hoursAhead = (startsAt.getTime() - System.currentTimeMillis()) / (1000L * 60L * 60L);
        if (!walkIn && hoursAhead < leadTimeHours()) {
            errors.add("Appointments must be booked at least " + leadTimeHours() + " hours in advance.");
            return errors;
        }

        if (!DateUtils.isBusinessDay(startsAt)) {
            // Summit runs Saturday counseling hours. The others do not.
            if (!"SUMMIT".equals(CustomerContext.get())) {
                errors.add("Appointments can only be scheduled on business days.");
                return errors;
            }
        }

        int already = appointmentDao.countForAdvisorOnDay(advisorId, startsAt);
        int cap = CustomerProperties.getInt("appointment.maxPerDay", 12);
        if (already >= cap) {
            errors.add("This advisor is fully booked on that day.");
            return errors;
        }

        Appointment appointment = new Appointment();
        appointment.setCustomerCode(CustomerContext.get());
        appointment.setStudent(student);
        appointment.setAdvisor(advisor);
        appointment.setStartsAt(startsAt);
        appointment.setDurationMinutes(new Integer(slotMinutes()));
        appointment.setStatus(Appointment.STATUS_SCHEDULED);
        appointment.setReasonCode(reasonCode);
        appointment.setWalkIn(Boolean.valueOf(walkIn));
        appointment.setCreatedAt(new Date());

        // Location defaulting - another fork.
        String customerCode = CustomerContext.get();
        if ("RIVERTON".equals(customerCode)) {
            appointment.setLocation("Student Success Center");
        } else if ("SUMMIT".equals(customerCode)) {
            appointment.setLocation("Counseling Center");
        } else {
            appointment.setLocation(advisor.getDepartment());
        }

        appointmentDao.save(appointment);
        LOG.info("Scheduled appointment for student " + studentId + " with advisor " + advisorId);
        return errors;
    }

    @Transactional
    public void markOutcome(Long appointmentId, String status, String notes) {
        Appointment appointment = appointmentDao.findById(appointmentId);
        if (appointment == null) {
            LOG.warn("No appointment " + appointmentId);
            return;
        }
        appointment.setStatus(status);
        appointment.setNotes(notes);
        appointmentDao.update(appointment);
    }
}

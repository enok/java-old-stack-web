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
import com.campusconnect.finance.service.FinancialHoldService;
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
 *
 * It is ALSO the class that makes advising depend on Student Finance. Since
 * 2016 schedule() asks FinancialHoldService whether the student's balance
 * blocks the booking. There is no interface between them, no event and no
 * anti-corruption layer: this jar imports com.campusconnect.finance.service
 * directly and calls it inside the advising transaction.
 */
public class AppointmentService {

    private static final Logger LOG = Logger.getLogger(AppointmentService.class);

    private AppointmentDao appointmentDao;
    private StudentDao studentDao;
    private AdvisorDao advisorDao;

    /**
     * CROSS-DOMAIN DEPENDENCY. An advising service holding a finance service.
     * Wired by setter in applicationContext.xml like everything else, so the
     * coupling is invisible unless you read the XML.
     */
    private FinancialHoldService financialHoldService;

    public void setAppointmentDao(AppointmentDao appointmentDao) { this.appointmentDao = appointmentDao; }
    public void setStudentDao(StudentDao studentDao) { this.studentDao = studentDao; }
    public void setAdvisorDao(AdvisorDao advisorDao) { this.advisorDao = advisorDao; }
    public void setFinancialHoldService(FinancialHoldService s) { this.financialHoldService = s; }

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

        // ------------------------------------------------------------------
        // FINANCE CHECK INSIDE THE ADVISING RULES.
        //
        // A balance over the customer's threshold blocks the booking. The call
        // is direct and synchronous, it runs inside this @Transactional method
        // on the same Hibernate session, and the message it returns is rendered
        // by the advising JSP. Deleting finance from this build would not
        // compile; running finance as a separate service would need this to
        // become a remote call on the booking path.
        //
        // XXX CC-1451: the bursar's office asked for an override for hardship
        // cases in 2017. It was never built, so advisors telephone the bursar,
        // who releases the hold, who then re-places it the next night when
        // FinancialHoldService.evaluateAll() runs.
        // ------------------------------------------------------------------
        if (financialHoldService != null && financialHoldService.blocksAppointment(studentId, reasonCode)) {
            errors.add(financialHoldService.blockMessage(studentId));
            LOG.warn("Appointment blocked by financial hold for student " + studentId);
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

        // RIVERTON BRANCH ONLY: early-alert appointments are staff-booked.
        // Students hitting this path get a hard stop.
        if ("EARLY_ALERT".equals(reasonCode) && walkIn) {
            errors.add("Early-alert appointments must be booked by a Success Coach.");
            return errors;
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

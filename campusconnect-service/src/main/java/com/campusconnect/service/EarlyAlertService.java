package com.campusconnect.service;

import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.transaction.annotation.Transactional;

import com.campusconnect.common.CustomerContext;
import com.campusconnect.common.CustomerProperties;
import com.campusconnect.common.DateUtils;
import com.campusconnect.common.ServiceLocator;
import com.campusconnect.domain.EarlyAlertCase;
import com.campusconnect.domain.Student;
import com.campusconnect.persistence.EarlyAlertCaseDao;
import com.campusconnect.persistence.StudentDao;

/**
 * Early-alert case handling.
 *
 * The severity rules here are a near-copy of the ones inside StudentService.
 * They have drifted: StudentService uses 1.50 as Summit's HIGH cut-off, this
 * class uses 1.60. Nobody knows which is correct.
 */
public class EarlyAlertService {

    private static final Logger LOG = Logger.getLogger(EarlyAlertService.class);

    private EarlyAlertCaseDao earlyAlertCaseDao;
    private StudentDao studentDao;

    public void setEarlyAlertCaseDao(EarlyAlertCaseDao dao) { this.earlyAlertCaseDao = dao; }
    public void setStudentDao(StudentDao studentDao) { this.studentDao = studentDao; }

    @Transactional(readOnly = true)
    public List listOpenCases() {
        return earlyAlertCaseDao.findOpenCases();
    }

    @Transactional(readOnly = true)
    public List listForStudent(Long studentId) {
        return earlyAlertCaseDao.findByStudentId(studentId);
    }

    @Transactional
    public void openCase(Long studentId, String reason, String raisedBy) {
        Student student = studentDao.findById(studentId);
        if (student == null) {
            LOG.warn("Cannot open case, unknown student " + studentId);
            return;
        }
        EarlyAlertCase alertCase = new EarlyAlertCase();
        alertCase.setCustomerCode(CustomerContext.get());
        alertCase.setStudent(student);
        alertCase.setAdvisor(student.getAdvisor());
        alertCase.setReason(reason);
        alertCase.setSeverity(severityFor(student));
        alertCase.setStatus(EarlyAlertCase.STATUS_OPEN);
        alertCase.setRaisedBy(raisedBy);
        alertCase.setOpenedAt(new Date());
        earlyAlertCaseDao.save(alertCase);
        notifyAdvisor(alertCase);
    }

    @Transactional
    public void closeCase(Long caseId, String outcomeNotes) {
        EarlyAlertCase alertCase = earlyAlertCaseDao.findById(caseId);
        if (alertCase == null) {
            return;
        }
        alertCase.setStatus(EarlyAlertCase.STATUS_CLOSED);
        alertCase.setClosedAt(new Date());
        alertCase.setOutcomeNotes(outcomeNotes);
        earlyAlertCaseDao.update(alertCase);
    }

    /**
     * Nightly sweep. Escalates cases older than the configured window.
     * TODO CC-1265: this iterates every open case for every customer on one
     * thread and takes 40 minutes at Riverton.
     */
    @Transactional
    public int escalateStaleCases() {
        int escalated = 0;
        int windowDays = CustomerProperties.getInt("earlyalert.escalateAfterDays", 10);
        List open = earlyAlertCaseDao.findOpenCases();
        Iterator it = open.iterator();
        while (it.hasNext()) {
            EarlyAlertCase alertCase = (EarlyAlertCase) it.next();
            if (EarlyAlertCase.STATUS_ESCALATED.equals(alertCase.getStatus())) {
                continue;
            }
            int age = DateUtils.daysBetween(alertCase.getOpenedAt(), new Date());
            if (age >= windowDays) {
                alertCase.setStatus(EarlyAlertCase.STATUS_ESCALATED);
                earlyAlertCaseDao.update(alertCase);
                notifyAdvisor(alertCase);
                escalated++;
            }
        }
        LOG.info("Escalated " + escalated + " stale cases for " + CustomerContext.get());
        return escalated;
    }

    private String severityFor(Student student) {
        if (student.getGpa() == null) {
            return "MEDIUM";
        }
        double gpa = student.getGpa().doubleValue();
        String customerCode = CustomerContext.get();
        if ("RIVERTON".equals(customerCode)) {
            if (student.getAthleticsCode() != null && student.getAthleticsCode().length() > 0) {
                return "CRITICAL";
            }
            return gpa < 2.00d ? "HIGH" : "MEDIUM";
        } else if ("SUMMIT".equals(customerCode)) {
            // Drifted copy: StudentService uses 1.50 here.
            return gpa < 1.60d ? "HIGH" : "MEDIUM";
        } else {
            return gpa < 2.00d ? "HIGH" : "MEDIUM";
        }
    }

    private void notifyAdvisor(EarlyAlertCase alertCase) {
        if (!CustomerProperties.getBoolean("earlyalert.notifyAdvisor")) {
            return;
        }
        Object mailer = ServiceLocator.get("notificationService");
        if (!(mailer instanceof NotificationService)) {
            LOG.error("notificationService missing from ServiceLocator");
            return;
        }
        String to = alertCase.getAdvisor() == null
                ? CustomerProperties.get("support.email")
                : alertCase.getAdvisor().getEmail();
        ((NotificationService) mailer).send(to,
                "Early alert case " + alertCase.getStatus(),
                "Case for student "
                        + (alertCase.getStudent() == null ? "?" : alertCase.getStudent().getFullName())
                        + " is now " + alertCase.getStatus() + ".");
    }
}

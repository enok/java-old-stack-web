package com.campusconnect.service;

import java.math.BigDecimal;
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
import com.campusconnect.finance.domain.AccountCharge;
import com.campusconnect.finance.service.StudentAccountService;
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

    /** Second cross-domain dependency: an advising service holding a finance service. */
    private StudentAccountService studentAccountService;

    public void setEarlyAlertCaseDao(EarlyAlertCaseDao dao) { this.earlyAlertCaseDao = dao; }
    public void setStudentDao(StudentDao studentDao) { this.studentDao = studentDao; }
    public void setStudentAccountService(StudentAccountService s) { this.studentAccountService = s; }

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

    /**
     * ONE TRANSACTION, TWO BOUNDED CONTEXTS.
     *
     * Closing a REGISTRATION_HOLD early-alert case also posts a late-registration
     * fee to the student's finance account. Both writes - the UPDATE on
     * early_alert_case and the INSERT on account_charge - happen inside THIS
     * @Transactional method, on the same Hibernate session, under the same
     * HibernateTransactionManager, and commit or roll back together.
     *
     * That is not an accident of layering, it is the behaviour the registrar
     * signed off on in 2017: a case is never closed without the fee, and the
     * fee is never posted without the case being closed. Splitting finance into
     * its own service turns this single local transaction into a distributed
     * one, and there is no compensating action written anywhere for the half
     * that fails.
     *
     * XXX CC-1455: if StudentAccountService.postCharge() throws, the case stays
     * open and the advisor sees the Close button do nothing. It has happened
     * twice, both times because the account row did not exist yet.
     */
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

        // --- finance write, same transaction as the advising write above ---
        if (studentAccountService != null
                && alertCase.getStudent() != null
                && lateRegistrationFeeApplies(alertCase)) {
            BigDecimal fee = new BigDecimal(String.valueOf(
                    studentAccountService.lateFeeAmountFor(
                            studentAccountService.getAccountForStudent(alertCase.getStudent().getId()))));
            studentAccountService.postCharge(
                    alertCase.getStudent().getId(),
                    AccountCharge.TYPE_LATE_FEE,
                    fee,
                    "Late registration fee, early-alert case " + caseId,
                    "EA-" + caseId);
            LOG.info("Posted late-registration fee for case " + caseId);
        }
    }

    /**
     * Per-customer rule for whether closing a case bills the student.
     * Summit never charges; Riverton charges unless the student is an athlete;
     * Northlake charges whenever the case was raised for registration reasons.
     */
    private boolean lateRegistrationFeeApplies(EarlyAlertCase alertCase) {
        String customerCode = CustomerContext.get();
        if ("SUMMIT".equals(customerCode)) {
            return false;
        }
        Student student = alertCase.getStudent();
        if ("RIVERTON".equals(customerCode)) {
            if (student != null && student.getAthleticsCode() != null
                    && student.getAthleticsCode().length() > 0) {
                return false;
            }
            return true;
        }
        String reason = alertCase.getReason();
        // Matching on the free-text reason string. It stops
        // matching every time someone rewords the automatic case text.
        return reason != null && reason.indexOf("registration") >= 0;
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

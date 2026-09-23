package com.campusconnect.finance.service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.transaction.annotation.Transactional;

import com.campusconnect.common.CustomerContext;
import com.campusconnect.common.CustomerProperties;
import com.campusconnect.common.ServiceLocator;
import com.campusconnect.common.StringHelper;
import com.campusconnect.domain.Student;
import com.campusconnect.finance.domain.FinancialHold;
import com.campusconnect.finance.domain.StudentAccount;
import com.campusconnect.finance.persistence.FinancialHoldDao;
import com.campusconnect.finance.persistence.StudentAccountDao;
import com.campusconnect.persistence.StudentDao;
import com.campusconnect.service.NotificationService;

/**
 * Financial holds: evaluate the per-customer balance threshold, place a hold,
 * release it.
 *
 * This is the class ADVISING calls. AppointmentService.schedule() asks
 * blocksAppointment() before it books anything, with no interface, no event and
 * no queue in between - a direct compile-time dependency from the advising
 * service on the finance service, in the same jar.
 *
 * LEGACY SMELL #5: it resolves the mailer through the static ServiceLocator,
 * exactly like StudentService and EarlyAlertService do.
 */
public class FinancialHoldService {

    private static final Logger LOG = Logger.getLogger(FinancialHoldService.class);

    private FinancialHoldDao financialHoldDao;
    private StudentAccountDao studentAccountDao;
    private StudentDao studentDao;

    public void setFinancialHoldDao(FinancialHoldDao dao) { this.financialHoldDao = dao; }
    public void setStudentAccountDao(StudentAccountDao dao) { this.studentAccountDao = dao; }
    public void setStudentDao(StudentDao studentDao) { this.studentDao = studentDao; }

    // ------------------------------------------------------------------
    // Threshold
    // ------------------------------------------------------------------

    /**
     * The per-customer balance at which a hold is placed.
     *
     * LEGACY SMELL #1, finance occurrence: the numbers are in the three
     * customer-*.properties files AND defaulted in the if-chain below. When a
     * key is missing, CustomerProperties silently falls back to Northlake's
     * value, so a new customer quietly inherits a 500.00 threshold.
     */
    public double holdThreshold() {
        String configured = CustomerProperties.get("billing.holdThreshold");
        if (configured != null) {
            try {
                return Double.parseDouble(configured.trim());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        String customerCode = CustomerContext.get();
        if ("RIVERTON".equals(customerCode)) {
            return 1000.00d;
        } else if ("SUMMIT".equals(customerCode)) {
            return 250.00d;
        } else {
            return 500.00d;
        }
    }

    @Transactional(readOnly = true)
    public FinancialHold getActiveHold(Long studentId) {
        return financialHoldDao.findActiveForStudent(studentId);
    }

    @Transactional(readOnly = true)
    public List listActiveHolds() {
        return financialHoldDao.findActive();
    }

    /**
     * THE ADVISING ENTRY POINT.
     *
     * AppointmentService calls this before it books an appointment. It is a
     * finance rule answering an advising question, and the answer differs per
     * customer: Riverton exempts athletes because the compliance office settles
     * their accounts separately, Summit only blocks EARLY_ALERT appointments.
     *
     * The reason string returned here is rendered straight into the
     * advising error list on appointmentDay.jsp, so a change to this text is a
     * change to an advising screen.
     */
    @Transactional(readOnly = true)
    public boolean blocksAppointment(Long studentId, String reasonCode) {
        FinancialHold hold = financialHoldDao.findActiveForStudent(studentId);
        if (hold == null) {
            return false;
        }

        String customerCode = CustomerContext.get();
        if ("RIVERTON".equals(customerCode)) {
            if (CustomerProperties.getBoolean("billing.athleteHoldExempt")) {
                Student student = studentDao.findById(studentId);
                if (student != null && !StringHelper.isEmpty(student.getAthleticsCode())) {
                    LOG.info("Hold ignored for Riverton athlete " + studentId);
                    return false;
                }
            }
            return true;
        } else if ("SUMMIT".equals(customerCode)) {
            // Summit lets a student on hold keep a counseling appointment unless
            // it was raised by the early-alert process.
            return "EARLY_ALERT".equals(reasonCode);
        }
        return true;
    }

    public String blockMessage(Long studentId) {
        FinancialHold hold = financialHoldDao.findActiveForStudent(studentId);
        double threshold = holdThreshold();
        StringBuffer sb = new StringBuffer();
        sb.append("A financial hold is on this student's account");
        if (hold != null && hold.getBalanceAtPlacement() != null) {
            sb.append(" (balance ").append(hold.getBalanceAtPlacement().toString());
            sb.append(", threshold ").append(threshold).append(")");
        }
        sb.append(". Contact the bursar before scheduling.");
        return sb.toString();
    }

    // ------------------------------------------------------------------
    // Place / release
    // ------------------------------------------------------------------

    /** Evaluates one account and places a hold if it is over the threshold. */
    @Transactional
    public FinancialHold evaluate(Long studentId) {
        StudentAccount account = studentAccountDao.findByStudentId(studentId);
        if (account == null || account.getBalance() == null) {
            return null;
        }
        FinancialHold existing = financialHoldDao.findActiveForStudent(studentId);
        double balance = account.getBalance().doubleValue();
        double threshold = holdThreshold();

        if (balance <= threshold) {
            if (existing != null) {
                release(existing.getId(), "Balance fell below threshold");
            }
            return null;
        }
        if (existing != null) {
            return existing;
        }
        return place(account, FinancialHold.REASON_BALANCE, "system");
    }

    @Transactional
    public FinancialHold place(StudentAccount account, String reasonCode, String placedBy) {
        if (account == null) {
            LOG.warn("place called with null account");
            return null;
        }
        FinancialHold hold = new FinancialHold();
        hold.setCustomerCode(CustomerContext.get());
        hold.setAccount(account);
        hold.setStudent(account.getStudent());
        hold.setReasonCode(reasonCode);
        hold.setThresholdAmount(new BigDecimal(String.valueOf(holdThreshold())));
        hold.setBalanceAtPlacement(account.getBalance());
        hold.setPlacedBy(placedBy);
        hold.setPlacedAt(new Date());
        financialHoldDao.save(hold);
        notifyAdvisor(hold);
        return hold;
    }

    @Transactional
    public void release(Long holdId, String notes) {
        FinancialHold hold = financialHoldDao.findById(holdId);
        if (hold == null) {
            return;
        }
        hold.setReleasedAt(new Date());
        hold.setNotes(notes);
        financialHoldDao.update(hold);
    }

    /**
     * Nightly sweep over every account, mirroring EarlyAlertService.escalateStaleCases().
     * Same shape, same single thread, same 40 minutes at Riverton.
     */
    @Transactional
    public int evaluateAll() {
        int placed = 0;
        List accounts = studentAccountDao.findWithBalanceOver(holdThreshold());
        Iterator it = accounts.iterator();
        while (it.hasNext()) {
            StudentAccount account = (StudentAccount) it.next();
            if (account.getStudent() == null) {
                continue;
            }
            FinancialHold existing = financialHoldDao.findActiveForStudent(account.getStudent().getId());
            if (existing != null) {
                continue;
            }
            place(account, FinancialHold.REASON_BALANCE, "system");
            placed++;
        }
        LOG.info("Placed " + placed + " financial holds for " + CustomerContext.get());
        return placed;
    }

    /**
     * The advisor, not the bursar, is the one told about a hold - because the
     * advisor is the one who will be turned away at the scheduling screen.
     * Resolved through the static registry, like the rest of the application.
     */
    private void notifyAdvisor(FinancialHold hold) {
        Object mailer = ServiceLocator.get("notificationService");
        if (!(mailer instanceof NotificationService)) {
            LOG.error("notificationService missing from ServiceLocator");
            return;
        }
        String to = CustomerProperties.get("support.email");
        if (hold.getStudent() != null && hold.getStudent().getAdvisor() != null) {
            to = hold.getStudent().getAdvisor().getEmail();
        }
        ((NotificationService) mailer).send(to,
                "Financial hold placed",
                "A financial hold was placed on the account of "
                        + (hold.getStudent() == null ? "?" : hold.getStudent().getFullName())
                        + ". Appointments may be blocked.");
    }
}

package com.campusconnect.finance.service;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.transaction.annotation.Transactional;

import com.campusconnect.common.CustomerContext;
import com.campusconnect.common.CustomerProperties;
import com.campusconnect.common.DateUtils;
import com.campusconnect.common.StringHelper;
import com.campusconnect.domain.Student;
import com.campusconnect.finance.domain.AccountCharge;
import com.campusconnect.finance.domain.AccountPayment;
import com.campusconnect.finance.domain.StudentAccount;
import com.campusconnect.finance.persistence.AccountChargeDao;
import com.campusconnect.finance.persistence.AccountPaymentDao;
import com.campusconnect.finance.persistence.StudentAccountDao;
import com.campusconnect.finance.persistence.jdbc.JdbcBillingExposureDao;
import com.campusconnect.persistence.StudentDao;

/**
 * Student Finance: balances, charges, payments.
 *
 * Wired by XML in applicationContext.xml next to the advising services, sharing
 * their HibernateTransactionManager and their SessionFactory. It also holds a
 * StudentDao - the ADVISING dao - because finance has no student record of its
 * own and has to go and fetch one.
 *
 * Carries the same set of 2014 habits as the advising services on purpose:
 * raw types, hand-rolled loops, a shared static SimpleDateFormat, swallowed
 * exceptions and per-customer if-chains.
 */
public class StudentAccountService {

    private static final Logger LOG = Logger.getLogger(StudentAccountService.class);

    /** Shared mutable formatter on a service used by every request thread. */
    private static final SimpleDateFormat STATEMENT_DATE = new SimpleDateFormat("MMMM d, yyyy");

    private StudentAccountDao studentAccountDao;
    private AccountChargeDao accountChargeDao;
    private AccountPaymentDao accountPaymentDao;
    private JdbcBillingExposureDao billingExposureDao;
    private StudentDao studentDao;

    public void setStudentAccountDao(StudentAccountDao dao) { this.studentAccountDao = dao; }
    public void setAccountChargeDao(AccountChargeDao dao) { this.accountChargeDao = dao; }
    public void setAccountPaymentDao(AccountPaymentDao dao) { this.accountPaymentDao = dao; }
    public void setBillingExposureDao(JdbcBillingExposureDao dao) { this.billingExposureDao = dao; }
    public void setStudentDao(StudentDao studentDao) { this.studentDao = studentDao; }

    // ------------------------------------------------------------------
    // Read paths
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public StudentAccount getAccountForStudent(Long studentId) {
        return studentAccountDao.findByStudentId(studentId);
    }

    @Transactional(readOnly = true)
    public BigDecimal getBalance(Long studentId) {
        StudentAccount account = studentAccountDao.findByStudentId(studentId);
        if (account == null || account.getBalance() == null) {
            return BigDecimal.ZERO;
        }
        return account.getBalance();
    }

    @Transactional(readOnly = true)
    public List listCharges(Long accountId) {
        return accountChargeDao.findByAccountId(accountId);
    }

    @Transactional(readOnly = true)
    public List listPayments(Long accountId) {
        return accountPaymentDao.findByAccountId(accountId);
    }

    /**
     * Statement model for the billing screen. Returns a raw Map of Strings,
     * because that is what the JSP expects and nobody wrote a view object.
     */
    @Transactional(readOnly = true)
    public Map buildStatement(Long studentId) {
        Map statement = new HashMap();
        StudentAccount account = studentAccountDao.findByStudentId(studentId);
        if (account == null) {
            statement.put("balanceLabel", formatMoney(BigDecimal.ZERO));
            statement.put("charges", new ArrayList());
            statement.put("payments", new ArrayList());
            statement.put("asOf", STATEMENT_DATE.format(new Date()));
            statement.put("layout", statementLayout());
            return statement;
        }
        statement.put("account", account);
        statement.put("charges", accountChargeDao.findByAccountId(account.getId()));
        statement.put("payments", accountPaymentDao.findByAccountId(account.getId()));
        statement.put("balanceLabel", formatMoney(account.getBalance()));
        statement.put("asOf", STATEMENT_DATE.format(new Date()));
        statement.put("layout", statementLayout());

        // Cross-domain read: the statement screen shows the student's ADVISING
        // appointments next to the money, because the bursar asked for it once.
        statement.put("exposure", billingExposureDao.findExposureForStudent(studentId));
        return statement;
    }

    // ------------------------------------------------------------------
    // Write paths
    // ------------------------------------------------------------------

    @Transactional
    public StudentAccount ensureAccount(Student student) {
        if (student == null) {
            return null;
        }
        StudentAccount account = studentAccountDao.findByStudentId(student.getId());
        if (account != null) {
            return account;
        }
        account = new StudentAccount();
        account.setCustomerCode(CustomerContext.get());
        account.setStudent(student);
        account.setTermCode(CustomerProperties.get("term.current"));
        account.setBalance(BigDecimal.ZERO);
        account.setStatus(StudentAccount.STATUS_OPEN);
        account.setCreatedAt(new Date());
        account.setUpdatedAt(new Date());
        studentAccountDao.save(account);
        LOG.info("Opened student account for student " + student.getId() + " at " + CustomerContext.get());
        return account;
    }

    @Transactional
    public AccountCharge postCharge(Long studentId, String chargeType, BigDecimal amount,
                                    String description, String sourceRef) {
        Student student = studentDao.findById(studentId);
        if (student == null) {
            LOG.warn("Cannot post charge, unknown student " + studentId);
            return null;
        }
        StudentAccount account = ensureAccount(student);

        AccountCharge charge = new AccountCharge();
        charge.setCustomerCode(CustomerContext.get());
        charge.setAccount(account);
        charge.setStudentId(studentId);
        charge.setChargeType(chargeType);
        charge.setAmount(amount);
        charge.setTermCode(CustomerProperties.get("term.current"));
        charge.setDescription(description);
        charge.setSourceRef(sourceRef);
        charge.setPostedAt(new Date());
        accountChargeDao.save(charge);

        account.setLastChargeAt(new Date());
        account.setUpdatedAt(new Date());
        studentAccountDao.update(account);

        recomputeBalance(account.getId());
        return charge;
    }

    @Transactional
    public List postPayment(Long studentId, BigDecimal amount, String method,
                            String referenceNo, String receivedBy) {
        List errors = new ArrayList();
        if (amount == null) {
            errors.add("Payment amount is required.");
            return errors;
        }
        if (amount.signum() <= 0) {
            errors.add("Payment amount must be greater than zero.");
            return errors;
        }
        if (StringHelper.isEmpty(method)) {
            errors.add("Payment method is required.");
            return errors;
        }
        Student student = studentDao.findById(studentId);
        if (student == null) {
            errors.add("Unknown student.");
            return errors;
        }

        // Riverton refuses card payments at the counter; the bursar's terminal is
        // a separate system and the reconciliation was never built.
        if ("RIVERTON".equals(CustomerContext.get()) && AccountPayment.METHOD_CARD.equals(method)) {
            errors.add("Card payments are not accepted at Riverton. Use the payment portal.");
            return errors;
        }

        StudentAccount account = ensureAccount(student);

        AccountPayment payment = new AccountPayment();
        payment.setCustomerCode(CustomerContext.get());
        payment.setAccount(account);
        payment.setAmount(amount);
        payment.setPaymentMethod(method);
        payment.setReferenceNo(referenceNo);
        payment.setReceivedBy(receivedBy);
        payment.setPostedAt(new Date());
        accountPaymentDao.save(payment);

        account.setLastPaymentAt(new Date());
        account.setUpdatedAt(new Date());
        studentAccountDao.update(account);

        recomputeBalance(account.getId());
        return errors;
    }

    /**
     * Recomputes the running balance from the ledger.
     *
     * XXX: MONEY IN A DOUBLE. The columns are DECIMAL(12,2) and the entities use
     * BigDecimal, but the sum below is accumulated in a primitive double because
     * "BigDecimal.add in a loop was slow on the 2016 box". At Riverton, where a
     * full-time student carries ~60 ledger rows a year, the recomputed balance
     * and the hand-totalled one disagree by a cent or two often enough that the
     * bursar keeps a spreadsheet. CC-1440, open.
     */
    @Transactional
    public BigDecimal recomputeBalance(Long accountId) {
        StudentAccount account = studentAccountDao.findById(accountId);
        if (account == null) {
            LOG.warn("recomputeBalance: no account " + accountId);
            return BigDecimal.ZERO;
        }

        double running = 0.0d;

        List charges = accountChargeDao.findByAccountId(accountId);
        Iterator ci = charges.iterator();
        while (ci.hasNext()) {
            AccountCharge charge = (AccountCharge) ci.next();
            if (charge.getAmount() != null) {
                running = running + charge.getAmount().doubleValue();
            }
        }

        List payments = accountPaymentDao.findByAccountId(accountId);
        Iterator pi = payments.iterator();
        while (pi.hasNext()) {
            AccountPayment payment = (AccountPayment) pi.next();
            if (payment.getAmount() != null) {
                running = running - payment.getAmount().doubleValue();
            }
        }

        BigDecimal balance = new BigDecimal(String.valueOf(running));
        account.setBalance(balance);
        account.setRecomputedAt(new Date());
        account.setUpdatedAt(new Date());
        if (running > 0.0d) {
            account.setStatus(StudentAccount.STATUS_PAST_DUE);
        } else {
            account.setStatus(StudentAccount.STATUS_OPEN);
        }
        studentAccountDao.update(account);
        return balance;
    }

    /**
     * Called by the nightly SIS job AFTER the student import, so the batch run
     * spans advising and finance. Opens a missing account for every imported
     * student and posts the term's late fee where the grace period has expired.
     *
     * It is not annotated @Transactional because the importers
     * that run before it are not either, and making only this half transactional
     * made the failure modes harder to explain rather than easier.
     */
    public int reconcileAfterImport() {
        int touched = 0;
        try {
            List students = studentDao.findByCustomer(CustomerContext.get());
            List pastDueAccountIds = new ArrayList();

            int graceDays = CustomerProperties.getInt("billing.gracePeriodDays", 10);
            Iterator it = students.iterator();
            while (it.hasNext()) {
                Student student = (Student) it.next();
                StudentAccount account = ensureAccount(student);
                touched++;

                if (account.getBalance() == null || account.getBalance().signum() <= 0) {
                    continue;
                }
                Date since = account.getLastPaymentAt() == null
                        ? account.getCreatedAt()
                        : account.getLastPaymentAt();
                if (DateUtils.daysBetween(since, new Date()) >= graceDays) {
                    pastDueAccountIds.add(account.getId());
                }
            }

            if (!pastDueAccountIds.isEmpty()) {
                // Straight into the raw-JDBC DAO, which autocommits outside any
                // transaction the caller may have opened.
                double fee = lateFeeAmountFor(null);
                int posted = billingExposureDao.postLateFeeBatch(pastDueAccountIds, fee,
                        CustomerProperties.get("term.current"),
                        "Late payment fee, nightly reconcile");
                LOG.info("Nightly reconcile posted " + posted + " late fees for " + CustomerContext.get());
            }
        } catch (Exception e) {
            // The batch wrapper still reports success. Same as everywhere else.
            e.printStackTrace();
        }
        return touched;
    }

    // ------------------------------------------------------------------
    // Per-customer money rules - the finance half of the fork
    // ------------------------------------------------------------------

    /**
     * LEGACY SMELL #1, the finance occurrence. The late-fee rule is expressed
     * here as an if-chain AND in the three customer-*.properties files, and the
     * two disagree at Summit exactly the way the appointment lead time does.
     *
     * The percentage branch is computed in a double, deliberately.
     */
    public double lateFeeAmountFor(StudentAccount account) {
        String customerCode = CustomerContext.get();
        double flat = 25.0d;
        String configured = CustomerProperties.get("billing.lateFeeAmount");
        if (configured != null) {
            try {
                flat = Double.parseDouble(configured);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if ("RIVERTON".equals(customerCode)) {
            // Riverton charges a percentage of the outstanding balance, floored
            // at the flat amount. Money arithmetic in binary floating point.
            double percent = 1.5d;
            String rawPercent = CustomerProperties.get("billing.lateFeePercent");
            if (rawPercent != null) {
                try {
                    percent = Double.parseDouble(rawPercent);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            double balance = 0.0d;
            if (account != null && account.getBalance() != null) {
                balance = account.getBalance().doubleValue();
            }
            double computed = (balance * percent) / 100.0d;
            return computed < flat ? flat : computed;
        } else if ("SUMMIT".equals(customerCode)) {
            // Summit caps the late fee at ten dollars by policy, whatever the
            // property file says. The property file says 10.00 too, for now.
            return flat > 10.0d ? 10.0d : flat;
        } else {
            return flat;
        }
    }

    /** SUMMIT groups its statement by term; the other two list a flat ledger. */
    public String statementLayout() {
        String configured = CustomerProperties.get("billing.statementFormat");
        if ("SUMMIT".equals(CustomerContext.get())) {
            return configured == null ? "SUMMIT_TERM_GROUPED" : configured;
        }
        return configured == null ? "STANDARD" : configured;
    }

    /**
     * Money formatting, by hand, with no NumberFormat and no locale - the exact
     * shape of StudentService.formatGpa(). StudentService has its own copy of
     * this method, which is the duplication the modernization has to reconcile.
     */
    public String formatMoney(BigDecimal amount) {
        if (amount == null) {
            return "0.00";
        }
        String symbol = CustomerProperties.get("billing.currencySymbol");
        if (symbol == null) {
            symbol = "$";
        }
        String raw = amount.toString();
        int dot = raw.indexOf('.');
        if (dot < 0) {
            return symbol + raw + ".00";
        }
        while (raw.length() < dot + 3) {
            raw = raw + "0";
        }
        return symbol + raw.substring(0, dot + 3);
    }
}

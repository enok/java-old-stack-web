package com.campusconnect.service;

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
import com.campusconnect.common.ServiceLocator;
import com.campusconnect.common.StringHelper;
import com.campusconnect.domain.Advisor;
import com.campusconnect.domain.EarlyAlertCase;
import com.campusconnect.domain.Student;
import com.campusconnect.domain.sis.SisStudentRecord;
import com.campusconnect.persistence.AdvisorDao;
import com.campusconnect.persistence.EarlyAlertCaseDao;
import com.campusconnect.persistence.StudentDao;
import com.campusconnect.persistence.jdbc.JdbcEnrollmentDao;

/**
 * LEGACY SMELL #3: THE GOD CLASS.
 *
 * In one type this class does persistence orchestration, field validation,
 * display formatting, e-mail body composition, early-alert business rules,
 * SIS record mapping and per-customer branching. It is the single hardest
 * class in the repository to change safely, and it has no tests.
 *
 * It also carries occurrence 3 of 6 of LEGACY SMELL #1 (customer branching),
 * LEGACY SMELL #4 (raw types, Vector-era idioms, shared SimpleDateFormat,
 * swallowed exceptions), LEGACY SMELL #5 (ServiceLocator lookups) and
 * LEGACY SMELL #7 (a write path with no transaction boundary).
 *
 * The modernization does NOT start by splitting this class. It starts by
 * pinning its current behaviour with characterization tests. See
 * docs/MODERNIZATION-BACKLOG.md item B-01.
 */
public class StudentService {

    private static final Logger LOG = Logger.getLogger(StudentService.class);

    /** LEGACY SMELL #4: shared mutable formatter on a service used by every request. */
    private static final SimpleDateFormat EMAIL_DATE = new SimpleDateFormat("MMMM d, yyyy");

    private static final String[] VALID_STATUSES = { "ACTIVE", "PROBATION", "WITHDRAWN", "GRADUATED", "LEAVE" };

    private StudentDao studentDao;
    private AdvisorDao advisorDao;
    private EarlyAlertCaseDao earlyAlertCaseDao;
    private JdbcEnrollmentDao enrollmentDao;

    public void setStudentDao(StudentDao studentDao) { this.studentDao = studentDao; }
    public void setAdvisorDao(AdvisorDao advisorDao) { this.advisorDao = advisorDao; }
    public void setEarlyAlertCaseDao(EarlyAlertCaseDao dao) { this.earlyAlertCaseDao = dao; }
    public void setEnrollmentDao(JdbcEnrollmentDao enrollmentDao) { this.enrollmentDao = enrollmentDao; }

    // ------------------------------------------------------------------
    // Read paths
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Student getStudent(Long id) {
        Student s = studentDao.findById(id);
        if (s == null) {
            LOG.warn("Student not found: " + id);
        }
        return s;
    }

    @Transactional(readOnly = true)
    public List searchStudents(String lastNameFragment) {
        List results = studentDao.search(lastNameFragment);

        // LEGACY SMELL #1, occurrence 3 of 6 (module: campusconnect-service).
        // Summit wants withdrawn students hidden from search; Riverton wants them
        // last; Northlake wants them mixed in. Three behaviours, one if-chain.
        String customerCode = CustomerContext.get();
        if ("SUMMIT".equals(customerCode)) {
            List filtered = new ArrayList();
            Iterator it = results.iterator();
            while (it.hasNext()) {
                Student s = (Student) it.next();
                if (!"WITHDRAWN".equals(s.getEnrollmentStatus())) {
                    filtered.add(s);
                }
            }
            return filtered;
        } else if ("RIVERTON".equals(customerCode)) {
            List active = new ArrayList();
            List inactive = new ArrayList();
            Iterator it = results.iterator();
            while (it.hasNext()) {
                Student s = (Student) it.next();
                if ("WITHDRAWN".equals(s.getEnrollmentStatus())) {
                    inactive.add(s);
                } else {
                    active.add(s);
                }
            }
            active.addAll(inactive);
            return active;
        } else {
            return results;
        }
    }

    @Transactional(readOnly = true)
    public List getEnrollments(Long studentId) {
        // Crosses from Hibernate into the raw-JDBC DAO mid-transaction. The JDBC
        // side opens its own connection, so this read is not in the same
        // transaction and can see rows the Hibernate session cannot.
        return enrollmentDao.findByStudentId(studentId);
    }

    @Transactional(readOnly = true)
    public Map getCaseloadSummary(Long advisorId) {
        Map summary = new HashMap();
        List students = studentDao.findByAdvisorId(advisorId);
        int atRisk = 0;
        int probation = 0;
        double gpaTotal = 0.0d;
        int gpaCount = 0;
        for (int i = 0; i < students.size(); i++) {
            Student s = (Student) students.get(i);
            if (s.getGpa() != null) {
                gpaTotal = gpaTotal + s.getGpa().doubleValue();
                gpaCount++;
                if (s.getGpa().doubleValue() < riskThreshold()) {
                    atRisk++;
                }
            }
            if ("PROBATION".equals(s.getEnrollmentStatus())) {
                probation++;
            }
        }
        summary.put("total", new Integer(students.size()));
        summary.put("atRisk", new Integer(atRisk));
        summary.put("probation", new Integer(probation));
        summary.put("averageGpa", new Double(gpaCount == 0 ? 0.0d : gpaTotal / gpaCount));
        summary.put("asOf", DateUtils.formatDisplay(new Date()));
        return summary;
    }

    // ------------------------------------------------------------------
    // Validation - belongs in its own type, lives here
    // ------------------------------------------------------------------

    /** Returns a raw List of error strings. No error codes, no i18n. */
    public List validate(Student student) {
        List errors = new ArrayList();
        if (student == null) {
            errors.add("Student is required.");
            return errors;
        }
        if (StringHelper.isEmpty(student.getSisId())) {
            errors.add("SIS ID is required.");
        }
        if (StringHelper.isEmpty(student.getLastName())) {
            errors.add("Last name is required.");
        }
        if (StringHelper.isEmpty(student.getEmail())) {
            errors.add("Email is required.");
        } else if (student.getEmail().indexOf('@') < 0) {
            // TODO CC-1090: replace with a real validator. This rejects nothing.
            errors.add("Email looks invalid.");
        }
        if (student.getGpa() != null) {
            double gpa = student.getGpa().doubleValue();
            if (gpa < 0.0d || gpa > 4.0d) {
                errors.add("GPA must be between 0.00 and 4.00.");
            }
        }
        if (!StringHelper.isEmpty(student.getEnrollmentStatus())) {
            boolean known = false;
            for (int i = 0; i < VALID_STATUSES.length; i++) {
                if (VALID_STATUSES[i].equals(student.getEnrollmentStatus())) {
                    known = true;
                }
            }
            if (!known) {
                errors.add("Unknown enrollment status: " + student.getEnrollmentStatus());
            }
        }

        // Customer rule folded into generic validation.
        if ("RIVERTON".equals(CustomerContext.get())) {
            if (StringHelper.isEmpty(student.getProgramCode())) {
                errors.add("Program code is required at Riverton.");
            }
        } else if ("SUMMIT".equals(CustomerContext.get())) {
            if (StringHelper.isEmpty(student.getHomeCampus())) {
                errors.add("Home campus is required at Summit.");
            }
        }
        return errors;
    }

    // ------------------------------------------------------------------
    // Write paths
    // ------------------------------------------------------------------

    @Transactional
    public List saveStudent(Student student) {
        List errors = validate(student);
        if (!errors.isEmpty()) {
            return errors;
        }
        Date now = new Date();
        if (student.getId() == null) {
            student.setCustomerCode(CustomerContext.get());
            student.setCreatedAt(now);
            student.setUpdatedAt(now);
            studentDao.save(student);
        } else {
            student.setUpdatedAt(now);
            studentDao.update(student);
        }
        evaluateEarlyAlert(student);
        return errors;
    }

    /**
     * LEGACY SMELL #7: NO TRANSACTION BOUNDARY.
     *
     * This is the method the nightly SIS import calls for every record. It writes
     * through Hibernate AND through the raw JDBC DAO, and it is deliberately not
     * annotated @Transactional. Each Hibernate write auto-flushes on its own and
     * the JDBC writes autocommit, so a failure halfway through a file leaves the
     * database in a state no one can describe.
     */
    public void importSisRecord(SisStudentRecord record) {
        try {
            Student existing = studentDao.findBySisId(record.getSisId());
            Student student = existing == null ? new Student() : existing;

            student.setCustomerCode(CustomerContext.get());
            student.setSisId(record.getSisId());
            student.setFirstName(record.getFirstName());
            student.setLastName(record.getLastName());
            student.setEmail(record.getEmail());
            student.setPhone(record.getPhone());
            student.setBirthDate(DateUtils.parseSis(record.getBirthDate()));
            student.setEnrollmentStatus(mapStatus(record.getEnrollmentStatus()));
            student.setProgramCode(record.getProgramCode());

            if (!StringHelper.isEmpty(record.getGpa())) {
                student.setGpa(new Double(Double.parseDouble(record.getGpa())));
            }
            if (!StringHelper.isEmpty(record.getCreditsCompleted())) {
                student.setCreditsCompleted(new Integer(Integer.parseInt(record.getCreditsCompleted())));
            }
            if (!StringHelper.isEmpty(record.getAdvisorStaffId())) {
                Advisor advisor = advisorDao.findByStaffId(record.getAdvisorStaffId());
                if (advisor != null) {
                    student.setAdvisor(advisor);
                }
            }

            student.setUpdatedAt(new Date());
            if (student.getId() == null) {
                student.setCreatedAt(new Date());
                studentDao.save(student);
            } else {
                studentDao.update(student);
            }

            evaluateEarlyAlert(student);
        } catch (Exception e) {
            // LEGACY SMELL #4. The import reports "0 errors" no matter what happens.
            e.printStackTrace();
        }
    }

    // ------------------------------------------------------------------
    // Early-alert rules - should be in EarlyAlertService, duplicated here
    // ------------------------------------------------------------------

    private double riskThreshold() {
        String configured = CustomerProperties.get("earlyalert.autoCaseThreshold");
        if (configured == null) {
            return 2.00d;
        }
        try {
            return Double.parseDouble(configured);
        } catch (Exception e) {
            e.printStackTrace();
            return 2.00d;
        }
    }

    private void evaluateEarlyAlert(Student student) {
        if (student.getGpa() == null) {
            return;
        }
        if (student.getGpa().doubleValue() >= riskThreshold()) {
            return;
        }

        EarlyAlertCase alertCase = new EarlyAlertCase();
        alertCase.setCustomerCode(CustomerContext.get());
        alertCase.setStudent(student);
        alertCase.setAdvisor(student.getAdvisor());
        alertCase.setReason("GPA below " + riskThreshold() + " threshold");
        alertCase.setStatus(EarlyAlertCase.STATUS_OPEN);
        alertCase.setRaisedBy("system");
        alertCase.setOpenedAt(new Date());

        // Severity rules diverge per customer, again.
        String customerCode = CustomerContext.get();
        if ("RIVERTON".equals(customerCode)) {
            if (!StringHelper.isEmpty(student.getAthleticsCode())) {
                alertCase.setSeverity("CRITICAL");
            } else {
                alertCase.setSeverity("HIGH");
            }
        } else if ("SUMMIT".equals(customerCode)) {
            alertCase.setSeverity(student.getGpa().doubleValue() < 1.50d ? "HIGH" : "MEDIUM");
        } else {
            alertCase.setSeverity("HIGH");
        }

        earlyAlertCaseDao.save(alertCase);

        // LEGACY SMELL #5: reaches into the static registry instead of being injected.
        Object mailer = ServiceLocator.get("notificationService");
        if (mailer instanceof NotificationService) {
            ((NotificationService) mailer).send(
                    advisorEmailFor(student),
                    buildAlertSubject(student),
                    buildAlertBody(student, alertCase));
        }
    }

    private String advisorEmailFor(Student student) {
        if (student.getAdvisor() != null && !StringHelper.isEmpty(student.getAdvisor().getEmail())) {
            return student.getAdvisor().getEmail();
        }
        String fallback = CustomerProperties.get("support.email");
        return fallback == null ? "noreply@campusconnect.example.edu" : fallback;
    }

    // ------------------------------------------------------------------
    // E-mail text - presentation concern inside a service
    // ------------------------------------------------------------------

    private String buildAlertSubject(Student student) {
        StringBuffer sb = new StringBuffer();
        sb.append("[");
        sb.append(CustomerProperties.get("customer.displayName"));
        sb.append("] Early alert: ");
        sb.append(student.getLastName());
        sb.append(", ");
        sb.append(student.getFirstName());
        return sb.toString();
    }

    private String buildAlertBody(Student student, EarlyAlertCase alertCase) {
        StringBuffer sb = new StringBuffer();
        sb.append("An early-alert case was opened automatically on ");
        // Shared static formatter, called from a batch thread and from request threads.
        sb.append(EMAIL_DATE.format(new Date()));
        sb.append(".\n\n");
        sb.append("Student: ").append(student.getFullName()).append("\n");
        sb.append("SIS ID: ").append(student.getSisId()).append("\n");
        sb.append("GPA: ").append(formatGpa(student.getGpa())).append("\n");
        sb.append("Credits completed: ").append(student.getCreditsCompleted()).append("\n");
        sb.append("Severity: ").append(alertCase.getSeverity()).append("\n");
        sb.append("Reason: ").append(alertCase.getReason()).append("\n\n");

        String customerCode = CustomerContext.get();
        if ("RIVERTON".equals(customerCode)) {
            sb.append("Per Riverton policy this case must be acknowledged within 5 business days.\n");
            if (!StringHelper.isEmpty(student.getAthleticsCode())) {
                sb.append("This student is an athlete (code ")
                  .append(student.getAthleticsCode())
                  .append("). The compliance office has been copied.\n");
            }
        } else if ("SUMMIT".equals(customerCode)) {
            sb.append("Summit counselors triage alerts weekly. No immediate action is required.\n");
        } else {
            sb.append("Please contact the student within 10 business days.\n");
        }

        sb.append("\n-- \n");
        sb.append("This message was generated by CampusConnect. Do not reply.\n");
        return sb.toString();
    }

    // ------------------------------------------------------------------
    // Formatting - also a presentation concern
    // ------------------------------------------------------------------

    public String formatGpa(Double gpa) {
        if (gpa == null) {
            return "n/a";
        }
        // No NumberFormat, no locale. Rounds by string truncation.
        String raw = String.valueOf(gpa.doubleValue());
        int dot = raw.indexOf('.');
        if (dot < 0) {
            return raw + ".00";
        }
        while (raw.length() < dot + 3) {
            raw = raw + "0";
        }
        return raw.substring(0, dot + 3);
    }

    public String formatStatusLabel(String status) {
        if (StringHelper.isEmpty(status)) {
            return "Unknown";
        }
        if ("ACTIVE".equals(status)) {
            return "Active";
        } else if ("PROBATION".equals(status)) {
            // Summit's registrar objected to the word "probation" in 2015.
            if ("SUMMIT".equals(CustomerContext.get())) {
                return "Academic Review";
            }
            return "Academic Probation";
        } else if ("WITHDRAWN".equals(status)) {
            return "Withdrawn";
        } else if ("GRADUATED".equals(status)) {
            return "Graduated";
        } else if ("LEAVE".equals(status)) {
            return "Leave of Absence";
        }
        return status;
    }

    public String formatRiskBadge(Student student) {
        if (student == null || student.getGpa() == null) {
            return "none";
        }
        double gpa = student.getGpa().doubleValue();
        double threshold = riskThreshold();
        if (gpa < threshold - 0.5d) {
            return "high";
        } else if (gpa < threshold) {
            return "medium";
        }
        return "low";
    }

    private String mapStatus(String sisStatus) {
        if (StringHelper.isEmpty(sisStatus)) {
            return "ACTIVE";
        }
        String upper = StringHelper.safeUpper(sisStatus);
        if ("E".equals(upper) || "ENROLLED".equals(upper)) {
            return "ACTIVE";
        } else if ("P".equals(upper) || "PROB".equals(upper)) {
            return "PROBATION";
        } else if ("W".equals(upper) || "WD".equals(upper)) {
            return "WITHDRAWN";
        } else if ("G".equals(upper)) {
            return "GRADUATED";
        } else if ("L".equals(upper)) {
            return "LEAVE";
        }
        // XXX: unknown SIS codes silently become ACTIVE. This has hidden at least
        // one real data problem at Riverton (CC-1194).
        return "ACTIVE";
    }
}

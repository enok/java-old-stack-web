package com.campusconnect.finance.persistence.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.campusconnect.common.CustomerContext;
import com.campusconnect.finance.domain.BillingExposureRow;
import com.campusconnect.persistence.jdbc.ConnectionHelper;

/**
 * Raw JDBC, copied from {@link com.campusconnect.persistence.jdbc.JdbcEnrollmentDao}
 * in 2016 when the bursar asked for "the advising appointments of everyone who
 * owes us money" and the answer had to be on a screen by Friday.
 *
 * LEGACY SMELL #6: the SQL is assembled by string concatenation and reuses the
 * advising module's ConnectionHelper, so it runs on its own DriverManager
 * connection outside every Spring transaction.
 *
 * LEGACY SMELL (new, finance): the main query below JOINS THE ADVISING TABLES TO
 * THE FINANCE TABLES in one statement - appointment and student on one side,
 * student_account and financial_hold on the other. This single SELECT is the
 * hardest thing in the repository to split: the moment finance owns its own
 * database this query cannot be written at all.
 *
 * LEGACY SMELL #1: customer branching, occurrence 8 of 8 (module: persistence).
 */
public class JdbcBillingExposureDao {

    private static final Logger LOG = Logger.getLogger(JdbcBillingExposureDao.class);

    /**
     * Scheduled advising appointments for students who carry a balance.
     *
     * Four tables, two bounded contexts, one statement. Nobody can tell from
     * the call site that half of this row comes from the advising schema.
     */
    public List findExposureForUpcomingAppointments() {
        List out = new ArrayList();
        Connection con = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            con = ConnectionHelper.getConnection();
            st = con.createStatement();

            String sql = "SELECT s.id AS student_id, s.sis_id, s.first_name, s.last_name, "
                    + "ap.id AS appointment_id, ap.starts_at, ap.status AS appointment_status, "
                    + "ap.reason_code, "
                    + "sa.id AS account_id, sa.balance, sa.term_code, "
                    + "fh.reason_code AS hold_reason_code, fh.placed_at AS hold_placed_at "
                    + "FROM appointment ap "
                    + "JOIN student s ON s.id = ap.student_id "
                    + "LEFT JOIN student_account sa ON sa.student_id = s.id "
                    + "  AND sa.customer_code = ap.customer_code "
                    + "LEFT JOIN financial_hold fh ON fh.student_id = s.id "
                    + "  AND fh.released_at IS NULL "
                    + "  AND fh.customer_code = ap.customer_code "
                    + "WHERE ap.customer_code = '" + CustomerContext.get() + "' "
                    + "AND ap.status = 'SCHEDULED'";

            // Riverton excludes athletes from the bursar's exposure list because
            // athletics compliance runs its own report. Summit only reports the
            // current term. Northlake reports everything.
            if ("RIVERTON".equals(CustomerContext.get())) {
                sql = sql + " AND (s.athletics_code IS NULL OR s.athletics_code = '')";
            } else if ("SUMMIT".equals(CustomerContext.get())) {
                sql = sql + " AND sa.term_code = 'FA2014'";
            }

            sql = sql + " ORDER BY sa.balance DESC, ap.starts_at";

            LOG.debug("billing exposure sql: " + sql);
            rs = st.executeQuery(sql);
            while (rs.next()) {
                out.add(map(rs));
            }
        } catch (Exception e) {
            // Swallowed, same as the enrollment DAO. An empty exposure report
            // looks exactly like "nobody owes us anything".
            e.printStackTrace();
        } finally {
            ConnectionHelper.close(rs);
            ConnectionHelper.close(st);
            ConnectionHelper.close(con);
        }
        return out;
    }

    /**
     * Same join, narrowed to one student. The advising student detail screen
     * calls this, which is why a page about advising opens a second database
     * connection outside the request's Hibernate transaction.
     */
    public List findExposureForStudent(Long studentId) {
        List out = new ArrayList();
        Connection con = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            con = ConnectionHelper.getConnection();
            st = con.createStatement();

            String sql = "SELECT s.id AS student_id, s.sis_id, s.first_name, s.last_name, "
                    + "ap.id AS appointment_id, ap.starts_at, ap.status AS appointment_status, "
                    + "ap.reason_code, "
                    + "sa.id AS account_id, sa.balance, sa.term_code, "
                    + "fh.reason_code AS hold_reason_code, fh.placed_at AS hold_placed_at "
                    + "FROM student s "
                    + "LEFT JOIN appointment ap ON ap.student_id = s.id "
                    + "LEFT JOIN student_account sa ON sa.student_id = s.id "
                    + "LEFT JOIN financial_hold fh ON fh.student_id = s.id AND fh.released_at IS NULL "
                    + "WHERE s.id = " + studentId
                    + " AND s.customer_code = '" + CustomerContext.get() + "'"
                    + " ORDER BY ap.starts_at DESC";

            rs = st.executeQuery(sql);
            while (rs.next()) {
                out.add(map(rs));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            ConnectionHelper.close(rs);
            ConnectionHelper.close(st);
            ConnectionHelper.close(con);
        }
        return out;
    }

    /**
     * Ledger-style insert used by the nightly reconcile. Autocommit is on, so
     * this write is NOT in the transaction the caller believes it is in.
     *
     * TODO CC-1445: this is the only finance write that is not done through
     * Hibernate. It exists because the reconcile job timed out doing it one
     * entity at a time and nobody wanted to touch the session flush mode.
     */
    public int postLateFeeBatch(List accountIds, double feeAmount, String termCode, String description) {
        int written = 0;
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = ConnectionHelper.getConnection();
            for (int i = 0; i < accountIds.size(); i++) {
                Long accountId = (Long) accountIds.get(i);

                // Mixed styles again: a PreparedStatement whose text is concatenated,
                // and a money amount that arrived here as a double.
                String sql = "INSERT INTO account_charge "
                        + "(customer_code, account_id, student_id, charge_type, amount, term_code, "
                        + " description, source_ref, posted_at) "
                        + "SELECT '" + CustomerContext.get() + "', sa.id, sa.student_id, 'LATE_FEE', "
                        + feeAmount + ", '" + termCode + "', ?, 'NIGHTLY', NOW() "
                        + "FROM student_account sa WHERE sa.id = " + accountId;

                ps = con.prepareStatement(sql);
                ps.setString(1, description);
                ps.executeUpdate();
                ps.close();
                written++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            ConnectionHelper.close(ps);
            ConnectionHelper.close(con);
        }
        return written;
    }

    private BillingExposureRow map(ResultSet rs) throws java.sql.SQLException {
        BillingExposureRow row = new BillingExposureRow();
        row.setStudentId(new Long(rs.getLong("student_id")));
        row.setSisId(rs.getString("sis_id"));
        row.setStudentName(rs.getString("first_name") + " " + rs.getString("last_name"));
        row.setAppointmentId(new Long(rs.getLong("appointment_id")));
        row.setAppointmentStartsAt(rs.getTimestamp("starts_at"));
        row.setAppointmentStatus(rs.getString("appointment_status"));
        row.setReasonCode(rs.getString("reason_code"));
        row.setAccountId(new Long(rs.getLong("account_id")));
        row.setBalance(rs.getBigDecimal("balance"));
        row.setTermCode(rs.getString("term_code"));
        row.setHoldReasonCode(rs.getString("hold_reason_code"));
        row.setHoldPlacedAt(rs.getTimestamp("hold_placed_at"));
        return row;
    }
}

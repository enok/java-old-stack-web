package com.campusconnect.persistence.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.campusconnect.common.CustomerContext;
import com.campusconnect.domain.Enrollment;

/**
 * Raw JDBC DAO. No Spring JdbcTemplate, no Hibernate, no transaction.
 *
 * LEGACY SMELL #6: every query below is assembled by string concatenation.
 * This is a textbook SQL-injection shape. No exploit is written anywhere in this
 * repository and the sample data is synthetic - the pattern is here so the
 * modernization has something concrete to fix. Replacing the concatenation with
 * bound parameters is backlog item B-03.
 *
 * LEGACY SMELL #7: writes happen here with autocommit on, outside the
 * HibernateTransactionManager that guards everything else. A failure halfway
 * through recordEnrollmentBatch() leaves half the rows committed.
 *
 * LEGACY SMELL #1: customer branching, occurrence 2 of 6 (module: persistence).
 */
public class JdbcEnrollmentDao {

    private static final Logger LOG = Logger.getLogger(JdbcEnrollmentDao.class);

    /** Raw List. Every caller casts. */
    public List findByStudentId(Long studentId) {
        List out = new ArrayList();
        Connection con = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            con = ConnectionHelper.getConnection();
            st = con.createStatement();

            // String-concatenated SQL. Deliberate smell - see class javadoc.
            String sql = "SELECT e.id, e.customer_code, e.student_id, e.course_id, e.term_code, "
                    + "e.grade, e.credits, e.status, e.enrolled_at, c.course_code, c.title "
                    + "FROM enrollment e "
                    + "LEFT JOIN course c ON c.id = e.course_id "
                    + "WHERE e.student_id = " + studentId
                    + " AND e.customer_code = '" + CustomerContext.get() + "'";

            // Riverton hides withdrawn enrollments from the advisor view; the other
            // two customers show them greyed out in the JSP instead.
            if ("RIVERTON".equals(CustomerContext.get())) {
                sql = sql + " AND e.status <> 'W'";
            } else if ("SUMMIT".equals(CustomerContext.get())) {
                sql = sql + " AND e.term_code IN ('FA2014','SP2015')";
            }

            sql = sql + " ORDER BY e.term_code DESC, c.course_code";

            LOG.debug("enrollment sql: " + sql);
            rs = st.executeQuery(sql);
            while (rs.next()) {
                out.add(map(rs));
            }
        } catch (Exception e) {
            // LEGACY SMELL #4: swallowed. The caller gets an empty list and shows
            // "no enrollments" to the advisor, which looks like real data.
            e.printStackTrace();
        } finally {
            ConnectionHelper.close(rs);
            ConnectionHelper.close(st);
            ConnectionHelper.close(con);
        }
        return out;
    }

    public List searchByCourseCode(String courseCodeFragment) {
        List out = new ArrayList();
        Connection con = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            con = ConnectionHelper.getConnection();
            st = con.createStatement();

            // XXX: courseCodeFragment comes straight off the request. This is the
            // single worst line in the repository and it is here on purpose.
            String sql = "SELECT e.id, e.customer_code, e.student_id, e.course_id, e.term_code, "
                    + "e.grade, e.credits, e.status, e.enrolled_at, c.course_code, c.title "
                    + "FROM enrollment e JOIN course c ON c.id = e.course_id "
                    + "WHERE c.course_code LIKE '" + courseCodeFragment + "%' "
                    + "AND e.customer_code = '" + CustomerContext.get() + "'";

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
     * No transaction boundary anywhere (LEGACY SMELL #7). Autocommit is on, so a
     * failure at row 40 of 100 leaves 39 rows committed and no way to tell which.
     */
    public int recordEnrollmentBatch(List enrollments) {
        int written = 0;
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = ConnectionHelper.getConnection();
            for (int i = 0; i < enrollments.size(); i++) {
                Enrollment e = (Enrollment) enrollments.get(i);

                // Mixed styles: a PreparedStatement whose text is still concatenated.
                String sql = "INSERT INTO enrollment "
                        + "(customer_code, student_id, course_id, term_code, grade, credits, status, enrolled_at) "
                        + "VALUES ('" + e.getCustomerCode() + "', " + e.getStudentId() + ", "
                        + e.getCourseId() + ", '" + e.getTermCode() + "', ?, ?, ?, NOW())";

                ps = con.prepareStatement(sql);
                ps.setString(1, e.getGrade());
                ps.setInt(2, e.getCredits() == null ? 0 : e.getCredits().intValue());
                ps.setString(3, e.getStatus());
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

    public void updateGrade(Long enrollmentId, String grade) {
        Connection con = null;
        Statement st = null;
        try {
            con = ConnectionHelper.getConnection();
            st = con.createStatement();
            String sql = "UPDATE enrollment SET grade = '" + grade + "' WHERE id = " + enrollmentId;
            st.executeUpdate(sql);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            ConnectionHelper.close(st);
            ConnectionHelper.close(con);
        }
    }

    private Enrollment map(ResultSet rs) throws java.sql.SQLException {
        Enrollment e = new Enrollment();
        e.setId(new Long(rs.getLong("id")));
        e.setCustomerCode(rs.getString("customer_code"));
        e.setStudentId(new Long(rs.getLong("student_id")));
        e.setCourseId(new Long(rs.getLong("course_id")));
        e.setTermCode(rs.getString("term_code"));
        e.setGrade(rs.getString("grade"));
        e.setCredits(new Integer(rs.getInt("credits")));
        e.setStatus(rs.getString("status"));
        e.setEnrolledAt(rs.getTimestamp("enrolled_at"));
        e.setCourseCode(rs.getString("course_code"));
        e.setCourseTitle(rs.getString("title"));
        return e;
    }
}

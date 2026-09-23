package com.campusconnect.persistence;

import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;

import com.campusconnect.common.CustomerContext;
import com.campusconnect.domain.Student;

/**
 * Hibernate 4.3 DAO using the native Session API (not JPA EntityManager).
 *
 * Note the mix: the class is wired by XML in applicationContext.xml but the
 * SessionFactory is injected by annotation. That inconsistency is deliberate -
 * it is what a codebase looks like when annotations arrived halfway through.
 *
 * TODO CC-1102: several methods here open a Session with no transaction and
 * rely on the caller having started one. See StudentService.importSisRecord().
 */
public class HibernateStudentDao implements StudentDao {

    private static final Logger LOG = Logger.getLogger(HibernateStudentDao.class);

    @Autowired
    private SessionFactory sessionFactory;

    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    protected Session session() {
        return sessionFactory.getCurrentSession();
    }

    public Student findById(Long id) {
        return (Student) session().get(Student.class, id);
    }

    public Student findBySisId(String sisId) {
        // HQL string concatenation of the customer code. It is not user input,
        // but it is still the same bad habit as JdbcEnrollmentDao.
        String hql = "from Student s where s.sisId = :sisId and s.customerCode = '"
                + CustomerContext.get() + "'";
        Query q = session().createQuery(hql);
        q.setString("sisId", sisId);
        List results = q.list();
        if (results.isEmpty()) {
            return null;
        }
        return (Student) results.get(0);
    }

    public List findByCustomer(String customerCode) {
        Criteria c = session().createCriteria(Student.class);
        c.add(Restrictions.eq("customerCode", customerCode));
        c.addOrder(Order.asc("lastName"));
        return c.list();
    }

    public List search(String lastNameFragment) {
        Criteria c = session().createCriteria(Student.class);
        c.add(Restrictions.eq("customerCode", CustomerContext.get()));
        if (lastNameFragment != null && lastNameFragment.trim().length() > 0) {
            c.add(Restrictions.ilike("lastName", lastNameFragment + "%"));
        }
        c.addOrder(Order.asc("lastName"));
        // XXX: no paging. Riverton has 41k students and this screen times out.
        return c.list();
    }

    public List findByAdvisorId(Long advisorId) {
        Query q = session().createQuery(
                "from Student s where s.advisor.id = :advisorId and s.customerCode = :cc order by s.lastName");
        q.setLong("advisorId", advisorId.longValue());
        q.setString("cc", CustomerContext.get());
        return q.list();
    }

    public List findAtRisk(double gpaThreshold) {
        Query q = session().createQuery(
                "from Student s where s.gpa is not null and s.gpa < :gpa and s.customerCode = :cc");
        q.setDouble("gpa", gpaThreshold);
        q.setString("cc", CustomerContext.get());
        return q.list();
    }

    public void save(Student student) {
        session().save(student);
    }

    public void update(Student student) {
        session().update(student);
    }

    public void delete(Student student) {
        session().delete(student);
    }

    public int countByCustomer(String customerCode) {
        Query q = session().createQuery("select count(*) from Student s where s.customerCode = :cc");
        q.setString("cc", customerCode);
        Object result = q.uniqueResult();
        if (result == null) {
            LOG.warn("count returned null for " + customerCode);
            return 0;
        }
        return ((Long) result).intValue();
    }
}

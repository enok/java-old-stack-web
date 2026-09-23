package com.campusconnect.finance.persistence;

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
import com.campusconnect.finance.domain.StudentAccount;

/**
 * Finance DAO, written in 2016 by copying HibernateStudentDao and renaming the
 * type. It shares the same SessionFactory, the same transaction manager and the
 * same customer_code convention as the advising DAOs, which is why nothing in
 * the persistence layer marks a boundary between the two domains.
 *
 * Note that findByStudentId() returns the FIRST row and logs nothing when
 * there is more than one. See the duplicate-account note on StudentAccount.
 */
public class HibernateStudentAccountDao implements StudentAccountDao {

    private static final Logger LOG = Logger.getLogger(HibernateStudentAccountDao.class);

    @Autowired
    private SessionFactory sessionFactory;

    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    protected Session session() {
        return sessionFactory.getCurrentSession();
    }

    public StudentAccount findById(Long id) {
        return (StudentAccount) session().get(StudentAccount.class, id);
    }

    public StudentAccount findByStudentId(Long studentId) {
        // Same HQL-with-a-concatenated-customer-code habit as HibernateStudentDao.
        String hql = "from StudentAccount a where a.student.id = :sid and a.customerCode = '"
                + CustomerContext.get() + "' order by a.id";
        Query q = session().createQuery(hql);
        q.setLong("sid", studentId.longValue());
        List results = q.list();
        if (results.isEmpty()) {
            return null;
        }
        return (StudentAccount) results.get(0);
    }

    public List findByCustomer(String customerCode) {
        Criteria c = session().createCriteria(StudentAccount.class);
        c.add(Restrictions.eq("customerCode", customerCode));
        c.addOrder(Order.asc("id"));
        // XXX: no paging here either. Riverton's bursar screen loads 41k rows.
        return c.list();
    }

    public List findWithBalanceOver(double threshold) {
        // Takes a double and compares it against a DECIMAL column. Hibernate
        // widens the parameter, so a balance of exactly the threshold sometimes
        // matches and sometimes does not.
        Query q = session().createQuery(
                "from StudentAccount a where a.balance > :bal and a.customerCode = :cc order by a.balance desc");
        q.setDouble("bal", threshold);
        q.setString("cc", CustomerContext.get());
        return q.list();
    }

    public void save(StudentAccount account) {
        session().save(account);
    }

    public void update(StudentAccount account) {
        if (account == null) {
            LOG.warn("update called with null account");
            return;
        }
        session().update(account);
    }
}

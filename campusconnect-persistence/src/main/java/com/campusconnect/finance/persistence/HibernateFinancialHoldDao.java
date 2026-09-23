package com.campusconnect.finance.persistence;

import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.campusconnect.common.CustomerContext;
import com.campusconnect.finance.domain.FinancialHold;

public class HibernateFinancialHoldDao implements FinancialHoldDao {

    @Autowired
    private SessionFactory sessionFactory;

    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    protected Session session() {
        return sessionFactory.getCurrentSession();
    }

    public FinancialHold findById(Long id) {
        return (FinancialHold) session().get(FinancialHold.class, id);
    }

    /** "Active" is spelled `releasedAt is null` here, and again in SQL, and again in a JSP. */
    public FinancialHold findActiveForStudent(Long studentId) {
        Query q = session().createQuery(
                "from FinancialHold h where h.student.id = :sid and h.releasedAt is null "
                        + "and h.customerCode = :cc order by h.placedAt desc");
        q.setLong("sid", studentId.longValue());
        q.setString("cc", CustomerContext.get());
        List results = q.list();
        if (results.isEmpty()) {
            return null;
        }
        return (FinancialHold) results.get(0);
    }

    public List findActive() {
        Query q = session().createQuery(
                "from FinancialHold h where h.releasedAt is null and h.customerCode = :cc order by h.placedAt");
        q.setString("cc", CustomerContext.get());
        return q.list();
    }

    public void save(FinancialHold hold) {
        session().save(hold);
    }

    public void update(FinancialHold hold) {
        session().update(hold);
    }
}

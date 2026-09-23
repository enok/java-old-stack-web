package com.campusconnect.persistence;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;

import com.campusconnect.common.CustomerContext;
import com.campusconnect.domain.Advisor;

public class HibernateAdvisorDao implements AdvisorDao {

    @Autowired
    private SessionFactory sessionFactory;

    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    protected Session session() {
        return sessionFactory.getCurrentSession();
    }

    public Advisor findById(Long id) {
        return (Advisor) session().get(Advisor.class, id);
    }

    public Advisor findByStaffId(String staffId) {
        Query q = session().createQuery(
                "from Advisor a where a.staffId = :staffId and a.customerCode = :cc");
        q.setString("staffId", staffId);
        q.setString("cc", CustomerContext.get());
        List results = q.list();
        if (results.isEmpty()) {
            return null;
        }
        return (Advisor) results.get(0);
    }

    public List findActive() {
        Criteria c = session().createCriteria(Advisor.class);
        c.add(Restrictions.eq("customerCode", CustomerContext.get()));
        c.add(Restrictions.eq("active", Boolean.TRUE));
        c.addOrder(Order.asc("lastName"));
        return c.list();
    }

    public void save(Advisor advisor) {
        session().save(advisor);
    }

    public void update(Advisor advisor) {
        session().update(advisor);
    }
}

package com.campusconnect.persistence;

import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.campusconnect.common.CustomerContext;
import com.campusconnect.domain.EarlyAlertCase;

public class HibernateEarlyAlertCaseDao implements EarlyAlertCaseDao {

    @Autowired
    private SessionFactory sessionFactory;

    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    protected Session session() {
        return sessionFactory.getCurrentSession();
    }

    public EarlyAlertCase findById(Long id) {
        return (EarlyAlertCase) session().get(EarlyAlertCase.class, id);
    }

    public List findOpenCases() {
        Query q = session().createQuery(
                "from EarlyAlertCase c where c.status <> 'CLOSED' and c.customerCode = :cc order by c.openedAt");
        q.setString("cc", CustomerContext.get());
        return q.list();
    }

    public List findByStudentId(Long studentId) {
        Query q = session().createQuery(
                "from EarlyAlertCase c where c.student.id = :sid and c.customerCode = :cc order by c.openedAt desc");
        q.setLong("sid", studentId.longValue());
        q.setString("cc", CustomerContext.get());
        return q.list();
    }

    public void save(EarlyAlertCase alertCase) {
        session().save(alertCase);
    }

    public void update(EarlyAlertCase alertCase) {
        session().update(alertCase);
    }
}

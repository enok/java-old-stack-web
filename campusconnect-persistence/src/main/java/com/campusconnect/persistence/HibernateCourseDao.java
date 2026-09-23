package com.campusconnect.persistence;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;

import com.campusconnect.common.CustomerContext;
import com.campusconnect.domain.Course;

public class HibernateCourseDao implements CourseDao {

    @Autowired
    private SessionFactory sessionFactory;

    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    protected Session session() {
        return sessionFactory.getCurrentSession();
    }

    public Course findById(Long id) {
        return (Course) session().get(Course.class, id);
    }

    public List findByTerm(String termCode) {
        Criteria c = session().createCriteria(Course.class);
        c.add(Restrictions.eq("customerCode", CustomerContext.get()));
        c.add(Restrictions.eq("termCode", termCode));
        c.addOrder(Order.asc("courseCode"));
        return c.list();
    }

    public void save(Course course) {
        session().save(course);
    }
}

package com.campusconnect.persistence;

import java.util.Date;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.campusconnect.common.CustomerContext;
import com.campusconnect.common.DateUtils;
import com.campusconnect.domain.Appointment;

public class HibernateAppointmentDao implements AppointmentDao {

    @Autowired
    private SessionFactory sessionFactory;

    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    protected Session session() {
        return sessionFactory.getCurrentSession();
    }

    public Appointment findById(Long id) {
        return (Appointment) session().get(Appointment.class, id);
    }

    public List findByStudentId(Long studentId) {
        Query q = session().createQuery(
                "from Appointment a where a.student.id = :sid and a.customerCode = :cc order by a.startsAt desc");
        q.setLong("sid", studentId.longValue());
        q.setString("cc", CustomerContext.get());
        return q.list();
    }

    public List findByAdvisorAndDay(Long advisorId, Date day) {
        Date start = startOfDay(day);
        Date end = DateUtils.addDays(start, 1);
        Query q = session().createQuery(
                "from Appointment a where a.advisor.id = :aid and a.startsAt >= :start and a.startsAt < :end "
                        + "and a.customerCode = :cc order by a.startsAt");
        q.setLong("aid", advisorId.longValue());
        q.setTimestamp("start", start);
        q.setTimestamp("end", end);
        q.setString("cc", CustomerContext.get());
        return q.list();
    }

    public List findUpcoming(Date from, Date to) {
        Query q = session().createQuery(
                "from Appointment a where a.startsAt between :from and :to and a.customerCode = :cc order by a.startsAt");
        q.setTimestamp("from", from);
        q.setTimestamp("to", to);
        q.setString("cc", CustomerContext.get());
        return q.list();
    }

    public void save(Appointment appointment) {
        session().save(appointment);
    }

    public void update(Appointment appointment) {
        session().update(appointment);
    }

    public int countForAdvisorOnDay(Long advisorId, Date day) {
        List existing = findByAdvisorAndDay(advisorId, day);
        return existing.size();
    }

    private Date startOfDay(Date day) {
        // TODO CC-1244: this drops the time component with string round-tripping
        // through a shared SimpleDateFormat. It is as bad as it looks.
        return DateUtils.parseSis(DateUtils.SIS.format(day));
    }
}

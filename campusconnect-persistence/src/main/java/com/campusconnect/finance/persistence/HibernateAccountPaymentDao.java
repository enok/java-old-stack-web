package com.campusconnect.finance.persistence;

import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.campusconnect.common.CustomerContext;
import com.campusconnect.finance.domain.AccountPayment;

public class HibernateAccountPaymentDao implements AccountPaymentDao {

    @Autowired
    private SessionFactory sessionFactory;

    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    protected Session session() {
        return sessionFactory.getCurrentSession();
    }

    public AccountPayment findById(Long id) {
        return (AccountPayment) session().get(AccountPayment.class, id);
    }

    public List findByAccountId(Long accountId) {
        Query q = session().createQuery(
                "from AccountPayment p where p.account.id = :aid and p.customerCode = :cc order by p.postedAt desc");
        q.setLong("aid", accountId.longValue());
        q.setString("cc", CustomerContext.get());
        return q.list();
    }

    public void save(AccountPayment payment) {
        session().save(payment);
    }
}

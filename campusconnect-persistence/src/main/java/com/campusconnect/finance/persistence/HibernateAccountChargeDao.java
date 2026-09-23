package com.campusconnect.finance.persistence;

import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.campusconnect.common.CustomerContext;
import com.campusconnect.finance.domain.AccountCharge;

public class HibernateAccountChargeDao implements AccountChargeDao {

    @Autowired
    private SessionFactory sessionFactory;

    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    protected Session session() {
        return sessionFactory.getCurrentSession();
    }

    public AccountCharge findById(Long id) {
        return (AccountCharge) session().get(AccountCharge.class, id);
    }

    public List findByAccountId(Long accountId) {
        Query q = session().createQuery(
                "from AccountCharge c where c.account.id = :aid and c.customerCode = :cc order by c.postedAt desc");
        q.setLong("aid", accountId.longValue());
        q.setString("cc", CustomerContext.get());
        return q.list();
    }

    public List findByAccountIdAndTerm(Long accountId, String termCode) {
        Query q = session().createQuery(
                "from AccountCharge c where c.account.id = :aid and c.termCode = :term "
                        + "and c.customerCode = :cc order by c.postedAt");
        q.setLong("aid", accountId.longValue());
        q.setString("term", termCode);
        q.setString("cc", CustomerContext.get());
        return q.list();
    }

    public void save(AccountCharge charge) {
        session().save(charge);
    }

    public void update(AccountCharge charge) {
        session().update(charge);
    }
}

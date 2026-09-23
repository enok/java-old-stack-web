package com.campusconnect.finance.service;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;

import org.junit.After;
import org.junit.Test;

import com.campusconnect.common.CustomerContext;

/**
 * The FOURTH test method in the repository, and the only one covering Student
 * Finance.
 *
 * It tests the one method on StudentAccountService that can be called without a
 * database, a Spring context or the static ServiceLocator. Nothing pins the
 * balance arithmetic, the hold threshold, the late-fee fork, the cross-domain
 * transaction in EarlyAlertService.closeCase() or the SQL join in
 * JdbcBillingExposureDao. That is deliberate: the missing safety net is the
 * exercise. See docs/MODERNIZATION-BACKLOG.md item B-01.
 */
public class StudentAccountServiceTest {

    private final StudentAccountService service = new StudentAccountService();

    @After
    public void tearDown() {
        CustomerContext.clear();
    }

    @Test
    public void formatsMoneyToTwoPlaces() {
        CustomerContext.set(CustomerContext.NORTHLAKE);
        assertEquals("0.00", service.formatMoney(null));
        assertEquals("$1240.00", service.formatMoney(new BigDecimal("1240.00")));
    }
}

package com.campusconnect.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Test;

/**
 * One of only THREE test methods in the entire repository.
 *
 * The missing safety net is the point of this exercise: there is no test that
 * pins the behaviour of StudentService, the importers, the scheduling rules or
 * the SQL. Backlog item B-01 is to write characterization tests BEFORE any
 * refactoring begins.
 */
public class CustomerContextTest {

    @After
    public void tearDown() {
        CustomerContext.clear();
    }

    @Test
    public void defaultsToNorthlakeWhenUnset() {
        CustomerContext.clear();
        assertEquals("NORTHLAKE", CustomerContext.get());
        CustomerContext.set(CustomerContext.RIVERTON);
        assertTrue(CustomerContext.is("RIVERTON"));
    }
}

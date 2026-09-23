package com.campusconnect.service;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Test;

import com.campusconnect.common.CustomerContext;

/**
 * The other TWO test methods in the repository.
 *
 * Note what is NOT tested: everything that matters. Both methods below happen
 * to be the only two methods on StudentService that can be called without a
 * database, a Spring context or the static ServiceLocator being populated -
 * which is itself the clearest evidence of the design problem.
 */
public class StudentServiceTest {

    private final StudentService service = new StudentService();

    @After
    public void tearDown() {
        CustomerContext.clear();
    }

    @Test
    public void formatsGpaToTwoPlaces() {
        assertEquals("n/a", service.formatGpa(null));
        assertEquals("3.40", service.formatGpa(new Double(3.4d)));
    }

    @Test
    public void probationLabelDivergesAtSummit() {
        CustomerContext.set(CustomerContext.NORTHLAKE);
        assertEquals("Academic Probation", service.formatStatusLabel("PROBATION"));
        CustomerContext.set(CustomerContext.SUMMIT);
        assertEquals("Academic Review", service.formatStatusLabel("PROBATION"));
    }
}

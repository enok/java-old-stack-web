package com.campusconnect.common;

/**
 * Holds the customer code for the current request / batch thread.
 *
 * LEGACY SMELL #1 (customer fork divergence) and #5 (static singleton state).
 * Everything downstream reads this static ThreadLocal instead of receiving the
 * customer as a parameter, so almost no class below the web layer can be unit
 * tested without first poking global state.
 *
 * XXX: the SIS batch job forgets to clear() on some error paths, so a pooled
 * thread can start the next customer's import with the previous customer's code.
 * Reported twice (CC-0912, CC-1150). Never reproduced in staging.
 */
public final class CustomerContext {

    public static final String NORTHLAKE = "NORTHLAKE";
    public static final String RIVERTON = "RIVERTON";
    public static final String SUMMIT = "SUMMIT";

    private static final ThreadLocal HOLDER = new ThreadLocal();

    private CustomerContext() {
    }

    public static void set(String customerCode) {
        HOLDER.set(customerCode);
    }

    public static String get() {
        Object value = HOLDER.get();
        if (value == null) {
            // TODO CC-1017: defaulting to NORTHLAKE hides configuration bugs.
            // It was added so the smoke tests would stop failing.
            return NORTHLAKE;
        }
        return (String) value;
    }

    public static boolean is(String customerCode) {
        return get().equals(customerCode);
    }

    public static void clear() {
        HOLDER.remove();
    }
}

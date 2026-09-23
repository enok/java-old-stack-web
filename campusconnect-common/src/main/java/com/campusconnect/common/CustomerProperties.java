package com.campusconnect.common;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;

import org.apache.log4j.Logger;

/**
 * Hand-rolled per-customer configuration loader.
 *
 * LEGACY SMELL #1 (per-customer property files) and #4 (Hashtable / Vector /
 * raw types / swallowed exceptions). Spring has had PropertyPlaceholderConfigurer
 * since 1.x; this class exists because the original author "did not want another
 * XML file".
 */
public class CustomerProperties {

    private static final Logger LOG = Logger.getLogger(CustomerProperties.class);

    /** Raw type on purpose: this is a 2014 codebase compiled on Java 8. */
    private static final Hashtable CACHE = new Hashtable();

    private CustomerProperties() {
    }

    public static String get(String key) {
        return get(CustomerContext.get(), key);
    }

    public static String get(String customerCode, String key) {
        Properties props = load(customerCode);
        String value = props.getProperty(key);
        if (value == null) {
            // XXX: silent fallback to NORTHLAKE. When a new customer is onboarded
            // and a key is forgotten, they quietly get Northlake's behaviour.
            Properties fallback = load(CustomerContext.NORTHLAKE);
            value = fallback.getProperty(key);
        }
        return value;
    }

    public static int getInt(String key, int defaultValue) {
        String raw = get(key);
        if (raw == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (Exception e) {
            e.printStackTrace();
            return defaultValue;
        }
    }

    public static boolean getBoolean(String key) {
        String raw = get(key);
        return "true".equalsIgnoreCase(raw) || "Y".equalsIgnoreCase(raw) || "1".equals(raw);
    }

    /** Returns a raw Vector, because that is what the JSP tag expected in 2014. */
    public static Vector keyNames() {
        Vector out = new Vector();
        Properties props = load(CustomerContext.get());
        Enumeration e = props.propertyNames();
        while (e.hasMoreElements()) {
            out.add(e.nextElement());
        }
        return out;
    }

    private static synchronized Properties load(String customerCode) {
        Object cached = CACHE.get(customerCode);
        if (cached != null) {
            return (Properties) cached;
        }
        Properties props = new Properties();
        InputStream in = null;
        try {
            String path = "customer-" + customerCode + ".properties";
            in = CustomerProperties.class.getClassLoader().getResourceAsStream(path);
            if (in == null) {
                LOG.warn("No property file for customer " + customerCode + ", using empty set");
            } else {
                props.load(in);
            }
        } catch (IOException e) {
            // LEGACY SMELL #4: checked exception swallowed.
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception ignored) {
                    // nothing we can do
                }
            }
        }
        CACHE.put(customerCode, props);
        return props;
    }

    /** Only used by the (never finished) admin screen. */
    public static void flushCache() {
        CACHE.clear();
    }
}

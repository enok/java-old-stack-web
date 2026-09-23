package com.campusconnect.common;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

/**
 * Static service registry.
 *
 * LEGACY SMELL #5. Half of the application is wired by Spring XML and the other
 * half reaches into this static registry, so collaborators cannot be substituted
 * in a test without mutating global state (and remembering to undo it).
 *
 * TODO CC-0977: delete this once every caller is constructor-injected.
 * Opened 2015. Still 14 callers.
 */
public class ServiceLocator {

    private static final Logger LOG = Logger.getLogger(ServiceLocator.class);

    /** Raw-ish map, static, no synchronization on read. */
    private static final Map REGISTRY = new HashMap();

    private ServiceLocator() {
    }

    public static void register(String name, Object service) {
        LOG.info("Registering service: " + name);
        REGISTRY.put(name, service);
    }

    public static Object get(String name) {
        Object service = REGISTRY.get(name);
        if (service == null) {
            // XXX: returning null rather than failing fast. Callers NPE far from here.
            LOG.error("Service not registered: " + name);
        }
        return service;
    }

    public static void reset() {
        REGISTRY.clear();
    }
}

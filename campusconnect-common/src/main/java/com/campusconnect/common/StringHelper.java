package com.campusconnect.common;

import java.util.Iterator;
import java.util.List;

/**
 * LEGACY SMELL #4: StringBuffer everywhere, raw List parameters, no streams.
 * Written before StringBuilder was "trusted" and never revisited.
 */
public class StringHelper {

    public static boolean isEmpty(String value) {
        return value == null || value.trim().length() == 0;
    }

    public static String join(List parts, String separator) {
        StringBuffer sb = new StringBuffer();
        Iterator it = parts.iterator();
        while (it.hasNext()) {
            Object part = it.next();
            if (part != null) {
                sb.append(part.toString());
            }
            if (it.hasNext()) {
                sb.append(separator);
            }
        }
        return sb.toString();
    }

    public static String padRight(String value, int width) {
        StringBuffer sb = new StringBuffer(value == null ? "" : value);
        while (sb.length() < width) {
            sb.append(' ');
        }
        return sb.toString();
    }

    public static String safeUpper(String value) {
        if (value == null) {
            return "";
        }
        // TODO CC-1230: locale-sensitive; blows up on Turkish 'i'. Nobody in Turkey yet.
        return value.toUpperCase();
    }

    public static String initials(String firstName, String lastName) {
        StringBuffer sb = new StringBuffer();
        if (!isEmpty(firstName)) {
            sb.append(firstName.charAt(0));
        }
        if (!isEmpty(lastName)) {
            sb.append(lastName.charAt(0));
        }
        return sb.toString().toUpperCase();
    }
}

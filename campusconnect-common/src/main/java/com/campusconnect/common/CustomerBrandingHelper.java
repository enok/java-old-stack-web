package com.campusconnect.common;

/**
 * LEGACY SMELL #1: hardcoded per-customer branching, occurrence 1 of 6
 * (module: campusconnect-common).
 *
 * Every new customer means editing this class, recompiling, and redeploying all
 * three installations. This is the cheapest of the six occurrences to move into
 * configuration, which is why the modernization backlog starts here.
 */
public class CustomerBrandingHelper {

    public String getDisplayName() {
        String customerCode = CustomerContext.get();
        if ("RIVERTON".equals(customerCode)) {
            return "Riverton State University";
        } else if ("SUMMIT".equals(customerCode)) {
            return "Summit Community College";
        } else if ("NORTHLAKE".equals(customerCode)) {
            return "Northlake University";
        } else {
            return "CampusConnect";
        }
    }

    public String getHeaderColor() {
        String customerCode = CustomerContext.get();
        if ("RIVERTON".equals(customerCode)) {
            return "#7B1113";
        } else if ("SUMMIT".equals(customerCode)) {
            return "#1F4E3D";
        } else {
            return "#123A6B";
        }
    }

    public String getAdvisorLabel() {
        String customerCode = CustomerContext.get();
        // Riverton calls them "Success Coaches"; Summit calls them "Counselors".
        if ("RIVERTON".equals(customerCode)) {
            return "Success Coach";
        } else if ("SUMMIT".equals(customerCode)) {
            return "Counselor";
        } else {
            return "Advisor";
        }
    }

    public String getSupportEmail() {
        // The one place that DOES read configuration. It works. Nobody copied the pattern.
        String configured = CustomerProperties.get("support.email");
        if (configured == null) {
            return "help@campusconnect.example.edu";
        }
        return configured;
    }
}

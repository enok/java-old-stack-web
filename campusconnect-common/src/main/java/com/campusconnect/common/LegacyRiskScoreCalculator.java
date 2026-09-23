package com.campusconnect.common;

import java.util.HashMap;
import java.util.Map;

/**
 * LEGACY SMELL #8: DEAD CLASS. Nothing in this repository references it.
 *
 * It was the first attempt at an early-alert risk score, superseded in 2015 by
 * the rules inside EarlyAlertService. It survived every cleanup because a
 * grep for "RiskScore" in the JSPs matched an unrelated request attribute and
 * whoever checked assumed it was live.
 *
 * Deleting it is the first, zero-risk commit of the modernization.
 */
public class LegacyRiskScoreCalculator {

    private static final double GPA_WEIGHT = 0.55d;
    private static final double ATTENDANCE_WEIGHT = 0.30d;
    private static final double CREDIT_WEIGHT = 0.15d;

    private final Map thresholds = new HashMap();

    public LegacyRiskScoreCalculator() {
        thresholds.put("HIGH", new Double(0.70d));
        thresholds.put("MEDIUM", new Double(0.45d));
    }

    public double score(double gpa, double attendanceRate, int creditsCompleted) {
        double gpaRisk = 1.0d - (gpa / 4.0d);
        double attendanceRisk = 1.0d - attendanceRate;
        double creditRisk = creditsCompleted >= 30 ? 0.0d : (30 - creditsCompleted) / 30.0d;
        return (gpaRisk * GPA_WEIGHT) + (attendanceRisk * ATTENDANCE_WEIGHT) + (creditRisk * CREDIT_WEIGHT);
    }

    public String band(double score) {
        Double high = (Double) thresholds.get("HIGH");
        Double medium = (Double) thresholds.get("MEDIUM");
        if (score >= high.doubleValue()) {
            return "HIGH";
        } else if (score >= medium.doubleValue()) {
            return "MEDIUM";
        }
        return "LOW";
    }
}

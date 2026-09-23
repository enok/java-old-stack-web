package com.campusconnect.service;

import java.util.Date;

import org.apache.log4j.Logger;

import com.campusconnect.common.CustomerContext;
import com.campusconnect.common.CustomerProperties;
import com.campusconnect.common.DateUtils;

/**
 * "Mail" service. It logs instead of sending, because the real implementation
 * lived in a separate closed-source jar on the customer boxes.
 *
 * It is registered into the static ServiceLocator at context startup
 * (see applicationContext.xml), which is how StudentService finds it.
 */
public class NotificationService {

    private static final Logger LOG = Logger.getLogger(NotificationService.class);

    private String fromAddress;

    public void setFromAddress(String fromAddress) {
        this.fromAddress = fromAddress;
    }

    public void send(String to, String subject, String body) {
        try {
            StringBuffer sb = new StringBuffer();
            sb.append("\n--- OUTBOUND MAIL (").append(CustomerContext.get()).append(") ---\n");
            sb.append("Date: ").append(DateUtils.formatLong(new Date())).append("\n");
            sb.append("From: ").append(fromAddress == null ? "noreply@campusconnect.example.edu" : fromAddress).append("\n");
            sb.append("To: ").append(to).append("\n");

            // Riverton copies the athletics compliance office on everything.
            if (CustomerProperties.getBoolean("earlyalert.ccAthletics")) {
                sb.append("Cc: athletics-compliance@riverton.example.edu\n");
            }

            sb.append("Subject: ").append(subject).append("\n\n");
            sb.append(body);
            sb.append("--- END MAIL ---\n");
            LOG.info(sb.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

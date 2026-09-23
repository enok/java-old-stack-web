package com.campusconnect.web.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

import com.campusconnect.common.CustomerContext;

/**
 * Populates the static CustomerContext ThreadLocal for each request.
 *
 * Declared in web.xml, not by annotation, even though the deployment descriptor
 * is servlet 3.0 and could use @WebFilter. The whole app is configured in XML.
 */
public class CustomerContextFilter implements Filter {

    private static final Logger LOG = Logger.getLogger(CustomerContextFilter.class);

    private String defaultCustomerCode;

    public void init(FilterConfig config) throws ServletException {
        defaultCustomerCode = config.getInitParameter("defaultCustomerCode");
        if (defaultCustomerCode == null) {
            defaultCustomerCode = CustomerContext.NORTHLAKE;
        }
        LOG.info("CustomerContextFilter default = " + defaultCustomerCode);
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        String customerCode = defaultCustomerCode;
        if (request instanceof HttpServletRequest) {
            HttpServletRequest http = (HttpServletRequest) request;
            // XXX: the customer can be overridden by a request parameter. It was
            // added "temporarily" for a demo in 2015 and never removed. Anyone
            // can view another institution's data by adding ?customer=RIVERTON.
            String override = http.getParameter("customer");
            if (override != null && override.trim().length() > 0) {
                customerCode = override.trim().toUpperCase();
            }
        }
        CustomerContext.set(customerCode);
        try {
            chain.doFilter(request, response);
        } finally {
            CustomerContext.clear();
        }
    }

    public void destroy() {
        // nothing
    }
}

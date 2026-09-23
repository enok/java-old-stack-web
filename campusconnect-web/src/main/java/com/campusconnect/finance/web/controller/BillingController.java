package com.campusconnect.finance.web.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.campusconnect.common.CustomerBrandingHelper;
import com.campusconnect.common.CustomerContext;
import com.campusconnect.domain.Student;
import com.campusconnect.finance.service.FinancialHoldService;
import com.campusconnect.finance.service.StudentAccountService;
import com.campusconnect.service.StudentService;

/**
 * Billing screens.
 *
 * It lives in campusconnect-web beside the advising controllers, it is picked
 * up by the SAME component-scan and ALSO declared explicitly in
 * dispatcher-servlet.xml (the same duplicate wiring as StudentController), and
 * it holds the advising StudentService as well as the two finance services -
 * because the statement header shows the student's name, advisor and GPA.
 *
 * LEGACY SMELL #1, the finance web occurrence: the statement layout branches on
 * the customer code here AND in accountStatement.jsp, on the same value.
 */
@Controller
@RequestMapping("/billing")
public class BillingController {

    private static final Logger LOG = Logger.getLogger(BillingController.class);

    @Autowired
    private StudentAccountService studentAccountService;

    @Autowired
    private FinancialHoldService financialHoldService;

    /** The advising service, on a finance controller. */
    @Autowired
    private StudentService studentService;

    private final CustomerBrandingHelper branding = new CustomerBrandingHelper();

    public void setStudentAccountService(StudentAccountService s) { this.studentAccountService = s; }
    public void setFinancialHoldService(FinancialHoldService s) { this.financialHoldService = s; }
    public void setStudentService(StudentService s) { this.studentService = s; }

    @RequestMapping(value = "/account", method = RequestMethod.GET)
    public String account(@RequestParam("studentId") Long studentId, ModelMap model) {
        Student student = studentService.getStudent(studentId);
        if (student == null) {
            LOG.warn("No student " + studentId);
            return "redirect:/students/list";
        }
        Map statement = studentAccountService.buildStatement(studentId);

        model.addAttribute("student", student);
        model.addAttribute("statement", statement);
        model.addAttribute("account", statement.get("account"));
        model.addAttribute("charges", statement.get("charges"));
        model.addAttribute("payments", statement.get("payments"));
        model.addAttribute("exposure", statement.get("exposure"));
        model.addAttribute("balanceLabel", statement.get("balanceLabel"));
        model.addAttribute("asOf", statement.get("asOf"));
        model.addAttribute("statementLayout", statement.get("layout"));
        model.addAttribute("hold", financialHoldService.getActiveHold(studentId));
        model.addAttribute("holdThreshold", new Double(financialHoldService.holdThreshold()));

        // Advising values on the billing screen.
        model.addAttribute("gpaLabel", studentService.formatGpa(student.getGpa()));
        model.addAttribute("statusLabel", studentService.formatStatusLabel(student.getEnrollmentStatus()));

        model.addAttribute("customerCode", CustomerContext.get());
        model.addAttribute("institutionName", branding.getDisplayName());
        model.addAttribute("advisorLabel", branding.getAdvisorLabel());
        model.addAttribute("headerColor", branding.getHeaderColor());

        // The same decision the JSP makes again, on the same customer code.
        String customerCode = CustomerContext.get();
        if ("SUMMIT".equals(customerCode)) {
            model.addAttribute("pageTitle", "Student Account Summary");
            model.addAttribute("showPaymentForm", Boolean.TRUE);
        } else if ("RIVERTON".equals(customerCode)) {
            model.addAttribute("pageTitle", "Bursar Statement");
            // Riverton takes payments in the portal, not here. See
            // StudentAccountService.postPayment().
            model.addAttribute("showPaymentForm", Boolean.FALSE);
        } else {
            model.addAttribute("pageTitle", "Student Account");
            model.addAttribute("showPaymentForm", Boolean.TRUE);
        }
        return "accountStatement";
    }

    /**
     * The amount arrives as a String off the request and is parsed
     * with new BigDecimal(String), so "12,50" throws NumberFormatException and
     * the advisor gets the 500 page. There is no binding and no validation.
     */
    @RequestMapping(value = "/payment", method = RequestMethod.POST)
    public String payment(@RequestParam("studentId") Long studentId,
                          @RequestParam("amount") String amount,
                          @RequestParam(value = "method", required = false) String method,
                          @RequestParam(value = "referenceNo", required = false) String referenceNo,
                          ModelMap model) {
        BigDecimal parsed = null;
        try {
            parsed = new BigDecimal(amount == null ? "" : amount.trim());
        } catch (Exception e) {
            e.printStackTrace();
        }
        List errors = studentAccountService.postPayment(studentId, parsed,
                method == null ? "CASH" : method, referenceNo, "web");
        if (!errors.isEmpty()) {
            LOG.warn("Payment rejected: " + errors);
            model.addAttribute("errors", errors);
            return account(studentId, model);
        }

        // Posting money can release a hold, which can unblock an ADVISING
        // appointment. The re-evaluation is done here, on the web layer, because
        // the service that posts the payment does not know about holds.
        financialHoldService.evaluate(studentId);

        return "redirect:/billing/account?studentId=" + studentId;
    }

    @RequestMapping(value = "/holds", method = RequestMethod.GET)
    public String holds(ModelMap model) {
        model.addAttribute("holds", financialHoldService.listActiveHolds());
        model.addAttribute("customerCode", CustomerContext.get());
        model.addAttribute("institutionName", branding.getDisplayName());
        model.addAttribute("headerColor", branding.getHeaderColor());
        model.addAttribute("holdThreshold", new Double(financialHoldService.holdThreshold()));
        return "holdList";
    }
}

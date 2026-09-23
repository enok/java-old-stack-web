package com.campusconnect.web.controller;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

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
import com.campusconnect.service.EarlyAlertService;
import com.campusconnect.service.StudentService;

/**
 * Spring MVC controller. Annotation-driven, but the bean itself is also listed
 * in dispatcher-servlet.xml - the component-scan and the explicit bean overlap,
 * which is exactly the sort of half-migrated wiring this repository is modelling.
 *
 * LEGACY SMELL #1, occurrence 6 of 6 in Java (module: campusconnect-web).
 * The seventh occurrence is in studentList.jsp.
 */
@Controller
@RequestMapping("/students")
public class StudentController {

    private static final Logger LOG = Logger.getLogger(StudentController.class);

    @Autowired
    private StudentService studentService;

    @Autowired
    private EarlyAlertService earlyAlertService;

    private final CustomerBrandingHelper branding = new CustomerBrandingHelper();

    public void setStudentService(StudentService studentService) {
        this.studentService = studentService;
    }

    public void setEarlyAlertService(EarlyAlertService earlyAlertService) {
        this.earlyAlertService = earlyAlertService;
    }

    @RequestMapping(value = "/list", method = RequestMethod.GET)
    public String list(@RequestParam(value = "q", required = false) String q, ModelMap model) {
        List students = studentService.searchStudents(q);
        model.addAttribute("students", students);
        model.addAttribute("query", q == null ? "" : q);
        model.addAttribute("customerCode", CustomerContext.get());
        model.addAttribute("institutionName", branding.getDisplayName());
        model.addAttribute("advisorLabel", branding.getAdvisorLabel());
        model.addAttribute("headerColor", branding.getHeaderColor());
        model.addAttribute("studentService", studentService);

        // Column set differs per customer. The JSP branches as well, on the same
        // customer code, which means the two can and do disagree.
        String customerCode = CustomerContext.get();
        if ("RIVERTON".equals(customerCode)) {
            model.addAttribute("showAthletics", Boolean.TRUE);
            model.addAttribute("showHomeCampus", Boolean.FALSE);
            model.addAttribute("pageTitle", "Student Caseload");
        } else if ("SUMMIT".equals(customerCode)) {
            model.addAttribute("showAthletics", Boolean.FALSE);
            model.addAttribute("showHomeCampus", Boolean.TRUE);
            model.addAttribute("pageTitle", "Student Directory");
        } else {
            model.addAttribute("showAthletics", Boolean.FALSE);
            model.addAttribute("showHomeCampus", Boolean.FALSE);
            model.addAttribute("pageTitle", "Students");
        }
        return "studentList";
    }

    @RequestMapping(value = "/detail", method = RequestMethod.GET)
    public String detail(@RequestParam("id") Long id, ModelMap model, HttpServletRequest request) {
        Student student = studentService.getStudent(id);
        if (student == null) {
            LOG.warn("No student " + id);
            return "redirect:/students/list";
        }
        model.addAttribute("student", student);
        model.addAttribute("enrollments", studentService.getEnrollments(id));
        model.addAttribute("cases", earlyAlertService.listForStudent(id));
        model.addAttribute("customerCode", CustomerContext.get());
        model.addAttribute("institutionName", branding.getDisplayName());
        model.addAttribute("advisorLabel", branding.getAdvisorLabel());
        model.addAttribute("headerColor", branding.getHeaderColor());
        model.addAttribute("gpaLabel", studentService.formatGpa(student.getGpa()));
        model.addAttribute("statusLabel", studentService.formatStatusLabel(student.getEnrollmentStatus()));
        model.addAttribute("riskBadge", studentService.formatRiskBadge(student));
        return "studentDetail";
    }

    @RequestMapping(value = "/caseload", method = RequestMethod.GET)
    public String caseload(@RequestParam("advisorId") Long advisorId, ModelMap model) {
        Map summary = studentService.getCaseloadSummary(advisorId);
        model.addAttribute("summary", summary);
        model.addAttribute("advisorLabel", branding.getAdvisorLabel());
        model.addAttribute("institutionName", branding.getDisplayName());
        model.addAttribute("headerColor", branding.getHeaderColor());
        model.addAttribute("customerCode", CustomerContext.get());
        return "caseload";
    }
}

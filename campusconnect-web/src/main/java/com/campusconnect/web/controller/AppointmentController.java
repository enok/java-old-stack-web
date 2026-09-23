package com.campusconnect.web.controller;

import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.campusconnect.common.CustomerBrandingHelper;
import com.campusconnect.common.CustomerContext;
import com.campusconnect.common.DateUtils;
import com.campusconnect.service.AppointmentService;

@Controller
@RequestMapping("/appointments")
public class AppointmentController {

    private static final Logger LOG = Logger.getLogger(AppointmentController.class);

    @Autowired
    private AppointmentService appointmentService;

    private final CustomerBrandingHelper branding = new CustomerBrandingHelper();

    public void setAppointmentService(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @RequestMapping(value = "/day", method = RequestMethod.GET)
    public String day(@RequestParam("advisorId") Long advisorId,
                      @RequestParam(value = "date", required = false) String date,
                      ModelMap model) {
        Date day = date == null ? new Date() : DateUtils.parseSis(date);
        if (day == null) {
            day = new Date();
        }
        List appointments = appointmentService.listForAdvisorDay(advisorId, day);
        model.addAttribute("appointments", appointments);
        model.addAttribute("day", DateUtils.formatDisplay(day));
        model.addAttribute("advisorId", advisorId);
        model.addAttribute("advisorLabel", branding.getAdvisorLabel());
        model.addAttribute("institutionName", branding.getDisplayName());
        model.addAttribute("headerColor", branding.getHeaderColor());
        model.addAttribute("customerCode", CustomerContext.get());
        model.addAttribute("slotMinutes", new Integer(appointmentService.slotMinutes()));
        model.addAttribute("leadTimeHours", new Integer(appointmentService.leadTimeHours()));
        model.addAttribute("walkInsAllowed", Boolean.valueOf(appointmentService.walkInsAllowed()));
        return "appointmentDay";
    }

    @RequestMapping(value = "/schedule", method = RequestMethod.POST)
    public String schedule(@RequestParam("studentId") Long studentId,
                           @RequestParam("advisorId") Long advisorId,
                           @RequestParam("startsAt") String startsAt,
                           @RequestParam(value = "reasonCode", required = false) String reasonCode,
                           @RequestParam(value = "walkIn", required = false) String walkIn,
                           ModelMap model) {
        Date start = DateUtils.parseSis(startsAt);
        List errors = appointmentService.schedule(studentId, advisorId, start, reasonCode,
                "on".equals(walkIn) || "true".equals(walkIn));
        if (!errors.isEmpty()) {
            LOG.warn("Scheduling rejected: " + errors);
            model.addAttribute("errors", errors);
            return day(advisorId, startsAt, model);
        }
        return "redirect:/appointments/day?advisorId=" + advisorId;
    }
}

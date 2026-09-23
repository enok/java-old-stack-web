package com.campusconnect.web.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.campusconnect.common.CustomerBrandingHelper;
import com.campusconnect.common.CustomerContext;
import com.campusconnect.service.EarlyAlertService;

@Controller
@RequestMapping("/alerts")
public class EarlyAlertController {

    @Autowired
    private EarlyAlertService earlyAlertService;

    private final CustomerBrandingHelper branding = new CustomerBrandingHelper();

    public void setEarlyAlertService(EarlyAlertService earlyAlertService) {
        this.earlyAlertService = earlyAlertService;
    }

    @RequestMapping(value = "/open", method = RequestMethod.GET)
    public String open(ModelMap model) {
        List cases = earlyAlertService.listOpenCases();
        model.addAttribute("cases", cases);
        model.addAttribute("institutionName", branding.getDisplayName());
        model.addAttribute("advisorLabel", branding.getAdvisorLabel());
        model.addAttribute("headerColor", branding.getHeaderColor());
        model.addAttribute("customerCode", CustomerContext.get());
        return "alertList";
    }

    @RequestMapping(value = "/close", method = RequestMethod.POST)
    public String close(@RequestParam("caseId") Long caseId,
                        @RequestParam(value = "notes", required = false) String notes) {
        earlyAlertService.closeCase(caseId, notes);
        return "redirect:/alerts/open";
    }
}

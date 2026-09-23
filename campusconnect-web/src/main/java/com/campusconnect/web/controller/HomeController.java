package com.campusconnect.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.campusconnect.common.CustomerBrandingHelper;
import com.campusconnect.common.CustomerContext;

@Controller
public class HomeController {

    private final CustomerBrandingHelper branding = new CustomerBrandingHelper();

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public String home(ModelMap model) {
        model.addAttribute("institutionName", branding.getDisplayName());
        model.addAttribute("advisorLabel", branding.getAdvisorLabel());
        model.addAttribute("headerColor", branding.getHeaderColor());
        model.addAttribute("supportEmail", branding.getSupportEmail());
        model.addAttribute("customerCode", CustomerContext.get());
        return "home";
    }
}

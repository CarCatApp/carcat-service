package com.carland.carland_service.controller;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Hidden
@Controller
public class BookingStaffWebController {

    @Value("${carland.swagger.auth-server-url}")
    private String authServerUrl;

    @Value("${carland.swagger.service-server-url}")
    private String serviceServerUrl;

    @GetMapping(value = {"/partner", "/partner/", "/partner/login"}, produces = MediaType.TEXT_HTML_VALUE)
    public String login(Model model) {
        model.addAttribute("authServerUrl", authServerUrl);
        model.addAttribute("serviceServerUrl", serviceServerUrl);
        return "partner-login";
    }

    @GetMapping(value = "/partner/app", produces = MediaType.TEXT_HTML_VALUE)
    public String app(Model model) {
        model.addAttribute("authServerUrl", authServerUrl);
        model.addAttribute("serviceServerUrl", serviceServerUrl);
        return "partner-app";
    }
}

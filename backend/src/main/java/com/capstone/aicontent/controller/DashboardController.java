package com.capstone.aicontent.controller;

import com.capstone.aicontent.dto.AdminStats;
import com.capstone.aicontent.dto.DashboardStats;
import com.capstone.aicontent.service.AnalyticsService;
import com.capstone.aicontent.service.CurrentUserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController @RequestMapping("/api")
public class DashboardController {
    private final AnalyticsService analytics; private final CurrentUserService current;
    public DashboardController(AnalyticsService analytics, CurrentUserService current) { this.analytics = analytics; this.current = current; }
    @GetMapping("/dashboard") public DashboardStats dashboard(Authentication auth) { return analytics.dashboard(current.get(auth)); }
    @GetMapping("/reports/admin") @PreAuthorize("hasRole('ADMIN')") public AdminStats admin() { return analytics.admin(); }
}

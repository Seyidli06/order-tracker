package com.ordertracker.security.integration;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("security-test")
@RestController
public class TestSecurityController {

    @PostMapping("/api/auth/login")
    public String login() {
        return "login";
    }

    @PostMapping("/api/auth/register")
    public String register() {
        return "register";
    }

    @GetMapping("/api/auth/test")
    public String authTest() {
        return "auth";
    }

    @GetMapping("/api/webhooks/test")
    public String webhook() {
        return "webhook";
    }

    @GetMapping("/api/test/protected")
    public String protectedEndpoint() {
        return "protected";
    }

    @GetMapping("/api/audit/test")
    public String audit() {
        return "audit";
    }
}
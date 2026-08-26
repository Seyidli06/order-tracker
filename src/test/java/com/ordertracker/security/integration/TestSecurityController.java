package com.ordertracker.security.integration;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestSecurityController {

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
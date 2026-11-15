package com.scaleup.admin;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@ConditionalOnProperty(name = "scaleup.admin.enabled", havingValue = "true")
public class AdminController {

    private final ResetService resetService;
    private final VerifyService verifyService;

    public AdminController(ResetService resetService, VerifyService verifyService) {
        this.resetService = resetService;
        this.verifyService = verifyService;
    }

    @PostMapping("/reset")
    public ResetResponse reset(@RequestParam SeedProfile profile) {
        return resetService.reset(profile);
    }

    @GetMapping("/verify")
    public VerifyResponse verify() {
        return verifyService.verify();
    }
}
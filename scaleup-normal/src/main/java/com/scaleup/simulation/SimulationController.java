package com.scaleup.simulation;

import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/simulation")
@ConditionalOnProperty(name = "scaleup.admin.enabled", havingValue = "true")
public class SimulationController {

    private final SimulationSettings settings;

    public SimulationController(SimulationSettings settings) {
        this.settings = settings;
    }

    @GetMapping
    public SimulationConfig get() {
        return settings.get();
    }

    @PutMapping
    public SimulationConfig update(@Valid @RequestBody SimulationConfig config) {
        settings.update(config);
        return settings.get();
    }
}
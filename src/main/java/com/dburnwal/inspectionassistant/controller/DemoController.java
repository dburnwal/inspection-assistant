package com.dburnwal.inspectionassistant.controller;

import com.dburnwal.inspectionassistant.demo.DemoScenarioRegistry;
import com.dburnwal.inspectionassistant.demo.InspectionScenario;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/demo")
@CrossOrigin(origins = "*")
public class DemoController {

    private final DemoScenarioRegistry registry;

    public DemoController(DemoScenarioRegistry registry) {
        this.registry = registry;
    }

    @GetMapping("/scenarios")
    public List<InspectionScenario> listScenarios() {
        return registry.all();
    }

    @GetMapping("/scenarios/{id}")
    public InspectionScenario getScenario(@PathVariable String id) {
        return registry.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Scenario not found: " + id));
    }
}

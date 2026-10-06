package com.example.demo.controller;

import com.example.demo.domain.Release;
import com.example.demo.domain.ReleaseStatus;
import com.example.demo.service.ReleaseService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/releases")
@CrossOrigin(origins = "*")
public class ReleaseController {

    private final ReleaseService releaseService;
    private final com.example.demo.service.MetricsService metricsService;
    private final com.example.demo.service.AiAnalystService aiAnalystService;

    public ReleaseController(ReleaseService releaseService, 
                             com.example.demo.service.MetricsService metricsService,
                             com.example.demo.service.AiAnalystService aiAnalystService) {
        this.releaseService = releaseService;
        this.metricsService = metricsService;
        this.aiAnalystService = aiAnalystService;
    }

    @PostMapping
    public ResponseEntity<Release> startRelease(@RequestParam String serviceName, @RequestParam String version) {
        Release release = releaseService.startNewRelease(serviceName, version);
        return ResponseEntity.ok(release);
    }

    @GetMapping
    public ResponseEntity<List<Release>> getAllReleases() {
        return ResponseEntity.ok(releaseService.getAllReleases());
    }

    @PostMapping("/{id}/promote")
    public ResponseEntity<Release> promoteRelease(@PathVariable Long id, @RequestParam ReleaseStatus nextStatus) {
        Release release = releaseService.promoteRelease(id, nextStatus);
        return ResponseEntity.ok(release);
    }

    @PostMapping("/{id}/inject-failure")
    public ResponseEntity<String> injectFailure(@PathVariable Long id) {
        metricsService.injectFailure(id);
        return ResponseEntity.ok("Failure injected for Release ID: " + id + ". Policy Engine will detect this shortly and initiate rollback.");
    }

    @PostMapping("/{id}/remove-failure")
    public ResponseEntity<String> removeFailure(@PathVariable Long id) {
        metricsService.removeFailure(id);
        return ResponseEntity.ok("Failure removed for Release ID: " + id);
    }

    @GetMapping("/{id}/analyze")
    public ResponseEntity<Map<String, String>> analyzeRelease(@PathVariable Long id) {
        String analysis = aiAnalystService.generateRcaForRelease(id);
        return ResponseEntity.ok(Map.of("summary", analysis));
    }
}

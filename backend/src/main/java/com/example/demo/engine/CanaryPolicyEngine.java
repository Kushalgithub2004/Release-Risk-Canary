package com.example.demo.engine;

import com.example.demo.domain.Release;
import com.example.demo.domain.ReleaseRepository;
import com.example.demo.domain.ReleaseStatus;
import com.example.demo.service.MetricsService;
import com.example.demo.service.ReleaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CanaryPolicyEngine {

    private static final Logger log = LoggerFactory.getLogger(CanaryPolicyEngine.class);
    
    // Strict thresholds for production risk
    private static final double MAX_ERROR_RATE_THRESHOLD = 1.0; // 1%
    private static final double MAX_P99_LATENCY_THRESHOLD = 200.0; // 200ms

    private final ReleaseRepository releaseRepository;
    private final ReleaseService releaseService;
    private final MetricsService metricsService;

    public CanaryPolicyEngine(ReleaseRepository releaseRepository, 
                              ReleaseService releaseService, 
                              MetricsService metricsService) {
        this.releaseRepository = releaseRepository;
        this.releaseService = releaseService;
        this.metricsService = metricsService;
    }

    // Run every 10 seconds to evaluate active releases
    @Scheduled(fixedDelay = 10000)
    public void evaluateActiveCanaries() {
        log.info("Running Canary Policy Evaluation Cycle...");

        // Find all releases that are currently in a Canary state or just created
        List<Release> activeReleases = releaseRepository.findAll().stream()
                .filter(r -> r.getStatus() != ReleaseStatus.FULL 
                          && r.getStatus() != ReleaseStatus.FAILED 
                          && r.getStatus() != ReleaseStatus.ROLLED_BACK)
                .toList();

        for (Release release : activeReleases) {
            evaluateRelease(release);
        }
    }

    private void evaluateRelease(Release release) {
        MetricsService.MetricSnapshot metrics = metricsService.getMetricsForRelease(release.getId());
        log.info("Evaluating Release ID {}: ErrorRate={}%, P99={}ms", 
                 release.getId(), String.format("%.2f", metrics.errorRatePercent()), String.format("%.2f", metrics.p99LatencyMs()));

        boolean isFailing = metrics.errorRatePercent() > MAX_ERROR_RATE_THRESHOLD || 
                            metrics.p99LatencyMs() > MAX_P99_LATENCY_THRESHOLD;

        try {
            if (isFailing) {
                log.warn("🚨 Release ID {} violated policy thresholds! Rolling back immediately.", release.getId());
                releaseService.promoteRelease(release.getId(), ReleaseStatus.ROLLED_BACK);
            } else {
                ReleaseStatus nextStatus = determineNextStatus(release.getStatus());
                if (nextStatus != release.getStatus()) {
                    log.info("✅ Release ID {} metrics are healthy. Promoting to {}", release.getId(), nextStatus);
                    releaseService.promoteRelease(release.getId(), nextStatus);
                }
            }
        } catch (Exception e) {
            log.error("Failed to update state for release {}: {}", release.getId(), e.getMessage());
        }
    }

    private ReleaseStatus determineNextStatus(ReleaseStatus current) {
        return switch (current) {
            case CREATED -> ReleaseStatus.CANARY_10;
            case CANARY_10 -> ReleaseStatus.CANARY_25;
            case CANARY_25 -> ReleaseStatus.CANARY_50;
            case CANARY_50 -> ReleaseStatus.FULL;
            default -> current;
        };
    }
}

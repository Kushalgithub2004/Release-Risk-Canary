package com.example.demo.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Random;

@Service
public class MetricsService {
    // Stores failure injection state: releaseId -> isFailing
    private final Map<Long, Boolean> failureInjections = new ConcurrentHashMap<>();
    private final Random random = new Random();

    public void injectFailure(Long releaseId) {
        failureInjections.put(releaseId, true);
    }

    public void removeFailure(Long releaseId) {
        failureInjections.remove(releaseId);
    }

    public MetricSnapshot getMetricsForRelease(Long releaseId) {
        boolean isFailing = failureInjections.getOrDefault(releaseId, false);
        
        if (isFailing) {
            // High error rate, high latency
            return new MetricSnapshot(
                    5.0 + random.nextDouble() * 15.0, // 5% to 20% error rate
                    500.0 + random.nextDouble() * 1000.0 // 500ms to 1500ms latency
            );
        } else {
            // Healthy baseline metrics
            return new MetricSnapshot(
                    random.nextDouble() * 0.5, // 0% to 0.5% error rate
                    50.0 + random.nextDouble() * 50.0 // 50ms to 100ms latency
            );
        }
    }

    public record MetricSnapshot(double errorRatePercent, double p99LatencyMs) {}
}

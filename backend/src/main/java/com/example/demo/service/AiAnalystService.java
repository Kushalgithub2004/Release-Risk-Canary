package com.example.demo.service;

import com.example.demo.domain.Release;
import com.example.demo.domain.ReleaseRepository;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.List;

@Service
public class AiAnalystService {

    private final String GEMINI_API_KEY = System.getenv("GEMINI_API_KEY") != null ? System.getenv("GEMINI_API_KEY") : "DUMMY_KEY_REPLACE_IN_PROD";
    private final ReleaseRepository releaseRepository;
    private final MetricsService metricsService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public AiAnalystService(ReleaseRepository releaseRepository, MetricsService metricsService) {
        this.releaseRepository = releaseRepository;
        this.metricsService = metricsService;
    }

    public String generateRcaForRelease(Long releaseId) {
        Release release = releaseRepository.findById(releaseId)
                .orElseThrow(() -> new IllegalArgumentException("Release not found"));

        MetricsService.MetricSnapshot metrics = metricsService.getMetricsForRelease(releaseId);

        String prompt = String.format(
            "You are a DevOps Site Reliability Engineer. The deployment for '%s' version '%s' was rolled back. " +
            "The Canary metrics right before rollback were: Error Rate = %.2f%%, P99 Latency = %.2fms. " +
            "Our max allowed thresholds are 1.0%% error rate and 200ms latency. " +
            "Write a concise, professional Root Cause Analysis summary (max 3 sentences) explaining to the team why the canary failed.",
            release.getServiceName(), release.getVersion(), metrics.errorRatePercent(), metrics.p99LatencyMs()
        );

        return callGemini(prompt);
    }

    private String callGemini(String prompt) {
        try {
            Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                    Map.of("parts", List.of(
                        Map.of("text", prompt)
                    ))
                )
            );

            String jsonBody = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + GEMINI_API_KEY))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            // Basic manual JSON parsing to extract the text
            String res = response.body();
            int textIndex = res.indexOf("\"text\": \"");
            if (textIndex > -1) {
                int start = textIndex + 9;
                int end = res.indexOf("\"", start);
                return res.substring(start, end).replace("\\n", " ").replace("\\\"", "\"");
            }
            return "AI Analysis unavailable at this time.";
        } catch (Exception e) {
            e.printStackTrace();
            return "Failed to communicate with AI model.";
        }
    }
}

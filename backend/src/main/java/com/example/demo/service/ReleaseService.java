package com.example.demo.service;

import com.example.demo.domain.Release;
import com.example.demo.domain.ReleaseRepository;
import com.example.demo.domain.ReleaseStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReleaseService {

    private final ReleaseRepository releaseRepository;
    private final KafkaProducerService kafkaProducerService;

    public ReleaseService(ReleaseRepository releaseRepository, KafkaProducerService kafkaProducerService) {
        this.releaseRepository = releaseRepository;
        this.kafkaProducerService = kafkaProducerService;
    }

    @Transactional
    public Release startNewRelease(String serviceName, String version) {
        Release release = new Release();
        release.setServiceName(serviceName);
        release.setVersion(version);
        release.setStatus(ReleaseStatus.CREATED);
        release.setCreatedAt(LocalDateTime.now());
        release.setUpdatedAt(LocalDateTime.now());
        Release saved = releaseRepository.save(release);
        
        kafkaProducerService.publishReleaseEvent(saved.getId(), saved.getServiceName(), "CREATED", "New deployment initialized");
        return saved;
    }

    public List<Release> getAllReleases() {
        return releaseRepository.findAll();
    }

    @Transactional
    public Release promoteRelease(Long releaseId, ReleaseStatus nextStatus) {
        Release release = releaseRepository.findById(releaseId)
                .orElseThrow(() -> new IllegalArgumentException("Release not found"));
        
        release.setStatus(nextStatus);
        release.setUpdatedAt(LocalDateTime.now());
        // Optimistic locking will automatically prevent race conditions here
        Release saved = releaseRepository.save(release);
        
        String msg = nextStatus == ReleaseStatus.ROLLED_BACK ? "Release rolled back due to anomaly" : "Release promoted to " + nextStatus;
        kafkaProducerService.publishReleaseEvent(saved.getId(), saved.getServiceName(), nextStatus.name(), msg);
        
        return saved;
    }
}

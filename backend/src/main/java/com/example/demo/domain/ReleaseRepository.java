package com.example.demo.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReleaseRepository extends JpaRepository<Release, Long> {
    List<Release> findByStatus(ReleaseStatus status);
    List<Release> findByServiceName(String serviceName);
}

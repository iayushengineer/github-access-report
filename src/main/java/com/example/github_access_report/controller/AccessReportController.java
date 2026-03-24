package com.example.github_access_report.controller;

import com.example.github_access_report.model.AccessReport;
import com.example.github_access_report.service.GitHubAccessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.cache.CacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AccessReportController {

    private final GitHubAccessService accessService;
    private final CacheManager cacheManager;

    @GetMapping("/access-report")
    public ResponseEntity<AccessReport> getAccessReport() throws Exception {
        log.info("Received request for access report");
        AccessReport report = accessService.generateReport();
        return ResponseEntity.ok(report);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Service is running");
    }

    @SuppressWarnings("null")
    @PostMapping("/access-report/refresh")
    public ResponseEntity<String> refreshReport() {
    cacheManager.getCache("accessReport").clear();
    return ResponseEntity.ok("Cache cleared. Next GET will fetch fresh data.");
}
}
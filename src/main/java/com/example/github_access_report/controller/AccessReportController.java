package com.example.github_access_report.controller;

import com.example.github_access_report.model.AccessReport;
import com.example.github_access_report.service.GitHubAccessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AccessReportController {
 private final GitHubAccessService accessService;
 /**
 * GET /api/access-report
 * Returns the full access report as JSON.
 * Optional query param: ?org=other-org to override default org.
 */
 @GetMapping("/access-report")
 public ResponseEntity<AccessReport> getAccessReport() {
 try {
 log.info("Received request for access report");
 AccessReport report = accessService.generateReport();
 return ResponseEntity.ok(report);
 } catch (Exception e) {
 log.error("Failed to generate report: {}", e.getMessage());
 return ResponseEntity.internalServerError().build();
 }
 }
 /**
 * GET /api/health
 * Simple health check endpoint.
 */
 @GetMapping("/health")
 public ResponseEntity<String> health() {
 return ResponseEntity.ok("Service is running");
 }
}
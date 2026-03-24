package com.example.github_access_report.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccessReport {
 private String organizationName;
 private LocalDateTime generatedAt;
 private int totalRepositories;
 private int totalUsers;
 // Key = username, Value = list of repos they can access
 private Map<String, List<String>> userToRepositories;
 // Detailed per-repo access info
 private List<RepositoryAccess> repositoryDetails;
}
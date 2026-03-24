package com.example.github_access_report.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RepositoryAccess {
 private String repositoryName; // e.g., "my-repo"
 private String repositoryUrl; // e.g., "https://github.com/org/my-repo"
 private String visibility; // "public" or "private"
 private List<UserAccess> collaborators; // list of users who have access
 
 @Data
 @AllArgsConstructor
 @NoArgsConstructor
 public static class UserAccess {
 private String username;
 private String permission; // e.g., ADMIN, PUSH, PULL
 }
}


package com.example.github_access_report.service;

import com.example.github_access_report.model.AccessReport;
import com.example.github_access_report.model.RepositoryAccess;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kohsuke.github.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GitHubAccessService {
    private final GitHub gitHub;
    private final ExecutorService executorService;
    @Value("${github.organization}")
    private String orgName;
     
    @Cacheable(value = "accessReport", key = "#root.method.name")
    public AccessReport generateReport() throws IOException {
        log.info("Starting access report for org: {}", orgName);
        // 1. Get the organization
        GHOrganization org = gitHub.getOrganization(orgName);
        // 2. List all repositories
        List<GHRepository> repositories = new ArrayList<>(org.listRepositories().toList());
        log.info("Found {} repositories", repositories.size());
        // 3. For each repo, fetch collaborators IN PARALLEL using CompletableFuture
        List<CompletableFuture<RepositoryAccess>> futures = repositories.stream()
            .map(repo -> CompletableFuture
                .supplyAsync(() -> fetchRepoAccess(repo), executorService)
                .exceptionally(ex -> {
                    log.warn("Failed to get access for {}: {}",
                        repo.getName(), ex.getMessage());
                    return null; // skip failed repos
                }))
            .collect(Collectors.toList());
        // 4. Wait for ALL parallel calls to complete
        List<RepositoryAccess> repoAccessList = futures.stream()
            .map(CompletableFuture::join)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        // 5. Build user -> [repos] mapping
        Map<String, List<String>> userToRepos = new HashMap<>();
        for (RepositoryAccess repoAccess: repoAccessList) {
            for (RepositoryAccess.UserAccess ua: repoAccess.getCollaborators()) {
                userToRepos
                    .computeIfAbsent(ua.getUsername(), k -> new ArrayList<>())
                    .add(repoAccess.getRepositoryName());
            }
        }
        log.info("Report complete: {} repos, {} unique users",
            repoAccessList.size(), userToRepos.size());
        // 6. Return structured report
        return new AccessReport(
            orgName,
            LocalDateTime.now(),
            repoAccessList.size(),
            userToRepos.size(),
            userToRepos,
            repoAccessList
        );
    }
    
     // Fetches collaborators for a single repository.
     // Called in parallel for all repos.
     
    private RepositoryAccess fetchRepoAccess(GHRepository repo) {
        try {
            List<RepositoryAccess.UserAccess> collaborators = new ArrayList<>();

            List<GHUser> users = repo.listCollaborators().toList();

            for (GHUser user: users) {
                GHPermissionType permission = repo.getPermission(user);
                collaborators.add(new RepositoryAccess.UserAccess(
                    user.getLogin(),
                    permission.toString()
                ));
            }

            return new RepositoryAccess(
                repo.getName(),
                repo.getHtmlUrl().toString(),
                repo.isPrivate() ? "private" : "public",
                collaborators
            );
        } catch (IOException e) {
            throw new RuntimeException(
                "Error fetching collaborators for " + repo.getName(), e);
        }
    }
}
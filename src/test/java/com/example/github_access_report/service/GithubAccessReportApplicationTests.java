package com.example.github_access_report.service;

import com.example.github_access_report.model.AccessReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kohsuke.github.*;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GitHubAccessServiceTest {

    @Mock
    private GitHub gitHub;

    @InjectMocks
    private GitHubAccessService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "executorService",
            Executors.newFixedThreadPool(2));
    }

    
    // TEST 1: Happy path — 1 repo, 1 user, correct mapping
    
    @Test
    void generateReport_shouldReturnReportWithCorrectOrgName() throws IOException {
        ReflectionTestUtils.setField(service, "orgName", "test-org");

        // --- Mock Org ---
        GHOrganization mockOrg = mock(GHOrganization.class);
        when(gitHub.getOrganization("test-org")).thenReturn(mockOrg);

        // --- Mock Repo ---
        GHRepository mockRepo = mock(GHRepository.class);
        when(mockRepo.getName()).thenReturn("test-repo");
        when(mockRepo.getHtmlUrl())
            .thenReturn(new URL("https://github.com/test-org/test-repo"));
        when(mockRepo.isPrivate()).thenReturn(false);

        // --- Mock Repo list ---
        PagedIterable<GHRepository> repoIterable = mock(PagedIterable.class);
        when(repoIterable.toList()).thenReturn(List.of(mockRepo));
        when(mockOrg.listRepositories()).thenReturn(repoIterable);

        // --- Mock User ---
        GHUser mockUser = mock(GHUser.class);
        when(mockUser.getLogin()).thenReturn("alice");
        when(mockRepo.getPermission(mockUser)).thenReturn(GHPermissionType.ADMIN);

        // --- Mock Collaborator list ---
        // Must mock toList() since service uses .toList()
        PagedIterable<GHUser> userIterable = mock(PagedIterable.class);
        when(userIterable.toList()).thenReturn(List.of(mockUser));
        when(mockRepo.listCollaborators()).thenReturn(userIterable);

        // --- ACT ---
        AccessReport report = service.generateReport();

        // --- ASSERT ---
        assertEquals("test-org", report.getOrganizationName());
        assertEquals(1, report.getTotalRepositories());
        assertEquals(1, report.getTotalUsers());
        assertTrue(report.getUserToRepositories().containsKey("alice"));
        assertEquals(List.of("test-repo"),
            report.getUserToRepositories().get("alice"));
    }

    // ─────────────────────────────────────────────────────────────
    // TEST 2: Empty org — zero repos and zero users
    // ─────────────────────────────────────────────────────────────
    @Test
    void generateReport_shouldHandleEmptyOrg() throws IOException {
        ReflectionTestUtils.setField(service, "orgName", "empty-org");

        GHOrganization mockOrg = mock(GHOrganization.class);
        when(gitHub.getOrganization("empty-org")).thenReturn(mockOrg);

        PagedIterable<GHRepository> emptyIterable = mock(PagedIterable.class);
        when(emptyIterable.toList()).thenReturn(Collections.emptyList());
        when(mockOrg.listRepositories()).thenReturn(emptyIterable);

        // --- ACT ---
        AccessReport report = service.generateReport();

        // --- ASSERT ---
        assertEquals(0, report.getTotalRepositories());
        assertEquals(0, report.getTotalUsers());
        assertTrue(report.getUserToRepositories().isEmpty());
    }

    // ─────────────────────────────────────────────────────────────
    // TEST 3: Org not found — IOException is thrown
    // ─────────────────────────────────────────────────────────────
    @Test
    void generateReport_shouldThrowWhenOrgNotFound() throws IOException {
        ReflectionTestUtils.setField(service, "orgName", "nonexistent-org");

        when(gitHub.getOrganization("nonexistent-org"))
            .thenThrow(new IOException("Organization not found"));

        assertThrows(IOException.class, () -> service.generateReport());
    }
}
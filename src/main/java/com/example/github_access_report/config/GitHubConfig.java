package com.example.github_access_report.config;

import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class GitHubConfig {
 @Value("${github.token}")
 private String token;
 @Value("${github.thread-pool-size:10}")
 private int threadPoolSize;
 // Creates authenticated GitHub client
 @Bean
 public GitHub gitHub() throws IOException {
 return new GitHubBuilder()
 .withOAuthToken(token)
 .build();
 }
 // Thread pool for parallel API calls
 @Bean
 public ExecutorService executorService() {
 return Executors.newFixedThreadPool(threadPoolSize);
 }
}

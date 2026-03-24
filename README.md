# GitHub Organization Access Report Service

A **Spring Boot REST API** that connects to GitHub and generates a structured report showing **which users have access to which repositories** within a given GitHub Organization.

---

## Table of Contents

- [Project Structure](#-project-structure)
- [How It Works — Application Flow](#-how-it-works--application-flow)
- [Prerequisites](#-prerequisites)
- [How to Configure application.properties](#-how-to-configure-applicationproperties)
- [How to Run the Project](#-how-to-run-the-project)
- [How to Call the API Endpoints](#-how-to-call-the-api-endpoints)
- [Sample API Response](#-sample-api-response)
- [How to Run Test Cases](#-how-to-run-test-cases)
- [Design Decisions & Assumptions](#-design-decisions--assumptions)
- [Tech Stack](#-tech-stack)

---

## Project Structure

```
github-access-report/
│
├── src/
│   ├── main/
│   │   ├── java/com/example/github_access_report/
│   │   │   │
│   │   │   ├── GithubAccessReportApplication.java     ← Entry point (main class)
│   │   │   │
│   │   │   ├── config/
│   │   │   │   └── GitHubConfig.java                  ← Creates GitHub client + thread pool
│   │   │   │
│   │   │   ├── controller/
│   │   │   │   └── AccessReportController.java        ← REST API endpoints
│   │   │   │
│   │   │   ├── exception/
│   │   │   │   └── GlobalExceptionHandler.java        ← Handles all errors globally
│   │   │   │
│   │   │   ├── model/
│   │   │   │   ├── AccessReport.java                  ← Full report response model
│   │   │   │   └── RepositoryAccess.java              ← Per-repository access model
│   │   │   │
│   │   │   └── service/
│   │   │       └── GitHubAccessService.java           ← Core business logic
│   │   │
│   │   └── resources/
│   │       └── application.properties                 ← App configuration (token, org, port)
│   │
│   └── test/
│       └── java/com/example/github_access_report/
│           └── service/
│               └── GitHubAccessServiceTest.java       ← Unit tests (3 test cases)
│
├── pom.xml                                            ← Maven dependencies
├── .gitignore                                         ← Ignores secrets and build files
└── README.md                                          ← This file
```

---

## How It Works — Application Flow

```
Client (Browser / Postman / curl)
        │
        │  GET /api/access-report
        ▼
AccessReportController.java
        │
        │  calls generateReport()
        ▼
GitHubAccessService.java
        │
        ├─ 1. Authenticates with GitHub using PAT token
        │
        ├─ 2. Fetches all repositories of the organization
        │
        ├─ 3. For each repository — fires PARALLEL API calls
        │      using CompletableFuture + Thread Pool (20 threads)
        │           │
        │           ├── Repo 1 → fetch collaborators ──┐
        │           ├── Repo 2 → fetch collaborators ──┤
        │           ├── Repo 3 → fetch collaborators ──┤ (all at same time)
        │           └── Repo N → fetch collaborators ──┘
        │
        ├─ 4. Aggregates results:
        │      - Per-repo view  → which users have access to each repo
        │      - Per-user view  → which repos each user can access
        │
        └─ 5. Returns structured JSON report
                │
                ▼
        AccessReport (JSON Response)
        {
          organizationName, generatedAt,
          totalRepositories, totalUsers,
          userToRepositories { user → [repos] },
          repositoryDetails  [ repo → [users] ]
        }
```

**If any error occurs**, it is caught by `GlobalExceptionHandler.java` which returns a clean JSON error response instead of a raw stack trace.

**If the same endpoint is called again within 10 minutes**, the cached result is returned instantly without hitting the GitHub API again (Caffeine Cache).

---

##  Prerequisites

Make sure these are installed before running:

| Tool | Version | Download |
|------|---------|----------|
| Java JDK | 17 or higher | https://adoptium.net |
| Maven | 3.8 or higher | https://maven.apache.org/download.cgi |
| Git | Any | https://git-scm.com |

**Verify installations:**
```bash
java -version    # Should show: openjdk 17.x.x
mvn -version     # Should show: Apache Maven 3.x.x
git --version    # Should show: git version 2.x.x
```

---

## 🔧 How to Configure `application.properties`

The file is located at:
```
src/main/resources/application.properties
```

### Current contents:
```properties
spring.application.name=github-access-report

# Server port — app runs on http://localhost:8080
server.port=8080

# GitHub configuration
github.token=${GITHUB_TOKEN:PUT_YOUR_GITHUB_TOKEN_HERE}
github.organization=${GITHUB_ORG:PUT_YOUR_GITHUB_ORGANIZATION_HERE}

# Thread pool size for parallel API calls
github.thread-pool-size=20

# Cache configuration — report is cached for 10 minutes
spring.cache.type=caffeine
spring.cache.caffeine.spec=maximumSize=1,expireAfterWrite=10m
```

### What to change:

#### Option A — Edit directly in the file (Quick for local testing only)
Replace the placeholder values:
```properties
github.token=ghp_yourActualTokenHere
github.organization=yourActualOrgName
```
>  **Warning:** Do NOT commit the real token to GitHub. Use Option B for submissions.

---

#### Option B — Use Environment Variables (Recommended / Secure)

The format `${GITHUB_TOKEN:PUT_YOUR_GITHUB_TOKEN_HERE}` means:
- Use the environment variable `GITHUB_TOKEN` if it exists
- Otherwise fall back to the placeholder text

Set environment variables before running:

**Windows (PowerShell):**
```powershell
$env:GITHUB_TOKEN = "ghp_yourTokenHere"
$env:GITHUB_ORG   = "yourOrgName"
```

**Windows (Command Prompt):**
```cmd
set GITHUB_TOKEN=ghp_yourTokenHere
set GITHUB_ORG=yourOrgName
```

**Mac / Linux:**
```bash
export GITHUB_TOKEN=ghp_yourTokenHere
export GITHUB_ORG=yourOrgName
```

---

### How to Create a GitHub Personal Access Token (PAT):

1. Go to **github.com** → click your profile photo → **Settings**
2. Scroll to the bottom → **Developer settings** → **Personal access tokens** → **Tokens (classic)**
3. Click **Generate new token (classic)**
4. Give it a name: `github-access-report`
5. Select these scopes:
   - `repo` — full repository access
   - `read:org` — read organization data
6. Click **Generate token**
7. **Copy it immediately** — GitHub shows it only once

### What to put as Organization Name:
- If you have a real GitHub org → use the name from the URL: `github.com/YOUR-ORG-NAME`
- If testing with your personal account → use your **GitHub username**
- Example: profile is `github.com/john123` → put `john123`

### Other Configurable Properties:

| Property | Default | What to Change |
|----------|---------|----------------|
| `server.port` | `8080` | Change to `8081` if port 8080 is busy |
| `github.thread-pool-size` | `20` | Increase for orgs with 200+ repos |
| `spring.cache.caffeine.spec` | `expireAfterWrite=10m` | Change `10m` to adjust cache duration |

---

## How to Run the Project

### Step 1 — Clone the repository
```bash
git clone https://github.com/iayushengineer/github-access-report.git
cd github-access-report
```

### Step 2 — Set your credentials
```powershell
# PowerShell
$env:GITHUB_TOKEN = "ghp_yourTokenHere"
$env:GITHUB_ORG   = "yourOrgName"
```

### Step 3 — Run the application

**Using Maven (Terminal / PowerShell):**
```bash
mvn spring-boot:run
```

**Using IntelliJ IDEA:**
1. Open the project in IntelliJ IDEA
2. Open `GithubAccessReportApplication.java`
3. Click the green ▶ **Run** button next to `main()`

### Step 4 — Confirm it started

You should see this line in the console:
```
Started GithubAccessReportApplication in 3.x seconds
```

Then test it:
```bash
curl http://localhost:8080/api/health
# Expected: Service is running
```

---

## How to Call the API Endpoints

### All Available Endpoints

| Method | URL | Description |
|--------|-----|-------------|
| `GET` | `/api/health` | Check if the service is running |
| `GET` | `/api/access-report` | Get the full access report as JSON |
| `POST` | `/api/access-report/refresh` | Clear cache and force fresh data from GitHub |

---

### 1. Health Check
```bash
curl http://localhost:8080/api/health
```
**Response:**
```
Service is running
```

---

### 2. Get Access Report
```bash
curl http://localhost:8080/api/access-report
```

**Using a browser:** Open `http://localhost:8080/api/access-report` directly.

**Using Postman:**
1. Open Postman → New Request
2. Set method to `GET`
3. Enter URL: `http://localhost:8080/api/access-report`
4. Click **Send**

> **Note:** First call may take 30–90 seconds depending on org size. Subsequent calls within 10 minutes return instantly from cache.

---

### 3. Refresh Cache
```bash
curl -X POST http://localhost:8080/api/access-report/refresh
```
**Response:**
```
Cache cleared. Next GET will fetch fresh data.
```

---

### Error Responses

When something goes wrong, the API returns a structured JSON error:

```json
{
  "error": "GitHub API connection failed",
  "message": "401 Unauthorized — check your token",
  "timestamp": "2024-03-15T14:30:00"
}
```

| HTTP Status | Meaning | Common Cause |
|-------------|---------|--------------|
| `400` | Bad Request | Wrong or misspelled organization name |
| `502` | Bad Gateway | Invalid or expired GitHub token |
| `500` | Internal Server Error | Unexpected error in the service |

---

## Sample API Response

`GET /api/access-report` returns:

```json
{
  "organizationName": "my-org",
  "generatedAt": "2024-03-15T14:30:00",
  "totalRepositories": 3,
  "totalUsers": 2,

  "userToRepositories": {
    "alice": ["repo-alpha", "repo-beta", "repo-gamma"],
    "bob":   ["repo-alpha", "repo-beta"]
  },

  "repositoryDetails": [
    {
      "repositoryName": "repo-alpha",
      "repositoryUrl":  "https://github.com/my-org/repo-alpha",
      "visibility":     "private",
      "collaborators": [
        { "username": "alice", "permission": "ADMIN" },
        { "username": "bob",   "permission": "PUSH"  }
      ]
    },
    {
      "repositoryName": "repo-beta",
      "repositoryUrl":  "https://github.com/my-org/repo-beta",
      "visibility":     "public",
      "collaborators": [
        { "username": "alice", "permission": "PUSH" },
        { "username": "bob",   "permission": "PULL" }
      ]
    }
  ]
}
```

---

## How to Run Test Cases

Tests are located at:
```
src/test/java/com/example/github_access_report/service/GitHubAccessServiceTest.java
```

### Run all tests via terminal:
```bash
mvn test
```

### Run tests via IntelliJ IDEA:
1. Open `GitHubAccessServiceTest.java`
2. Click the green ▶ button next to the class name
3. All 3 tests run — green tick = pass ✅, red = fail ❌

### Expected output when all tests pass:
```
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

---

### What Each Test Covers:

#### Test 1 — `generateReport_shouldReturnReportWithCorrectOrgName`
**Purpose:** Verifies the happy path — service correctly returns data when everything works.

**Setup (Mocks):**
- GitHub org → 1 repository (`test-repo`)
- Repository → 1 collaborator (`alice` with `ADMIN` permission)

**Assertions:**
- Organization name equals `"test-org"`
- `totalRepositories` = 1
- `totalUsers` = 1
- `alice` maps to `["test-repo"]` in `userToRepositories`

---

#### Test 2 — `generateReport_shouldHandleEmptyOrg`
**Purpose:** Verifies the service handles an organization with no repositories gracefully.

**Setup (Mocks):**
- GitHub org → empty repository list

**Assertions:**
- `totalRepositories` = 0
- `totalUsers` = 0
- `userToRepositories` map is empty (not null)

---

#### Test 3 — `generateReport_shouldThrowWhenOrgNotFound`
**Purpose:** Verifies that when GitHub throws an error (org not found), the service propagates it correctly — it does not silently swallow it.

**Setup (Mocks):**
- `gitHub.getOrganization()` throws `IOException("Organization not found")`

**Assertions:**
- `IOException` is thrown by `generateReport()`

---

### Important Notes About Tests:
- Tests use **Mockito** — no real GitHub connection is needed
- Tests run completely **offline**
- The `mockito-inline` dependency is required to mock GitHub library's final methods

---

## Design Decisions & Assumptions

### 1. Parallel API Calls for Scale
**Problem:** An org with 100 repos needs 100 collaborator API calls. Done sequentially, this takes several minutes.

**Solution:** All collaborator fetch calls run **concurrently** using `CompletableFuture` with a configurable fixed thread pool:
```java
CompletableFuture.supplyAsync(() -> fetchRepoAccess(repo), executorService)
```
This reduces total time from **O(n) sequential to O(1) parallel**, making the service efficient for 100+ repos and 1000+ users.

---

### 2. Error Isolation Per Repository
If fetching collaborators for one repository fails (e.g., access denied), only that repo is skipped and its failure is logged. All other repos continue successfully. This prevents one inaccessible repo from breaking the entire report.

---

### 3. Response Caching (10 Minutes)
The report is cached using **Caffeine Cache** for 10 minutes.
- Prevents excessive GitHub API usage on repeated requests
- GitHub allows 5,000 authenticated requests/hour — caching preserves this budget
- A manual refresh endpoint (`POST /api/access-report/refresh`) allows clearing the cache when fresh data is needed immediately

---

### 4. Dual-View Response Structure
The JSON response includes two perspectives on the same data:
- `repositoryDetails` — per-repo list of collaborators (useful for repository administrators)
- `userToRepositories` — per-user list of accessible repos (useful for security/access audits)

Both views are generated in a **single pass** — no extra API calls or computation needed.

---

### 5. Global Exception Handling with `@ControllerAdvice`
All exceptions are handled in one central `GlobalExceptionHandler` class. This keeps controllers clean with no try-catch blocks, ensures consistent JSON error format across all endpoints, and makes it easy to add new exception types in one place.

---

### 6. OAuth Token Authentication
GitHub Personal Access Token (PAT) was chosen because it is the standard recommended approach for server-to-server GitHub API access. The token is always read from environment variables and never hardcoded in source code.

---

### 7. Assumptions
- The token owner must be an **owner or member** of the target GitHub organization
- Only collaborators visible to the authenticated token are included
- The service reports on **all repositories** in the org (both public and private)
- External collaborators are included alongside organization members

---

## Tech Stack

| Technology | Purpose |
|------------|---------|
| Java 17 | Core programming language |
| Spring Boot 3.2 | Web framework and dependency injection |
| Maven | Build tool and dependency management |
| `org.kohsuke:github-api` | GitHub REST API Java client library |
| Caffeine Cache | Fast in-memory caching of API results |
| Lombok | Reduces boilerplate (`@Data`, `@Slf4j`, `@RequiredArgsConstructor`) |
| JUnit 5 | Unit testing framework |
| Mockito + mockito-inline | Mocking GitHub API calls in tests |
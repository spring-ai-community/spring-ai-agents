# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build and Development Commands

### Basic Commands
- `./mvnw clean compile` - Compile all modules
- `./mvnw clean test` - Run unit tests
- `./mvnw clean verify` - Run full build including integration tests
- `./mvnw clean install` - Install artifacts to local repository
- `./mvnw spring-boot:run` - Run sample applications (from sample directories)

### Integration Tests
- `./mvnw clean verify -Pfailsafe` - Run integration tests with failsafe profile
- `./mvnw clean install -Pfailsafe` - **Use for CI/multi-module builds** - installs artifacts to local repository ensuring dependencies are available for later modules
- Integration tests follow the `*IT.java` naming convention
- `./mvnw test -Dsandbox.integration.test=true -Dtest="*DockerInfraIT"` - Run Docker infrastructure tests

### Important: Maven Lifecycle Phases for CI
- **Use `install` not `verify`** when running integration tests in CI or multi-module environments
- The `verify` phase runs integration tests but doesn't install artifacts to local repository
- The `install` phase ensures artifacts are available for dependency resolution in later reactor modules
- This prevents "Could not find artifact" errors when modules depend on each other

### Documentation
- `./mvnw antora:antora -pl docs` - Build Antora documentation site
- Output location: `docs/target/antora/site/`
- Open `file:///home/mark/community/spring-ai-agents/docs/target/antora/site/index.html` to view locally

### Code Quality
- Code formatting is enforced via `spring-javaformat-maven-plugin`
- Formatting validation runs during the `validate` phase
- Use Spring's Java code formatting conventions

### MANDATORY: Java Formatting Before Commits
- **ALWAYS run `./mvnw spring-javaformat:apply` before any commit**
- CI will fail if formatting violations are found
- Never commit unformatted code - this breaks the build for everyone
- Formatting violations are unacceptable and must be prevented

### Git Commit Guidelines
- **NEVER add Claude Code attribution** in commit messages
- **NEVER reference internal planning documents** (plans/, roadmap files, internal notes) in commit messages
- Keep commit messages clean and professional without AI attribution
- Commit messages should describe what changed and why, not reference planning artifacts

### JBang Agent Launcher
- `jbang jbang/launcher.java hello-world path=test.txt` - Run agents without build process
- `jbang jbang/launcher.java coverage target_coverage=90` - Run coverage agent
- Uses configuration precedence: Agent defaults → run.yaml → CLI arguments
- See `jbang/README.md` for complete usage guide
- Integration tests: `./mvnw test -pl agents/hello-world-agent -Dtest=HelloWorldAgentIT`

### Sample Applications
- `cd samples/hello-world && mvn spring-boot:run` - Run the Hello World sample
- `cd samples/context-engineering && mvn spring-boot:run` - Run the Context Engineering sample
- Authentication: Uses Claude CLI session authentication (recommended) or ANTHROPIC_API_KEY
- **IMPORTANT**: All sample modules must include maven-deploy-plugin configuration with `<skip>true</skip>` to exclude them from Maven Central publishing

### CLI Tool Locations (for this environment)
- **Claude CLI**: `/home/mark/.nvm/versions/node/v22.15.0/bin/claude` (version 1.0.128)
- **Gemini CLI**: `/home/mark/.nvm/versions/node/v22.15.0/bin/gemini` (version 0.5.5)
- **JBang**: `/home/mark/.sdkman/candidates/jbang/current/bin/jbang`

These paths are automatically discovered by the respective CLI discovery utilities, but documenting them here helps avoid repeated discovery overhead during development.
- Note: Session authentication (from `claude auth login`) is preferred over API keys to avoid conflicts

## Architecture Overview

### Multi-Module Maven Project Structure

**Core Abstractions** (`agent-models/spring-ai-agent-model/`)
- `AgentModel` - Core interface for autonomous development agents
- `AgentTaskRequest` - Task specification with goal, workspace, and constraints
- `AgentResponse` - Execution result with metadata and content
- `AgentOptions` - Configuration options for agent behavior

**Provider SDKs** (`provider-sdks/`)
- `claude-agent-sdk/` - CLI client for Claude Code with resilience features
- `gemini-cli-sdk/` - CLI client for Gemini with robust transport
- `swe-agent-sdk/` - SWE-bench agent integration

**Agent Implementations** (`agent-models/`)
- `spring-ai-claude-agent/` - Spring AI adapter for Claude Code
- `spring-ai-gemini/` - Spring AI adapter for Gemini CLI  
- `spring-ai-swe-agent/` - Spring AI adapter for SWE-bench agent

**Client Layer** (`spring-ai-agent-client/`)
- `AgentClient` - High-level fluent API following ChatClient pattern
- Builder pattern for configuration
- Supports goal-based task execution with working directory context

**JBang Agent Infrastructure** (`spring-ai-agents-core/`, `jbang/`, `agents/`)
- `jbang/launcher.java` - Ultra-thin JBang script for agent execution
- `AgentRunner` - Functional interface for black-box agent implementations
- `Launcher` - Agent discovery, loading, and execution orchestration
- `AgentSpecLoader` - YAML agent specification loading and parsing
- `InputMerger` - Input precedence and default value merging
- `LocalConfigLoader` - Configuration loading from CLI, YAML, and defaults
- Individual agent modules in `agents/` directory (e.g., `hello-world-agent/`)

### Key Design Patterns

**Two-Layer Architecture**
- `AgentClient` provides high-level fluent API
- `AgentModel` provides low-level model interface
- Follows Spring AI's established ChatClient/ChatModel pattern

**JBang Agent Pattern**
- `AgentRunner` functional interface for black-box agent execution (inputs → outputs)
- `Launcher` class for agent discovery and orchestration
- Configuration precedence: Agent defaults → run.yaml → CLI arguments
- Ultra-thin launcher script for zero-build development experience

**CLI Integration Pattern**
- All provider SDKs wrap external CLI tools (claude, gemini, swe-agent, vendir)
- **MANDATORY**: Use zt-exec library for all process management (see Process Management section below)
- Unix-style `--` separator for complex prompts to prevent shell parsing issues
- Comprehensive permission handling with `--dangerously-skip-permissions` for autonomous mode
- Circuit breakers, retries, and timeouts for resilience
- JSON-based communication with CLI tools
- Runtime exception-only design (no checked exceptions in codebase)

**Sandbox Infrastructure** (`agent-models/spring-ai-agent-model/`)
- `Sandbox` - Core interface for secure command execution (runtime exceptions only)
- `DockerSandbox` - Docker container-based isolation (preferred)
- `LocalSandbox` - Local process execution with zt-exec (fallback, enhanced logging)
- `ExecSpec`/`ExecResult` - Command specification and results
- `SandboxException` - Runtime exception wrapper for all sandbox errors
- `TimeoutException` - Runtime exception for command timeouts

**Spring Integration**
- Uses Spring Boot auto-configuration patterns
- Direct dependency injection of `Sandbox` implementations
- Conditional beans for Docker vs Local sandbox selection
- Micrometer metrics integration
- Externalized configuration support

### Agent Task Flow
1. Create `AgentTaskRequest` with goal and working directory
2. `AgentClient` delegates to configured `AgentModel` implementation
3. Model uses injected `Sandbox` for secure command execution
4. Sandbox executes CLI tool in isolated environment
5. Provider SDK parses results and returns as `AgentResponse`

### Configuration Properties
- `spring.ai.agent.provider` - Select agent provider (claude-code, gemini, swebench)
- `spring.ai.agent.max-steps` - Maximum execution steps
- `spring.ai.agent.timeout` - Execution timeout
- `spring.ai.claude-code.model` - Claude model selection (default: claude-sonnet-4-20250514)
- `spring.ai.claude-code.bin` - Claude CLI binary path
- `spring.ai.claude-code.yolo` - Enable autonomous mode with --dangerously-skip-permissions (default: true)
- `spring.ai.agents.sandbox.docker.enabled` - Enable/disable Docker sandbox (default: true)
- `spring.ai.agents.sandbox.docker.image-tag` - Docker image for sandbox execution
- `spring.ai.agents.sandbox.local.working-directory` - Working directory for local sandbox

### Authentication and Environment Variables
- **Preferred**: Claude CLI session authentication (`claude auth login`)
- **Alternative**: ANTHROPIC_API_KEY environment variable (may conflict with session auth)
- **Internal**: CLAUDE_CODE_ENTRYPOINT=sdk-java (set automatically by SDK)

### Testing Strategy
- Unit tests for all core abstractions
- Integration tests (`*IT.java`) for CLI integrations
- Smoke tests for end-to-end workflows
- Spring testing patterns:
  - `@Import(MockSandboxConfiguration.class)` for unit tests with mocked sandbox
  - `@TestPropertySource(properties = "spring.ai.agents.sandbox.docker.enabled=false")` for local sandbox testing
  - Direct `Sandbox` dependency injection in test classes

## Package Structure
- `org.springaicommunity.agents.model.*` - Core abstractions
- `org.springaicommunity.agents.client.*` - High-level client API
- `org.springaicommunity.agents.claudecode.*` - Claude Code implementation
- `org.springaicommunity.agents.gemini.*` - Gemini implementation
- `org.springaicommunity.agents.sweagent.*` - SWE-bench implementation
- `org.springaicommunity.agents.*.sdk.*` - Provider SDK implementations
- `org.springaicommunity.agents.core.*` - JBang agent infrastructure
- `org.springaicommunity.agents.helloworld.*` - Example agent implementations

## JBang Directory Structure
```
spring-ai-agents/
├── jbang/
│   ├── launcher.java         # Ultra-thin JBang launcher script
│   └── README.md            # JBang usage documentation
├── jbang-catalog.json       # JBang catalog for distribution
├── spring-ai-agents-core/   # Core agent infrastructure
│   └── src/main/java/.../core/
│       ├── AgentRunner.java      # Functional interface
│       ├── Launcher.java         # Orchestration
│       ├── AgentSpecLoader.java  # YAML loading
│       ├── InputMerger.java      # Input merging
│       └── LocalConfigLoader.java # Config loading
└── agents/                  # Individual agent modules
    ├── hello-world-agent/
    │   ├── src/main/java/.../HelloWorldAgentRunner.java
    │   └── src/main/resources/agents/hello-world.yaml
    └── code-coverage-agent/
        └── src/main/resources/agents/coverage.yaml
```

## Process Management

### MANDATORY: Use zt-exec for All Process Execution

**All process/subprocess execution MUST use the zt-exec library.** Never use Java's built-in `ProcessBuilder` or `Runtime.exec()`.

#### Why zt-exec?
- Robust process lifecycle management
- Reliable output capture (stdout/stderr)
- Timeout handling without thread interruption issues
- Exit code handling
- Process destruction safeguards

#### Dependency
zt-exec is available through the `spring-ai-agent-model` dependency (transitive):
```xml
<!-- Already included transitively via spring-ai-agent-model -->
<dependency>
    <groupId>org.zeroturnaround</groupId>
    <artifactId>zt-exec</artifactId>
</dependency>
```

#### Basic Usage Pattern
```java
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;

// Execute a command with timeout
ProcessResult result = new ProcessExecutor()
    .command("vendir", "--version")
    .timeout(5, TimeUnit.SECONDS)
    .readOutput(true)
    .execute();

String output = result.outputUTF8();
int exitCode = result.getExitValue();
boolean success = exitCode == 0;
```

#### Advanced Usage
```java
// Execute with working directory and environment
ProcessResult result = new ProcessExecutor()
    .command("vendir", "sync", "--file", "vendir.yml")
    .directory(workingDir.toFile())
    .environment("VENDIR_CACHE_DIR", "/tmp/cache")
    .timeout(300, TimeUnit.SECONDS)
    .readOutput(true)
    .execute();
```

#### Error Handling
```java
try {
    ProcessResult result = new ProcessExecutor()
        .command("some-cli", "arg1", "arg2")
        .timeout(60, TimeUnit.SECONDS)
        .readOutput(true)
        .execute();

    if (result.getExitValue() != 0) {
        logger.warn("Command failed: {}", result.outputUTF8());
    }
} catch (org.zeroturnaround.exec.InvalidExitValueException e) {
    // zt-exec throws this for non-zero exit codes if not configured otherwise
    // Handle or let it propagate as runtime exception
} catch (Exception e) {
    logger.error("Process execution failed", e);
}
```

#### Examples in Codebase
- `LocalSandbox.java` - Sandbox command execution
- `ClaudeCliDiscovery.java` - CLI discovery with version check
- `VendirContextAdvisor.java` - Context engineering with vendir
- `CLITransport.java` - Claude/Gemini CLI communication

## CI/CD

### Docker Image for CI
The project maintains a pre-built Docker image (`ghcr.io/spring-ai-community/agents-runtime:latest`) with all required CLIs pre-installed:
- JDK 17 (Temurin)
- Maven 3.9.11
- Node.js LTS
- Claude Code CLI (latest)
- Gemini CLI (latest)
- Vendir CLI v0.44.0

**Building the image locally:**
```bash
docker build -f Dockerfile.agents-runtime -t agents-runtime:local .
```

**Using the image in CI:**
The CI workflow installs CLIs directly on GitHub Actions runners for now. Future optimization will use the pre-built Docker container to speed up builds.

**Triggering image rebuild:**
The image is automatically rebuilt:
- Weekly on Mondays (scheduled)
- When Dockerfile changes are pushed to main
- Manually via workflow_dispatch

## Troubleshooting

### Complex Prompt Issues
**Problem**: Agent hangs or fails with complex prompts containing quotes, commas, special characters
**Solution**: The SDK now uses Unix-style `--` separator before prompts to prevent shell parsing issues
**Example**: `claude --print -- "Create a directory called 'project', make README.md with info"`

### Authentication Conflicts
**Problem**: Tests hang or CLI authentication fails
**Solution**: Use Claude CLI session authentication instead of mixing API keys and session auth
```bash
claude auth login  # Preferred approach
unset ANTHROPIC_API_KEY  # Avoid conflicts
```

### Performance Expectations
- **Simple tasks**: ~20-30 seconds
- **Complex tasks**: ~40-60 seconds
- **Timeout**: Tests use 3+ minute timeouts for complex operations

### Sandbox Selection

**LocalSandbox** (default):
- Direct host execution (⚠️ NO ISOLATION)
- Uses zt-exec for robust process management
- Fast startup, no Docker required
- Enhanced logging for debugging
- Suitable for development and trusted environments

**DockerSandbox** (optional, recommended for production):
- Complete container isolation
- Requires Docker daemon
- Automatic container lifecycle management
- Enable via `spring.ai.agents.sandbox.docker.enabled=true`
- Safer for untrusted code execution

**Configuration:**
```properties
# Use Docker sandbox (recommended for production)
spring.ai.agents.sandbox.docker.enabled=true
spring.ai.agents.sandbox.docker.image-tag=ghcr.io/spring-ai-community/agents-runtime:latest

# Use Local sandbox (default, faster for development)
spring.ai.agents.sandbox.docker.enabled=false
spring.ai.agents.sandbox.local.working-directory=/path/to/workspace
```

### Exception Handling
All exceptions are runtime exceptions:
- `SandboxException` - Wraps all sandbox execution errors
- `TimeoutException` - Command timeout (runtime, not checked)
- `ClaudeSDKException` - Claude CLI errors (runtime, not checked)

### Verifying Snapshot Deployments to Maven Central

Maven Central has a known UI bug that prevents browsing SNAPSHOT artifacts via the web interface. However, snapshots ARE published and accessible.

**To verify a snapshot was published successfully:**

1. Check the maven-metadata.xml directly:
   ```
   https://central.sonatype.com/repository/maven-snapshots/org/springaicommunity/agents/{artifact-id}/{version}-SNAPSHOT/maven-metadata.xml
   ```

2. Example for spring-ai-agent-model:
   ```
   https://central.sonatype.com/repository/maven-snapshots/org/springaicommunity/agents/spring-ai-agent-model/0.1.0-SNAPSHOT/maven-metadata.xml
   ```

3. Direct artifact download URLs (from maven-metadata.xml):
   ```
   https://central.sonatype.com/repository/maven-snapshots/org/springaicommunity/agents/{artifact-id}/{version}-SNAPSHOT/{artifact-id}-{timestamp-buildNumber}.{extension}
   ```

**Note:** The web UI at `https://central.sonatype.com/` will show "SNAPSHOT Browsing Unavailable" but artifacts are still accessible via Maven and direct URLs.

## Development Guidelines

### Commit Messages
- **Do NOT credit Claude Code in commit messages**
- **Do NOT reference internal planning documents** (plans/, roadmap files, internal notes)
- Use clear, descriptive commit messages that explain the change
- Follow conventional commit format when appropriate (feat:, fix:, docs:, etc.)
- Focus on what changed and why, not planning artifacts or internal processes

### Test Policy
- **NEVER disable Claude Code or Gemini CLI tests to fix CI issues**
- These tests are critical for ensuring proper integration with external CLI tools
- If tests fail due to missing CLI tools, fix the environment setup (install CLIs, add API keys)
- Integration tests validate real-world usage and must remain enabled
- Disabling these tests masks real problems and breaks the development workflow

### CI/CD Success Criteria
- **CI optimization work is ONLY complete when all previously passing tests are passing again**
- **Leaving CI failing is NEVER acceptable** - any optimization that breaks existing functionality is incomplete
- Performance improvements are meaningless if they come at the cost of test failures
- The goal is to improve CI speed AND maintain 100% test reliability
- If optimization work introduces failures, the work must continue until failures are resolved
- "Separate issue" or "can be addressed later" are not acceptable conclusions for failing CI
## Running Integration Tests

To run integration tests (IT) specifically:
```bash
# Run all IT tests
./mvnw test -pl provider-sdks/claude-agent-sdk -Dtest="*IT"

# Run a specific IT test
./mvnw test -pl provider-sdks/claude-agent-sdk -Dtest="HookIntegrationIT"
```

Note: IT tests are run via surefire (not failsafe). They require Claude CLI to be installed and available.

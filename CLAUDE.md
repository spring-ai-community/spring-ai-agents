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
- Integration tests follow the `*IT.java` naming convention

### Code Quality
- Code formatting is enforced via `spring-javaformat-maven-plugin`
- Formatting validation runs during the `validate` phase
- Use Spring's Java code formatting conventions

### JBang Agent Launcher
- `jbang jbang/launcher.java --agent hello-world --path test.txt` - Run agents without build process
- `jbang jbang/launcher.java --agent coverage --target_coverage 90` - Run coverage agent
- Uses configuration precedence: Agent defaults → run.yaml → CLI arguments
- See `jbang/README.md` for complete usage guide
- Integration tests: `./mvnw test -pl agents/hello-world-agent -Dtest=HelloWorldAgentIT`

### Sample Applications
- `cd samples/hello-world && mvn spring-boot:run` - Run the Hello World sample
- Authentication: Uses Claude CLI session authentication (recommended) or ANTHROPIC_API_KEY
- Note: Session authentication (from `claude auth login`) is preferred over API keys to avoid conflicts

## Architecture Overview

### Multi-Module Maven Project Structure

**Core Abstractions** (`agent-models/spring-ai-agent-model/`)
- `AgentModel` - Core interface for autonomous development agents
- `AgentTaskRequest` - Task specification with goal, workspace, and constraints
- `AgentResponse` - Execution result with metadata and content
- `AgentOptions` - Configuration options for agent behavior

**Provider SDKs** (`provider-sdks/`)
- `claude-code-sdk/` - CLI client for Claude Code with resilience features
- `gemini-cli-sdk/` - CLI client for Gemini with robust transport
- `swe-agent-sdk/` - SWE-bench agent integration

**Agent Implementations** (`agent-models/`)
- `spring-ai-claude-code/` - Spring AI adapter for Claude Code
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
- All provider SDKs wrap external CLI tools (claude, gemini, swe-agent)
- Robust process management using zt-exec library for reliable execution
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
**Docker Sandbox** (preferred):
- Provides complete isolation
- Requires Docker daemon
- Automatic container lifecycle management

**Local Sandbox** (fallback):
- Direct host execution (⚠️ NO ISOLATION)
- Uses zt-exec for robust process management
- Enhanced logging for debugging

### Exception Handling
All exceptions are runtime exceptions:
- `SandboxException` - Wraps all sandbox execution errors
- `TimeoutException` - Command timeout (runtime, not checked)
- `ClaudeSDKException` - Claude CLI errors (runtime, not checked)

## Development Guidelines

### Commit Messages
- **Do NOT credit Claude Code in commit messages**
- Use clear, descriptive commit messages that explain the change
- Follow conventional commit format when appropriate (feat:, fix:, docs:, etc.)
- Focus on what the change accomplishes, not who created it
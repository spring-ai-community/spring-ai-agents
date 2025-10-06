# Spring AI Agents

> **⚠️ Important Notice for Fork Owners**: We cleaned up git history on Sept 28, 2025. If you have a fork, please see [Issue #2](https://github.com/spring-ai-community/spring-ai-agents/issues/2) for update instructions.

## 📊 SpringOne 2025 Presentation
This project was featured in a talk at SpringOne 2025 by Mark Pollack. View the presentation: [springone-2025-presentation.html](springone-2025-presentation.html)

<!-- [![Build Status](https://github.com/spring-ai-community/spring-ai-agents/workflows/CI/badge.svg)](https://github.com/spring-ai-community/spring-ai-agents/actions) -->
<!-- [![Maven Central](https://img.shields.io/maven-central/v/org.springaicommunity.agents/spring-ai-agents-parent.svg)](https://search.maven.org/search?q=g:org.springaicommunity.agents) -->

**Maven Snapshot Artifacts**: Available from [Maven Central Snapshots](https://central.sonatype.com/repository/maven-snapshots/org/springaicommunity/agents/)

📖 **[Documentation](https://spring-ai-community.github.io/spring-ai-agents/)** | [Getting Started](https://spring-ai-community.github.io/spring-ai-agents/getting-started.html) | [API Reference](https://spring-ai-community.github.io/spring-ai-agents/api/agentclient.html) | [Spring AI Bench](https://github.com/spring-ai-community/spring-ai-bench)

> **Note**: This project is currently in development. The repository structure and APIs are subject to change.

Spring AI Agents provides autonomous CLI agent integrations for the Spring AI ecosystem. This project brings Claude Code, Gemini CLI, and SWE-bench agents to Spring applications as first-class citizens with Spring Boot auto-configuration support and secure sandbox isolation.

## Overview

Transform autonomous CLI agents into pluggable Spring components:
- **Claude Code CLI** - Production-ready code assistance and autonomous development tasks
- **Gemini CLI** - Google's Gemini models through command-line interface
- **SWE-bench Agent** - Software engineering benchmarking and evaluation
- **Secure Sandbox Execution** - Docker container isolation with local fallback

## Quick Start

### Try with JBang (Zero Setup Required)

The fastest way to try Spring AI Agents - no cloning, no building, just run:

```bash
# One-time setup: Add the catalog
jbang catalog add --name=springai https://raw.githubusercontent.com/spring-ai-community/spring-ai-agents/main/jbang-catalog.json

# Static content example
jbang agents@springai hello-world \
  path=greeting.txt \
  content="Hello Spring AI Agents!"

# AI-powered examples (requires API keys)
export ANTHROPIC_API_KEY="your-key-here"

jbang agents@springai hello-world-agent-ai \
  path=ai-greeting.txt \
  content="a creative message about AI agents" \
  provider=claude

jbang agents@springai hello-world-agent-ai \
  path=ai-future.txt \
  content="a vision of the future of AI" \
  provider=gemini
```

> **Note**: Gemini CLI can only write files within your current project directory due to workspace restrictions. Use relative paths like `myfile.txt` instead of absolute paths like `/tmp/myfile.txt`.

### Maven Dependencies

```xml
<dependency>
    <groupId>org.springaicommunity.agents</groupId>
    <artifactId>spring-ai-starter-agent</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

### Basic Usage

```java
@Autowired
private AgentClient agentClient;

// Simple goal execution
String result = agentClient.run("Fix the failing test in UserServiceTest");

// Advanced goal configuration
AgentClientResponse response = agentClient
    .goal("Generate comprehensive API documentation")
    .workingDirectory(projectRoot)
    .run();
```

## Real-World Results: Code Coverage Agent

Our **first real-world agent** autonomously increased test coverage from **0% to 71.4%** on Spring's [gs-rest-service](https://spring.io/guides/gs/rest-service) tutorial in just 6 minutes.

### The Challenge

Run an autonomous agent on a real Spring codebase (not a toy example) and measure:
- **Coverage improvement** - From baseline to final percentage
- **Test quality** - Does it follow Spring OSS best practices?
- **Model differences** - Do Claude and Gemini perform differently with identical prompts?

### The Results

```java
// Simple, focused usage with judge verification
CoverageJudge judge = new CoverageJudge(80.0);

AgentClientResponse response = agentClient
    .goal("Increase JaCoCo test coverage to 80%")
    .workingDirectory(projectRoot)
    .advisors(JudgeAdvisor.builder().judge(judge).build())
    .run();
```

| Metric | Result |
|--------|--------|
| **Baseline Coverage** | 0% (no tests) |
| **Final Coverage** | 71.4% line, 87.5% instruction |
| **Target** | 20% (exceeded by 3.5x) |
| **Tests Generated** | 8 comprehensive methods |
| **Execution Time** | ~6 minutes |

### Claude vs Gemini: Quality Matters

Both models achieved **71.4% coverage**, but Claude perfectly followed Spring WebMVC best practices while Gemini didn't:

| Practice | Claude | Gemini | Why It Matters |
|----------|:------:|:------:|----------------|
| **@WebMvcTest** | ✅ | ❌ | 10x faster, loads only web layer |
| **jsonPath()** | ✅ | ❌ | Cleaner API, less boilerplate |
| **AssertJ** | ✅ | ✅ | Both used fluent assertions |
| **BDD naming** | ✅ | ❌ | Better test readability |
| **Edge cases** | ✅ | ✅ | Both comprehensive |

**Claude's generated test** (production-quality):

```java
@WebMvcTest(GreetingController.class)  // ✅ Fast, focused testing
public class GreetingControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void greetingShouldReturnDefaultMessageWhenNoParameterProvided() throws Exception {
        mockMvc.perform(get("/greeting"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").value("Hello, World!"))  // ✅ Clean validation
            .andExpect(jsonPath("$.id").isNumber());
    }

    @Test
    public void greetingShouldHandleUnicodeCharactersInName() throws Exception {
        mockMvc.perform(get("/greeting").param("name", "世界"))
            .andExpect(jsonPath("$.content").value("Hello, 世界!"));  // ✅ Edge cases
    }
    // ... 6 more comprehensive tests
}
```

> **Key Insight**: Same coverage percentage, different code quality. Model choice matters for enterprise standards.

📖 **[Read the full analysis →](https://spring-ai-community.github.io/spring-ai-agents/getting-started/code-coverage-agent.html)**

### Agent Advisors

Spring AI Agents implements the same advisor pattern as Spring AI's ChatClient, providing powerful interception points for execution flows:

```java
// Create an advisor to inject workspace context
public class WorkspaceContextAdvisor implements AgentCallAdvisor {

    @Override
    public AgentClientResponse adviseCall(AgentClientRequest request,
                                          AgentCallAdvisorChain chain) {
        // Inject context before execution
        String workspaceInfo = analyzeWorkspace(request.workingDirectory());
        request.context().put("workspace_info", workspaceInfo);

        // Execute agent
        AgentClientResponse response = chain.nextCall(request);

        // Add post-execution metrics
        response.context().put("files_modified", countModifiedFiles());
        return response;
    }

    @Override
    public String getName() {
        return "WorkspaceContext";
    }

    @Override
    public int getOrder() {
        return 100;
    }
}

// Register advisors with AgentClient builder
AgentClient client = AgentClient.builder(agentModel)
    .defaultAdvisor(new WorkspaceContextAdvisor())
    .defaultAdvisor(new TestExecutionAdvisor())
    .build();
```

**Common Advisor Use Cases**:
- **Context Engineering**: Git cloning, dependency sync, workspace preparation
- **Evaluation (Judges)**: Post-execution test running, file verification, quality checks
- **Security**: Goal validation, dangerous operation blocking
- **Observability**: Metrics collection, execution logging, performance tracking

See the [Agent Advisors documentation](https://spring-ai-community.github.io/spring-ai-agents/api/advisors.html) for complete details.

### Configuration

```yaml
spring:
  ai:
    agent:
      provider: claude-code  # or gemini, swebench
      max-steps: 6
      timeout: 300s
    agents:
      sandbox:
        docker:
          enabled: true
          image-tag: ghcr.io/spring-ai-community/agents-runtime:latest
        local:
          working-directory: /tmp
    claude-code:
      model: claude-sonnet-4-20250514
      bin: /usr/local/bin/claude
      yolo: true
    gemini:
      model: gemini-2.0-flash
      bin: /usr/local/bin/gemini
```

## Architecture

### Mono-repo Structure

```
spring-ai-agents/
├── agent-models/                    # Agent implementations
│   ├── spring-ai-agent-model/       # Core abstractions
│   ├── spring-ai-claude-agent/       # Claude Code agent
│   ├── spring-ai-gemini/           # Gemini CLI agent
│   └── spring-ai-swebench-agent/   # SWE-bench agent
├── provider-sdks/                   # CLI/SDK integrations
│   ├── claude-agent-sdk/            # Claude Code CLI client
│   ├── gemini-cli-sdk/             # Gemini CLI client
│   └── swe-agent-sdk/              # SWE-bench agent SDK
├── agents/                          # JBang-compatible agents
│   ├── hello-world-agent/          # Static file creation
│   └── hello-world-agent-ai/       # AI-powered file creation
├── spring-ai-agent-client/          # Unified client façade
├── spring-ai-agents-core/           # Agent launcher framework
├── spring-ai-spring-boot-starters/  # Auto-configuration
└── samples/                         # Example applications
```

### Key Components

- **`AgentModel`** - Core interface for autonomous agents
- **`AgentClient`** - ChatClient-inspired fluent API
- **`Sandbox`** - Secure execution environment (Docker/Local)
- **Provider SDKs** - CLI integrations with resilience features
- **Spring Boot Starter** - Auto-configuration and metrics

## Modules

| Module | Description | Maven Coordinates |
|--------|-------------|-------------------|
| Core Abstractions | `AgentModel`, `AgentTaskRequest`, `AgentCallResult` | `org.springaicommunity.agents:spring-ai-agent-model` |
| Claude Agent SDK | CLI client with resilience features | `org.springaicommunity.agents:claude-agent-sdk` |
| Gemini CLI SDK | Gemini command-line interface client | `org.springaicommunity.agents:gemini-cli-sdk` |
| SWE Agent SDK | SWE-bench agent SDK | `org.springaicommunity.agents:swe-agent-sdk` |
| Claude Code Agent | Spring AI adapter for Claude Code | `org.springaicommunity.agents:spring-ai-claude-agent` |
| Gemini Agent | Spring AI adapter for Gemini CLI | `org.springaicommunity.agents:spring-ai-gemini` |
| SWE-bench Agent | Software engineering benchmarking agent | `org.springaicommunity.agents:spring-ai-swebench-agent` |
| Agent Client | Unified fluent API | `org.springaicommunity.agents:spring-ai-agent-client` |
| Agents Core | Agent launcher framework | `org.springaicommunity.agents:spring-ai-agents-core` |
| Hello World Agent | Static file creation agent | `org.springaicommunity.agents:hello-world-agent` |
| Hello World AI Agent | AI-powered file creation agent | `org.springaicommunity.agents:hello-world-agent-ai` |
| Code Coverage Agent | Test coverage improvement agent | `org.springaicommunity.agents:code-coverage-agent` |
| Spring Boot Starter | Auto-configuration | `org.springaicommunity.agents:spring-ai-starter-agent` |

## Features

- **Production Ready**: Circuit breakers, retries, timeouts, and comprehensive error handling
- **Secure by Default**: Docker container isolation with automatic fallback to local execution
- **Spring Boot Integration**: Auto-configuration, externalized configuration, and actuator support
- **Observability**: Micrometer metrics and structured logging
- **Type Safe**: Full Java type safety with comprehensive JavaDoc
- **Flexible**: Provider-agnostic `AgentClient` with pluggable implementations
- **Advisor Pattern**: Powerful interception points for context engineering, validation, and evaluation

## Examples

See the [`samples/`](samples/) directory for complete examples:
- [`hello-world/`](samples/hello-world/) - Simple Spring Boot application demonstrating AgentClient basics
- [`context-engineering/`](samples/context-engineering/) - Advanced context engineering with VendirContextAdvisor

## Documentation

- [Getting Started Guide](docs/quickstart.md)
- [Architecture Overview](docs/architecture.md)
- [API Reference](docs/api-reference.md)

## Building and Testing

### Prerequisites

- Java 17 or higher
- Maven 3.8+ or Gradle 7+
- Claude CLI (for Claude Code agent)
- Gemini CLI (for Gemini agent)
- Docker (recommended for secure sandbox execution)

### Build Commands

#### Basic Build
```bash
# Compile all modules
./mvnw clean compile

# Build and run unit tests only
./mvnw clean test

# Full build with unit tests, no integration tests
./mvnw clean install
```

#### Integration Tests
```bash
# Run integration tests (requires live APIs and Docker)
./mvnw clean verify -Pfailsafe

# Run all tests including integration tests
./mvnw clean verify

# Run specific integration test (Failsafe - proper way)
./mvnw failsafe:integration-test -pl agent-models/spring-ai-claude-agent -Dit.test=ClaudeCodeLocalSandboxIT

# Run Docker infrastructure tests (Failsafe - proper way)
./mvnw failsafe:integration-test -pl agent-models/spring-ai-claude-agent -Dit.test=ClaudeDockerInfraIT

# Alternative: Surefire can run IT tests when explicitly specified
./mvnw test -pl agent-models/spring-ai-claude-agent -Dtest=ClaudeDockerInfraIT
```

#### Authentication for Tests
```bash
# Option 1: Claude CLI session authentication (recommended)
claude auth login
./mvnw test

# Option 2: Environment variables (may conflict with session)
export ANTHROPIC_API_KEY="your-key"
export GEMINI_API_KEY="your-key"
./mvnw test
```

### Test Categories

- **Unit Tests** (`*Test.java`): Fast, mocked dependencies
- **Integration Tests** (`*IT.java`): Real CLI execution, requires authentication
- **Docker Tests** (`*DockerInfraIT.java`): Container infrastructure testing

### Performance Expectations

- **Unit tests**: < 10 seconds total
- **Integration tests**: 20-60 seconds per test (depends on complexity)
- **Docker tests**: 10-15 seconds per test (container overhead)

## Contributing

This project follows the [Spring AI Community Guidelines](https://github.com/spring-ai-community). 

## License

Spring AI Agents is Open Source software released under the [Apache 2.0 license](LICENSE).

## Status

**Current Status**: ⚠️ **Development Phase** - APIs and structure may change

This project is actively being developed. While the core functionality is working, we recommend waiting for the `0.1.0` stable release for production use.

## Migration Path

This project is designed to eventually integrate with the main [Spring AI](https://github.com/spring-projects/spring-ai) project. The package structure and module organization are designed to make this transition seamless when ready.
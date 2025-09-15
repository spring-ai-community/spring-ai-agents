# Spring AI Agents

## 📊 SpringOne 2025 Presentation
This project was featured in a talk at SpringOne 2025 by Mark Pollack. View the presentation: [springone-2025-presentation.html](springone-2025-presentation.html)

[![Build Status](https://github.com/spring-ai-community/spring-ai-agents/workflows/CI/badge.svg)](https://github.com/spring-ai-community/spring-ai-agents/actions)
[![Maven Central](https://img.shields.io/maven-central/v/org.springaicommunity.agents/spring-ai-agents-parent.svg)](https://search.maven.org/search?q=g:org.springaicommunity.agents)

📖 **[Documentation](https://spring-ai-community.github.io/spring-ai-agents/)** | [Getting Started](https://spring-ai-community.github.io/spring-ai-agents/getting-started.html) | [API Reference](https://spring-ai-community.github.io/spring-ai-agents/api/agentclient.html)

> **Note**: This project is currently in development. The repository structure and APIs are subject to change.

Spring AI Agents provides autonomous CLI agent integrations for the Spring AI ecosystem. This project brings Claude Code, Gemini CLI, and SWE-bench agents to Spring applications as first-class citizens with Spring Boot auto-configuration support.

## Overview

Transform autonomous CLI agents into pluggable Spring components:
- **Claude Code CLI** - Production-ready code assistance and autonomous development tasks
- **Gemini CLI** - Google's Gemini models through command-line interface
- **SWE-bench Agent** - Software engineering benchmarking and evaluation

## Quick Start

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

### Configuration

```yaml
spring:
  ai:
    agent:
      provider: claude-code  # or gemini, swebench
      max-steps: 6
      timeout: 300s
    claude-code:
      model: claude-3-5-sonnet-20241022
      bin: /usr/local/bin/claude
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
│   ├── spring-ai-claude-code/       # Claude Code agent
│   ├── spring-ai-gemini/           # Gemini CLI agent
│   └── spring-ai-swebench-agent/   # SWE-bench agent
├── provider-sdks/                   # CLI/SDK integrations
│   ├── claude-code-sdk/            # Claude Code CLI client
│   └── gemini-cli-sdk/             # Gemini CLI client
├── spring-ai-agent-client/          # Unified client façade
├── spring-ai-spring-boot-starters/  # Auto-configuration
└── samples/                         # Example applications
```

### Key Components

- **`AgentModel`** - Core interface for autonomous agents
- **`AgentClient`** - ChatClient-inspired fluent API
- **Provider SDKs** - CLI integrations with resilience features
- **Spring Boot Starter** - Auto-configuration and metrics

## Modules

| Module | Description | Maven Coordinates |
|--------|-------------|-------------------|
| Core Abstractions | `AgentModel`, `AgentTaskRequest`, `AgentCallResult` | `org.springaicommunity.agents:spring-ai-agent-model` |
| Claude Code SDK | CLI client with resilience features | `org.springaicommunity.agents:claude-code-sdk` |
| Gemini CLI SDK | Gemini command-line interface client | `org.springaicommunity.agents:gemini-cli-sdk` |
| Claude Code Agent | Spring AI adapter for Claude Code | `org.springaicommunity.agents:spring-ai-claude-code` |
| Gemini Agent | Spring AI adapter for Gemini CLI | `org.springaicommunity.agents:spring-ai-gemini` |
| SWE-bench Agent | Software engineering benchmarking agent | `org.springaicommunity.agents:spring-ai-swebench-agent` |
| Agent Client | Unified fluent API | `org.springaicommunity.agents:spring-ai-agent-client` |
| Spring Boot Starter | Auto-configuration | `org.springaicommunity.agents:spring-ai-starter-agent` |

## Features

- **Production Ready**: Circuit breakers, retries, timeouts, and comprehensive error handling
- **Spring Boot Integration**: Auto-configuration, externalized configuration, and actuator support
- **Observability**: Micrometer metrics and structured logging
- **Type Safe**: Full Java type safety with comprehensive JavaDoc
- **Flexible**: Provider-agnostic `AgentClient` with pluggable implementations

## Examples

See the [`samples/`](samples/) directory for complete examples:
- [`hello-world/`](samples/hello-world/) - Simple Spring Boot application demonstrating AgentClient basics

## Documentation

- [Getting Started Guide](docs/quickstart.md)
- [Architecture Overview](docs/architecture.md)
- [API Reference](docs/api-reference.md)

## Requirements

- Java 17 or higher
- Maven 3.8+ or Gradle 7+
- Claude CLI (for Claude Code agent)
- Gemini CLI (for Gemini agent)

## Contributing

This project follows the [Spring AI Community Guidelines](https://github.com/spring-ai-community). 

## License

Spring AI Agents is Open Source software released under the [Apache 2.0 license](LICENSE).

## Status

**Current Status**: ⚠️ **Development Phase** - APIs and structure may change

This project is actively being developed. While the core functionality is working, we recommend waiting for the `0.1.0` stable release for production use.

## Migration Path

This project is designed to eventually integrate with the main [Spring AI](https://github.com/spring-projects/spring-ai) project. The package structure and module organization are designed to make this transition seamless when ready.
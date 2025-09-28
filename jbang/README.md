# JBang Launcher Scripts

This directory contains JBang scripts for running Spring AI Agents without requiring a build process.

## launcher.java

The main entry point for executing agents via JBang. Provides a simple command-line interface for running agents with various configurations.

### Usage

```bash
# Basic usage
jbang launcher.java --agent <agent-name> [options]

# Hello World example
jbang launcher.java --agent hello-world --path myfile.txt --content "Hello World!"

# Using default values
jbang launcher.java --agent hello-world --path myfile.txt

# Coverage agent example
jbang launcher.java --agent coverage --target_coverage 90 --module core
```

### Command Line Options

- `--agent <name>` - Agent to run (required)
- `--workdir <path>` - Working directory for execution
- `--sandbox <type>` - Sandbox type (local, docker)
- `--<key> <value>` - Agent-specific input parameters

### Configuration Files

You can also use a `run.yaml` file in your working directory:

```yaml
agent: hello-world
inputs:
  path: "example.txt"
  content: "Hello from YAML!"
workingDirectory: "/tmp/workspace"
env:
  sandbox: local
```

Then simply run:
```bash
jbang launcher.java
```

### Precedence Rules

Configuration values are merged with this precedence:
1. Agent defaults (from agent YAML spec)
2. `run.yaml` file values
3. Command line arguments (highest priority)

### Agent Types

Currently supported agents:
- `hello-world` - Creates files with specified content
- `coverage` - Code coverage analysis (work in progress)

### Requirements

- JBang 0.118.0 or later
- Java 21 or later
- Maven artifacts available in local repository or Maven Central

### Distribution

This launcher can be distributed via JBang catalog:

```bash
# Add the catalog
jbang catalog add springai https://raw.githubusercontent.com/spring-ai-community/spring-ai-agents/main/jbang-catalog.json

# Use the alias
jbang springai@agents --agent hello-world --path test.txt
```

### Development

For local development, ensure the Spring AI Agents modules are installed:

```bash
# From project root
./mvnw clean install

# Then run the launcher
jbang jbang/launcher.java --agent hello-world --path test.txt
```

### Architecture

The launcher follows a clean architecture:
- **launcher.java** - Ultra-thin JBang script (3 lines of logic)
- **LocalConfigLoader** - Handles configuration loading and merging
- **Launcher** - Agent discovery and execution orchestration with inline input merging
- **AgentRunner** - Functional interface for agent implementations
- **AgentSpecLoader** - YAML specification loading with filesystem fallback
- **Result** - Execution result with data payload for judge integration

This design provides a simple entry point while maintaining clean separation of concerns and extensibility. The Result record now includes a data field to support future judge integration patterns.
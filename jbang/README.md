# JBang Launcher Scripts

This directory contains JBang scripts for running Spring AI Agents without requiring a build process.

## launcher.java

The main entry point for executing agents via JBang. Provides a simple command-line interface for running agents with various configurations.

## Distribution via JBang Catalog (Recommended)

The easiest way to use Spring AI Agents is through the JBang catalog:

```bash
# One-time setup: Add the catalog
jbang catalog add --name=springai \
  https://raw.githubusercontent.com/spring-ai-community/spring-ai-agents/main/jbang-catalog.json

# Verify installation
jbang catalog list | grep springai
jbang alias list springai

# Use agents via catalog alias
jbang agents@springai hello-world path=test.txt content="Hello World!"

# AI-powered agent with Claude
export ANTHROPIC_API_KEY="your-key"
jbang agents@springai hello-world-agent-ai \
  path=ai-test.txt \
  content="a creative message" \
  provider=claude
```

## Alternative: Direct URL Invocation

You can also run the launcher directly without the catalog:

```bash
jbang https://raw.githubusercontent.com/spring-ai-community/spring-ai-agents/main/jbang/launcher.java \
  hello-world \
  path=greeting.txt \
  content="Hello from Direct URL!"
```

### Usage

```bash
# Basic usage - CLI provides inputs
jbang agents@springai <agent-id> key=value key2=value2 ...

# Hello World example (static content)
jbang agents@springai hello-world path=myfile.txt content="Hello World!"

# AI-powered Hello World examples (requires API keys)
jbang agents@springai hello-world-agent-ai path=greeting.txt content="a creative message" provider=claude
jbang agents@springai hello-world-agent-ai path=ai-info.txt content="information about AI agents" provider=gemini

# Using default content (agent provides defaults)
jbang agents@springai hello-world path=myfile.txt

# Coverage agent example
jbang agents@springai coverage target_coverage=90 module=core

# Values can contain equals signs
jbang agents@springai hello-world path=output.txt content="url=http://example.com?a=b"

# Windows/PowerShell - quote values with spaces
jbang agents@springai hello-world path=output.txt content="Hello World with spaces"

# Empty values allowed
jbang agents@springai hello-world path=output.txt content=
```

### CLI Format

- First argument: Agent ID (format: `[a-z0-9][a-z0-9-]{0,63}`)
- Remaining arguments: `key=value` pairs (split on first `=`)
- Values are strings (agents handle type conversion)
- **Duplicate keys**: Last value wins
- **Empty values**: `key=` produces empty string
- **Complex values**: `url=http://x.com?a=b=c` preserves all `=` after first

### Optional Environment Configuration

You can use `.agents/run.yaml` for environment settings only (not inputs):

```yaml
workingDirectory: /tmp/workspace
env:
  sandbox: docker
  image: ghcr.io/spring-ai/agent:latest
```

**Why .agents/ is better:**
- **Separation**: CLI = inputs, `.agents/` = environment settings
- **Local dev**: Add `.agents/` to `.gitignore` to keep personal settings local
- **Scalable**: Perfect place for logs, temp files, caches later
- **Secure**: Central location for sensitive environment values

Priority order for runspec files:
1. `SPRING_AI_RUNSPEC` environment variable path
2. `.agents/run.yaml` (preferred)
3. `./run.yaml` (backward compatibility)
4. `./runspec.yaml` (backward compatibility)
5. None (defaults: cwd=".", env={})

### Agent Types

Currently supported agents:
- `hello-world` - Creates files with specified content (static/deterministic)
- `hello-world-agent-ai` - **AI-powered** file creation using Claude Code or Gemini CLI
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
jbang springai@agents hello-world path=test.txt content="Hello World!"
```

### Development

For local development, ensure the Spring AI Agents modules are installed:

```bash
# From project root
./mvnw clean install

# Then run the launcher
jbang jbang/launcher.java hello-world path=test.txt
```

### Architecture

The launcher follows a clean architecture:
- **launcher.java** - Ultra-thin JBang script (3 lines of logic)
- **LocalConfigLoader** - CLI k=v parsing with optional runspec.yaml for environment
- **Launcher** - Agent discovery and execution orchestration
- **AgentRunner** - Functional interface for agent implementations with self-contained validation
- **AgentSpecLoader** - YAML specification loading with filesystem fallback
- **Result** - Execution result with data payload for judge integration

Key principles:
- **CLI provides inputs**: All agent parameters come from command line
- **YAML provides environment**: Optional runspec.yaml for working directory and environment variables
- **Agent-owned validation**: Each agent handles its own defaults, type conversion, and validation
- **No hidden merging**: What you type is what you get
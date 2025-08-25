# Spring AI Agents - Hello World Sample

A simple Spring Boot 3.5.0 application demonstrating Spring AI Agents with Claude Code.

## What it does

Creates a `hello.txt` file with "Hello, World!" content in the current directory using an autonomous AI agent.

## Prerequisites

1. **Java 17+** and **Maven 3.6.3+**
2. **Claude CLI**: `npm install -g @anthropic-ai/claude-code`
3. **API Key**: Get from [Anthropic Console](https://console.anthropic.com/)

## Quick Start

```bash
# Set your API key
export ANTHROPIC_API_KEY="your-api-key-here"

# Run the sample
mvn spring-boot:run
```

## Expected Output

```
Starting Spring AI Agents Hello World sample...
Working directory: /path/to/samples/hello-world
Executing goal: Create a simple hello.txt file with the content 'Hello, World!'
Goal completed successfully!
✅ File created successfully!
📄 File content: Hello, World!
```

The `hello.txt` file will be created in the same directory where you ran the command.

## Troubleshooting

- **"ANTHROPIC_API_KEY not set"**: Set environment variable with your API key
- **"Claude CLI not available"**: Run `npm install -g @anthropic-ai/claude-code`
- **API key issues**: Verify your key at [console.anthropic.com](https://console.anthropic.com/)

## Learn More

- [Spring AI Agents Documentation](../../docs/src/main/antora/modules/ROOT/pages/index.adoc)
- [AgentClient API Guide](../../docs/src/main/antora/modules/ROOT/pages/api/agentclient.adoc)
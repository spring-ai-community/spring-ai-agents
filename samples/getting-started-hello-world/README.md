# Getting Started - Hello World

This sample demonstrates Spring AI Agents with Spring Boot autoconfiguration.

## Prerequisites

1. **Claude CLI installed**:
   ```bash
   npm install -g @anthropic-ai/claude-code
   ```

2. **Authentication** (choose one):
   - Session authentication (recommended): `claude auth login`
   - API key: `export ANTHROPIC_API_KEY="your-api-key"`

## Running the Sample

From the `samples/getting-started-hello-world` directory:

```bash
../../mvnw spring-boot:run
```

Or from the project root:

```bash
./mvnw spring-boot:run -pl samples/getting-started-hello-world
```

## What It Does

The sample demonstrates:

1. **Spring Boot autoconfiguration** - Just add the dependency and inject `AgentClient.Builder`

2. **AgentClient fluent API** - Execute agent goals with a simple builder:
   ```java
   AgentClient client = agentClientBuilder.build();
   AgentClientResponse response = client
       .goal("Create a simple Hello World Java class in HelloWorld.java")
       .workingDirectory(workingDirectory)
       .run();
   ```

3. **File judge verification** - Using FileExistsJudge to verify the agent created the expected file

## Expected Output

The agent will:
- Create a `HelloWorld.java` file in the working directory
- Log the execution results
- Verify file creation using the FileExistsJudge

## Autoconfiguration

Spring Boot provides:
- `AgentClient.Builder` bean (prototype scope) - automatically configured
- Claude agent with sensible defaults (model: claude-sonnet-4-5, yolo: true)
- LocalSandbox for secure command execution

No manual configuration needed!

## Key Classes

- `GettingStartedApplication` - Spring Boot main class
- `HelloWorldRunner` - CommandLineRunner demonstrating AgentClient injection and usage

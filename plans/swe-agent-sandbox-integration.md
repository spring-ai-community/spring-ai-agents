# SWE Agent Sandbox Integration Plan

## Status: ✅ Completed (September 2025)

## Overview
Update SweAgentModel to use the constructor-based dependency injection pattern with Sandbox, similar to GeminiAgentModel and ClaudeCodeAgentModel, to enable full TCK integration.

## Background
During the TCK implementation (September 2025), all agent models were updated to use a consistent pattern except SweAgentModel:

**Current Pattern (Gemini/Claude Code)**:
```java
public GeminiAgentModel(GeminiClient client, AgentOptions options, Sandbox sandbox) {
    this.client = client;
    this.options = options;
    this.sandbox = sandbox;  // Constructor-based DI
}
```

**Legacy Pattern (SWE Agent)**:
```java
public SweAgentModel(SweCliApi sweCliApi, SweAgentOptions defaultOptions) {
    this.sweCliApi = sweCliApi;
    this.defaultOptions = defaultOptions;
    // No sandbox - uses direct CLI execution
}
```

## Required Changes

### 1. Update SweAgentModel Constructor
Add Sandbox parameter to constructor:
```java
public SweAgentModel(SweCliApi sweCliApi, SweAgentOptions options, Sandbox sandbox) {
    this.sweCliApi = sweCliApi;
    this.options = options;
    this.sandbox = sandbox;
}
```

### 2. Update Execution Pattern
Change from direct CLI execution to sandbox-based execution:
```java
// Current: Direct CLI execution
SweResult result = sweCliApi.execute(command, workingDir);

// Target: Sandbox-based execution
ExecResult result = sandbox.exec(ExecSpec.builder()
    .command(buildSweCommand())
    .timeout(options.getTimeout())
    .build());
```

### 3. Add TCK Tests
Once updated, add full TCK integration:
- `SweAgentLocalSandboxIT.java` - extends AbstractAgentModelTCK with LocalSandbox
- `SweAgentDockerSandboxIT.java` - extends AbstractAgentModelTCK with DockerSandbox

### 4. Update Auto-Configuration
Add Spring Boot auto-configuration similar to other agents:
- `SweAgentAutoConfiguration.java`
- `SweAgentProperties.java`
- Factory methods for LocalSandbox and DockerSandbox variants

## Current Test Structure
Infrastructure tests are already in place:
- ✅ `DockerSandboxInfrastructureIT.java` - Tests Python execution and SWE-specific environment variables
- ✅ `SweAgentLocalExecutionIT.java` - Legacy tests (renamed from SweAgentModelIT)

## Benefits
1. **Consistent Architecture** - All agent models follow the same pattern
2. **Full TCK Coverage** - SWE Agent gets same test coverage as other agents
3. **Docker Support** - Enables Docker-based execution for better isolation
4. **Spring Integration** - Enables auto-configuration and dependency injection

## Implementation Notes
- SweAgentModel currently lacks Spring Boot auto-configuration (unlike other agents)
- The mini-SWE-agent CLI integration may need adjustments for sandbox execution
- Consider backward compatibility for existing SweAgentModel users

## Related Files
- `SweAgentModel.java` - Main implementation to update
- `SweAgentModelTest.java` - Unit tests to update
- `SweAgentLocalExecutionIT.java` - Integration tests to migrate to TCK pattern
- `AbstractAgentModelTCK.java` - TCK base class ready for SWE Agent
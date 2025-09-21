# Phase 2: AgentModel-centric Sandbox Integration

## Design: AgentClient → AgentModel(+Sandbox) → SDK → CLI

### Architecture Overview
The elegant, flexible path where:
- **SDKs stay pure**: Build commands, parse outputs only
- **AgentModels own execution**: Choose sandbox, run, stream, retry
- **AgentClient provides configuration**: Users choose execution strategy

### 1. Public API Surface (minimal, future-proof)
- Add `AgentClient.Builder.sandbox(Sandbox sandbox)` method (optional, defaults from auto-config)
- Update AgentModel constructors to accept Sandbox parameter
- No SDK changes - they remain pure command builders/parsers

### 2. AgentModel Orchestration Pattern
For each AgentModel (ClaudeCode, Gemini):

```java
// Pattern for each AgentModel
public AgentResponse call(AgentTaskRequest request) throws Exception {
    // 1. SDK builds command
    List<String> cmd = sdk.buildCommand(request);

    // 2. Create ExecSpec
    ExecSpec spec = ExecSpec.builder()
        .command(cmd)
        .workdir(request.workingDirectory())
        .env(Map.of("ANTHROPIC_API_KEY", apiKey))
        .timeout(request.timeout())
        .build();

    // 3. Execute via sandbox
    ExecResult result = sandbox.exec(spec);

    // 4. Parse via SDK
    return sdk.parseResult(result.stdout(), result.stderr(), result.exitCode());
}
```

### 3. Spring Configuration & Defaults
- Central sandbox selection via Spring properties
- Auto-configuration provides LocalSandbox when Docker unavailable
- Models receive sandbox via dependency injection

### 4. Error Handling
- Map non-zero exit codes to domain errors in AgentModel
- Timeouts configured in ExecSpec
- Optional retry logic in AgentModel (for idempotent operations)

### 5. Testing Strategy
- Unit tests: Mock sandbox, verify AgentModel calls with expected commands
- Integration tests: Hello world echo, CLI version checks, error mapping
- No SDK test changes needed

### 6. Streaming Support (Future)
- Add optional `OutputListener` to ExecSpec for stdout/stderr line callbacks
- AgentModel wires listener to surface progress through AgentClient

## Implementation Tasks

### Phase 2A: Claude Code Integration ✅ COMPLETED
- [x] ~~Add `sandbox(Sandbox)` method to AgentClient.Builder~~ **Used SandboxProvider pattern instead**
- [x] Update ClaudeCodeAgentModel to use SandboxProvider for execution
- [x] Create unit tests with mock sandbox for Claude (7 tests)
- [x] Add integration tests for Docker sandbox with Claude (11 tests)
- [x] Fix DockerSandbox environment variable and timeout handling
- [x] Add comprehensive test coverage (28 total tests across 4 test suites)

### Phase 2B: Gemini CLI Integration
- [ ] Update GeminiAgentModel to use SandboxProvider for execution
- [ ] Create unit tests with mock sandbox for Gemini
- [ ] Add integration tests for Docker sandbox with Gemini

### Phase 2C: Spring Configuration
- [x] Create SandboxProvider interface for dependency injection
- [x] Create DefaultSandboxProvider with auto-detection
- [ ] Create Spring auto-configuration for sandbox defaults
- [ ] Add sandbox configuration properties
- [ ] Document sandbox configuration properties

## Accomplished (Phase 2A Complete)

### Infrastructure Created
- **SandboxProvider Interface**: Spring-idiomatic dependency injection pattern
- **DefaultSandboxProvider**: Auto-detection with singleton behavior
- **DockerSandbox Fixes**: Environment variables and timeout support
- **Dockerfile.agents-runtime**: JDK 17, Maven, Claude CLI, Gemini CLI
- **SDK Integration**: buildCommand() and parseResult() methods

### Test Coverage (28 tests total)
- **11 full-stack integration tests** with real Docker containers via TestContainers
- **7 unit tests** with mock SandboxProvider proving dependency injection
- **6 SDK tests** proving command building and result parsing
- **4 infrastructure tests** proving SandboxProvider interface functionality

### Key Pattern Implemented
```java
// AgentModel-centric execution pattern (PROVEN working)
public AgentResponse call(AgentTaskRequest request) {
    // 1. SDK builds command
    List<String> command = claudeCodeClient.buildCommand(prompt, cliOptions);

    // 2. Execute via sandbox with env vars and timeout
    ExecSpec spec = ExecSpec.builder()
        .command(command)
        .env(Map.of("CLAUDE_CODE_ENTRYPOINT", "sdk-java"))
        .timeout(cliOptions.getTimeout())
        .build();
    ExecResult execResult = sandboxProvider.getSandbox().exec(spec);

    // 3. SDK parses result
    QueryResult queryResult = claudeCodeClient.parseResult(execResult.mergedLog(), cliOptions);
    return buildAgentResponse(queryResult, execResult);
}
```

## Benefits
- **Single responsibility**: SDKs handle protocol, Models handle execution
- **Minimal integration points**: Only AgentModels need sandbox awareness
- **Future-proof**: New sandbox backends (AWS, E2B) plug in without SDK changes
- **Clean abstraction**: Sandbox is execution detail, not communication detail

## Future Extensions
- AWS Lambda sandbox backend
- E2B cloud sandbox backend
- Kubernetes pod sandbox backend
- All without changing SDK or AgentModel interfaces

## Learnings from Phase 2A Implementation

### Design Decisions That Worked Well
1. **SandboxProvider Pattern**: Spring-idiomatic constructor injection proved superior to factory methods
2. **AgentModel-centric Architecture**: SDKs stay pure (build command/parse result), AgentModels own execution
3. **Interface-based Design**: Easy to inject test doubles and swap implementations
4. **Auto-detection Fallback**: DefaultSandboxProvider automatically chooses Docker vs LocalSandbox

### Critical Implementation Details
1. **DockerSandbox Environment Variables**: TestContainers doesn't support env vars directly - must inject via shell script
2. **DockerSandbox Timeouts**: TestContainers lacks timeout support - implemented via CompletableFuture with cancellation
3. **Docker Image Requirements**: JDK 17, Maven, Claude CLI, Gemini CLI all needed for agent runtime
4. **Spring Java Format**: Always build entire project to avoid dependency issues between modules

### Test Strategy That Proved Effective
1. **Layered Testing**: Unit tests with mocks + Integration tests with real Docker
2. **TestContainers Integration**: Critical for proving end-to-end functionality
3. **Debug Tests**: Temporary test files help isolate specific issues during debugging
4. **Comprehensive Coverage**: 28 tests across 4 dimensions ensures all critical paths work

### Key Technical Discoveries
1. **TestContainers API**: `execInContainer(String... command)` is the correct method signature
2. **Shell Environment Injection**: `export VAR='value'; exec "$@"` pattern works reliably
3. **Timeout Implementation**: `CompletableFuture.get(timeout, MILLISECONDS)` with cancellation
4. **Maven Module Dependencies**: Always install parent modules before running tests in dependent modules

### Commands to Remember
- **Build entire project**: `mvn clean install -DskipTests -Dmaven.javadoc.skip=true`
- **Run integration tests**: `mvn test -Dtest=*FullStackIntegration* -Dsandbox.integration.test=true`
- **Format code**: `mvn spring-javaformat:apply`
- **Docker image for tests**: Tag existing image as `ghcr.io/spring-ai-community/agents-runtime:latest`

### Most Important Success Factor
**Integration tests with real Docker containers were non-negotiable** - they proved the complete solution works end-to-end and caught issues that unit tests missed (environment variables, timeouts, CLI availability).
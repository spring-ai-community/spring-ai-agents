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

### Phase 2A: Claude Code Integration
- [ ] Add `sandbox(Sandbox)` method to AgentClient.Builder
- [ ] Update ClaudeCodeAgentModel to use Sandbox for execution
- [ ] Create unit tests with mock sandbox for Claude
- [ ] Add integration tests for Docker sandbox with Claude

### Phase 2B: Gemini CLI Integration
- [ ] Update GeminiAgentModel to use Sandbox for execution
- [ ] Create unit tests with mock sandbox for Gemini
- [ ] Add integration tests for Docker sandbox with Gemini

### Phase 2C: Spring Configuration
- [ ] Create Spring auto-configuration for sandbox defaults
- [ ] Add sandbox configuration properties
- [ ] Document sandbox configuration properties

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
# Infrastructure Test Refactoring Plan

## Problem Statement
1. **Name Collisions**: All three agent modules have `DockerSandboxInfrastructureIT.java` - confusing in IDE
2. **Code Duplication**: ~80% of infrastructure tests are identical across modules
3. **Long Names**: Current naming pattern is getting verbose
4. **Onboarding Friction**: New agents need to duplicate all infrastructure tests

## Proposed Solution

### 1. Short Prefix Naming Convention
Rename infrastructure tests with 2-3 letter prefixes:

```
Current                           →  Proposed
DockerSandboxInfrastructureIT   →  CCDockerInfraIT   (Claude Code)
DockerSandboxInfrastructureIT   →  GemDockerInfraIT  (Gemini)
DockerSandboxInfrastructureIT   →  SweDockerInfraIT  (SWE Agent)
```

Benefits:
- Unique names for IDE navigation
- 40% shorter names
- Clear agent identification

### 2. Create AbstractDockerInfraTCK

Create a TCK for Docker infrastructure tests in `spring-ai-agent-model`:

```java
public abstract class AbstractDockerInfraTCK {

    protected DockerSandbox dockerSandbox;

    @BeforeEach
    void setUp() {
        dockerSandbox = new DockerSandbox(getDockerImage(), getVolumeMounts());
    }

    @AfterEach
    void tearDown() throws Exception {
        if (dockerSandbox != null) {
            dockerSandbox.close();
        }
    }

    // ========== Common Infrastructure Tests ==========

    @Test
    void testBasicExecution() throws Exception {
        ExecResult result = dockerSandbox.exec(
            ExecSpec.builder()
                .command("echo", "Hello from Docker")
                .timeout(Duration.ofSeconds(30))
                .build()
        );
        assertThat(result.success()).isTrue();
        assertThat(result.mergedLog()).contains("Hello from Docker");
    }

    @Test
    void testEnvironmentVariables() throws Exception {
        ExecResult result = dockerSandbox.exec(
            ExecSpec.builder()
                .command("printenv", "TEST_VAR")
                .env(Map.of("TEST_VAR", "test-value"))
                .timeout(Duration.ofSeconds(30))
                .build()
        );
        assertThat(result.success()).isTrue();
        assertThat(result.mergedLog().trim()).isEqualTo("test-value");
    }

    @Test
    void testWorkingDirectory() throws Exception {
        ExecResult result = dockerSandbox.exec(
            ExecSpec.builder()
                .command("pwd")
                .timeout(Duration.ofSeconds(30))
                .build()
        );
        assertThat(result.success()).isTrue();
        assertThat(result.mergedLog().trim()).isEqualTo("/work");
    }

    @Test
    void testTimeoutHandling() throws Exception {
        ExecSpec timeoutTest = ExecSpec.builder()
            .command("sleep", "10")
            .timeout(Duration.ofSeconds(2))
            .build();

        assertThatThrownBy(() -> dockerSandbox.exec(timeoutTest))
            .isInstanceOf(TimeoutException.class);
    }

    @Test
    void testErrorHandling() throws Exception {
        ExecResult result = dockerSandbox.exec(
            ExecSpec.builder()
                .command("ls", "/nonexistent")
                .timeout(Duration.ofSeconds(30))
                .build()
        );
        assertThat(result.failed()).isTrue();
        assertThat(result.exitCode()).isNotEqualTo(0);
    }

    @Test
    void testMultipleExecutions() throws Exception {
        for (int i = 1; i <= 3; i++) {
            ExecResult result = dockerSandbox.exec(
                ExecSpec.builder()
                    .command("echo", "execution " + i)
                    .build()
            );
            assertThat(result.success()).isTrue();
            assertThat(result.mergedLog()).contains("execution " + i);
        }
    }

    // ========== Abstract Methods for Agent-Specific Tests ==========

    protected abstract String getDockerImage();
    protected abstract List<String> getVolumeMounts();
    protected abstract void testAgentRuntime() throws Exception;
    protected abstract void testAgentEnvironment() throws Exception;
}
```

### 3. Simplified Agent-Specific Tests

Each agent only implements what's unique:

#### Claude Code
```java
@EnabledIfSystemProperty(named = "sandbox.integration.test", matches = "true")
class CCDockerInfraIT extends AbstractDockerInfraTCK {

    @Override
    protected String getDockerImage() {
        return "ghcr.io/spring-ai-community/agents-runtime:latest";
    }

    @Override
    protected List<String> getVolumeMounts() {
        return List.of();
    }

    @Test
    @Override
    protected void testAgentRuntime() throws Exception {
        // Test Node.js for Claude CLI
        ExecResult result = dockerSandbox.exec(
            ExecSpec.builder()
                .command("node", "--version")
                .timeout(Duration.ofSeconds(30))
                .build()
        );
        assertThat(result.success()).isTrue();
        assertThat(result.mergedLog()).startsWith("v");
    }

    @Test
    @Override
    protected void testAgentEnvironment() throws Exception {
        // Test Claude-specific environment
        ExecResult result = dockerSandbox.exec(
            ExecSpec.builder()
                .command("printenv")
                .env(Map.of("ANTHROPIC_API_KEY", "test-key"))
                .timeout(Duration.ofSeconds(30))
                .build()
        );
        assertThat(result.success()).isTrue();
        assertThat(result.mergedLog()).contains("ANTHROPIC_API_KEY=test-key");
    }
}
```

#### Gemini
```java
@EnabledIfSystemProperty(named = "sandbox.integration.test", matches = "true")
class GemDockerInfraIT extends AbstractDockerInfraTCK {

    @Override
    protected String getDockerImage() {
        return "ghcr.io/spring-ai-community/agents-runtime:latest";
    }

    @Override
    protected List<String> getVolumeMounts() {
        return List.of();
    }

    @Test
    @Override
    protected void testAgentRuntime() throws Exception {
        // Test Node.js for Gemini CLI
        ExecResult result = dockerSandbox.exec(
            ExecSpec.builder()
                .command("node", "--version")
                .timeout(Duration.ofSeconds(30))
                .build()
        );
        assertThat(result.success()).isTrue();
        assertThat(result.mergedLog()).startsWith("v");
    }

    @Test
    @Override
    protected void testAgentEnvironment() throws Exception {
        // Test Gemini-specific environment
        ExecResult result = dockerSandbox.exec(
            ExecSpec.builder()
                .command("printenv")
                .env(Map.of(
                    "GEMINI_API_KEY", "test-key",
                    "GOOGLE_API_KEY", "test-google-key"
                ))
                .timeout(Duration.ofSeconds(30))
                .build()
        );
        assertThat(result.success()).isTrue();
        assertThat(result.mergedLog()).contains("GEMINI_API_KEY=test-key");
        assertThat(result.mergedLog()).contains("GOOGLE_API_KEY=test-google-key");
    }
}
```

#### SWE Agent
```java
@EnabledIfSystemProperty(named = "sandbox.integration.test", matches = "true")
class SweDockerInfraIT extends AbstractDockerInfraTCK {

    @Override
    protected String getDockerImage() {
        return "ghcr.io/spring-ai-community/agents-runtime:latest";
    }

    @Override
    protected List<String> getVolumeMounts() {
        return List.of();
    }

    @Test
    @Override
    protected void testAgentRuntime() throws Exception {
        // Test Python for SWE Agent
        ExecResult result = dockerSandbox.exec(
            ExecSpec.builder()
                .command("python3", "--version")
                .timeout(Duration.ofSeconds(30))
                .build()
        );
        assertThat(result.success()).isTrue();
        assertThat(result.mergedLog()).containsIgnoringCase("python");
    }

    @Test
    @Override
    protected void testAgentEnvironment() throws Exception {
        // Test SWE-specific environment
        ExecResult result = dockerSandbox.exec(
            ExecSpec.builder()
                .command("printenv")
                .env(Map.of(
                    "OPENAI_API_KEY", "test-key",
                    "SWE_AGENT_CONFIG", "test-config"
                ))
                .timeout(Duration.ofSeconds(30))
                .build()
        );
        assertThat(result.success()).isTrue();
        assertThat(result.mergedLog()).contains("OPENAI_API_KEY=test-key");
        assertThat(result.mergedLog()).contains("SWE_AGENT_CONFIG=test-config");
    }
}
```

## Benefits

### Code Reduction
- **Before**: ~140 lines per infrastructure test × 3 = 420 lines
- **After**: ~100 lines TCK + ~50 lines per agent × 3 = 250 lines
- **Savings**: 40% less code

### Maintenance Benefits
1. **Single source of truth** for common infrastructure tests
2. **DRY principle** - no duplicated test logic
3. **Consistent test coverage** across all agents
4. **Easy to add new tests** - add once in TCK, all agents get it
5. **Clear separation** of common vs agent-specific concerns

### Developer Experience
1. **No IDE confusion** - unique class names
2. **Shorter names** - easier to type and read
3. **Clear structure** - obvious what's common vs specific
4. **Fast onboarding** - new agent just extends TCK

## Implementation Steps

1. **Create AbstractDockerInfraTCK** in `spring-ai-agent-model/src/test/java`
2. **Rename existing tests** with prefixes (CC, Gem, Swe)
3. **Refactor tests** to extend TCK
4. **Remove duplicated methods**
5. **Add to test-jar** for sharing across modules
6. **Update documentation** with new naming convention

## Alternative Naming Options Considered

| Option | Claude Code | Gemini | SWE Agent | Pros | Cons |
|--------|------------|---------|-----------|------|------|
| 1. Two-letter | CCDockerInfraIT | GmDockerInfraIT | SwDockerInfraIT | Shortest | Gm less intuitive |
| 2. Three-letter | CCDockerInfraIT | GemDockerInfraIT | SweDockerInfraIT | Clear, short | Inconsistent length |
| 3. Four-letter | ClauDockerInfraIT | GemiDockerInfraIT | SweADockerInfraIT | Consistent | Longer |

**Recommendation**: Option 2 (CC, Gem, Swe) balances clarity and brevity.

## Timeline
- Implementation: 1-2 hours
- Testing: 30 minutes
- Documentation: 30 minutes

## Notes
- Consider applying same pattern to LocalSandbox infrastructure tests if needed
- Could extend pattern to other shared test scenarios in future
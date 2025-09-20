# Docker Sandbox Integration Plan

**Status**: Planning
**Created**: 2025-09-19
**Last Updated**: 2025-09-19

## Overview

Integrate Docker-based sandbox isolation from spring-ai-bench into spring-ai-agents to provide secure-by-default execution for autonomous AI agents. This addresses the security risk of running AI agents with YOLO mode enabled directly on host systems.

## Background

### Current State Analysis (2025-09-19)

**Spring AI Agents** (Target for integration):
- ❌ No Docker sandbox implementation
- ⚠️ Uses `zt-exec` ProcessExecutor for direct host execution
- ⚠️ YOLO mode enabled by default - agents run with full host access
- ✅ Gemini CLI SDK has sandbox flags (`--sandbox`, `--sandbox-image`) but relies on CLI's built-in container support
- ⚠️ Primary user-facing library - needs to be secure by default

**Spring AI Bench** (Source of proven implementation):
- ✅ Complete `DockerSandbox` class using TestContainers
- ✅ All 15 DockerSandbox tests passing (verified 2025-09-19)
- ✅ Runs commands in isolated Docker containers with `/work` directory
- ✅ Uses long-lived containers with "sleep infinity" process
- ✅ Supports customizable base images and execution customizers
- ✅ Testing/benchmarking runtime - can remain more flexible

### Security Risk Assessment

**Current Risk Level**: HIGH
- AI agents execute arbitrary commands directly on host
- File system access unrestricted
- Package installation capabilities
- Network access unrestricted
- Environment variable exposure

**Target Risk Level**: LOW
- Commands executed in isolated containers
- File system access limited to container
- Network access configurable
- Host system protected from agent actions

## Implementation Plan

### Phase 0: Container Image Creation (Week 1)
**Goal**: Create standardized "spring-ai-agents" Docker image with all CLI tools

#### Critical Challenge
The current spring-ai-bench DockerSandbox uses generic base images like `openjdk:17`, but spring-ai-agents needs:
- Claude Code CLI pre-installed
- Gemini CLI pre-installed
- Node.js runtime (for Claude Code CLI)
- Python runtime (for various tools)
- Common development tools (git, curl, etc.)

#### Container Image Strategy - CI Knowledge Integration

**CRITICAL**: The Dockerfile incorporates hard-won knowledge from CI/CD debugging:

```dockerfile
# Build-capable image with JDK + Maven + Node + CLIs
FROM eclipse-temurin:17-jdk-jammy

# OS dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
    git python3 python3-pip ca-certificates curl \
 && rm -rf /var/lib/apt/lists/*

# Maven installation
ARG MAVEN_VERSION=3.9.8
RUN curl -fsSL https://downloads.apache.org/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz \
 | tar -xz -C /opt && ln -s /opt/apache-maven-${MAVEN_VERSION}/bin/mvn /usr/local/bin/mvn

# Node LTS (matching GitHub runners)
RUN curl -fsSL https://deb.nodesource.com/setup_lts.x | bash - \
 && apt-get update && apt-get install -y --no-install-recommends nodejs \
 && rm -rf /var/lib/apt/lists/*

# Non-root user and workspace
RUN useradd -m agent && mkdir -p /work && chown -R agent:agent /work
USER agent
WORKDIR /work

# Install CLIs EXACTLY as proven in CI
ARG CLAUDE_CODE_VERSION=latest
ARG GEMINI_CLI_VERSION=latest

# Install Claude Code CLI from npm globally - npm handles the symlink automatically
RUN npm install -g @anthropic-ai/claude-code@${CLAUDE_CODE_VERSION} --silent
# Install official Gemini CLI from npm
RUN npm install -g @google/gemini-cli@${GEMINI_CLI_VERSION} --silent

# Verify installations (matching CI verification logic)
RUN echo "=== Verifying CLI installations ===" && \
    if [ -L "/usr/local/bin/claude" ] && [ -f "/usr/local/bin/claude" ]; then \
      echo "✅ Claude CLI verified: $(/usr/local/bin/claude --version 2>&1)"; \
    else \
      echo "❌ Claude CLI not found" && exit 1; \
    fi && \
    if [ -L "/usr/local/bin/gemini" ] && [ -f "/usr/local/bin/gemini" ]; then \
      echo "✅ Gemini CLI verified: $(/usr/local/bin/gemini --version 2>&1)"; \
    else \
      echo "❌ Gemini CLI not found" && exit 1; \
    fi

CMD ["sleep", "infinity"]
```

### Critical CI/CD Knowledge Transfer

The following installation details were discovered through extensive CI/CD debugging:

1. **NPM Package Names** (exact, case-sensitive):
   - Claude: `@anthropic-ai/claude-code` (NOT @anthropic-ai/claude-3-5-sonnet)
   - Gemini: `@google/gemini-cli`

2. **Installation Commands** (proven in CI):
   ```bash
   npm install -g @anthropic-ai/claude-code --silent
   npm install -g @google/gemini-cli --silent
   ```

3. **Verification Pattern** (must check both symlink AND file):
   ```bash
   if [ -L "/usr/local/bin/claude" ] && [ -f "/usr/local/bin/claude" ]; then
     echo "✅ Claude CLI verified: $(/usr/local/bin/claude --version 2>&1)"
   fi
   ```

4. **Key Environment Variables** (passed at runtime, never baked):
   - `ANTHROPIC_API_KEY`
   - `GEMINI_API_KEY`

5. **Build Requirements**:
   - JDK 17 (not JRE) for Java compilation
   - Maven for build tools
   - Debian/Ubuntu base for compatibility

This knowledge is preserved from `.github/workflows/ci.yml` and must be maintained in the Docker image.

#### Tasks
- [x] ✅ Research current Claude Code CLI installation method (from CI)
- [x] ✅ Research current Gemini CLI installation method (from CI)
- [x] ✅ Create Dockerfile for spring-ai-agents-runtime image
- [x] ✅ Set up CI/CD to build and publish image to registry
- [ ] Test image build locally
- [ ] Version the image alongside spring-ai-agents releases
- [ ] Document image usage and customization

### Phase 1: Core Infrastructure (Week 2)
**Goal**: Add Docker sandbox capability to spring-ai-agents

#### Tasks
- [ ] Add TestContainers dependency to spring-ai-agents parent POM
- [ ] Create sandbox package structure in agent-core module
- [ ] Port core classes from spring-ai-bench
- [ ] Create LocalSandbox as non-isolated fallback
- [ ] Implement SandboxFactory for configuration

#### Dependencies
```xml
<!-- Add to spring-ai-agents/pom.xml -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
    <version>${testcontainers.version}</version>
</dependency>
```

#### Package Structure
```
spring-ai-agents/
├── agent-core/src/main/java/org/springaicommunity/agents/core/
│   └── sandbox/
│       ├── Sandbox.java                    (interface)
│       ├── DockerSandbox.java             (ported from bench)
│       ├── LocalSandbox.java              (non-isolated fallback)
│       └── SandboxFactory.java            (configuration)
```

#### Files to Port
- `DockerSandbox.java` - Core sandbox implementation
- `ExecSpec` - Command specification interface
- `ExecResult` - Execution result container
- `ExecSpecCustomizer` - Command customization interface
- Related exception classes

### Phase 2: SDK Integration (Week 3)
**Goal**: Integrate sandbox into existing CLI transport layers

#### Claude Code SDK Changes
**File**: `provider-sdks/claude-code-sdk/src/main/java/.../CLITransport.java`

```java
// New constructor option
public CLITransport(Path workingDirectory, Duration timeout,
                   String claudePath, Sandbox sandbox) {
    this.sandbox = sandbox; // Optional, defaults to LocalSandbox
}

// Modified execution logic
private ProcessResult executeCommand(List<String> command) {
    if (sandbox instanceof DockerSandbox) {
        ExecResult result = sandbox.exec(new ExecSpec(command));
        return convertToProcessResult(result);
    } else {
        return new ProcessExecutor().command(command)...execute();
    }
}
```

#### Gemini CLI SDK Changes
**File**: `provider-sdks/gemini-cli-sdk/src/main/java/.../CLITransport.java`

- Same pattern: optional sandbox parameter
- Leverage existing `--sandbox` flags when DockerSandbox is used
- Fall back to host execution for LocalSandbox

#### Integration Points
- `CLITransport.java:103,163` - Main CLI execution points
- `ClaudeCodeAgentModel.java:321` - Logout process execution
- All SDK transport layers using ProcessExecutor

### Phase 3: Safe Defaults (Week 4)
**Goal**: Make Docker isolation the default for safety

#### Configuration Strategy
```yaml
# application.yml
spring:
  ai:
    agents:
      sandbox:
        enabled: true                              # Default: true
        image: "springai/agents-runtime:latest" # Custom image with CLIs
        timeout: "5m"                           # Container timeout
        customizers: []       # Optional customizers
```

#### Auto-Configuration
```java
@ConditionalOnProperty(name = "spring.ai.agents.sandbox.enabled",
                       havingValue = "true", matchIfMissing = true)
@Bean
public Sandbox dockerSandbox() {
    return new DockerSandbox(sandboxProperties.getImage());
}

@ConditionalOnProperty(name = "spring.ai.agents.sandbox.enabled",
                       havingValue = "false")
@Bean
public Sandbox localSandbox() {
    return new LocalSandbox();
}
```

### Phase 4: Integration Testing (Week 5)
**Goal**: Ensure sandbox works with real agent workflows

#### Test Matrix
- [ ] Claude Code + Docker sandbox ✅
- [ ] Claude Code + Local sandbox ✅
- [ ] Gemini + Docker sandbox ✅
- [ ] Gemini + CLI sandbox ✅
- [ ] File operations isolated ✅
- [ ] Environment isolation ✅
- [ ] Multi-agent session isolation ✅

#### Performance Testing
- [ ] Container startup overhead measurement
- [ ] Memory usage comparison
- [ ] Throughput impact analysis
- [ ] Concurrent sandbox performance

#### Security Testing
- [ ] Host file system isolation verification
- [ ] Network isolation testing
- [ ] Environment variable isolation
- [ ] Process isolation validation

## Migration Strategy

### Backward Compatibility
- ✅ Default to Docker sandbox for new installations
- ✅ Allow opt-out via configuration for performance-sensitive use cases
- ✅ Existing code works unchanged (sandbox integration transparent)
- ✅ Clear migration guide in documentation

### Deployment Options
- **Development**: Docker sandbox (safe exploration)
- **CI/CD**: Docker sandbox (reproducible testing)
- **Production**: Configurable based on security requirements
- **Performance Critical**: Local sandbox with explicit opt-in

### Documentation Requirements
- [ ] Security benefits explanation
- [ ] Performance trade-offs documentation
- [ ] Configuration examples
- [ ] Troubleshooting guide for Docker issues
- [ ] Migration guide from unsafe to safe defaults

## Success Criteria

### Functional
1. **Security**: No agent can affect host system by default
2. **Usability**: Works out-of-box without Docker knowledge required
3. **Compatibility**: All existing tests pass with sandbox enabled
4. **Flexibility**: Easy to disable for performance-critical scenarios

### Performance
1. **Startup**: <2s container startup overhead acceptable
2. **Memory**: <500MB additional memory usage per sandbox
3. **Throughput**: <20% performance degradation acceptable
4. **Cleanup**: Containers properly cleaned up after use

### User Experience
1. **Installation**: No additional setup required for Docker sandbox
2. **Configuration**: Simple YAML configuration for customization
3. **Debugging**: Clear error messages for Docker-related issues
4. **Documentation**: Complete usage examples and troubleshooting

## Risks and Mitigation

### Technical Risks
| Risk | Impact | Probability | Mitigation |
|------|--------|-------------|------------|
| Docker not available | High | Medium | Graceful fallback to LocalSandbox with warnings |
| Container startup overhead | Medium | High | Performance testing and optimization |
| Memory usage increase | Medium | Medium | Resource limits and monitoring |
| TestContainers compatibility | High | Low | Thorough testing across environments |

### User Experience Risks
| Risk | Impact | Probability | Mitigation |
|------|--------|-------------|------------|
| Complex Docker setup | High | Low | Use TestContainers auto-configuration |
| Performance complaints | Medium | Medium | Clear documentation of trade-offs |
| Breaking existing workflows | High | Low | Thorough backward compatibility testing |

## Timeline

### Week 1 (Phase 0): Container Image
- Days 1-2: Research CLI installation methods and create Dockerfile
- Days 3-4: Build and test spring-ai-agents-runtime image locally
- Days 5-7: Set up CI/CD for automated image building and publishing

### Week 2 (Phase 1): Infrastructure
- Days 8-9: Set up project structure and dependencies
- Days 10-11: Port core sandbox classes from spring-ai-bench
- Days 12-14: Create LocalSandbox and SandboxFactory

### Week 3 (Phase 2): SDK Integration
- Days 15-17: Integrate with Claude Code SDK
- Days 18-19: Integrate with Gemini CLI SDK
- Days 20-21: Update agent models to use sandbox

### Week 4 (Phase 3): Configuration
- Days 22-23: Implement auto-configuration
- Days 24-25: Set up safe defaults
- Days 26-28: Create configuration documentation

### Week 5 (Phase 4): Testing
- Days 29-31: Integration testing across all scenarios
- Days 32-33: Performance and security testing
- Days 34-35: Documentation and final validation

## Learning and Status Log

### 2025-09-19
- **Discovery**: Verified spring-ai-bench DockerSandbox implementation is solid (15/15 tests passing)
- **Analysis**: Identified clear integration points in spring-ai-agents CLI transport layers
- **Decision**: Proceed with porting approach rather than shared library to maintain project independence
- **Risk Assessment**: Current default behavior (direct host execution) is unacceptable for production use
- **Critical Insight**: Need custom Docker image with pre-installed CLI tools (Claude Code CLI, Gemini CLI)
- **Updated Plan**: Added Phase 0 for container image creation as foundational requirement
- **CI Knowledge Transfer**: Extracted exact npm package names and installation patterns from proven CI script
- **Files Created**:
  - `Dockerfile.agents-runtime` with CI-proven CLI installations
  - `.github/workflows/image.yml` for automated image building

### Future Updates
- Track implementation progress here
- Document encountered issues and solutions
- Record performance benchmarks
- Update timeline based on actual progress

## References

### Source Files (spring-ai-bench)
- `/home/mark/community/spring-ai-bench/bench-core/src/main/java/org/springaicommunity/bench/core/exec/sandbox/DockerSandbox.java`
- `/home/mark/community/spring-ai-bench/bench-core/src/test/java/org/springaicommunity/bench/core/exec/sandbox/DockerSandboxTest.java`

### Target Files (spring-ai-agents)
- `provider-sdks/claude-code-sdk/src/main/java/.../CLITransport.java:103,163`
- `provider-sdks/gemini-cli-sdk/src/main/java/.../CLITransport.java:172,177`
- `agent-models/spring-ai-claude-code/src/main/java/.../ClaudeCodeAgentModel.java:321`

### Related Documentation
- [TestContainers Documentation](https://www.testcontainers.org/)
- [Spring Boot Auto-Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.developing-auto-configuration)
- [Docker Security Best Practices](https://docs.docker.com/engine/security/)
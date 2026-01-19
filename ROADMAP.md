# spring-ai-agents Roadmap

**Last Updated**: 2026-01-16
**Status**: Active Development

---

## Project Overview

spring-ai-agents provides the orchestration layer for AI agent development:
- **AgentClient** - High-level fluent API for agent invocation
- **Provider SDKs** - Claude, Gemini, SWE-agent integrations
- **JBang Infrastructure** - Zero-build agent development

### Upcoming Rename

| Current | Future |
|---------|--------|
| `spring-ai-agents` | `spring-ai-agent-client` |

### Related Projects

| Project | Purpose | Status |
|---------|---------|--------|
| **agent-harness** | Loop patterns (TurnLimited, EvaluatorOptimizer, etc.) | Active |
| **spring-ai-judge** | Judge/Jury evaluation framework | Extracted ✅ |
| **spring-ai-sandbox** | Process execution and workspace setup | To be extracted |
| **spring-ai-agent-tui** | Interactive CLI with Plan Mode | Future (separate repo) |

---

## Completed Work

### P0: Architecture Unification ✅
- Unified ReactiveTransport with BidirectionalTransport
- All reactive queries use robust BidirectionalTransport foundation
- 153 unit tests, 78 integration tests passing

### P1: Extended Thinking & Structured Output ✅
- `--max-thinking-tokens` CLI flag support
- `ThinkingBlock` content type
- `--json-schema` CLI flag support
- `ResultMessage.structuredOutput` field with type-safe helpers

---

## Roadmap

### Step 1: Planner Component in AgentClient

**Priority**: HIGH

#### Goal
Add planning as a first-class component to AgentClient, enabling systematic task decomposition before execution.

#### Entry Conditions
- [ ] All existing tests passing
- [ ] Review `learnings/PLANNING-AS-FIRST-CLASS.md` in agent-harness

#### Context

Planning is a **dimension**, not a pattern. Any execution strategy can benefit from planning:

```
           ┌─────────────────────────────────────┐
           │         PLANNING DIMENSION          │
           │  None → Implicit → Explicit → Algo  │
           └─────────────────────────────────────┘
                           ×
           ┌─────────────────────────────────────┐
           │        EXECUTION DIMENSION          │
           │  TurnLimited | StateMachine | etc.  │
           └─────────────────────────────────────┘
```

#### Implementation Tasks

**1.1 Define Planner Interface**

```java
public interface Planner<T> {
    Plan plan(T task, PlanningContext context);
    Plan replan(Plan original, ExecutionFeedback feedback);
}

public record Plan(
    List<PlanStep> steps,
    String reasoning,
    Map<String, Object> metadata
) {}

public record PlanStep(
    String description,
    String acceptanceCriteria,
    Set<String> toolsNeeded
) {}
```

**1.2 Implement Planner Strategies**

| Strategy | Description |
|----------|-------------|
| `NullPlanner` | Single-step plan (no decomposition) |
| `LlmPlanner` | LLM decomposes task into steps |
| `TemplatePlanner` | Pre-defined templates for known workflows |

**1.3 Integrate with AgentClient**

```java
AgentClient.builder()
    .planner(llmPlanner)
    .agentModel(claudeAgent)
    .build()
    .execute(task);
```

#### Tests Required
- [ ] Unit tests for each Planner implementation
- [ ] Unit tests for Plan/PlanStep records
- [ ] Integration test: LlmPlanner decomposes multi-step task
- [ ] Integration test: Replanning on step failure

#### Exit Criteria
- [ ] `Planner` interface defined in `spring-ai-agent-client`
- [ ] At least `NullPlanner` and `LlmPlanner` implemented
- [ ] AgentClient builder supports `.planner()` method
- [ ] All tests pass
- [ ] Learning document written: `learnings/PLANNER-COMPONENT.md`

---

### Step 2: Judge Framework Extraction ✅

**Priority**: MEDIUM
**Status**: COMPLETE (with boundary refinement pending)

#### Summary
The Judge framework has been extracted to `spring-ai-judge` as an independent, **agent-agnostic** project.

#### What Was Done
- Created new `spring-ai-judge` multi-module project
- Extracted core, exec, llm modules
- Package renamed: `org.springaicommunity.agents.judge` → `org.springaicommunity.judge`
- Created `ProcessRunner` abstraction (replaces Sandbox dependency)

#### Design Decision: Agent-Based Judging Stays Here
See `spring-ai-judge/plans/learnings/AGENT-JUDGING-BOUNDARY.md`

The initial extraction included agent-based judging in `spring-ai-judge`, but this revealed a boundary mismatch. Key insight: **`AgentJudge` is "an agent whose job is judging", not "a judge that uses agents"**.

Agent-based judging will be re-homed to `spring-ai-agents` (see Step 2a below).

#### New Dependency Coordinates
```xml
<dependency>
    <groupId>org.springaicommunity</groupId>
    <artifactId>spring-ai-judge-core</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

---

### Step 2a: Re-home Agent-Based Judging

**Priority**: HIGH
**Status**: PENDING

#### Goal
Move agent-based judging from `spring-ai-judge` back to `spring-ai-agents`, where it belongs.

#### Rationale
- `spring-ai-judge` should be agent-agnostic (usable without agents)
- `AgentJudge` requires `AgentClient` directly - no adapters needed
- Clean dependency direction: `spring-ai-agents` depends on `spring-ai-judge-core`

#### Implementation Tasks

**Phase 1: Remove from spring-ai-judge**
- [ ] Delete `spring-ai-judge-agent` module
- [ ] Delete `spring-ai-judge-advisor` module
- [ ] Remove `JudgeAgentClient`, `JudgeAgentResponse` bridge interfaces
- [ ] Update parent POM, BOM, CI workflows

**Phase 2: Create in spring-ai-agents**
- [ ] Create `spring-ai-agent-judge` module
- [ ] Move `AgentJudge` (implements `Judge` from spring-ai-judge-core)
- [ ] `AgentJudge` uses `AgentClient` directly (no adapters)
- [ ] Move `JudgeAdvisor`, `JuryAdvisor` to appropriate module

**Phase 3: Cleanup**
- [ ] Delete `AgentClientAdapter` and `AgentClientAdapterResponse`
- [ ] Update all imports and dependencies
- [ ] Verify builds pass in both projects

#### Target Architecture
```
spring-ai-judge (agent-agnostic)
├── spring-ai-judge-core      # Judge, Judgment, Score, Jury
├── spring-ai-judge-exec      # ProcessRunner-based judges
├── spring-ai-judge-llm       # ChatClient-based judges
└── spring-ai-judge-bom

spring-ai-agents (depends on spring-ai-judge-core)
├── spring-ai-agent-client
├── spring-ai-agent-judge     # AgentJudge implements Judge
└── spring-ai-agent-advisor   # Judge/Jury advisors
```

#### Exit Criteria
- [ ] `spring-ai-judge` has zero agent knowledge
- [ ] `AgentJudge` lives in `spring-ai-agents`, uses `AgentClient` directly
- [ ] No adapter classes exist
- [ ] All tests pass in both projects

---

### Step 2b: Extract Sandbox to Standalone Project

**Priority**: HIGH
**Status**: PENDING

See `plans/learnings/SANDBOX-EXTRACTION.md` for full rationale.

#### Goal

Extract the `Sandbox` abstraction from `spring-ai-agents` into a standalone `spring-ai-sandbox` project with enhanced workspace setup features.

#### Rationale

During judge extraction, we created a duplicate `ProcessRunner` abstraction in `spring-ai-judge-exec`. Both projects need process execution capabilities. Extracting `Sandbox` to a shared project:
- Eliminates duplicate code
- Provides workspace setup features needed for testing
- Enables clean dependency direction (both depend on shared utility)

#### Entry Conditions

- [ ] All existing tests passing in `spring-ai-agents`
- [ ] Review `plans/learnings/SANDBOX-EXTRACTION.md`
- [ ] Review reference implementations:
  - `agent-harness-cli/harness-test/.../workspace/WorkspaceManager.java`
  - `gemini-cli/integration-tests/test-helper.ts`

#### Current Sandbox API

```java
public interface Sandbox extends AutoCloseable {
    ExecResult exec(ExecSpec spec);
    Process startInteractive(ExecSpec spec);
    Path workDir();
    boolean isClosed();
    void close();
}
```

#### Enhanced API (New Features)

Following Spring's fluent API conventions (RestClient, WebClient), file operations are grouped under a `files()` accessor to keep the main interface clean.

**Sandbox Interface (Enhanced)**
```java
public interface Sandbox extends AutoCloseable {
    // Core execution (unchanged)
    ExecResult exec(ExecSpec spec);
    Process startInteractive(ExecSpec spec);
    Path workDir();
    boolean isClosed();
    void close();
    boolean shouldCleanupOnClose();

    // Single accessor for all file operations
    SandboxFiles files();
}
```

**SandboxFiles Accessor (New)**
```java
public interface SandboxFiles {
    // Write operations - return this for chaining
    SandboxFiles create(String relativePath, String content);
    SandboxFiles createDirectory(String relativePath);
    SandboxFiles setup(List<FileSpec> files);

    // Read operations
    String read(String relativePath);
    boolean exists(String relativePath);

    // Return to sandbox for continued chaining
    Sandbox and();
}
```

**FileSpec Record**
```java
public record FileSpec(String path, String content) {
    public static FileSpec of(String path, String content) {
        return new FileSpec(path, content);
    }
}
```

**Builder Enhancement**
```java
LocalSandbox sandbox = LocalSandbox.builder()
    .tempDirectory("test-")
    .withFile("src/Main.java", "public class Main {}")
    .withFile("pom.xml", "<project>...</project>")
    .build();
```

**Usage Example**
```java
sandbox.files()
    .create("src/Main.java", javaCode)
    .create("pom.xml", pomContent)
    .and()  // return to Sandbox
    .exec(ExecSpec.of("mvn", "compile"));

// Verification
assertTrue(sandbox.files().exists("target/classes/Main.class"));
```

#### Implementation Tasks

**Phase 1: Create New Project**
- [ ] Create GitHub repository `spring-ai-community/spring-ai-sandbox`
- [ ] Set up Maven wrapper (copy from `~/acp/acp-java`):
  - `mvnw` (Unix script)
  - `mvnw.cmd` (Windows script)
  - `.mvn/wrapper/maven-wrapper.properties`
  - `.mvn/wrapper/maven-wrapper.jar`
- [ ] Set up multi-module Maven structure:
  ```
  spring-ai-sandbox/
  ├── .mvn/wrapper/              # Maven wrapper
  ├── mvnw, mvnw.cmd             # Wrapper scripts
  ├── spring-ai-sandbox-core/    # Sandbox, ExecSpec, ExecResult, FileSpec
  └── spring-ai-sandbox-docker/  # DockerSandbox (optional module)
  ```
- [ ] Copy `Sandbox`, `ExecSpec`, `ExecResult`, `LocalSandbox` from spring-ai-agents
- [ ] Package rename: `org.springaicommunity.agents.model.sandbox` → `org.springaicommunity.sandbox`
- [ ] Copy corresponding tests

**Phase 2: Add Workspace Setup Features**
- [ ] Add `FileSpec` record
- [ ] Add `SandboxFiles` interface (accessor pattern)
- [ ] Add `files()` method to `Sandbox` interface
- [ ] Create `LocalSandboxFiles` implementation:
  - `create()` - uses `Files.write()`, creates parent dirs, returns `this`
  - `createDirectory()` - uses `Files.createDirectories()`, returns `this`
  - `setup()` - bulk file creation, returns `this`
  - `read()` - uses `Files.readString()`
  - `exists()` - uses `Files.exists()`
  - `and()` - returns parent `Sandbox` for chaining
- [ ] Add `shouldCleanupOnClose()` for temp directory management
- [ ] Enhance `LocalSandbox.Builder`:
  - `tempDirectory()` / `tempDirectory(String prefix)`
  - `withFile(String path, String content)`
  - `withFiles(List<FileSpec> files)`

**Phase 3: Early Testing (Before Downstream Migration)**
- [ ] Unit tests for all new file operations
- [ ] Unit tests for builder patterns
- [ ] Integration test: temp directory creation and cleanup
- [ ] Integration test: bulk file setup with nested directories
- [ ] Test: `./mvnw clean verify` passes standalone

**Phase 4: Migrate DockerSandbox (Optional Module)**
- [ ] Move `DockerSandbox` to `spring-ai-sandbox-docker`
- [ ] Implement file operations using container APIs:
  - `container.copyFileToContainer()`
  - `container.copyFileFromContainer()`
- [ ] Add testcontainers dependency
- [ ] Integration tests for Docker file operations

**Phase 5: Update Downstream Projects**

*spring-ai-judge-exec:*
- [ ] Add dependency on `spring-ai-sandbox-core`
- [ ] Delete `ProcessRunner`, `ProcessSpec`, `ProcessResult`, `LocalProcessRunner`
- [ ] Update `CommandJudge`, `BuildSuccessJudge` to use `Sandbox`
- [ ] Update tests
- [ ] Verify: `./mvnw clean verify` passes

*spring-ai-agents:*
- [ ] Add dependency on `spring-ai-sandbox-core` (or `-docker`)
- [ ] Delete `org.springaicommunity.agents.model.sandbox` package
- [ ] Update all imports
- [ ] Update Spring auto-configuration
- [ ] Verify: `./mvnw clean verify -Pfailsafe` passes

**Phase 6: Publishing Setup**
- [ ] Create `spring-ai-sandbox-bom` module
- [ ] Configure GPG signing
- [ ] Configure `central-publishing-maven-plugin`
- [ ] Create GitHub Actions workflows (ci.yml, publish-snapshot.yml, release.yml)
- [ ] Publish SNAPSHOT to Maven Central

#### Tests Required

**Unit Tests**
- [ ] `FileSpec` record creation and equality
- [ ] `LocalSandbox.Builder` - all builder methods
- [ ] `SandboxFiles.create()` - creates file and parent directories
- [ ] `SandboxFiles.createDirectory()` - creates nested directories
- [ ] `SandboxFiles.read()` - reads file content
- [ ] `SandboxFiles.exists()` - true/false cases
- [ ] `SandboxFiles.setup()` - bulk creation
- [ ] `SandboxFiles.and()` - returns parent sandbox for chaining
- [ ] `LocalSandbox.close()` - temp directory cleanup
- [ ] `LocalSandbox.close()` - user directory not deleted
- [ ] Fluent chaining: `sandbox.files().create().create().and().exec()`

**Integration Tests**
- [ ] Full workflow: create temp sandbox → setup files → exec command → verify outputs → cleanup
- [ ] Docker sandbox file operations (if DockerSandbox module included)

#### Target Architecture

```
spring-ai-sandbox (new standalone project)
├── spring-ai-sandbox-core
│   ├── Sandbox interface + SandboxFiles accessor
│   ├── ExecSpec, ExecResult, FileSpec
│   ├── LocalSandbox + LocalSandboxFiles (zt-exec)
│   └── SandboxException, TimeoutException
├── spring-ai-sandbox-docker (optional)
│   └── DockerSandbox + DockerSandboxFiles (testcontainers)
└── spring-ai-sandbox-bom

spring-ai-judge-exec
└── depends on spring-ai-sandbox-core
    └── ProcessRunner removed (uses Sandbox)

spring-ai-agents
└── depends on spring-ai-sandbox-core
    └── sandbox package removed
```

#### Exit Criteria

- [ ] `spring-ai-sandbox` is independent project with all tests passing
- [ ] `SandboxFiles` accessor implemented with fluent chaining
- [ ] Builder pattern supports temp directories and file setup
- [ ] `spring-ai-judge-exec` uses `Sandbox` (no `ProcessRunner`)
- [ ] `spring-ai-agents` uses external `spring-ai-sandbox` dependency
- [ ] No duplicate process execution code exists
- [ ] SNAPSHOT published to Maven Central
- [ ] Learning document complete: `plans/learnings/SANDBOX-EXTRACTION.md`

---

### Step 3: Project Rename

**Priority**: MEDIUM

#### Goal
Rename `spring-ai-agents` to `spring-ai-agent-client` to clarify purpose.

#### Entry Conditions
- [x] Step 2 complete (Judge Extraction)
- [ ] All module dependencies updated

#### Implementation Tasks
- [ ] Rename repository
- [ ] Update Maven coordinates (groupId, artifactId)
- [ ] Update package names if needed
- [ ] Update all documentation references
- [ ] Update dependent projects

#### Tests Required
- [ ] Full build passes after rename
- [ ] All dependent projects build with new coordinates

#### Exit Criteria
- [ ] Repository is `spring-ai-agent-client`
- [ ] Maven Central coordinates updated (if published)
- [ ] All downstream projects updated

---

### Future: spring-ai-agent-tui (Separate Project)

**Priority**: LOW (Future)
**Status**: Deferred to separate project

#### Goal
Create interactive TUI/CLI with Plan Mode support, similar to Claude Code.

#### Why Separate Project?
See `plans/learnings/TUI-ARCHITECTURE-DECISION.md`:
- TUI is an APPLICATION, not a library
- Would muddy the purity of spring-ai-agent-client
- Different release cycle and dependencies

#### Scope (when implemented)
- Plan Mode state machine (explore → plan → approve → execute)
- Human approval flows
- Terminal UI with JLine
- Status display, plan panels

#### Dependencies
```xml
<dependency>
    <groupId>org.springaicommunity.agents</groupId>
    <artifactId>spring-ai-agent-client</artifactId>
</dependency>
<dependency>
    <groupId>org.springaicommunity.agents</groupId>
    <artifactId>agent-harness-patterns</artifactId>
</dependency>
<dependency>
    <groupId>org.jline</groupId>
    <artifactId>jline</artifactId>
</dependency>
```

---

## Testing Standards

### Unit Tests
- All new code must have unit tests
- Use `@ExtendWith(MockitoExtension.class)` for mocking
- Target: >80% coverage for new code

### Integration Tests
- Named `*IT.java`
- Run with: `./mvnw clean verify -Pfailsafe`
- Use `@Import(MockSandboxConfiguration.class)` for sandbox mocking

### Formatting
- **MANDATORY**: Run `./mvnw spring-javaformat:apply` before commits
- CI will fail on formatting violations

---

## Reference Documents

| Document | Location |
|----------|----------|
| Sandbox Extraction | `plans/learnings/SANDBOX-EXTRACTION.md` |
| Agent-Judging Boundary | `spring-ai-judge/plans/learnings/AGENT-JUDGING-BOUNDARY.md` |
| TUI Architecture Decision | `plans/learnings/TUI-ARCHITECTURE-DECISION.md` |
| Planning as First-Class | `agent-harness/plans/learnings/PLANNING-AS-FIRST-CLASS.md` |
| OpenAGI vs GOAP | `agent-harness/plans/learnings/OPENAGI-VS-GOAP.md` |
| Functional Parity Plan | `plans/FUNCTIONAL-PARITY-PLAN.md` |
| P1 Features Plan | `plans/P1-FEATURES-PLAN.md` |

---

*Last updated: 2026-01-16*

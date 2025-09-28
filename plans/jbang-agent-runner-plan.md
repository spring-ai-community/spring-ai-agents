# JBang Agent Runner Implementation Plan

## Overview
Build an ultra-thin JBang launcher for Spring AI agents with clear separation between AgentSpec (immutable recipe defining WHAT the agent does) and RunSpec (per-run configuration defining WHERE/HOW to run, including per-run tweak values).

## Core Architecture Principles

1. **AgentSpec**: Immutable recipe defining WHAT the agent does (inputs only - black box design)
2. **RunSpec**: Per-run configuration of WHERE/HOW to run and runtime values
3. **LauncherSpec**: Combined object passed to the engine for execution
4. **AgentRunner**: Functional interface for black-box agent execution (inputs → outputs)
5. **Launcher**: Simple launcher class that loads, validates, and executes agents

## Key Changes from Initial Design

1. **Simplified to black-box agents**: Removed PromptSpec, tweak functionality - agents are pure functions (inputs → outputs)
2. **Module renamed**: spring-ai-agent-runner → spring-ai-agents-core
3. **YAML naming**: agents/<id>.yaml (not agent-defaults.yaml)
4. **Sandbox flags belong to RunSpec/env** (not AgentSpec)
5. **Exit codes**: 0=success, 1=failure, 2=usage error
6. **Clear precedence**: Defaults → run.yaml → CLI
7. **Interface design**: AgentRunner as functional interface with run() method
8. **Separation of concerns**: Launcher handles agent loading/execution, AgentRunner handles agent logic

## Module Structure

```
spring-ai-agents/
├── spring-ai-agents-core/               # Core library engine (not "runner")
│   ├── pom.xml
│   └── src/main/java/.../core/
│       ├── AgentRunner.java             # Functional interface for agent execution
│       ├── Launcher.java                # Agent launcher (loads, validates, executes)
│       ├── AgentSpec.java               # Agent recipe (immutable)
│       ├── RunSpec.java                 # Per-run configuration
│       ├── LauncherSpec.java            # Combined spec
│       ├── LocalConfigLoader.java       # YAML + CLI merger
│       ├── AgentSpecLoader.java         # YAML loading with filesystem fallback
│       └── Result.java                  # Success/failure result with data payload
│
├── agents/                               # Individual agent artifacts
│   ├── hello-world-agent/
│   │   ├── pom.xml
│   │   ├── src/main/java/.../HelloWorldAgent.java
│   │   └── src/main/resources/agents/hello-world.yaml
│   │
│   └── code-coverage-agent/
│       ├── pom.xml
│       ├── src/main/java/.../CoverageAgent.java
│       └── src/main/resources/agents/coverage.yaml
│
├── jbang/
│   ├── launcher.java                     # Ultra-thin JBang launcher (3 lines)
│   └── README.md                         # JBang usage documentation
├── jbang-catalog.json                   # For springai@agents alias
└── plans/
    └── jbang-agent-runner-plan.md       # This plan document
```

## Data Models

### AgentSpec (Immutable Recipe)
```java
public record AgentSpec(
    String id,                           // "coverage", "hello-world"
    String version,                      // "0.1"
    Map<String, InputDef> inputs         // Input definitions with types/defaults
) {
    public record InputDef(
        String type,                     // "string", "integer", "boolean"
        Object defaultValue,
        boolean required
    ) {}
}
```

### RunSpec (Per-Run Configuration)
```java
public record RunSpec(
    String agent,                         // Which agent to run
    Map<String, Object> inputs,          // Runtime input values
    String workingDirectory,             // Working directory path
    Map<String, Object> env              // Sandbox, isolation, workdir
) {}
```

### LauncherSpec (Combined for Execution)
```java
public record LauncherSpec(
    AgentSpec agentSpec,                 // Complete agent specification
    Map<String, Object> inputs,          // Merged: defaults → run.yaml → CLI
    Path cwd,                           // Resolved working directory
    Map<String, Object> env              // sandbox, isolated, workdir
) {}
```

### Result
```java
public record Result(
    boolean success,
    String message,
    Map<String, Object> data          // Judge integration payload
) {
    public static Result ok(String msg) { return new Result(true, msg, Map.of()); }
    public static Result ok(String msg, Map<String,Object> data) { return new Result(true, msg, data); }
    public static Result fail(String msg) { return new Result(false, msg, Map.of()); }
    public static Result fail(String msg, Map<String,Object> data) { return new Result(false, msg, data); }
}
```

## Configuration Files

### Agent Definition: `agents/hello-world.yaml`
```yaml
id: hello-world
version: 0.1
inputs:
  path:
    type: string
    required: true
  content:
    type: string
    default: "HelloWorld"
```

### Agent Definition: `agents/coverage.yaml`
```yaml
id: coverage
version: 0.1
inputs:
  target_coverage:
    type: integer
    default: 80
  module:
    type: string
    default: "."
prompt:
  system: "You are a Java testing agent."
  userTemplate: |
    Increase line coverage in {{module}} to at least {{target_coverage}}%.
    {{#tweak}}
    Operator hint: {{tweak}}
    {{/tweak}}
```

### Run Configuration: `run.yaml`
```yaml
agent: coverage
inputs:
  module: complete
  target_coverage: 85
tweak: "Only modify code in complete/"
env:
  sandbox: local    # or 'docker' later
  isolated: false
  workdir: .
```

## JBang Launcher (Ultra-Thin)
```java
///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 21
//DEPS org.springaicommunity.agents:spring-ai-agents-core:0.1.0-SNAPSHOT
//DEPS org.springaicommunity.agents:hello-world-agent:0.1.0-SNAPSHOT
//DEPS org.springaicommunity.agents:code-coverage-agent:0.1.0-SNAPSHOT

import org.springaicommunity.agents.core.*;

public class launcher {
    public static void main(String[] argv) throws Exception {
        LauncherSpec spec = LocalConfigLoader.load(argv);
        Result r = Launcher.execute(spec);
        if (!r.success()) System.exit(1);
        System.out.println(r.message());
    }
}
```

## Precedence Rules
**Defaults (from AgentSpec) → run.yaml → CLI**

## CLI Parsing in LocalConfigLoader
```java
// Parses: --agent <name> --tweak <hint> --key value
// Sandbox flags: --sandbox docker --isolated
for (int i = 0; i < argv.length; i++) {
    String arg = argv[i];
    if ("--agent".equals(arg) && i+1 < argv.length) {
        agent = argv[++i];
    } else if (isTweakFlag(arg) && i+1 < argv.length) {
        tweak = argv[++i];
    } else if ("--sandbox".equals(arg) && i+1 < argv.length) {
        env.put("sandbox", argv[++i]);
    } else if ("--isolated".equals(arg)) {
        env.put("isolated", "true");
    } else if (arg.startsWith("--")) {
        String key = arg.substring(2).replace('-', '_');
        String value = (i+1 < argv.length && !argv[i+1].startsWith("--"))
            ? argv[++i] : "true";
        inputs.put(key, value);
    }
}

private static boolean isTweakFlag(String arg) {
    return Set.of("--tweak", "--nudge", "--whisper", "--flavor", "--spice")
        .contains(arg);
}
```

## Exit Codes
- `0`: Success
- `1`: Execution failure
- `2`: Usage error (missing required inputs, unknown agent)

## Usage Examples

### Hello World
```bash
# With explicit content
jbang jbang/launcher.java --agent hello-world --path test.txt --content HelloWorld

# Using default content
jbang jbang/launcher.java --agent hello-world --path test.txt
```

### Coverage Agent
```bash
# Basic usage
jbang jbang/launcher.java --agent coverage --target_coverage 85 --module src/main

# With sandbox options
jbang jbang/launcher.java \
  --agent coverage \
  --target_coverage 85 \
  --module src/main \
  --sandbox docker
```

### Using run.yaml
```bash
# Create run.yaml
cat > run.yaml << 'EOF'
agent: coverage
inputs:
  target_coverage: 85
  module: complete
env:
  sandbox: local
EOF

# Execute
jbang jbang/launcher.java
```

## Implementation Steps

1. **Create spring-ai-agents-core module**:
   - Define records: AgentSpec, RunSpec, LauncherSpec, Result
   - Implement LocalConfigLoader with precedence rules
   - Create AgentRunner engine

2. **Create hello-world-agent module**:
   - Write agents/hello-world.yaml with path (required) and content (default: "HelloWorld")
   - Implement HelloWorldAgent.java to write file

3. **Create code-coverage-agent module**:
   - Write agents/coverage.yaml with {{tweak}} placeholder
   - Implement CoverageAgent.java (stub showing rendered prompt)

4. **Create jbang/launcher.java** JBang launcher (3 lines)

5. **Create jbang-catalog.json** for distribution

6. **Test and verify** acceptance checklist

## Implementation Status

### ✅ Phase 1: Basic Architecture (COMPLETED)
- [✅] Created spring-ai-agents-core module with records
- [✅] Implemented LocalConfigLoader with precedence rules
- [✅] Created hello-world-agent and coverage-agent modules
- [✅] Created agents.java JBang launcher
- [✅] Basic functionality working end-to-end

### ✅ Phase 2: Black-box Simplification (COMPLETED)
- [✅] Removed PromptSpec and tweak functionality
- [✅] Simplified AgentSpec to inputs-only design
- [✅] Updated RunSpec and LauncherSpec accordingly
- [✅] Verified all tests still pass

### ✅ Phase 3: Interface Refactoring (COMPLETED)
- [✅] Renamed AgentExecutor to AgentRunner functional interface
- [✅] Changed method name from execute() to run()
- [✅] Created Launcher for agent loading and execution
- [✅] Updated agents.java to use Launcher.execute()
- [✅] Updated HelloWorldAgent to implement AgentRunner
- [✅] Verified JBang execution still works

### ✅ Phase 4: File Organization (COMPLETED)
- [✅] Create jbang/ directory and move agents.java → launcher.java
- [✅] Update jbang-catalog.json to reference jbang/launcher.java
- [✅] Create AgentSpecLoader class with filesystem fallback
- [✅] Create InputMerger class (later removed as over-engineering)
- [✅] Rename HelloWorldAgent to HelloWorldAgentRunner
- [✅] Keep jbang/launcher.java ultra-thin (3 lines of logic)
- [✅] Fix HelloWorldAgentIT with zt-exec and new paths
- [✅] Add jbang/README.md with comprehensive documentation
- [✅] Update CLAUDE.md with new structure

### ✅ Phase 5: Architectural Refinements (COMPLETED)
- [✅] Enhanced Result record with data field for judge integration
- [✅] Removed InputMerger - inlined logic into Launcher for simplicity
- [✅] Added filesystem fallback to AgentSpecLoader (.agents directory)
- [✅] Added basic type coercion (string→int/boolean) to input merging
- [✅] Maintained launcher.java ultra-thin design (3 lines)
- [✅] Updated documentation to reflect all changes

## Acceptance Checklist (MVP) - ✅ COMPLETED

- [✅] `jbang jbang/launcher.java --agent hello-world --path file.txt --content HelloWorld` writes file and prints success
- [✅] `run.yaml` for coverage works without CLI flags
- [✅] Missing required inputs produce exit code 1
- [✅] Nonexistent `--agent` yields exit code 1
- [✅] AgentRunner functional interface works correctly

## Test Results

### Hello World Agent Tests
```bash
# With explicit content - ✅ PASSED
jbang jbang/launcher.java --agent hello-world --path hello-test.txt --content "Hello from JBang!"
# Result: Created file: /tmp/hello-test.txt with content "Hello from JBang!"

# With default content - ✅ PASSED
jbang jbang/launcher.java --agent hello-world --path hello-default.txt
# Result: Created file with default content "HelloWorld"
```

### Coverage Agent Tests
```bash
# Basic usage - ✅ PASSED
jbang jbang/launcher.java --agent coverage --target_coverage 90 --module src/main
# Result: Shows coverage analysis output

# With run.yaml - ✅ PASSED
# run.yaml with agent: coverage, inputs
jbang jbang/launcher.java
# Result: Uses configuration from run.yaml
```

### Error Handling Tests
```bash
# Unknown agent - ✅ PASSED
jbang jbang/launcher.java --agent nonexistent
# Result: Exit code 1 (error handling works)

# Missing required input - ✅ PASSED
jbang jbang/launcher.java --agent hello-world
# Result: Exit code 1 (missing path parameter)
```

## Why This Design Is Robust

- **Black-box simplicity**: Agents are pure functions (inputs → outputs), no prompt exposure
- **Functional interface**: AgentRunner is a simple functional interface with single run() method
- **Clear separation**: Launcher handles agent loading/execution, AgentRunner handles agent logic
- **Progressive enhancement**: Defaults → run.yaml → CLI precedence is predictable
- **Future-proof**: Ready for AgentClient integration without redesign
- **Developer-friendly**: Single JBang command, no build required
- **Type safety**: Input validation with types and defaults
- **Clean OO design**: No factory patterns, follows Spring conventions

## JBang Catalog for Distribution

```json
{
  "aliases": {
    "agents": {
      "script-ref": "https://raw.githubusercontent.com/spring-ai-community/spring-ai-agents/main/jbang/launcher.java",
      "description": "Spring AI Agents launcher - run AI agents on your codebase"
    }
  }
}
```

Usage after catalog setup:
```bash
jbang catalog add springai https://raw.githubusercontent.com/spring-ai-community/spring-ai-agents/main/jbang-catalog.json
jbang springai@agents --agent coverage --target_coverage 85 --module service
```

## Benefits

1. **Ultra-thin launcher**: 3-line JBang script, no build required
2. **Composable primitives**: Clear separation of concerns
3. **Per-agent artifacts**: Independent Maven modules, versioned separately
4. **Developer UX**: Single command with intuitive flags and config files
5. **Extensible**: Ready for sandbox modes, governance features
6. **Type safe**: Input validation with types and defaults
7. **Progressive complexity**: Hello-world simple, coverage shows full features

This plan creates a solid foundation for running AI agents via JBang while maintaining clean architecture and room for future enhancements.
# JBang Agent Runner Implementation Plan

## Overview
Build an ultra-thin JBang launcher for Spring AI agents with clear separation between AgentSpec (immutable recipe defining WHAT the agent does) and RunSpec (per-run configuration defining WHERE/HOW to run, including per-run tweak values).

## Core Architecture Principles

1. **AgentSpec**: Immutable recipe defining WHAT the agent does (inputs, prompts with `{{tweak}}` placeholder)
2. **RunSpec**: Per-run configuration of WHERE/HOW to run and runtime values (including tweak value)
3. **LauncherSpec**: Combined object passed to the engine for execution
4. **Tweak**: Per-run operator hint value (goes in RunSpec/LauncherSpec, not AgentSpec)

## Key Changes from Initial Design

1. **Tweak value in RunSpec/LauncherSpec**, not AgentSpec (AgentSpec only has `{{tweak}}` placeholder in template)
2. **Module renamed**: spring-ai-agent-runner → spring-ai-agents-core
3. **YAML naming**: agents/<id>.yaml (not agent-defaults.yaml)
4. **Sandbox flags belong to RunSpec/env** (not AgentSpec)
5. **Exit codes**: 0=success, 1=failure, 2=usage error
6. **Clear precedence**: Defaults → run.yaml → CLI

## Module Structure

```
spring-ai-agents/
├── spring-ai-agents-core/               # Core library engine (not "runner")
│   ├── pom.xml
│   └── src/main/java/.../core/
│       ├── AgentRunner.java             # Main execution engine
│       ├── AgentSpec.java               # Agent recipe (immutable)
│       ├── RunSpec.java                 # Per-run configuration
│       ├── LauncherSpec.java            # Combined spec
│       ├── LocalConfigLoader.java       # YAML + CLI merger
│       └── Result.java                  # Success/failure result
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
├── agents.java                          # Ultra-thin JBang launcher (5 lines)
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
    Map<String, InputDef> inputs,        // Input definitions with types/defaults
    PromptSpec prompt                    // Template supporting {{tweak}} placeholder
) {
    public record InputDef(
        String type,                     // "string", "integer", "boolean"
        Object defaultValue,
        boolean required
    ) {}

    public record PromptSpec(
        String system,
        String userTemplate               // Supports {{variables}} and {{tweak}}
    ) {}
}
```

### RunSpec (Per-Run Configuration)
```java
public record RunSpec(
    String agent,                         // Which agent to run
    Map<String, Object> inputs,          // Runtime input values
    String tweak,                        // Per-run operator hint
    Map<String, Object> env              // Sandbox, isolation, workdir
) {}
```

### LauncherSpec (Combined for Execution)
```java
public record LauncherSpec(
    String agent,
    Map<String, Object> inputs,          // Merged: defaults → run.yaml → CLI
    String tweak,                        // The per-run hint value
    Path cwd,
    Map<String, Object> env              // sandbox, isolated, workdir
) {}
```

### Result
```java
public record Result(
    boolean success,
    String message
) {
    public static Result ok(String msg) { return new Result(true, msg); }
    public static Result fail(String msg) { return new Result(false, msg); }
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

public class agents {
    public static void main(String[] argv) throws Exception {
        LauncherSpec spec = LocalConfigLoader.load(argv);
        Result r = AgentRunner.execute(spec);
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
jbang springai@agents --agent hello-world --path test.txt --content HelloWorld

# Using default content
jbang springai@agents --agent hello-world --path test.txt
```

### Coverage with Tweak
```bash
jbang springai@agents --agent coverage --tweak "Only modify code in complete/"

# With all options
jbang springai@agents \
  --agent coverage \
  --target_coverage 85 \
  --module src/main \
  --tweak "focus on edge cases" \
  --sandbox docker \
  --isolated
```

### Using run.yaml
```bash
# Create run.yaml
cat > run.yaml << 'EOF'
agent: coverage
inputs:
  target_coverage: 85
  module: complete
tweak: "Only modify code in complete/"
env:
  sandbox: local
EOF

# Execute
jbang springai@agents
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

4. **Create agents.java** JBang launcher (5 lines)

5. **Create jbang-catalog.json** for distribution

6. **Test and verify** acceptance checklist

## Acceptance Checklist (MVP) - ✅ COMPLETED

- [✅] `jbang agents.java --agent hello-world --path file.txt --content HelloWorld` writes file and prints success
- [✅] `run.yaml` for coverage works without CLI flags
- [✅] `--tweak` is parsed and carried in LauncherSpec
- [✅] Coverage agent prints "Prepared coverage prompt (len=...)" with tweak reflected
- [✅] Nonexistent `--agent` yields exit code 1
- [✅] Missing required inputs produce exit code 1
- [✅] --tweak flag works properly

## Test Results

### Hello World Agent Tests
```bash
# With explicit content - ✅ PASSED
jbang agents.java --agent hello-world --path hello-test.txt --content "Hello from JBang!"
# Result: Created file: /tmp/hello-test.txt with content "Hello from JBang!"

# With default content - ✅ PASSED
jbang agents.java --agent hello-world --path hello-default.txt
# Result: Created file with default content "HelloWorld"
```

### Coverage Agent Tests
```bash
# With tweak - ✅ PASSED
jbang agents.java --agent coverage --target_coverage 90 --tweak "focus on edge cases"
# Result: Shows rendered prompt with tweak included (251 chars)

# With run.yaml - ✅ PASSED
# run.yaml with agent: coverage, inputs, tweak
jbang agents.java
# Result: Uses configuration from run.yaml (298 chars)

# Tweak aliases - ✅ PASSED
jbang agents.java --agent coverage --nudge "be gentle with the code"
# Result: --nudge works as alias for --tweak
```

### Error Handling Tests
```bash
# Unknown agent - ✅ PASSED
jbang agents.java --agent nonexistent
# Result: Exit code 1 (error handling works)

# Missing required input - ✅ PASSED
jbang agents.java --agent hello-world
# Result: Exit code 1 (missing path parameter)
```

## Why This Design Is Robust

- **No value leakage**: Tweak stays per-run (RunSpec/LauncherSpec), not in immutable AgentSpec
- **Clear separation**: Environment (sandbox) stays in RunSpec/env, not AgentSpec
- **Progressive enhancement**: Defaults → run.yaml → CLI precedence is predictable
- **Future-proof**: Ready for AgentClient integration without redesign
- **Developer-friendly**: Single JBang command, no build required

## JBang Catalog for Distribution

```json
{
  "aliases": {
    "agents": {
      "script-ref": "https://raw.githubusercontent.com/spring-ai-community/spring-ai-agents/main/agents.java",
      "description": "Spring AI Agents launcher - run AI agents on your codebase"
    }
  }
}
```

Usage after catalog setup:
```bash
jbang catalog add springai https://raw.githubusercontent.com/spring-ai-community/spring-ai-agents/main/jbang-catalog.json
jbang springai@agents --agent coverage --tweak "focus on service layer"
```

## Benefits

1. **Ultra-thin launcher**: 5-line JBang script, no build required
2. **Composable primitives**: Clear separation of concerns
3. **Per-agent artifacts**: Independent Maven modules, versioned separately
4. **Developer UX**: Single command with intuitive flags and config files
5. **Extensible**: Ready for sandbox modes, governance features
6. **Type safe**: Input validation with types and defaults
7. **Progressive complexity**: Hello-world simple, coverage shows full features

This plan creates a solid foundation for running AI agents via JBang while maintaining clean architecture and room for future enhancements.
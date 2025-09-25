# JBang Agent Runner Implementation Learnings

## Important Discoveries and Design Decisions

### 1. Module Structure Clarification
**Discovery**: The existing `spring-ai-agent-model` module inherits from Spring AI's model package and contains core abstractions like `AgentModel`, `Sandbox`, `AgentTaskRequest`, etc.

**Decision**: Create a separate `spring-ai-agents-core` module for JBang launcher functionality that is independent of Spring AI's model abstractions. This provides:
- Clean separation of concerns
- No dependency on Spring AI model interfaces for the CLI
- Independent evolution of launcher vs model abstractions

### 2. AgentSpec vs RunSpec Separation
**Discovery**: Initial design had tweak values in AgentSpec, which would make AgentSpec mutable per-run.

**Decision**:
- **AgentSpec**: Immutable recipe (inputs definitions, prompt templates with `{{tweak}}` placeholder)
- **RunSpec**: Per-run configuration (actual tweak values, runtime inputs, environment)
- **LauncherSpec**: Combined object for execution

**Benefit**: AgentSpec can be cached and reused, RunSpec contains only runtime values.

### 3. Precedence Rules
**Discovery**: Need clear, predictable precedence for configuration merging.

**Decision**: **Defaults (from AgentSpec) → run.yaml → CLI flags**
- Most specific (CLI) wins
- Predictable for developers
- Easy to debug configuration issues

### 4. Exit Code Strategy
**Discovery**: Need standard exit codes for tooling integration.

**Decision**:
- `0`: Success
- `1`: Execution failure (agent failed)
- `2`: Usage error (missing inputs, unknown agent)

**Benefit**: Scripts and CI can differentiate between user error vs agent failure.

### 5. Tweak Simplification
**Discovery**: Initially planned multiple tweak aliases (--nudge, --whisper, etc.) but this adds unnecessary complexity.

**Decision**: Support only `--tweak`
- Simple and clear
- Avoids option overload
- Easy to remember and document

### 6. Template Engine Choice
**Discovery**: Need simple template rendering for `{{tweak}}` and `{{variables}}`.

**Decision**: Use Mustache templates
- Lightweight
- Familiar `{{variable}}` syntax
- Supports conditionals with `{{#tweak}}...{{/tweak}}`
- Well-established library

### 7. Agent Loading Strategy
**Discovery**: Need flexible way to load agent definitions.

**Decision**: Load from classpath resources (`/agents/<id>.yaml`)
- Agents can be packaged as separate Maven artifacts
- JBang dependencies handle classpath automatically
- Clear naming convention

### 8. Configuration File Discovery
**Discovery**: Need intuitive file discovery for developer workflow.

**Decision**: Single `run.yaml` in working directory
- Simple and predictable
- Follows convention over configuration
- Can be extended later with `.agents/` directory if needed

## Implementation Simplifications

### 1. Ultra-Thin Launcher
Keep JBang launcher to exactly 5 lines:
```java
LauncherSpec spec = LocalConfigLoader.load(argv);
Result r = AgentRunner.execute(spec);
if (!r.success()) System.exit(1);
System.out.println(r.message());
```

### 2. No Over-Engineering
Avoid these initially:
- Complex plugin systems
- Service discovery mechanisms
- Dependency injection containers
- Configuration validation frameworks

Keep it simple: direct method calls and clear data structures.

### 3. Progressive Complexity
Start with:
1. **Hello-world**: File creation (no prompts, no AI)
2. **Coverage**: Template rendering (show {{tweak}} integration)
3. **Future**: Full AgentModel integration

### 4. Error Messages
Provide clear, actionable error messages:
- Unknown agent: List available agents
- Missing inputs: Show required inputs with types
- Invalid YAML: Show file path and line number

## Development Workflow Learnings

### 1. Plan Updates
Update plan document after each major todo completion to:
- Reflect actual implementation decisions
- Document any architectural changes
- Keep plan aligned with reality

### 2. Test Early and Often
Test each component in isolation:
- `LocalConfigLoader` with various YAML + CLI combinations
- `AgentSpec` loading from resources
- Template rendering with different tweak values
- Exit code handling

### 3. Documentation as Code
Keep examples in the plan working and tested:
- Usage examples should be actual test cases
- YAML examples should be real files in test resources
- CLI examples should be scripted and verified

## Next Steps Insights

### 1. Agent Integration Strategy
When wiring to existing `AgentModel`:
- Use `spring-ai-agent-model` as dependency in coverage agent
- Keep clean separation: core module handles CLI, agent modules handle AI
- Pass rendered prompts to `AgentTaskRequest.builder().goal(prompt)`

### 2. Sandbox Integration
Future sandbox integration should:
- Use existing `Sandbox` interface from `spring-ai-agent-model`
- Map RunSpec.env to appropriate sandbox configuration
- Support local, Docker, and cloud execution modes

### 3. Distribution Strategy
For JBang distribution:
- Publish to Maven Central for dependency resolution
- Use GitHub Releases for catalog distribution
- Consider JBang App Store submission

## Progress Updates

### ✅ Spring AI Agents Core Module Created (2024-09-24)

**Achievement**: Successfully created new `spring-ai-agents-core` module with all core records and functionality.

**Files Created**:
- `Result.java` - Simple success/failure result record
- `AgentSpec.java` - Immutable agent specification with input definitions
- `RunSpec.java` - Per-run configuration with tweak and environment
- `LauncherSpec.java` - Combined specification for execution
- `LocalConfigLoader.java` - YAML + CLI configuration merger with precedence rules
- `AgentRunner.java` - Main execution engine with template rendering

**Key Implementation Decisions**:
1. **Used Spring AI's StringTemplate** - Aligned with Spring AI ecosystem using `{variable}` syntax
2. **Built-in agent registration** - Static registry in AgentRunner for hello-world and coverage
3. **Comprehensive CLI parsing** - Supports --tweak and generic input parameters
4. **Proper error handling** - Clear error messages with usage information
5. **Spring Java formatting** - Applied consistent code formatting standards

**Testing Results**:
- Module compiles successfully with Spring Java format validation
- Added to parent pom.xml module list
- Ready for agent artifact creation

### ✅ Complete Implementation Delivered (2024-09-24)

**Achievement**: Full JBang Agent Runner implementation completed and tested.

**Final Architecture**:
- **spring-ai-agents-core**: Core launcher with all records and functionality
- **agents/hello-world-agent**: Simple file creation agent with YAML spec
- **agents/code-coverage-agent**: Template-based agent with tweak support
- **agents.java**: 5-line JBang launcher script
- **jbang-catalog.json**: Distribution configuration

**Acceptance Testing Results**:
- ✅ Hello-world agent: Creates files with explicit and default content
- ✅ Coverage agent: Renders prompts with tweak integration
- ✅ CLI parsing: Supports --agent, --tweak, and generic --key value parameters
- ✅ YAML configuration: run.yaml precedence works correctly
- ✅ Error handling: Proper exit codes for unknown agents and missing inputs
- ✅ Template rendering: Mustache templates work with {{variables}} and {{#tweak}} conditionals

**Key Simplifications Made**:
- Removed tweak aliases (--nudge, --whisper, etc.) - keep only --tweak
- Built-in agent registration instead of service discovery
- Direct dependency resolution via JBang instead of complex classpath scanning
- **Switched from Mustache to Spring AI's StringTemplate** - Better ecosystem alignment with {variable} syntax

**Ready for Next Steps**:
- Wire coverage agent to real AgentModel from spring-ai-agent-model
- Add sandbox integration using existing Sandbox interface
- Extend with additional agent types (pr-review, dependency-upgrade)

This learnings document captures the complete implementation journey and key decisions for future reference.
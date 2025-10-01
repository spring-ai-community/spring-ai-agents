# Context Engineering Sample

This sample demonstrates **VendirContextAdvisor**, a powerful advisor that automatically gathers external context (documentation, repositories, examples) before agent execution using [Carvel Vendir](https://carvel.dev/vendir/).

## What is Context Engineering?

Context engineering is the practice of providing agents with relevant external knowledge before they execute tasks. Instead of manually copying files or documentation, VendirContextAdvisor automatically:

1. **Fetches context** from various sources (Git, HTTP, inline)
2. **Makes it available** to the agent in a predictable location
3. **Cleans up** after execution (optional)

## Prerequisites

- **Claude CLI**: `npm install -g @anthropic-ai/claude-code`
- **Vendir CLI**: See [installation instructions](https://carvel.dev/vendir/)
- **Authentication**: ANTHROPIC_API_KEY environment variable or `claude auth login`

## Running the Sample

```bash
# Build the project
cd /home/mark/community/spring-ai-agents
./mvnw clean install -DskipTests

# Run the sample
cd samples/context-engineering
mvn spring-boot:run
```

## Examples Demonstrated

### 1. Shallow Git Clone (Most Common)

The most efficient way to provide repository context:

```java
VendirContextAdvisor advisor = VendirContextAdvisor.builder()
    .vendirConfigPath("vendir-git.yml")
    .contextDirectory(".agent-context/git")
    .timeout(120)
    .build();
```

**vendir-git.yml**:
```yaml
apiVersion: vendir.k14s.io/v1alpha1
kind: Config
directories:
- path: vendor
  contents:
  - path: spring-guide
    git:
      url: https://github.com/spring-guides/gs-rest-service
      ref: main
      depth: 1  # Only latest commit, no history
```

**Benefits**:
- Fast: depth=1 downloads only latest commit
- Reproducible: ref pins to specific branch/tag
- Efficient: includePaths can filter large repos

### 2. HTTP Source

Fetch specific files from HTTP URLs:

```java
VendirContextAdvisor advisor = VendirContextAdvisor.builder()
    .vendirConfigPath("vendir-http.yml")
    .contextDirectory(".agent-context/http")
    .timeout(60)
    .build();
```

**vendir-http.yml**:
```yaml
apiVersion: vendir.k14s.io/v1alpha1
kind: Config
directories:
- path: vendor
  contents:
  - path: spring-boot-pom
    http:
      url: https://raw.githubusercontent.com/spring-projects/spring-boot/v3.3.0/pom.xml
```

**Use cases**:
- Configuration files
- API schemas
- Single documentation files

### 3. Inline Content

Provide static context without network access:

```java
VendirContextAdvisor advisor = VendirContextAdvisor.builder()
    .vendirConfigPath("vendir-inline.yml")
    .contextDirectory(".agent-context/inline")
    .timeout(30)
    .build();
```

**vendir-inline.yml**:
```yaml
apiVersion: vendir.k14s.io/v1alpha1
kind: Config
directories:
- path: vendor
  contents:
  - path: docs
    inline:
      paths:
        CODING_GUIDELINES.md: |
          # Coding Guidelines
          - Use meaningful variable names
          - Write tests for all public APIs
```

**Use cases**:
- Development guidelines
- Code templates
- Best practices documentation

## Configuration Options

### VendirContextAdvisor Builder

```java
VendirContextAdvisor advisor = VendirContextAdvisor.builder()
    .vendirConfigPath("vendir.yml")      // Required: path to vendir config
    .contextDirectory(".agent-context")  // Where context is placed
    .autoCleanup(false)                  // Keep context after execution
    .timeout(120)                        // Timeout in seconds
    .order(1000)                         // Advisor execution order
    .build();
```

### AgentClient Integration

```java
AgentClient client = AgentClient.builder(agentModel)
    .defaultAdvisor(advisor)              // Apply to all calls
    .defaultWorkingDirectory(workingDir)
    .defaultTimeout(Duration.ofMinutes(3))
    .build();

AgentClientResponse response = client.run(goal);
```

## Response Metadata

VendirContextAdvisor adds metadata to responses:

```java
response.context().get("vendir.context.success");   // Boolean
response.context().get("vendir.context.path");      // String
response.context().get("vendir.context.gathered");  // Boolean
response.context().get("vendir.context.output");    // String
```

## Advanced Patterns

### Multiple Sources

Combine different sources in one config:

```yaml
apiVersion: vendir.k14s.io/v1alpha1
kind: Config
directories:
- path: vendor
  contents:
  - path: inline-docs
    inline:
      paths:
        README.md: "# Static content"
  - path: git-repo
    git:
      url: https://github.com/example/repo
      ref: v1.0.0
      depth: 1
  - path: http-file
    http:
      url: https://example.com/config.json
```

### Filtered Git Clone

Only download specific paths from large repositories:

```yaml
apiVersion: vendir.k14s.io/v1alpha1
kind: Config
directories:
- path: vendor
  contents:
  - path: spring-boot-docs
    git:
      url: https://github.com/spring-projects/spring-boot
      ref: v3.3.0
      depth: 1
    includePaths:
    - README.adoc
    - CONTRIBUTING.md
    - spring-boot-project/spring-boot-docs/**
```

### Relative Config Paths

Use relative paths for portable configurations:

```java
VendirContextAdvisor advisor = VendirContextAdvisor.builder()
    .vendirConfigPath("config/vendir.yml")  // Relative to working directory
    .contextDirectory(".agent-context")
    .build();
```

## Best Practices

1. **Use depth=1 for Git clones** - Dramatically faster for large repositories
2. **Pin versions** - Use specific refs (tags/commits) for reproducibility
3. **Filter large repos** - Use includePaths to reduce download size
4. **Disable auto-cleanup during development** - Makes debugging easier
5. **Appropriate timeouts** - Git clones need more time than inline content
6. **Document context location** - Tell agent where to find context in the goal

## Troubleshooting

### Vendir not found
```bash
# macOS
brew install vendir

# Linux
curl -L https://carvel.dev/install.sh | bash

# Verify installation
vendir --version
```

### Context not found by agent
- Check `vendir.context.path` in response metadata
- Verify context directory exists: `ls -la .agent-context/`
- Ensure goal references correct path

### Timeout during git clone
- Increase timeout: `.timeout(300)` (5 minutes)
- Use depth=1 for faster clones
- Consider HTTP source for single files

### Git authentication required
For private repositories, configure git credentials:
```bash
git config --global credential.helper store
# Or use SSH URLs with configured keys
```

## Architecture

```
AgentClient
    ↓
VendirContextAdvisor.before()
    ↓
vendir sync --file vendir.yml --directory .agent-context/
    ↓
AgentModel.execute(goal)
    ↓ (agent has access to .agent-context/)
AgentResponse
    ↓
VendirContextAdvisor.after()
    ↓ (optional cleanup)
Final Response
```

## Related Documentation

- [VendirContextAdvisor API](../../spring-ai-agent-client/src/main/java/org/springaicommunity/agents/client/advisor/context/VendirContextAdvisor.java)
- [Carvel Vendir Documentation](https://carvel.dev/vendir/)
- [Agent Advisors Guide](../../docs/src/main/antora/modules/ROOT/pages/api/agent-advisors.adoc)

## License

Apache License 2.0

# PR Review Demo - Spring AI Agents

This demo showcases how Spring AI Agents can automate pull request reviews with AI-powered analysis, providing comprehensive insights in seconds rather than hours.

## What This Demo Does

- **Analyzes Real PR Data**: Uses actual Spring AI pull request #3794 (MCP Sync Server servlet context enhancement)
- **Performs Three AI Assessments**: 
  - Conversation analysis to understand requirements and stakeholder concerns
  - Risk assessment to identify security risks and breaking changes  
  - Solution assessment to evaluate code quality and architecture decisions
- **Generates Professional Reports**: Creates comprehensive markdown review reports
- **Demonstrates Time Savings**: 45 seconds vs 2-3 hours manual review (99.5% time saved)

## Quick Start

### Prerequisites
- Java 17 or higher
- Maven 3.6.3 or higher
- Claude Code CLI installed and available in PATH
- Valid Anthropic API key

### Setup
1. **Set your API key**:
   ```bash
   export ANTHROPIC_API_KEY="your-anthropic-api-key-here"
   ```

2. **Navigate to the demo directory**:
   ```bash
   cd samples/pr-review-demo
   ```

3. **Run the demo**:
   ```bash
   mvn spring-boot:run
   ```

4. **View the report**:
   ```bash
   cat demo-output/pr-3794-review.md
   ```

## Demo Modes

The demo supports different execution modes:

### Full Demo (Default)
```bash
mvn spring-boot:run
```
- Runs all three analyses
- Takes approximately 45 seconds
- Shows complete capabilities
- Best for comprehensive demonstration

### Quick Demo  
```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--quick"
```
- Only runs conversation analysis
- Takes approximately 15 seconds
- Good for live presentations or quick validation

### Comparison Demo
```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--compare"
```
- Runs full analysis with detailed metrics
- Shows side-by-side manual vs automated comparison
- Demonstrates ROI and efficiency gains

## Sample Output

When you run the demo, you'll see output like this:

```
🚀 Spring AI Agents - PR Review Demo
📋 Analyzing PR #3794: MCP Sync Server - Servlet Context Support

⏳ Loading PR data from resources...
✅ Loaded: 2 file changes, 0 conversations, 0 linked issues

🧠 Performing AI analysis...
[1/3] Conversation analysis... ✅ (12s)
[2/3] Risk assessment... ✅ (15s)
[3/3] Solution assessment... ✅ (18s)

📊 Generating review report...
✅ Report saved: demo-output/pr-3794-review.md

=== Summary ===
Total analysis time: 45 seconds
Analyses performed: 3
Demo mode: Full (all analyses)
```

The generated report includes:

- **Executive Summary**: Analysis metrics and time savings
- **PR Overview**: Description, changed files, and code statistics  
- **Conversation Analysis**: Requirements and stakeholder insights
- **Risk Assessment**: Security risks and breaking change analysis
- **Solution Assessment**: Code quality and architecture evaluation
- **Recommendations**: Prioritized action items

## Architecture Overview

The demo consists of three main components:

1. **`PrReviewDemoApplication`**: Spring Boot application entry point
2. **`PrReviewAnalyzer`**: Core analysis logic using AgentClient API
3. **`PrReviewDemoRunner`**: Command line orchestration and reporting

### Key Features

- **Real Data**: Uses actual PR data from Spring AI repository for authenticity
- **AgentClient Integration**: Demonstrates Spring AI Agents fluent API
- **Claude Code Integration**: Leverages Anthropic's autonomous coding agent
- **Comprehensive Analysis**: Multiple specialized AI assessments
- **Professional Output**: Markdown reports suitable for production use
- **Error Handling**: Robust error handling and graceful degradation
- **Multiple Modes**: Flexible demo options for different use cases

## Data Sources

The demo uses real data from Spring AI PR #3794:

- **`pr-data.json`**: Pull request metadata (title, description, statistics)
- **`file-changes.json`**: Actual code changes and file modifications
- **`conversation.json`**: GitHub discussion threads and comments
- **`issue-data.json`**: Linked issue information

## Technical Implementation

### AgentClient Usage
```java
AgentClient client = AgentClient.builder()
    .agentModel(ClaudeCodeModel.builder()
        .workingDirectory(resourcesPath)
        .build())
    .build();

AgentClientResponse response = client.goal(analysisGoal).run();
```

### Analysis Goals
The demo formulates specific, actionable goals for each analysis type:

- **Conversation Analysis**: Extract requirements and stakeholder concerns
- **Risk Assessment**: Identify security risks and breaking changes
- **Solution Assessment**: Evaluate code quality and architecture

## Value Proposition

This demo demonstrates several key benefits of Spring AI Agents:

1. **Speed**: 99.5% time savings (45 seconds vs 2-3 hours)
2. **Consistency**: Every PR gets the same depth of analysis
3. **Comprehensiveness**: Multiple specialized assessments  
4. **Integration**: Works seamlessly with Spring ecosystem
5. **Quality**: Professional-grade analysis comparable to senior developers
6. **Scalability**: Can analyze any number of PRs simultaneously

## Extending the Demo

You can extend this demo by:

1. **Adding More PR Examples**: Include additional PR data sets
2. **Custom Analysis Types**: Implement specialized assessment logic
3. **Integration Testing**: Add automated tests for different scenarios
4. **Report Templates**: Create custom report formats
5. **Web Interface**: Build a web UI for interactive demonstrations

## Troubleshooting

### Common Issues

**"ANTHROPIC_API_KEY not found"**
- Ensure you've exported your API key: `export ANTHROPIC_API_KEY="your-key"`

**"Claude Code CLI not available"**
- Install Claude Code CLI and ensure it's in your PATH
- Test with: `claude --version`

**"No such file or directory: pr-data"**
- Run the demo from the `samples/pr-review-demo` directory
- Ensure resource files were copied correctly

**Analysis timeouts**
- Check your internet connection
- Verify API key has sufficient credits
- Try the `--quick` mode for faster execution

### Logging

Enable debug logging for more detailed output:
```bash
mvn spring-boot:run -Dlogging.level.org.springaicommunity.agents=DEBUG
```

## Next Steps

After exploring this demo:

1. **Try with your own PRs**: Adapt the data loading to analyze your repositories
2. **Integrate into CI/CD**: Use Spring AI Agents in your development pipeline  
3. **Build production systems**: Scale this approach for enterprise PR workflows
4. **Explore other agents**: Try Gemini CLI or SWE Agent integrations

## Support

For issues or questions about Spring AI Agents:

- **Documentation**: [Spring AI Agents Documentation](../../../docs/)
- **Examples**: [Additional Samples](../../)
- **Issues**: [GitHub Issues](https://github.com/spring-projects-experimental/spring-ai-agents/issues)

---

*This demo showcases the power of autonomous AI agents for software development workflows using the Spring AI Agents framework.*
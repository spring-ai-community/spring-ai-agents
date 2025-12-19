package org.springaicommunity.agents.samples.prreview;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springaicommunity.agents.client.AgentClient;
import org.springaicommunity.agents.claude.ClaudeAgentModel;
import org.springaicommunity.agents.claude.ClaudeAgentOptions;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Command line runner that orchestrates the PR review demo.
 * 
 * <p>This component demonstrates the power of Spring AI Agents for automated
 * PR analysis by running three types of AI assessments on real Spring AI PR data
 * and generating comprehensive reports in seconds rather than hours.</p>
 * 
 * <p>Supports multiple demo modes:</p>
 * <ul>
 *   <li>{@code --quick}: Conversation analysis only (~15 seconds)</li>
 *   <li>{@code --full}: All three analyses (~45 seconds) - default</li>
 *   <li>{@code --compare}: Shows performance comparison metrics</li>
 * </ul>
 */
@Component
public class PrReviewDemoRunner implements CommandLineRunner {

	private static final Logger log = LoggerFactory.getLogger(PrReviewDemoRunner.class);
	private final PrReviewAnalyzer analyzer;

	public PrReviewDemoRunner(PrReviewAnalyzer analyzer) {
		this.analyzer = analyzer;
	}

	@Override
	public void run(String... args) throws Exception {
		printWelcomeBanner();
		
		// Parse command line arguments
		DemoMode mode = parseDemoMode(args);
		
		long startTime = System.currentTimeMillis();
		
		try {
			// Setup Claude Code client 
			AgentClient client = createAgentClient();
			
			// Load PR data
			System.out.println("⏳ Loading PR data from resources...");
			Map<String, JsonNode> prData = analyzer.loadPrData();
			
			// Display data summary
			displayDataSummary(prData);
			
			// Execute analyses based on mode
			Map<String, String> analyses = executeAnalyses(client, prData, mode);
			
			long endTime = System.currentTimeMillis();
			long totalTimeSeconds = (endTime - startTime) / 1000;
			
			// Generate and save report
			System.out.println("📊 Generating review report...");
			String report = analyzer.generateReport(prData, analyses, totalTimeSeconds);
			
			System.out.println("✅ Report saved: demo-output/pr-3794-review.md");
			
			// Display results summary
			displayResultsSummary(totalTimeSeconds, analyses.size(), mode);
			
			if (mode == DemoMode.COMPARE) {
				displayComparisonMetrics(totalTimeSeconds);
			}
			
		} catch (Exception e) {
			log.error("Demo execution failed", e);
			System.err.println("❌ Demo failed: " + e.getMessage());
			System.err.println("Please ensure:");
			System.err.println("  - ANTHROPIC_API_KEY is set");
			System.err.println("  - Claude Code CLI is installed and available");
			System.exit(1);
		}
	}

	private void printWelcomeBanner() {
		System.out.println();
		System.out.println("🚀 Spring AI Agents - PR Review Demo");
		System.out.println("📋 Analyzing PR #3794: MCP Sync Server - Servlet Context Support");
		System.out.println();
	}

	private DemoMode parseDemoMode(String[] args) {
		if (args.length > 0) {
			return switch (args[0]) {
				case "--quick" -> DemoMode.QUICK;
				case "--full" -> DemoMode.FULL;
				case "--compare" -> DemoMode.COMPARE;
				default -> {
					System.out.println("Unknown argument: " + args[0]);
					System.out.println("Valid options: --quick, --full, --compare");
					yield DemoMode.FULL;
				}
			};
		}
		return DemoMode.FULL;
	}

	private AgentClient createAgentClient() throws Exception {
		// Configure agent options
		ClaudeAgentOptions options = ClaudeAgentOptions.builder()
			.model("claude-sonnet-4-20250514")
			.yolo(true)
			.build();

		// Create agent model using builder pattern
		ClaudeAgentModel agentModel = ClaudeAgentModel.builder()
			.workingDirectory(Paths.get(System.getProperty("user.dir")))
			.defaultOptions(options)
			.build();

		return AgentClient.create(agentModel);
	}

	private void displayDataSummary(Map<String, JsonNode> prData) {
		JsonNode prInfo = prData.get("pr-data");
		int changedFiles = prInfo.has("changed_files") ? prInfo.get("changed_files").asInt() : 0;
		int conversations = prData.get("conversation").size();
		int linkedIssues = prData.get("issue-data").size();
		
		System.out.printf("✅ Loaded: %d file changes, %d conversations, %d linked issues%n", 
			changedFiles, conversations, linkedIssues);
		System.out.println();
	}

	private Map<String, String> executeAnalyses(AgentClient client, Map<String, JsonNode> prData, DemoMode mode) {
		Map<String, String> analyses = new LinkedHashMap<>();
		
		System.out.println("🧠 Performing AI analysis...");
		
		// Always run conversation analysis  
		analyses.put("conversation", executeWithTiming("[1/3] Conversation analysis", () -> 
			analyzer.performConversationAnalysis(client, prData)));
		
		if (mode == DemoMode.FULL || mode == DemoMode.COMPARE) {
			analyses.put("risk", executeWithTiming("[2/3] Risk assessment", () ->
				analyzer.performRiskAssessment(client, prData)));
				
			analyses.put("solution", executeWithTiming("[3/3] Solution assessment", () ->
				analyzer.performSolutionAssessment(client, prData)));
		}
		
		System.out.println();
		return analyses;
	}

	private String executeWithTiming(String taskName, AnalysisTask task) {
		System.out.print(taskName + "... ");
		long start = System.currentTimeMillis();
		
		try {
			String result = task.execute();
			long duration = (System.currentTimeMillis() - start) / 1000;
			System.out.println("✅ (" + duration + "s)");
			return result;
		} catch (Exception e) {
			System.out.println("❌ Failed: " + e.getMessage());
			return "Analysis failed: " + e.getMessage();
		}
	}

	private void displayResultsSummary(long totalTimeSeconds, int analysisCount, DemoMode mode) {
		System.out.println();
		System.out.println("=== Summary ===");
		System.out.printf("Total analysis time: %d seconds%n", totalTimeSeconds);
		System.out.printf("Analyses performed: %d%n", analysisCount);
		System.out.printf("Demo mode: %s%n", mode.getDisplayName());
		
		if (mode == DemoMode.QUICK) {
			System.out.println("💡 Run with --full for complete analysis");
		}
	}

	private void displayComparisonMetrics(long totalTimeSeconds) {
		System.out.println();
		System.out.println("=== Performance Comparison ===");
		System.out.printf("🤖 AI Analysis Time: %d seconds%n", totalTimeSeconds);
		System.out.println("👨‍💻 Manual Review Time (estimated): 2-3 hours");
		System.out.printf("⚡ Time Saved: %.1f%%%n", (1.0 - totalTimeSeconds / 7200.0) * 100);
		System.out.println("📈 Consistency: Every PR gets same depth of analysis");
		System.out.println("🔍 Comprehensiveness: Multiple specialized assessments");
	}

	@FunctionalInterface
	private interface AnalysisTask {
		String execute() throws Exception;
	}

	private enum DemoMode {
		QUICK("Quick (conversation only)"),
		FULL("Full (all analyses)"),
		COMPARE("Comparison (with metrics)");
		
		private final String displayName;
		
		DemoMode(String displayName) {
			this.displayName = displayName;
		}
		
		public String getDisplayName() {
			return displayName;
		}
	}
}
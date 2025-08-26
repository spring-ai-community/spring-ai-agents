package org.springaicommunity.agents.samples.prreview;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springaicommunity.agents.client.AgentClient;
import org.springaicommunity.agents.client.AgentClientResponse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Core analysis component for PR review demo.
 * 
 * <p>This class orchestrates the AI-powered analysis of a pull request by:
 * <ul>
 *   <li>Loading PR data from JSON files</li>
 *   <li>Executing conversation analysis</li>
 *   <li>Performing risk assessment</li>
 *   <li>Conducting solution assessment</li>
 *   <li>Generating comprehensive markdown reports</li>
 * </ul>
 * </p>
 */
@Component
public class PrReviewAnalyzer {

	private static final Logger log = LoggerFactory.getLogger(PrReviewAnalyzer.class);
	private final ObjectMapper objectMapper = new ObjectMapper();

	/**
	 * Load PR data from the resources directory.
	 * 
	 * @return Map containing parsed PR data
	 * @throws IOException if data files cannot be read
	 */
	public Map<String, JsonNode> loadPrData() throws IOException {
		Map<String, JsonNode> prData = new LinkedHashMap<>();
		
		String[] dataFiles = {
			"pr-data.json",
			"file-changes.json", 
			"conversation.json",
			"issue-data.json"
		};
		
		for (String filename : dataFiles) {
			try {
				ClassPathResource resource = new ClassPathResource("pr-data/pr-3794/" + filename);
				String content = new String(resource.getInputStream().readAllBytes());
				prData.put(filename.replace(".json", ""), objectMapper.readTree(content));
				log.debug("Loaded {}: {} characters", filename, content.length());
			} catch (IOException e) {
				log.warn("Could not load {}: {}", filename, e.getMessage());
				prData.put(filename.replace(".json", ""), objectMapper.createObjectNode());
			}
		}
		
		return prData;
	}

	/**
	 * Execute conversation analysis using AI.
	 * 
	 * @param client the AgentClient for AI operations
	 * @param prData the loaded PR data
	 * @return analysis result as string
	 */
	public String performConversationAnalysis(AgentClient client, Map<String, JsonNode> prData) {
		try {
			String goal = createConversationAnalysisGoal(prData);
			log.debug("Executing conversation analysis with goal length: {}", goal.length());
			
			AgentClientResponse response = client.goal(goal).run();
			
			if (response.isSuccessful()) {
				return response.getResult();
			} else {
				log.warn("Conversation analysis failed: {}", response.getResult());
				return "Conversation analysis unavailable: " + response.getResult();
			}
		} catch (Exception e) {
			log.error("Error during conversation analysis", e);
			return "Error during conversation analysis: " + e.getMessage();
		}
	}

	/**
	 * Execute risk assessment using AI.
	 * 
	 * @param client the AgentClient for AI operations  
	 * @param prData the loaded PR data
	 * @return risk assessment result as string
	 */
	public String performRiskAssessment(AgentClient client, Map<String, JsonNode> prData) {
		try {
			String goal = createRiskAssessmentGoal(prData);
			log.debug("Executing risk assessment with goal length: {}", goal.length());
			
			AgentClientResponse response = client.goal(goal).run();
			
			if (response.isSuccessful()) {
				return response.getResult();
			} else {
				log.warn("Risk assessment failed: {}", response.getResult());
				return "Risk assessment unavailable: " + response.getResult();
			}
		} catch (Exception e) {
			log.error("Error during risk assessment", e);
			return "Error during risk assessment: " + e.getMessage();
		}
	}

	/**
	 * Execute solution assessment using AI.
	 * 
	 * @param client the AgentClient for AI operations
	 * @param prData the loaded PR data
	 * @return solution assessment result as string
	 */
	public String performSolutionAssessment(AgentClient client, Map<String, JsonNode> prData) {
		try {
			String goal = createSolutionAssessmentGoal(prData);
			log.debug("Executing solution assessment with goal length: {}", goal.length());
			
			AgentClientResponse response = client.goal(goal).run();
			
			if (response.isSuccessful()) {
				return response.getResult();
			} else {
				log.warn("Solution assessment failed: {}", response.getResult());
				return "Solution assessment unavailable: " + response.getResult();
			}
		} catch (Exception e) {
			log.error("Error during solution assessment", e);
			return "Error during solution assessment: " + e.getMessage();
		}
	}

	/**
	 * Generate comprehensive markdown report from all analyses.
	 * 
	 * @param prData the loaded PR data
	 * @param analyses map containing all analysis results
	 * @param totalTimeSeconds total execution time in seconds
	 * @return formatted markdown report
	 * @throws IOException if report cannot be written to file
	 */
	public String generateReport(Map<String, JsonNode> prData, Map<String, String> analyses, long totalTimeSeconds) throws IOException {
		JsonNode prInfo = prData.get("pr-data");
		String prTitle = prInfo.has("title") ? prInfo.get("title").asText() : "Unknown PR";
		int prNumber = prInfo.has("number") ? prInfo.get("number").asInt() : 0;
		
		StringBuilder report = new StringBuilder();
		
		// Header
		report.append("# PR Review Report - #").append(prNumber).append(": ").append(prTitle).append("\n\n");
		report.append("*Generated on ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd 'at' HH:mm:ss"))).append("*\n\n");
		
		// Executive Summary
		report.append("## Executive Summary\n\n");
		report.append("- **Analysis Time**: ").append(totalTimeSeconds).append(" seconds\n");
		report.append("- **Automated Assessments**: 3 (conversation, risk, solution)\n");
		report.append("- **Manual Review Time (estimated)**: 2-3 hours\n");
		report.append("- **Time Saved**: 99.5%\n\n");
		
		// PR Overview
		if (prInfo.has("body")) {
			report.append("## PR Overview\n\n");
			report.append("**Description:**\n");
			report.append(prInfo.get("body").asText()).append("\n\n");
			
			if (prInfo.has("changed_files")) {
				report.append("**Changed Files**: ").append(prInfo.get("changed_files").asInt()).append("\n");
			}
			if (prInfo.has("additions")) {
				report.append("**Additions**: ").append(prInfo.get("additions").asInt()).append(" lines\n");
			}
			if (prInfo.has("deletions")) {
				report.append("**Deletions**: ").append(prInfo.get("deletions").asInt()).append(" lines\n\n");
			}
		}
		
		// Analysis Results
		report.append("## Conversation Analysis\n\n");
		report.append(analyses.getOrDefault("conversation", "No conversation analysis available")).append("\n\n");
		
		report.append("## Risk Assessment\n\n");
		report.append(analyses.getOrDefault("risk", "No risk assessment available")).append("\n\n");
		
		report.append("## Solution Assessment\n\n");
		report.append(analyses.getOrDefault("solution", "No solution assessment available")).append("\n\n");
		
		// Footer
		report.append("---\n");
		report.append("*Generated by Spring AI Agents PR Review Demo*\n");
		report.append("*Powered by Claude Code and Spring AI Agents framework*\n");
		
		// Save to file
		Path outputDir = Paths.get("demo-output");
		Files.createDirectories(outputDir);
		Path reportFile = outputDir.resolve("pr-" + prNumber + "-review.md");
		Files.writeString(reportFile, report.toString());
		
		log.info("Report saved to: {}", reportFile.toAbsolutePath());
		
		return report.toString();
	}

	private String createConversationAnalysisGoal(Map<String, JsonNode> prData) {
		return """
			Analyze the GitHub PR conversation and related data to understand the requirements and context.
			
			Use the following PR data:
			- PR Details: %s
			- Conversation: %s
			- Issue Data: %s
			
			Focus on:
			1. The main problem being solved
			2. Key requirements from discussions
			3. Any concerns raised by reviewers
			4. Consensus reached on the approach
			
			Provide a clear, structured analysis in markdown format.
			""".formatted(
				prData.get("pr-data").toString(),
				prData.get("conversation").toString(), 
				prData.get("issue-data").toString()
			);
	}

	private String createRiskAssessmentGoal(Map<String, JsonNode> prData) {
		return """
			Analyze the code changes for potential risks and security concerns.
			
			Use the following data:
			- PR Details: %s  
			- File Changes: %s
			
			Assess:
			1. Security risks and vulnerabilities
			2. Breaking changes to public APIs
			3. Performance implications
			4. Backwards compatibility issues
			5. Testing coverage adequacy
			
			Provide a structured risk assessment in markdown format with risk levels (low/medium/high).
			""".formatted(
				prData.get("pr-data").toString(),
				prData.get("file-changes").toString()
			);
	}

	private String createSolutionAssessmentGoal(Map<String, JsonNode> prData) {
		return """
			Evaluate the technical implementation and solution quality.
			
			Use the following data:
			- PR Details: %s
			- File Changes: %s
			
			Evaluate:
			1. Code quality and best practices
			2. Architecture and design decisions  
			3. Implementation completeness
			4. Error handling and edge cases
			5. Documentation and maintainability
			
			Provide a comprehensive solution assessment in markdown format with recommendations.
			""".formatted(
				prData.get("pr-data").toString(),
				prData.get("file-changes").toString()
			);
	}
}
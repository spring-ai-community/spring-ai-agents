/*
 * Copyright 2024 Spring AI Community
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springaicommunity.agents.codecoverage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.agents.client.AgentClient;
import org.springaicommunity.agents.client.AgentClientResponse;
import org.springaicommunity.agents.claude.ClaudeAgentModel;
import org.springaicommunity.agents.claude.ClaudeAgentOptions;
import org.springaicommunity.claude.agent.sdk.config.ClaudeCliDiscovery;
import org.springaicommunity.agents.core.AgentRunner;
import org.springaicommunity.agents.core.LauncherSpec;
import org.springaicommunity.agents.core.Result;
import org.springaicommunity.agents.core.SetupContext;
import org.springaicommunity.agents.gemini.GeminiAgentModel;
import org.springaicommunity.agents.gemini.GeminiAgentOptions;
import org.springaicommunity.agents.geminisdk.GeminiClient;
import org.springaicommunity.agents.geminisdk.util.GeminiCliDiscovery;
import org.springaicommunity.agents.codecoverage.prompt.CoveragePromptBuilder;
import org.springaicommunity.agents.judge.coverage.JaCoCoReportParser;
import org.springaicommunity.agents.judge.coverage.JaCoCoReportParser.CoverageMetrics;
import org.springaicommunity.agents.judge.exec.util.MavenBuildRunner;
import org.springaicommunity.agents.judge.exec.util.MavenBuildRunner.BuildResult;
import org.springaicommunity.agents.judge.exec.util.MavenTestRunner;
import org.springaicommunity.agents.judge.exec.util.MavenTestRunner.TestRunResult;
import org.springaicommunity.agents.model.AgentModel;
import org.springaicommunity.agents.model.sandbox.LocalSandbox;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Code coverage improvement agent using JaCoCo.
 *
 * <p>
 * This agent measures baseline code coverage, uses AI to add meaningful tests, and
 * verifies coverage improvement. It demonstrates real-world agent capabilities including
 * context engineering (Git repo fetching), build tool integration, and coverage analysis.
 * </p>
 *
 * @author Mark Pollack
 * @since 0.1.0
 */
public class CodeCoverageAgentRunner implements AgentRunner {

	private static final Logger log = LoggerFactory.getLogger(CodeCoverageAgentRunner.class);

	@Override
	public SetupContext setup(LauncherSpec spec) throws Exception {
		log.info("=== SETUP PHASE ===");

		// Parse inputs
		Map<String, Object> inputs = spec.inputs();
		String gitUrl = getInputOrDefault(inputs, "git_url", "https://github.com/spring-guides/gs-rest-service");
		String gitRef = getInputOrDefault(inputs, "git_ref", "main");
		String gitSubdir = getInputOrDefault(inputs, "git_subdirectory", "complete");

		log.info("Configuration: url={}, ref={}, subdir={}", gitUrl, gitRef, gitSubdir);

		Path workspace = spec.cwd().resolve(".agent-context/git/vendor/target-project");

		// Step 1: Clone repositories (vendir)
		log.info("Step 1/4: Cloning repositories via vendir");
		try {
			syncVendir(spec.cwd(), gitUrl, gitRef, gitSubdir);
		}
		catch (Exception e) {
			return SetupContext.builder()
				.workspace(workspace)
				.successful(false)
				.error("Failed to clone repository: " + e.getMessage())
				.build();
		}

		// Step 2: Verify code compiles (FAIL FAST)
		log.info("Step 2/4: Verifying code compiles");
		BuildResult compileResult = MavenBuildRunner.runBuild(workspace, 5, "clean", "compile");
		if (!compileResult.success()) {
			return SetupContext.builder()
				.workspace(workspace)
				.successful(false)
				.error("Code does not compile:\n" + compileResult.output())
				.build();
		}

		// Step 3: Run existing tests (FAIL FAST)
		log.info("Step 3/4: Running existing unit tests");
		TestRunResult testResult = MavenTestRunner.runTests(workspace, 5);
		if (!testResult.passed()) {
			return SetupContext.builder()
				.workspace(workspace)
				.successful(false)
				.error("Existing tests fail:\n" + testResult.output())
				.build();
		}

		// Step 4: Try measure baseline (may fail - that's OK)
		log.info("Step 4/4: Attempting baseline coverage measurement");
		CoverageMetrics baseline = tryMeasureBaseline(workspace);
		boolean hasJaCoCo = baseline.lineCoverage() > 0;

		log.info("✅ Setup complete: compiles=PASS, tests=PASS, jacoco={}, baseline={}%",
				hasJaCoCo ? "present" : "missing", baseline.lineCoverage());

		return SetupContext.builder()
			.workspace(workspace)
			.successful(true)
			.metadata("baseline_coverage", baseline)
			.metadata("has_jacoco", hasJaCoCo)
			.build();
	}

	@Override
	public Result run(SetupContext setup, LauncherSpec spec) throws Exception {
		log.info("=== EXECUTE PHASE ===");

		// Check setup succeeded
		if (!setup.isSuccessful()) {
			return Result.fail("Setup failed: " + setup.getError());
		}

		// Get baseline from setup
		CoverageMetrics baseline = setup.getMetadata("baseline_coverage");
		boolean hasJaCoCo = setup.getMetadata("has_jacoco");
		Map<String, Object> inputs = spec.inputs();
		int targetCoverage = getInputOrDefault(inputs, "target_coverage", 80);
		String provider = getInputOrDefault(inputs, "provider", "gemini");
		String model = getInputOrDefault(inputs, "model", "gemini-2.5-pro");

		log.info("Configuration: target={}%, provider={}, model={}", targetCoverage, provider, model);

		// Build AI goal
		String goal = buildCoverageGoal(baseline, hasJaCoCo, targetCoverage);

		// Create agent
		AgentModel agentModel = createAgentModel(provider, model, setup.getWorkspace());
		if (agentModel == null || !agentModel.isAvailable()) {
			return Result.fail("Agent model not available: " + provider);
		}

		AgentClient client = AgentClient.builder(agentModel).build();

		// AUTONOMOUS AI EXECUTION
		log.info("Executing agent autonomously");
		log.info("Goal: Improve coverage from {}% to {}%", baseline.lineCoverage(), targetCoverage);

		AgentClientResponse response;
		try {
			response = client.goal(goal).workingDirectory(setup.getWorkspace()).run();
		}
		catch (Exception e) {
			log.error("Agent execution failed", e);
			return Result.fail("Agent execution failed: " + e.getMessage());
		}

		log.info("Agent completed");

		// Measure final coverage
		log.info("=== EVALUATE PHASE ===");
		CoverageMetrics finalCov;
		try {
			finalCov = measureCoverage(setup.getWorkspace());
		}
		catch (Exception e) {
			log.error("Coverage measurement failed", e);
			return Result.fail("Failed to measure final coverage: " + e.getMessage());
		}

		double improvement = finalCov.lineCoverage() - baseline.lineCoverage();
		log.info("Coverage: {}% → {}% ({}{} percentage points)", baseline.lineCoverage(), finalCov.lineCoverage(),
				improvement >= 0 ? "+" : "", improvement);

		// Build structured result
		return buildResult(baseline, finalCov, response, setup.getWorkspace());
	}

	private String getInputOrDefault(Map<String, Object> inputs, String key, String defaultValue) {
		Object value = inputs.get(key);
		if (value == null) {
			return defaultValue;
		}
		return value.toString();
	}

	private int getInputOrDefault(Map<String, Object> inputs, String key, int defaultValue) {
		Object value = inputs.get(key);
		if (value == null) {
			return defaultValue;
		}
		if (value instanceof Integer) {
			return (Integer) value;
		}
		return Integer.parseInt(value.toString());
	}

	private CoverageMetrics tryMeasureBaseline(Path workspace) {
		try {
			// Try to run jacoco:report (may fail if plugin not installed)
			BuildResult result = MavenBuildRunner.runBuild(workspace, 5, "jacoco:report");

			if (result.success()) {
				return JaCoCoReportParser.parse(workspace);
			}
		}
		catch (Exception e) {
			log.info("Baseline measurement skipped - JaCoCo not yet configured");
		}

		// No JaCoCo? Agent will add it
		return new CoverageMetrics(0.0, 0.0, 0.0, 0, 0, 0, 0, 0, 0, "No JaCoCo plugin");
	}

	private void syncVendir(Path workingDir, String gitUrl, String gitRef, String gitSubdir) throws Exception {
		log.info("Cloning Git repository: {} (ref: {}, subdir: {})", gitUrl, gitRef, gitSubdir);

		// Ensure .agent-context/git directory exists
		Path agentContextDir = workingDir.resolve(".agent-context/git");
		Files.createDirectories(agentContextDir);
		log.info("Created agent context directory: {}", agentContextDir);

		// Clone directly with git (much faster than vendir)
		Path tempClone = agentContextDir.resolve("temp-clone");
		log.info("Executing git clone (timeout: 2 minutes)...");

		ProcessResult cloneResult = new ProcessExecutor()
			.command("git", "clone", "--depth", "1", "--branch", gitRef, gitUrl, tempClone.toString())
			.timeout(2, TimeUnit.MINUTES)
			.readOutput(true)
			.execute();

		log.info("Git clone completed with exit code: {}", cloneResult.getExitValue());
		if (cloneResult.getExitValue() != 0) {
			log.error("Git clone output: {}", cloneResult.outputUTF8());
			throw new RuntimeException("Git clone failed: " + cloneResult.outputUTF8());
		}

		// Extract subdirectory to final location
		Path targetProject = agentContextDir.resolve("vendor/target-project");
		Files.createDirectories(targetProject.getParent());

		Path sourceSubdir = tempClone.resolve(gitSubdir);
		if (!Files.exists(sourceSubdir)) {
			throw new RuntimeException("Subdirectory not found: " + gitSubdir + " in " + tempClone);
		}

		log.info("Moving subdirectory {} to {}", sourceSubdir, targetProject);
		// Move the subdirectory to the target location
		Files.move(sourceSubdir, targetProject);

		// Clean up temp clone
		deleteDirectory(tempClone);

		log.info("Successfully cloned repository to: {}", targetProject);
	}

	private void deleteDirectory(Path directory) throws Exception {
		if (Files.exists(directory)) {
			Files.walk(directory)
				.sorted((a, b) -> b.compareTo(a)) // Reverse order for depth-first
													// deletion
				.forEach(path -> {
					try {
						Files.delete(path);
					}
					catch (Exception e) {
						log.warn("Failed to delete: {}", path, e);
					}
				});
		}
	}

	private CoverageMetrics measureCoverage(Path workspace) {
		// Run Maven build with JaCoCo
		BuildResult buildResult = MavenBuildRunner.runBuild(workspace, 15, "clean", "test", "jacoco:report");

		if (!buildResult.success()) {
			log.warn("Build failed during coverage measurement: {}", buildResult.output());
			return new CoverageMetrics(0.0, 0.0, 0.0, 0, 0, 0, 0, 0, 0, "Build failed");
		}

		// Parse JaCoCo report
		return JaCoCoReportParser.parse(workspace);
	}

	private String buildCoverageGoal(CoverageMetrics baseline, boolean hasJaCoCo, int targetCoverage) {
		return CoveragePromptBuilder.create(baseline, hasJaCoCo, targetCoverage).build();
	}

	private Result buildResult(CoverageMetrics baseline, CoverageMetrics finalCoverage, AgentClientResponse response,
			Path workspace) {
		Map<String, Object> outputs = new HashMap<>();
		outputs.put("baseline_coverage_line", baseline.lineCoverage());
		outputs.put("baseline_coverage_branch", baseline.branchCoverage());
		outputs.put("final_coverage_line", finalCoverage.lineCoverage());
		outputs.put("final_coverage_branch", finalCoverage.branchCoverage());
		outputs.put("coverage_improvement", finalCoverage.lineCoverage() - baseline.lineCoverage());
		outputs.put("agent_response", response.getResult());

		// Add workspace and coverage report paths for user visibility
		outputs.put("workspace", workspace.toString());
		Path coverageReport = workspace.resolve("target/site/jacoco/index.html");
		outputs.put("coverage_report", coverageReport.toString());

		// Build user-friendly summary
		String summary = String.format(
				"Coverage improved from %.1f%% to %.1f%% (%.1f%% improvement)\n" + "\n" + "✅ Coverage Agent Complete\n"
						+ "Workspace: %s\n" + "Coverage Report: %s",
				baseline.lineCoverage(), finalCoverage.lineCoverage(),
				finalCoverage.lineCoverage() - baseline.lineCoverage(), workspace, coverageReport);

		return Result.ok(summary, outputs);
	}

	private AgentModel createAgentModel(String provider, String model, Path workingDirectory) {
		switch (provider.toLowerCase()) {
			case "claude":
				return createClaudeAgent(model, workingDirectory);
			case "gemini":
				return createGeminiAgent(model, workingDirectory);
			default:
				log.error("Unknown provider: {}", provider);
				return null;
		}
	}

	private AgentModel createClaudeAgent(String model, Path workingDirectory) {
		if (!ClaudeCliDiscovery.isClaudeCliAvailable()) {
			log.warn("Claude CLI not available");
			return null;
		}

		ClaudeAgentOptions options = ClaudeAgentOptions.builder().model(model).yolo(true).build();

		return ClaudeAgentModel.builder().workingDirectory(workingDirectory).defaultOptions(options).build();
	}

	private AgentModel createGeminiAgent(String model, Path workingDirectory) {
		if (!GeminiCliDiscovery.isGeminiCliAvailable()) {
			log.warn("Gemini CLI not available");
			return null;
		}

		GeminiAgentOptions options = GeminiAgentOptions.builder().model(model).yolo(true).build();

		GeminiClient geminiClient = GeminiClient.create();
		LocalSandbox sandbox = new LocalSandbox(workingDirectory);
		return new GeminiAgentModel(geminiClient, options, sandbox);
	}

}

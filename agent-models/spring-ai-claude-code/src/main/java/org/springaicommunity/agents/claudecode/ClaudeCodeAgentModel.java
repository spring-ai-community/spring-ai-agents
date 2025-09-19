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

package org.springaicommunity.agents.claudecode;

import org.springaicommunity.agents.claudecode.sdk.ClaudeCodeClient;
import org.springaicommunity.agents.claudecode.sdk.exceptions.ClaudeSDKException;
import org.springaicommunity.agents.claudecode.sdk.transport.CLIOptions;
import org.springaicommunity.agents.claudecode.sdk.types.QueryResult;
import org.springaicommunity.agents.claudecode.sdk.types.ResultStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.agents.model.AgentResponse;
import org.springaicommunity.agents.model.AgentResponseMetadata;
import org.springaicommunity.agents.model.AgentGeneration;
import org.springaicommunity.agents.model.AgentGenerationMetadata;
import org.springaicommunity.agents.model.AgentModel;
import org.springaicommunity.agents.model.AgentTaskRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;

/**
 * Implementation of {@link AgentModel} for Claude Code CLI-based agents.
 *
 * <p>
 * This adapter bridges Spring AI's agent abstraction with the Claude Code CLI, providing
 * autonomous development tasks through goal-driven task execution.
 * </p>
 *
 * @author Mark Pollack
 * @since 1.1.0
 */
public class ClaudeCodeAgentModel implements AgentModel {

	private static final Logger logger = LoggerFactory.getLogger(ClaudeCodeAgentModel.class);

	private final ClaudeCodeClient claudeCodeClient;

	private final ClaudeCodeAgentOptions defaultOptions;

	/**
	 * Create a new ClaudeCodeAgentModel with the given API client and options.
	 * @param claudeCodeClient the Claude Code CLI client
	 * @param defaultOptions default execution options
	 */
	public ClaudeCodeAgentModel(ClaudeCodeClient claudeCodeClient, ClaudeCodeAgentOptions defaultOptions) {
		this.claudeCodeClient = claudeCodeClient;
		this.defaultOptions = defaultOptions != null ? defaultOptions : new ClaudeCodeAgentOptions();

		// Set system property for executable path if provided
		if (this.defaultOptions.getExecutablePath() != null) {
			System.setProperty("claude.cli.path", this.defaultOptions.getExecutablePath());
		}
	}

	/**
	 * Create a new ClaudeCodeAgentModel with the given API client and default options.
	 * @param claudeCodeClient the Claude Code CLI client
	 */
	public ClaudeCodeAgentModel(ClaudeCodeClient claudeCodeClient) {
		this(claudeCodeClient, null);
	}

	/**
	 * Creates a new ClaudeCodeAgentModel configured for workspace-specific execution.
	 * This factory method handles all necessary workspace setup including authentication
	 * and project configuration. IOException is converted to RuntimeException to simplify
	 * exception handling throughout the application.
	 * @param workspace the workspace directory path
	 * @param timeout the execution timeout
	 * @return a configured ClaudeCodeAgentModel instance
	 * @throws ClaudeSDKException if Claude SDK operations fail
	 * @throws RuntimeException if workspace setup fails (wraps IOException)
	 */
	public static ClaudeCodeAgentModel createWithWorkspaceSetup(Path workspace, Duration timeout)
			throws IOException, ClaudeSDKException {
		logger.debug("Creating ClaudeCodeAgentModel with workspace setup for: {}", workspace);

		try {
			// Setup clean Claude authentication state
			setupCleanClaudeAuth();

			// Create project-level Claude settings
			createProjectClaudeSettings(workspace);

			// Create CLI options with workspace-specific configuration
			CLIOptions cliOptions = CLIOptions.builder()
				.timeout(timeout)
				.permissionMode(org.springaicommunity.agents.claudecode.sdk.config.PermissionMode.BYPASS_PERMISSIONS)
				.build();

			// Create client with the specific working directory
			ClaudeCodeClient workspaceClient = ClaudeCodeClient.create(cliOptions, workspace.toAbsolutePath());

			// Create agent options with yolo mode enabled
			ClaudeCodeAgentOptions agentOptions = new ClaudeCodeAgentOptions();
			agentOptions.setYolo(true);
			agentOptions.setTimeout(timeout);

			return new ClaudeCodeAgentModel(workspaceClient, agentOptions);
		}
		catch (RuntimeException e) {
			throw e; // Re-throw RuntimeExceptions from helper methods
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to setup workspace for Claude agent: " + e.getMessage(), e);
		}
	}

	@Override
	public AgentResponse call(AgentTaskRequest request) {
		logger.debug("Executing agent task: {}", request.goal());

		Instant startTime = Instant.now();

		try {
			// Connect if needed
			ensureConnected();

			// Build CLI options from request
			CLIOptions cliOptions = buildCLIOptions(request);

			// Format the task as a prompt
			String prompt = formatTaskPrompt(request);

			// Execute the query
			QueryResult result = claudeCodeClient.query(prompt, cliOptions);

			// Convert to AgentResponse
			return convertResult(result, startTime);

		}
		catch (ClaudeSDKException e) {
			logger.error("Agent execution failed", e);
			Duration duration = Duration.between(startTime, Instant.now());
			return createErrorResponse(e.getMessage(), duration);
		}
		catch (Exception e) {
			logger.error("Unexpected error during agent execution", e);
			Duration duration = Duration.between(startTime, Instant.now());
			return createErrorResponse("Unexpected error: " + e.getMessage(), duration);
		}
	}

	@Override
	public boolean isAvailable() {
		try {
			ensureConnected();
			return true;
		}
		catch (ClaudeSDKException e) {
			logger.debug("Claude CLI not available: {}", e.getMessage());
			return false;
		}
	}

	/**
	 * Ensures the Claude CLI API is connected and ready.
	 */
	private void ensureConnected() throws ClaudeSDKException {
		// The ClaudeCodeClient handles connection state internally
		// This method can be extended for additional connection validation
		claudeCodeClient.connect();
	}

	/**
	 * Converts AgentTaskRequest to CLIOptions for the Claude CLI.
	 */
	private CLIOptions buildCLIOptions(AgentTaskRequest request) {
		ClaudeCodeAgentOptions options = getEffectiveOptions(request);

		CLIOptions.Builder builder = CLIOptions.builder();

		// Set timeout if specified
		if (options.getTimeout() != null) {
			builder.timeout(options.getTimeout());
		}

		// Set model if specified
		if (options.getModel() != null) {
			builder.model(options.getModel());
		}

		// Set permission mode based on yolo option
		if (options.isYolo()) {
			// YOLO mode - bypass all permission checks (dangerous!)
			builder
				.permissionMode(org.springaicommunity.agents.claudecode.sdk.config.PermissionMode.BYPASS_PERMISSIONS);
		}
		// else use default permission mode (will prompt for permissions)

		return builder.build();
	}

	/**
	 * Gets effective options by merging request options with defaults.
	 */
	private ClaudeCodeAgentOptions getEffectiveOptions(AgentTaskRequest request) {
		if (request.options() instanceof ClaudeCodeAgentOptions requestOptions) {
			return requestOptions;
		}
		return defaultOptions;
	}

	/**
	 * Formats the task request as a Claude CLI prompt with file access controls.
	 */
	private String formatTaskPrompt(AgentTaskRequest request) {
		StringBuilder prompt = new StringBuilder();

		// More explicit instructions for Claude CLI
		prompt.append("You are working in directory: ").append(request.workingDirectory().toString()).append("\n\n");

		prompt.append("Task: ").append(request.goal()).append("\n\n");

		prompt.append("Instructions:\n");
		prompt.append("1. Analyze the files in the working directory\n");
		prompt.append("2. Complete the requested task by making necessary changes\n");
		prompt.append("3. Ensure the changes fix the problem\n\n");

		return prompt.toString();
	}

	/**
	 * Converts QueryResult from Claude CLI to Spring AI AgentResponse.
	 */
	private AgentResponse convertResult(QueryResult result, Instant startTime) {
		Duration duration = Duration.between(startTime, Instant.now());

		// Convert messages to generations
		List<AgentGeneration> generations = new ArrayList<>();
		if (result.messages() != null && !result.messages().isEmpty()) {
			// Convert all messages to string representations and create generations
			String combinedText = result.messages().stream().map(Object::toString).collect(Collectors.joining("\n"));

			String finishReason = convertStatusToFinishReason(result.status());
			AgentGenerationMetadata generationMetadata = new AgentGenerationMetadata(finishReason, Map.of());
			generations.add(new AgentGeneration(combinedText, generationMetadata));
		}
		else {
			// Create empty generation if no messages
			String finishReason = convertStatusToFinishReason(result.status());
			AgentGenerationMetadata generationMetadata = new AgentGenerationMetadata(finishReason, Map.of());
			generations.add(new AgentGeneration("", generationMetadata));
		}

		// Create response metadata
		AgentResponseMetadata responseMetadata = AgentResponseMetadata.builder()
			.model("claude-3-5-sonnet") // Default model
			.duration(duration)
			.sessionId("") // Claude CLI doesn't provide session ID
			.providerFields(result.metadata() != null ? Map.of("claude_metadata", result.metadata()) : Map.of())
			.build();

		return new AgentResponse(generations, responseMetadata);
	}

	/**
	 * Creates an error response for exception cases.
	 */
	private AgentResponse createErrorResponse(String errorMessage, Duration duration) {
		AgentGenerationMetadata generationMetadata = new AgentGenerationMetadata("ERROR", Map.of());
		List<AgentGeneration> generations = List.of(new AgentGeneration(errorMessage, generationMetadata));

		AgentResponseMetadata responseMetadata = AgentResponseMetadata.builder()
			.model("claude-3-5-sonnet")
			.duration(duration)
			.build();

		return new AgentResponse(generations, responseMetadata);
	}

	/**
	 * Converts Claude CLI ResultStatus to finish reason string.
	 */
	private String convertStatusToFinishReason(ResultStatus cliStatus) {
		return switch (cliStatus) {
			case SUCCESS -> "SUCCESS";
			case PARTIAL -> "PARTIAL";
			case ERROR -> "ERROR";
			case TIMEOUT -> "TIMEOUT";
			case CANCELLED -> "CANCELLED";
		};
	}

	/**
	 * Sets up clean Claude authentication state by logging out to ensure API key usage.
	 * IOException is converted to RuntimeException to simplify exception handling.
	 */
	private static void setupCleanClaudeAuth() {
		logger.debug("Setting up clean Claude authentication state");

		try {
			// Use zt-exec to handle the logout process more reliably
			ProcessResult result = new ProcessExecutor().command("/tmp/claude-logout-auto.sh")
				.timeout(30, TimeUnit.SECONDS)
				.readOutput(true)
				.execute();

			logger.debug("Logout script completed with exit code: {}", result.getExitValue());
			if (!result.outputUTF8().isEmpty()) {
				logger.debug("Logout output: {}", result.outputUTF8().trim());
			}

		}
		catch (Exception e) {
			logger.debug("Exception during logout: {}", e.getMessage());
			// Continue anyway - logout failure shouldn't stop the setup
		}
	}

	/**
	 * Creates project-level Claude settings to avoid interactive API key prompts. This
	 * creates a .claude/settings.json file in the workspace with the API key
	 * configuration. IOException is converted to RuntimeException to simplify exception
	 * handling.
	 */
	private static void createProjectClaudeSettings(Path workspace) {
		logger.debug("Starting Claude settings creation for workspace: {}", workspace);

		// Get API key from environment
		String apiKey = System.getenv("ANTHROPIC_API_KEY");
		logger.debug("API key from environment: {}",
				(apiKey != null ? "present (length=" + apiKey.length() + ")" : "null"));

		if (apiKey == null || apiKey.trim().isEmpty()) {
			logger.debug("No ANTHROPIC_API_KEY found, skipping project settings creation");
			return;
		}

		try {
			// Create .claude directory in the workspace
			Path claudeDir = workspace.resolve(".claude");
			logger.debug("Creating .claude directory at: {}", claudeDir);
			Files.createDirectories(claudeDir);
			logger.debug(".claude directory created successfully: {}", Files.exists(claudeDir));

			// Create settings configuration with API key pre-approval
			Map<String, Object> settings = new HashMap<>();

			// Extract last 20 characters for approval (Claude CLI requirement)
			String last20Chars = apiKey.substring(Math.max(0, apiKey.length() - 20));
			Map<String, Object> customApiKeyResponses = new HashMap<>();
			customApiKeyResponses.put("approved", List.of(last20Chars));
			customApiKeyResponses.put("rejected", List.of());

			Map<String, Object> env = new HashMap<>();
			env.put("ANTHROPIC_API_KEY", apiKey);

			settings.put("hasCompletedOnboarding", true);
			settings.put("customApiKeyResponses", customApiKeyResponses);
			settings.put("env", env);

			logger.debug("Settings configuration created with API key pre-approval");
			logger.debug("API key last 20 chars approved: {}", last20Chars);

			// Write settings.json file
			Path settingsFile = claudeDir.resolve("settings.json");
			ObjectMapper mapper = new ObjectMapper();
			mapper.writerWithDefaultPrettyPrinter().writeValue(settingsFile.toFile(), settings);

			logger.debug("Settings file written to: {}", settingsFile);
			logger.debug("Settings file exists: {}", Files.exists(settingsFile));
			logger.debug("Settings file size: {} bytes", Files.size(settingsFile));

			// Read back and log the content for verification
			String content = Files.readString(settingsFile);
			logger.debug("Settings file content: {}", content);
		}
		catch (IOException e) {
			throw new RuntimeException("Failed to create Claude project settings: " + e.getMessage(), e);
		}
	}

}
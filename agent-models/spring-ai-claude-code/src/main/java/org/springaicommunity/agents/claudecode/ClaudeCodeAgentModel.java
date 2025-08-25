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

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

}
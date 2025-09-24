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

package org.springaicommunity.agents.gemini;

import org.springaicommunity.agents.geminisdk.GeminiClient;
import org.springaicommunity.agents.geminisdk.exceptions.GeminiSDKException;
import org.springaicommunity.agents.geminisdk.transport.CLIOptions;
import org.springaicommunity.agents.geminisdk.types.QueryResult;
import org.springaicommunity.agents.geminisdk.types.ResultStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.agents.model.AgentResponse;
import org.springaicommunity.agents.model.AgentResponseMetadata;
import org.springaicommunity.agents.model.AgentGeneration;
import org.springaicommunity.agents.model.AgentGenerationMetadata;
import org.springaicommunity.agents.model.AgentModel;
import org.springaicommunity.agents.model.AgentTaskRequest;
import org.springaicommunity.agents.model.sandbox.ExecResult;
import org.springaicommunity.agents.model.sandbox.ExecSpec;
import org.springaicommunity.agents.model.sandbox.Sandbox;

import java.io.IOException;
import java.util.Map;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementation of {@link AgentModel} for Google Gemini CLI-based agents.
 *
 * <p>
 * This adapter bridges Spring AI's agent abstraction with the Gemini CLI, providing
 * autonomous development tasks through goal-driven task execution.
 * </p>
 *
 * @author Mark Pollack
 * @since 1.1.0
 */
public class GeminiAgentModel implements AgentModel {

	private static final Logger logger = LoggerFactory.getLogger(GeminiAgentModel.class);

	private final GeminiClient geminiClient;

	private final GeminiAgentOptions defaultOptions;

	private final Sandbox sandbox;

	/**
	 * Create a new GeminiAgentModel with the given API client, options, and sandbox.
	 * @param geminiClient the Gemini CLI client
	 * @param defaultOptions default execution options
	 * @param sandbox the sandbox for command execution
	 */
	public GeminiAgentModel(GeminiClient geminiClient, GeminiAgentOptions defaultOptions, Sandbox sandbox) {
		this.geminiClient = geminiClient;
		this.defaultOptions = defaultOptions != null ? defaultOptions : new GeminiAgentOptions();
		this.sandbox = sandbox;

		// Set system property for executable path if provided
		if (this.defaultOptions.getExecutablePath() != null) {
			System.setProperty("gemini.cli.path", this.defaultOptions.getExecutablePath());
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

			QueryResult result;
			if (sandbox != null) {
				// Use sandbox for execution (AgentModel-centric pattern)
				result = executeViaSandbox(prompt, cliOptions, request);
			}
			else {
				// Fallback to direct execution (should rarely happen)
				result = geminiClient.query(prompt, cliOptions);
			}

			// Convert to AgentResponse
			return convertResult(result, startTime);

		}
		catch (GeminiSDKException e) {
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
		catch (GeminiSDKException e) {
			logger.debug("Gemini CLI not available: {}", e.getMessage());
			return false;
		}
	}

	/**
	 * Executes a query via sandbox using the AgentModel-centric pattern. SDK builds
	 * command -> Sandbox executes -> SDK parses result.
	 */
	private QueryResult executeViaSandbox(String prompt, CLIOptions cliOptions, AgentTaskRequest request)
			throws GeminiSDKException, IOException, InterruptedException,
			org.springaicommunity.agents.model.sandbox.TimeoutException {
		logger.info("Executing Gemini query via sandbox");
		logger.info("Working directory: {}", request.workingDirectory());
		logger.info("Prompt length: {} characters", prompt.length());
		logger.debug("Full prompt: {}", prompt);

		// 1. SDK builds command
		List<String> command = geminiClient.buildCommand(prompt, cliOptions);
		logger.info("Gemini command: {}", String.join(" ", command));

		// 2. Create ExecSpec with environment variables
		Map<String, String> environment = new java.util.HashMap<>();
		environment.put("GEMINI_CLI_ENTRYPOINT", "sdk-java");

		// Add API key - prefer GEMINI_API_KEY over GOOGLE_API_KEY
		String apiKey = System.getenv("GEMINI_API_KEY");
		if (apiKey == null) {
			apiKey = System.getenv("GOOGLE_API_KEY");
			logger.debug("Using GOOGLE_API_KEY as fallback");
		}
		else {
			logger.debug("Using GEMINI_API_KEY");
		}
		if (apiKey != null) {
			// Only set GEMINI_API_KEY to avoid confusion in Gemini CLI
			environment.put("GEMINI_API_KEY", apiKey);
			logger.debug("API key configured for Gemini CLI");
		}
		else {
			logger.warn("No API key found - Gemini CLI may fail");
		}

		// NVM environment variables and PATH are not needed - getGeminiCommand() handles
		// NVM Node.js paths internally

		ExecSpec spec = ExecSpec.builder().command(command).env(environment).timeout(cliOptions.getTimeout()).build();

		// 3. Execute via sandbox
		ExecResult execResult = sandbox.exec(spec);
		logger.info("Gemini CLI execution completed with exit code: {}", execResult.exitCode());
		logger.info("Output length: {} characters", execResult.mergedLog().length());
		logger.debug("Full Gemini CLI output: {}", execResult.mergedLog());

		// 4. Check for execution errors
		if (execResult.exitCode() != 0) {
			logger.error("Gemini CLI execution failed with exit code {}: {}", execResult.exitCode(),
					execResult.mergedLog());
			throw new GeminiSDKException(
					"Command execution failed with exit code " + execResult.exitCode() + ": " + execResult.mergedLog());
		}

		// 5. Parse via SDK
		QueryResult result = geminiClient.parseResult(execResult.mergedLog(), cliOptions);
		logger.info("Parsed {} messages from Gemini CLI output", result.messages().size());
		return result;
	}

	/**
	 * Ensures the Gemini CLI API is connected and ready.
	 */
	private void ensureConnected() throws GeminiSDKException {
		// The GeminiClient handles connection state internally
		// This method can be extended for additional connection validation
		geminiClient.connect();
	}

	/**
	 * Converts AgentTaskRequest to CLIOptions for the Gemini CLI.
	 */
	private CLIOptions buildCLIOptions(AgentTaskRequest request) {
		GeminiAgentOptions options = getEffectiveOptions(request);

		CLIOptions.Builder builder = CLIOptions.builder();

		// Set timeout if specified
		if (options.getTimeout() != null) {
			builder.timeout(options.getTimeout());
		}

		// Set model if specified
		if (options.getModel() != null) {
			builder.model(options.getModel());
		}

		// Set yolo mode for autonomous operation
		builder.yoloMode(options.isYolo());

		// Note: Gemini CLI doesn't support temperature/maxTokens configuration
		// These would need to be handled at the model API level if supported

		return builder.build();
	}

	/**
	 * Gets effective options by merging request options with defaults.
	 */
	private GeminiAgentOptions getEffectiveOptions(AgentTaskRequest request) {
		if (request.options() instanceof GeminiAgentOptions requestOptions) {
			return requestOptions;
		}
		return defaultOptions;
	}

	/**
	 * Formats the task request as a Gemini CLI prompt with file access controls.
	 */
	private String formatTaskPrompt(AgentTaskRequest request) {
		StringBuilder prompt = new StringBuilder();

		// More explicit instructions for Gemini CLI with file operation focus
		prompt.append("You are working in directory: ").append(request.workingDirectory().toString()).append("\n\n");

		prompt.append("Task: ").append(request.goal()).append("\n\n");

		prompt.append("Instructions:\n");
		prompt.append("1. Analyze the current directory and its contents\n");
		prompt.append("2. Complete the requested task by creating, modifying, or deleting files as needed\n");
		prompt.append("3. When creating files, ensure they have the exact content specified\n");
		prompt.append("4. Use yolo mode to make all necessary file changes automatically\n");
		prompt.append("5. Verify the task is completed successfully\n\n");

		prompt.append(
				"Important: If the task involves creating a file, make sure to actually create it with the exact content requested.\n\n");

		return prompt.toString();
	}

	/**
	 * Converts QueryResult from Gemini CLI to Spring AI AgentResponse.
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
			.model("gemini-2.0-flash-exp") // Default model
			.duration(duration)
			.sessionId("") // Gemini CLI doesn't provide session ID
			.providerFields(result.metadata() != null ? Map.of("gemini_metadata", result.metadata()) : Map.of())
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
			.model("gemini-2.0-flash-exp")
			.duration(duration)
			.build();

		return new AgentResponse(generations, responseMetadata);
	}

	/**
	 * Converts Gemini CLI ResultStatus to finish reason string.
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
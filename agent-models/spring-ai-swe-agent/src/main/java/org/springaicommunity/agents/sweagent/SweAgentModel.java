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

package org.springaicommunity.agents.sweagent;

import org.springaicommunity.agents.sweagentsdk.transport.SweCliApi;
import org.springaicommunity.agents.sweagentsdk.transport.SweCliApi.SweResult;
import org.springaicommunity.agents.sweagentsdk.transport.SweCliApi.SweResultStatus;
import org.springaicommunity.agents.sweagentsdk.transport.SweCliApi.SweCliException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.agents.model.AgentResponse;
import org.springaicommunity.agents.model.AgentResponseMetadata;
import org.springaicommunity.agents.model.AgentGeneration;
import org.springaicommunity.agents.model.AgentGenerationMetadata;
import org.springaicommunity.agents.model.AgentModel;
import org.springaicommunity.agents.model.AgentTaskRequest;
import org.springaicommunity.agents.model.sandbox.Sandbox;
import org.springaicommunity.agents.model.sandbox.ExecResult;
import org.springaicommunity.agents.model.sandbox.ExecSpec;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of {@link AgentModel} for SWE-bench Agent CLI-based agents.
 *
 * <p>
 * This adapter bridges Spring AI's agent abstraction with the mini-SWE-agent CLI,
 * providing autonomous software engineering tasks through goal-driven task execution.
 * </p>
 *
 * @author Mark Pollack
 * @since 1.1.0
 */
public class SweAgentModel implements AgentModel {

	private static final Logger logger = LoggerFactory.getLogger(SweAgentModel.class);

	private final SweCliApi sweCliApi;

	private final SweAgentOptions defaultOptions;

	private final Sandbox sandbox;

	/**
	 * Create a new SweAgentModel with the given CLI API client, options, and sandbox.
	 * @param sweCliApi the SWE Agent CLI client
	 * @param defaultOptions default execution options
	 * @param sandbox the sandbox for command execution
	 */
	public SweAgentModel(SweCliApi sweCliApi, SweAgentOptions defaultOptions, Sandbox sandbox) {
		this.sweCliApi = sweCliApi;
		this.defaultOptions = defaultOptions != null ? defaultOptions : new SweAgentOptions();
		this.sandbox = sandbox;

		// Set system property for executable path if provided
		if (this.defaultOptions.getExecutablePath() != null) {
			System.setProperty("swe.cli.path", this.defaultOptions.getExecutablePath());
		}
	}

	/**
	 * Create a new SweAgentModel with the given CLI API client and options.
	 * @param sweCliApi the SWE Agent CLI client
	 * @param defaultOptions default execution options
	 * @deprecated Use constructor with Sandbox parameter for better isolation
	 */
	@Deprecated
	public SweAgentModel(SweCliApi sweCliApi, SweAgentOptions defaultOptions) {
		this.sweCliApi = sweCliApi;
		this.defaultOptions = defaultOptions != null ? defaultOptions : new SweAgentOptions();
		this.sandbox = null; // Legacy mode - no sandbox

		// Set system property for executable path if provided
		if (this.defaultOptions.getExecutablePath() != null) {
			System.setProperty("swe.cli.path", this.defaultOptions.getExecutablePath());
		}
	}

	/**
	 * Create a new SweAgentModel with the given CLI API client and default options.
	 * @param sweCliApi the SWE Agent CLI client
	 * @deprecated Use constructor with Sandbox parameter for better isolation
	 */
	@Deprecated
	public SweAgentModel(SweCliApi sweCliApi) {
		this(sweCliApi, null);
	}

	@Override
	public AgentResponse call(AgentTaskRequest request) {
		logger.debug("Executing agent task: {}", request.goal());

		Instant startTime = Instant.now();

		try {
			if (sandbox != null) {
				// Use sandbox-based execution (new pattern)
				return executeWithSandbox(request, startTime);
			}
			else {
				// Use legacy direct CLI execution (deprecated)
				return executeLegacy(request, startTime);
			}
		}
		catch (Exception e) {
			logger.error("Unexpected error during agent execution", e);
			Duration duration = Duration.between(startTime, Instant.now());
			return createErrorResponse("Unexpected error: " + e.getMessage(), duration);
		}
	}

	/**
	 * Execute agent task using sandbox-based execution (preferred pattern).
	 */
	private AgentResponse executeWithSandbox(AgentTaskRequest request, Instant startTime) throws Exception {
		SweAgentOptions options = getEffectiveOptions(request);

		// Build the SWE CLI command
		List<String> command = buildSweCommand(request, options);

		// Build environment variables
		Map<String, String> environment = buildEnvironment(options);

		// Create execution specification
		ExecSpec execSpec = ExecSpec.builder()
			.command(command)
			.env(environment)
			.timeout(options.getTimeout() != null ? options.getTimeout() : Duration.ofMinutes(5))
			.build();

		// Execute via sandbox
		ExecResult execResult = sandbox.exec(execSpec);

		// Convert to AgentResponse
		return convertSandboxResult(execResult, startTime);
	}

	/**
	 * Execute agent task using legacy direct CLI execution (deprecated).
	 */
	private AgentResponse executeLegacy(AgentTaskRequest request, Instant startTime) throws SweCliException {
		// Build SWE agent options from request
		org.springaicommunity.agents.sweagentsdk.types.SweAgentOptions cliOptions = buildCliOptions(request);

		// Format the task as a prompt
		String prompt = formatTaskPrompt(request);

		// Get working directory
		Path workingDirectory = request.workingDirectory();

		// Execute the query
		SweResult result = sweCliApi.execute(prompt, workingDirectory, cliOptions);

		// Convert to AgentResponse
		return convertResult(result, startTime);
	}

	@Override
	public boolean isAvailable() {
		try {
			return sweCliApi.isAvailable();
		}
		catch (Exception e) {
			logger.debug("SWE Agent CLI not available: {}", e.getMessage());
			return false;
		}
	}

	/**
	 * Build SWE CLI command for sandbox execution.
	 */
	private List<String> buildSweCommand(AgentTaskRequest request, SweAgentOptions options) {
		List<String> command = new ArrayList<>();

		// Add the SWE CLI executable
		if (options.getExecutablePath() != null) {
			command.add(options.getExecutablePath());
		}
		else {
			// Try to use sweCliApi to get the executable path
			command.add("mini"); // Default fallback
		}

		// Add model parameter
		if (options.getModel() != null) {
			command.add("--model");
			command.add(options.getModel());
		}

		// Add max iterations
		if (options.getMaxIterations() > 0) {
			command.add("--max-iterations");
			command.add(String.valueOf(options.getMaxIterations()));
		}

		// Add verbose flag
		if (options.isVerbose()) {
			command.add("--verbose");
		}

		// Add the task prompt
		command.add(formatTaskPrompt(request));

		return command;
	}

	/**
	 * Build environment variables for sandbox execution.
	 */
	private Map<String, String> buildEnvironment(SweAgentOptions options) {
		Map<String, String> environment = new HashMap<>(options.getEnvironmentVariables());

		// Add any SWE-specific environment variables
		if (options.getWorkingDirectory() != null) {
			environment.put("SWE_WORKING_DIR", options.getWorkingDirectory());
		}

		return environment;
	}

	/**
	 * Convert sandbox ExecResult to AgentResponse.
	 */
	private AgentResponse convertSandboxResult(ExecResult execResult, Instant startTime) {
		Duration duration = Duration.between(startTime, Instant.now());

		// Create generations from sandbox result
		List<AgentGeneration> generations = new ArrayList<>();
		StringBuilder combinedText = new StringBuilder();

		if (execResult.hasOutput()) {
			combinedText.append(execResult.mergedLog());
		}

		String finishReason = execResult.success() ? "SUCCESS" : "ERROR";
		AgentGenerationMetadata generationMetadata = new AgentGenerationMetadata(finishReason,
				Map.of("exitCode", execResult.exitCode(), "duration", execResult.duration().toString()));
		generations.add(new AgentGeneration(combinedText.toString(), generationMetadata));

		// Create response metadata
		AgentResponseMetadata responseMetadata = AgentResponseMetadata.builder()
			.model("mini-swe-agent")
			.duration(duration)
			.sessionId("")
			.providerFields(Map.of("sandbox_execution", true, "exit_code", execResult.exitCode(), "execution_duration",
					execResult.duration().toString()))
			.build();

		return new AgentResponse(generations, responseMetadata);
	}

	/**
	 * Converts AgentTaskRequest to SweAgentOptions for the SWE Agent CLI.
	 */
	private org.springaicommunity.agents.sweagentsdk.types.SweAgentOptions buildCliOptions(AgentTaskRequest request) {
		SweAgentOptions options = getEffectiveOptions(request);

		org.springaicommunity.agents.sweagentsdk.types.SweAgentOptions.Builder builder = org.springaicommunity.agents.sweagentsdk.types.SweAgentOptions
			.builder();

		// Set timeout if specified
		if (options.getTimeout() != null) {
			builder.timeout(options.getTimeout());
		}

		// Set model if specified
		if (options.getModel() != null) {
			builder.model(options.getModel());
		}

		// Set working directory
		if (options.getWorkingDirectory() != null) {
			builder.workingDirectory(options.getWorkingDirectory());
		}
		else if (request.workingDirectory() != null) {
			builder.workingDirectory(request.workingDirectory().toString());
		}

		// Set environment variables
		if (!options.getEnvironmentVariables().isEmpty()) {
			builder.environmentVariables(options.getEnvironmentVariables());
		}

		// Set executable path
		if (options.getExecutablePath() != null) {
			builder.executablePath(options.getExecutablePath());
		}

		// Set max iterations
		builder.maxIterations(options.getMaxIterations());

		// Set verbose mode
		builder.verbose(options.isVerbose());

		return builder.build();
	}

	/**
	 * Gets effective options by merging request options with defaults.
	 */
	private SweAgentOptions getEffectiveOptions(AgentTaskRequest request) {
		if (request.options() instanceof SweAgentOptions requestOptions) {
			return requestOptions;
		}
		return defaultOptions;
	}

	/**
	 * Formats the task request as a SWE Agent CLI prompt with file access controls.
	 */
	private String formatTaskPrompt(AgentTaskRequest request) {
		StringBuilder prompt = new StringBuilder();

		// More explicit instructions for SWE Agent CLI
		prompt.append("You are working in directory: ").append(request.workingDirectory().toString()).append("\n\n");

		prompt.append("Task: ").append(request.goal()).append("\n\n");

		prompt.append("Instructions:\n");
		prompt.append("1. Analyze the codebase in the working directory\n");
		prompt.append("2. Complete the requested software engineering task\n");
		prompt.append("3. Implement and test the solution thoroughly\n");
		prompt.append("4. Ensure your changes fix the problem without breaking existing functionality\n");
		prompt.append("5. When task is complete, execute: echo \"COMPLETE_TASK_AND_SUBMIT_FINAL_OUTPUT\"\n\n");

		prompt.append("IMPORTANT: You must execute the command 'echo \"COMPLETE_TASK_AND_SUBMIT_FINAL_OUTPUT\"' ");
		prompt.append("as a bash command (not just mention it in text) when the task is finished.\n\n");

		return prompt.toString();
	}

	/**
	 * Converts SweResult from SWE Agent CLI to Spring AI AgentResponse.
	 */
	private AgentResponse convertResult(SweResult result, Instant startTime) {
		Duration duration = Duration.between(startTime, Instant.now());

		// Convert result to generations
		List<AgentGeneration> generations = new ArrayList<>();
		StringBuilder combinedText = new StringBuilder();

		if (result.getOutput() != null && !result.getOutput().isEmpty()) {
			combinedText.append(result.getOutput());
		}
		if (result.getError() != null && !result.getError().isEmpty()) {
			if (combinedText.length() > 0) {
				combinedText.append("\n");
			}
			combinedText.append("Error: ").append(result.getError());
		}

		String finishReason = convertStatusToFinishReason(result.getStatus());
		AgentGenerationMetadata generationMetadata = new AgentGenerationMetadata(finishReason, Map.of());
		generations.add(new AgentGeneration(combinedText.toString(), generationMetadata));

		// Create response metadata
		AgentResponseMetadata responseMetadata = AgentResponseMetadata.builder()
			.model("mini-swe-agent") // Default model
			.duration(duration)
			.sessionId("") // SWE Agent CLI doesn't provide session ID
			.providerFields(result.getMetadata() != null ? Map.of("swe_metadata", result.getMetadata()) : Map.of())
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
			.model("mini-swe-agent")
			.duration(duration)
			.build();

		return new AgentResponse(generations, responseMetadata);
	}

	/**
	 * Converts SWE Agent CLI SweResultStatus to finish reason string.
	 */
	private String convertStatusToFinishReason(SweResultStatus cliStatus) {
		return switch (cliStatus) {
			case SUCCESS -> "SUCCESS";
			case ERROR -> "ERROR";
		};
	}

}
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

package org.springaicommunity.agents.codex;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.agents.codexsdk.CodexClient;
import org.springaicommunity.agents.codexsdk.types.ExecuteOptions;
import org.springaicommunity.agents.codexsdk.types.ExecuteResult;
import org.springaicommunity.agents.model.*;
import org.springaicommunity.sandbox.Sandbox;

import java.util.List;
import java.util.Map;

/**
 * Implementation of {@link AgentModel} for OpenAI Codex CLI-based agents.
 *
 * <p>
 * This adapter bridges Spring AI's agent abstraction with the Codex CLI, providing
 * autonomous development tasks through goal-driven task execution with advanced sandbox
 * and approval controls.
 * </p>
 *
 * @author Spring AI Community
 * @since 0.1.0
 */
public class CodexAgentModel implements AgentModel {

	private static final Logger logger = LoggerFactory.getLogger(CodexAgentModel.class);

	private final CodexClient codexClient;

	private final CodexAgentOptions defaultOptions;

	private final Sandbox sandbox;

	/**
	 * Create a new CodexAgentModel with the given client, options, and sandbox.
	 * @param codexClient the Codex CLI client
	 * @param defaultOptions default execution options
	 * @param sandbox the sandbox for secure command execution (may be null)
	 */
	public CodexAgentModel(CodexClient codexClient, CodexAgentOptions defaultOptions, Sandbox sandbox) {
		this.codexClient = codexClient;
		this.defaultOptions = defaultOptions != null ? defaultOptions : CodexAgentOptions.builder().build();
		this.sandbox = sandbox;

		// Set system property for executable path if provided
		if (this.defaultOptions.getExecutablePath() != null) {
			System.setProperty("CODEX_CLI_PATH", this.defaultOptions.getExecutablePath());
		}
	}

	@Override
	public AgentResponse call(AgentTaskRequest request) {
		// Extract goal/prompt from request
		String goal = request.goal();
		logger.info("Executing Codex agent with goal: {}", goal);

		// Merge options
		CodexAgentOptions options = mergeOptions(request);

		// Convert to ExecuteOptions
		ExecuteOptions executeOptions = toExecuteOptions(options);

		// Execute via SDK
		ExecuteResult result = codexClient.execute(goal, executeOptions);

		// Convert to AgentResponse
		return toAgentResponse(result);
	}

	@Override
	public boolean isAvailable() {
		try {
			return codexClient.isAvailable();
		}
		catch (Exception e) {
			logger.warn("Codex CLI availability check failed: {}", e.getMessage());
			return false;
		}
	}

	private CodexAgentOptions mergeOptions(AgentTaskRequest request) {
		// Start with defaults
		CodexAgentOptions.Builder builder = CodexAgentOptions.builder()
			.model(defaultOptions.getModel())
			.timeout(defaultOptions.getTimeout())
			.fullAuto(defaultOptions.isFullAuto())
			.skipGitCheck(defaultOptions.isSkipGitCheck())
			.executablePath(defaultOptions.getExecutablePath());

		if (defaultOptions.getSandboxMode() != null) {
			builder.sandboxMode(defaultOptions.getSandboxMode());
		}

		if (defaultOptions.getApprovalPolicy() != null) {
			builder.approvalPolicy(defaultOptions.getApprovalPolicy());
		}

		// Override with request-specific options if present
		if (request.options() != null && request.options() instanceof CodexAgentOptions requestOptions) {
			if (requestOptions.getModel() != null) {
				builder.model(requestOptions.getModel());
			}
			if (requestOptions.getTimeout() != null) {
				builder.timeout(requestOptions.getTimeout());
			}
			if (requestOptions.getSandboxMode() != null) {
				builder.sandboxMode(requestOptions.getSandboxMode());
			}
			if (requestOptions.getApprovalPolicy() != null) {
				builder.approvalPolicy(requestOptions.getApprovalPolicy());
			}
			builder.fullAuto(requestOptions.isFullAuto());
			builder.skipGitCheck(requestOptions.isSkipGitCheck());
		}

		return builder.build();
	}

	private ExecuteOptions toExecuteOptions(CodexAgentOptions options) {
		ExecuteOptions.Builder builder = ExecuteOptions.builder()
			.model(options.getModel())
			.timeout(options.getTimeout())
			.fullAuto(options.isFullAuto())
			.skipGitCheck(options.isSkipGitCheck());

		if (!options.isFullAuto()) {
			if (options.getSandboxMode() != null) {
				builder.sandboxMode(options.getSandboxMode());
			}
			if (options.getApprovalPolicy() != null) {
				builder.approvalPolicy(options.getApprovalPolicy());
			}
		}

		return builder.build();
	}

	private AgentResponse toAgentResponse(ExecuteResult result) {
		// Create generation with output
		AgentGeneration generation = new AgentGeneration(result.getOutput(),
				new AgentGenerationMetadata(result.getModel(),
						Map.of("exitCode", result.getExitCode(), "sessionId",
								result.getSessionId() != null ? result.getSessionId() : "", "activityLog",
								result.getActivityLog() != null ? result.getActivityLog() : "")));

		// Create response metadata with sessionId
		AgentResponseMetadata metadata = new AgentResponseMetadata(result.getModel(), result.getDuration(),
				result.getSessionId() != null ? result.getSessionId() : "",
				Map.of("exitCode", result.getExitCode(), "successful", result.isSuccessful(), "activityLog",
						result.getActivityLog() != null ? result.getActivityLog() : ""));

		return new AgentResponse(List.of(generation), metadata);
	}

}

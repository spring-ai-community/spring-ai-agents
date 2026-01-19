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

package org.springaicommunity.agents.amp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.agents.amp.AmpAgentOptions;
import org.springaicommunity.agents.ampsdk.AmpClient;
import org.springaicommunity.agents.ampsdk.types.ExecuteOptions;
import org.springaicommunity.agents.ampsdk.types.ExecuteResult;
import org.springaicommunity.agents.model.*;
import org.springaicommunity.sandbox.Sandbox;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Implementation of {@link AgentModel} for Sourcegraph Amp CLI-based agents.
 *
 * <p>
 * This adapter bridges Spring AI's agent abstraction with the Amp CLI, providing
 * autonomous development tasks through goal-driven task execution.
 * </p>
 *
 * @author Spring AI Community
 * @since 0.1.0
 */
public class AmpAgentModel implements AgentModel {

	private static final Logger logger = LoggerFactory.getLogger(AmpAgentModel.class);

	private final AmpClient ampClient;

	private final AmpAgentOptions defaultOptions;

	private final Sandbox sandbox;

	/**
	 * Create a new AmpAgentModel with the given client, options, and sandbox.
	 * @param ampClient the Amp CLI client
	 * @param defaultOptions default execution options
	 * @param sandbox the sandbox for secure command execution (may be null)
	 */
	public AmpAgentModel(AmpClient ampClient, AmpAgentOptions defaultOptions, Sandbox sandbox) {
		this.ampClient = ampClient;
		this.defaultOptions = defaultOptions != null ? defaultOptions : new AmpAgentOptions();
		this.sandbox = sandbox;

		// Set system property for executable path if provided
		if (this.defaultOptions.getExecutablePath() != null) {
			System.setProperty("AMP_CLI_PATH", this.defaultOptions.getExecutablePath());
		}
	}

	@Override
	public AgentResponse call(AgentTaskRequest request) {
		// Extract goal/prompt from request
		String goal = request.goal();
		logger.info("Executing Amp agent with goal: {}", goal);

		// Merge options
		AmpAgentOptions options = mergeOptions(request);

		// Convert to ExecuteOptions
		ExecuteOptions executeOptions = toExecuteOptions(options);

		// Execute via SDK
		ExecuteResult result = ampClient.execute(goal, executeOptions);

		// Convert to AgentResponse
		return toAgentResponse(result);
	}

	@Override
	public boolean isAvailable() {
		try {
			return ampClient.isAvailable();
		}
		catch (Exception e) {
			logger.warn("Amp CLI availability check failed: {}", e.getMessage());
			return false;
		}
	}

	private AmpAgentOptions mergeOptions(AgentTaskRequest request) {
		// Start with default options
		AmpAgentOptions merged = new AmpAgentOptions();
		merged.setModel(defaultOptions.getModel());
		merged.setTimeout(defaultOptions.getTimeout());
		merged.setDangerouslyAllowAll(defaultOptions.isDangerouslyAllowAll());
		merged.setExecutablePath(defaultOptions.getExecutablePath());

		// Override with request-specific options if present
		if (request.options() != null && request.options() instanceof AmpAgentOptions requestOptions) {
			if (requestOptions.getModel() != null) {
				merged.setModel(requestOptions.getModel());
			}
			if (requestOptions.getTimeout() != null) {
				merged.setTimeout(requestOptions.getTimeout());
			}
			merged.setDangerouslyAllowAll(requestOptions.isDangerouslyAllowAll());
		}

		return merged;
	}

	private ExecuteOptions toExecuteOptions(AmpAgentOptions options) {
		ExecuteOptions.Builder builder = ExecuteOptions.builder()
			.dangerouslyAllowAll(options.isDangerouslyAllowAll())
			.timeout(options.getTimeout());

		if (options.getModel() != null) {
			builder.model(options.getModel());
		}

		return builder.build();
	}

	private AgentResponse toAgentResponse(ExecuteResult result) {
		// Create generation with output
		AgentGeneration generation = new AgentGeneration(result.getOutput(),
				new AgentGenerationMetadata(result.getModel(), Map.of("exitCode", result.getExitCode())));

		// Create response metadata with sessionId (empty for Amp)
		AgentResponseMetadata metadata = new AgentResponseMetadata(result.getModel(), result.getDuration(), "",
				Map.of("exitCode", result.getExitCode(), "successful", result.isSuccessful()));

		return new AgentResponse(List.of(generation), metadata);
	}

}

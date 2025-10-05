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

package org.springaicommunity.agents.amazonq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.agents.amazonqsdk.AmazonQClient;
import org.springaicommunity.agents.amazonqsdk.types.ExecuteOptions;
import org.springaicommunity.agents.amazonqsdk.types.ExecuteResult;
import org.springaicommunity.agents.model.*;
import org.springaicommunity.agents.model.sandbox.Sandbox;

import java.util.List;
import java.util.Map;

/**
 * Implementation of {@link AgentModel} for Amazon Q Developer CLI-based agents.
 *
 * <p>
 * This adapter bridges Spring AI's agent abstraction with the Amazon Q CLI, providing
 * autonomous development tasks through goal-driven task execution.
 * </p>
 *
 * @author Spring AI Community
 * @since 0.1.0
 */
public class AmazonQAgentModel implements AgentModel {

	private static final Logger logger = LoggerFactory.getLogger(AmazonQAgentModel.class);

	private final AmazonQClient amazonQClient;

	private final AmazonQAgentOptions defaultOptions;

	private final Sandbox sandbox;

	/**
	 * Create a new AmazonQAgentModel with the given client, options, and sandbox.
	 * @param amazonQClient the Amazon Q CLI client
	 * @param defaultOptions default execution options
	 * @param sandbox the sandbox for secure command execution (may be null)
	 */
	public AmazonQAgentModel(AmazonQClient amazonQClient, AmazonQAgentOptions defaultOptions, Sandbox sandbox) {
		this.amazonQClient = amazonQClient;
		this.defaultOptions = defaultOptions != null ? defaultOptions : AmazonQAgentOptions.builder().build();
		this.sandbox = sandbox;

		// Set system property for executable path if provided
		if (this.defaultOptions.getExecutablePath() != null) {
			System.setProperty("Q_CLI_PATH", this.defaultOptions.getExecutablePath());
		}
	}

	@Override
	public AgentResponse call(AgentTaskRequest request) {
		// Extract goal/prompt from request
		String goal = request.goal();
		logger.info("Executing Amazon Q agent with goal: {}", goal);

		// Merge options
		AmazonQAgentOptions options = mergeOptions(request);

		// Convert to ExecuteOptions
		ExecuteOptions executeOptions = toExecuteOptions(options);

		// Execute via SDK
		ExecuteResult result = amazonQClient.execute(goal, executeOptions);

		// Convert to AgentResponse
		return toAgentResponse(result);
	}

	@Override
	public boolean isAvailable() {
		try {
			return amazonQClient.isAvailable();
		}
		catch (Exception e) {
			logger.warn("Amazon Q CLI availability check failed: {}", e.getMessage());
			return false;
		}
	}

	private AmazonQAgentOptions mergeOptions(AgentTaskRequest request) {
		// Start with defaults
		AmazonQAgentOptions.Builder builder = AmazonQAgentOptions.builder()
			.model(defaultOptions.getModel())
			.timeout(defaultOptions.getTimeout())
			.trustAllTools(defaultOptions.isTrustAllTools())
			.trustTools(defaultOptions.getTrustTools())
			.agent(defaultOptions.getAgent())
			.verbose(defaultOptions.isVerbose())
			.executablePath(defaultOptions.getExecutablePath());

		// Override with request-specific options if present
		if (request.options() != null && request.options() instanceof AmazonQAgentOptions requestOptions) {
			if (requestOptions.getModel() != null) {
				builder.model(requestOptions.getModel());
			}
			if (requestOptions.getTimeout() != null) {
				builder.timeout(requestOptions.getTimeout());
			}
			builder.trustAllTools(requestOptions.isTrustAllTools());
			if (requestOptions.getTrustTools() != null && !requestOptions.getTrustTools().isEmpty()) {
				builder.trustTools(requestOptions.getTrustTools());
			}
			if (requestOptions.getAgent() != null) {
				builder.agent(requestOptions.getAgent());
			}
			builder.verbose(requestOptions.isVerbose());
		}

		return builder.build();
	}

	private ExecuteOptions toExecuteOptions(AmazonQAgentOptions options) {
		return ExecuteOptions.builder()
			.model(options.getModel())
			.timeout(options.getTimeout())
			.trustAllTools(options.isTrustAllTools())
			.trustTools(options.getTrustTools())
			.agent(options.getAgent())
			.verbose(options.isVerbose())
			.noInteractive(true) // Always non-interactive for programmatic use
			.build();
	}

	private AgentResponse toAgentResponse(ExecuteResult result) {
		// Create generation with output
		AgentGeneration generation = new AgentGeneration(result.getOutput(),
				new AgentGenerationMetadata(result.getModel(), Map.of("exitCode", result.getExitCode(),
						"conversationId", result.getConversationId() != null ? result.getConversationId() : "")));

		// Create response metadata
		AgentResponseMetadata metadata = new AgentResponseMetadata(result.getModel(), result.getDuration(),
				result.getConversationId() != null ? result.getConversationId() : "",
				Map.of("exitCode", result.getExitCode(), "successful", result.isSuccessful()));

		return new AgentResponse(List.of(generation), metadata);
	}

}

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

package org.springaicommunity.agents.claude.sdk;

import org.springaicommunity.agents.claude.sdk.exceptions.ClaudeSDKException;
import org.springaicommunity.agents.claude.sdk.transport.CLIOptions;
import org.springaicommunity.agents.claude.sdk.transport.CLITransport;
import org.springaicommunity.agents.claude.sdk.types.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

/**
 * Main entry point for synchronous Claude Code queries. Corresponds to query() function
 * in Python SDK.
 */
public class Query {

	private static final Logger logger = LoggerFactory.getLogger(Query.class);

	/**
	 * Executes a synchronous query with default options.
	 */
	public static QueryResult execute(String prompt) throws ClaudeSDKException {
		return execute(prompt, CLIOptions.defaultOptions());
	}

	/**
	 * Executes a synchronous query with specified options.
	 */
	public static QueryResult execute(String prompt, CLIOptions options) throws ClaudeSDKException {
		return execute(prompt, options, Paths.get(System.getProperty("user.dir")));
	}

	/**
	 * Executes a synchronous query with specified options and working directory.
	 */
	public static QueryResult execute(String prompt, CLIOptions options, Path workingDirectory)
			throws ClaudeSDKException {

		logger.info("Executing query with prompt length: {}", prompt.length());

		try (CLITransport transport = new CLITransport(workingDirectory, options.getTimeout())) {
			// Validate CLI availability
			if (!transport.isAvailable()) {
				throw new ClaudeSDKException("Claude CLI is not available");
			}

			// Execute the query
			List<Message> messages = transport.executeQuery(prompt, options);

			// Process messages to build domain-rich result
			return buildQueryResult(messages, options);

		}
		catch (Exception e) {
			if (e instanceof ClaudeSDKException) {
				throw e;
			}
			throw new ClaudeSDKException("Failed to execute query", e);
		}
	}

	/**
	 * Builds a QueryResult from raw messages with domain-rich metadata.
	 */
	private static QueryResult buildQueryResult(List<Message> messages, CLIOptions options) {
		// Find the result message to extract metadata
		Optional<ResultMessage> resultMessage = messages.stream()
			.filter(m -> m instanceof ResultMessage)
			.map(m -> (ResultMessage) m)
			.findFirst();

		// Build metadata from result message
		Metadata metadata = resultMessage
			.map(rm -> rm.toMetadata(options.getModel() != null ? options.getModel() : "unknown"))
			.orElse(createDefaultMetadata(options));

		// Determine result status
		ResultStatus status = determineStatus(messages, resultMessage);

		return QueryResult.builder().messages(messages).metadata(metadata).status(status).build();
	}

	/**
	 * Creates default metadata when no result message is available.
	 */
	private static Metadata createDefaultMetadata(CLIOptions options) {
		return Metadata.builder()
			.model(options.getModel() != null ? options.getModel() : "unknown")
			.cost(Cost.builder()
				.inputTokenCost(0.0)
				.outputTokenCost(0.0)
				.inputTokens(0)
				.outputTokens(0)
				.model(options.getModel() != null ? options.getModel() : "unknown")
				.build())
			.usage(Usage.builder().inputTokens(0).outputTokens(0).thinkingTokens(0).build())
			.durationMs(0)
			.apiDurationMs(0)
			.sessionId("unknown")
			.numTurns(1)
			.build();
	}

	/**
	 * Determines the result status based on messages and result data.
	 */
	private static ResultStatus determineStatus(List<Message> messages, Optional<ResultMessage> resultMessage) {
		if (resultMessage.isPresent()) {
			ResultMessage rm = resultMessage.get();
			if (rm.isError()) {
				return ResultStatus.ERROR;
			}
		}

		// Check if we have any messages at all
		if (messages.isEmpty()) {
			return ResultStatus.ERROR;
		}

		// Check if we have at least one assistant message
		boolean hasAssistantMessage = messages.stream().anyMatch(m -> m instanceof AssistantMessage);

		return hasAssistantMessage ? ResultStatus.SUCCESS : ResultStatus.PARTIAL;
	}

}
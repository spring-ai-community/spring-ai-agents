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

import org.springaicommunity.agents.claude.sdk.config.OutputFormat;
import org.springaicommunity.agents.claude.sdk.exceptions.ClaudeSDKException;
import org.springaicommunity.agents.claude.sdk.transport.CLIOptions;
import org.springaicommunity.agents.claude.sdk.transport.CLITransport;
import org.springaicommunity.agents.claude.sdk.transport.ReactiveTransport;
import org.springaicommunity.agents.claude.sdk.types.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Claude CLI API for managing Claude Code CLI subprocess communication. Provides
 * low-level access to the Claude Code CLI with bidirectional streaming. This follows
 * Spring AI naming conventions (e.g., OpenAiApi, AnthropicApi).
 */
public class ClaudeAgentClient implements AutoCloseable {

	private static final Logger logger = LoggerFactory.getLogger(ClaudeAgentClient.class);

	private final CLITransport transport;

	private final ReactiveTransport reactiveTransport;

	private final CLIOptions defaultOptions;

	private volatile boolean connected = false;

	private ClaudeAgentClient(CLITransport transport, ReactiveTransport reactiveTransport, CLIOptions defaultOptions) {
		this.transport = transport;
		this.reactiveTransport = reactiveTransport;
		this.defaultOptions = defaultOptions;
	}

	/**
	 * Creates a new API client with default working directory and options.
	 * @return a new ClaudeAgentClient instance
	 * @throws ClaudeSDKException if client creation fails
	 */
	public static ClaudeAgentClient create() throws ClaudeSDKException {
		return create(CLIOptions.defaultOptions());
	}

	/**
	 * Creates a new API client with specified options.
	 * @param options the CLI options
	 * @return a new ClaudeAgentClient instance
	 * @throws ClaudeSDKException if client creation fails
	 */
	public static ClaudeAgentClient create(CLIOptions options) throws ClaudeSDKException {
		return create(options, Paths.get(System.getProperty("user.dir")));
	}

	/**
	 * Creates a new API client with specified options and working directory.
	 * @param options the CLI options
	 * @param workingDirectory the working directory path
	 * @return a new ClaudeAgentClient instance
	 * @throws ClaudeSDKException if client creation fails
	 */
	public static ClaudeAgentClient create(CLIOptions options, Path workingDirectory) throws ClaudeSDKException {
		return create(options, workingDirectory, null);
	}

	/**
	 * Creates a new API client with specified options, working directory, and CLI path.
	 * @param options the CLI options
	 * @param workingDirectory the working directory path
	 * @param claudePath the path to Claude CLI executable
	 * @return a new ClaudeAgentClient instance
	 * @throws ClaudeSDKException if client creation fails
	 */
	public static ClaudeAgentClient create(CLIOptions options, Path workingDirectory, String claudePath)
			throws ClaudeSDKException {
		CLITransport transport = new CLITransport(workingDirectory, options.getTimeout(), claudePath);
		ReactiveTransport reactiveTransport = new ReactiveTransport(workingDirectory, options.getTimeout(), claudePath);
		return new ClaudeAgentClient(transport, reactiveTransport, options);
	}

	/**
	 * Connects to Claude CLI and validates availability.
	 * @throws ClaudeSDKException if connection fails or CLI is not available
	 */
	public void connect() throws ClaudeSDKException {
		logger.info("Connecting to Claude CLI");

		if (!transport.isAvailable()) {
			throw new ClaudeSDKException("Claude CLI is not available");
		}

		String version = transport.getVersion();
		logger.info("Connected to Claude CLI version: {}", version);

		connected = true;
	}

	/**
	 * Builds the command line arguments for a query without executing it. This is useful
	 * for integrating with external execution environments.
	 * @param prompt the query prompt
	 * @return the command line arguments as a list
	 */
	public List<String> buildCommand(String prompt) {
		return buildCommand(prompt, defaultOptions);
	}

	/**
	 * Builds the command line arguments for a query with specified options without
	 * executing it. This is useful for integrating with external execution environments.
	 * @param prompt the query prompt
	 * @param options the CLI options
	 * @return the command line arguments as a list
	 */
	public List<String> buildCommand(String prompt, CLIOptions options) {
		// Delegate to transport's buildCommand method
		return transport.buildCommand(prompt, options);
	}

	/**
	 * Parses the output from external command execution into a QueryResult. This is
	 * useful for integrating with external execution environments.
	 * @param output the command output to parse
	 * @param options the CLI options used for the command
	 * @return the parsed query result
	 * @throws ClaudeSDKException if parsing fails
	 */
	public QueryResult parseResult(String output, CLIOptions options) throws ClaudeSDKException {
		// Parse the output into messages using the transport's parsing logic
		List<Message> messages = transport.parseOutput(output, options);
		return buildQueryResult(messages, options);
	}

	/**
	 * Executes a synchronous query and returns the complete result.
	 * @param prompt the query prompt
	 * @return the query result
	 * @throws ClaudeSDKException if query execution fails
	 */
	public QueryResult query(String prompt) throws ClaudeSDKException {
		return query(prompt, defaultOptions);
	}

	/**
	 * Executes a synchronous query with specified options.
	 * @param prompt the query prompt
	 * @param options the CLI options
	 * @return the query result
	 * @throws ClaudeSDKException if query execution fails
	 */
	public QueryResult query(String prompt, CLIOptions options) throws ClaudeSDKException {
		validateConnected();

		logger.info("Executing client query with prompt length: {}", prompt.length());

		List<Message> messages = transport.executeQuery(prompt, options);
		return buildQueryResult(messages, options);
	}

	/**
	 * Executes a streaming query with real-time message processing.
	 * @param prompt the query prompt
	 * @param messageHandler the message handler for streaming responses
	 * @throws ClaudeSDKException if streaming query execution fails
	 */
	public void queryStreaming(String prompt, Consumer<Message> messageHandler) throws ClaudeSDKException {
		// Override output format to STREAM_JSON for streaming queries
		CLIOptions streamingOptions = CLIOptions.builder()
			.model(defaultOptions.getModel())
			.systemPrompt(defaultOptions.getSystemPrompt())
			.maxTokens(defaultOptions.getMaxTokens())
			.timeout(defaultOptions.getTimeout())
			.allowedTools(defaultOptions.getAllowedTools())
			.disallowedTools(defaultOptions.getDisallowedTools())
			.permissionMode(defaultOptions.getPermissionMode())
			.interactive(defaultOptions.isInteractive())
			.outputFormat(OutputFormat.STREAM_JSON) // Force streaming format
			.build();
		queryStreaming(prompt, streamingOptions, messageHandler);
	}

	/**
	 * Executes a streaming query with specified options and real-time message processing.
	 * @param prompt the query prompt
	 * @param options the CLI options
	 * @param messageHandler the message handler for streaming responses
	 * @throws ClaudeSDKException if streaming query execution fails
	 */
	public void queryStreaming(String prompt, CLIOptions options, Consumer<Message> messageHandler)
			throws ClaudeSDKException {
		validateConnected();

		logger.info("Executing streaming client query with prompt length: {}", prompt.length());

		transport.executeStreamingQuery(prompt, options, messageHandler);
	}

	/**
	 * Executes an async query returning a Mono&lt;QueryResult&gt;. True reactive
	 * implementation with non-blocking I/O.
	 * @param prompt the query prompt
	 * @return a Mono emitting the query result
	 */
	public Mono<QueryResult> queryAsync(String prompt) {
		return queryAsync(prompt, defaultOptions);
	}

	/**
	 * Executes an async query with specified options returning a Mono&lt;QueryResult&gt;.
	 * @param prompt the query prompt
	 * @param options the CLI options
	 * @return a Mono emitting the query result
	 */
	public Mono<QueryResult> queryAsync(String prompt, CLIOptions options) {
		return Mono.fromRunnable(() -> {
			try {
				validateConnected();
			}
			catch (ClaudeSDKException e) {
				throw new RuntimeException(e);
			}
		})
			.then(reactiveTransport.executeReactiveQueryComplete(prompt, options))
			.map(messages -> buildQueryResult(messages, options))
			.doOnSuccess(result -> logger.info("Async query completed with {} messages", result.messages().size()));
	}

	/**
	 * Executes an async streaming query returning a Flux&lt;Message&gt;. True reactive
	 * streaming with backpressure support.
	 * @param prompt the query prompt
	 * @return a Flux emitting streaming messages
	 */
	public Flux<Message> queryStreamAsync(String prompt) {
		return queryStreamAsync(prompt, defaultOptions);
	}

	/**
	 * Executes an async streaming query with specified options returning a
	 * Flux&lt;Message&gt;.
	 * @param prompt the query prompt
	 * @param options the CLI options
	 * @return a Flux emitting streaming messages
	 */
	public Flux<Message> queryStreamAsync(String prompt, CLIOptions options) {
		return reactiveTransport.executeReactiveQuery(prompt, options).doOnSubscribe(subscription -> {
			try {
				validateConnected();
				logger.info("Starting reactive streaming query");
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		})
			.doOnComplete(() -> logger.info("Reactive streaming query completed"))
			.doOnError(error -> logger.error("Reactive streaming query failed", error));
	}

	/**
	 * Checks if the client is connected.
	 * @return true if connected and CLI is available
	 */
	public boolean isConnected() {
		return connected && transport.isAvailable();
	}

	/**
	 * Disconnects from Claude CLI.
	 */
	public void disconnect() {
		logger.info("Disconnecting from Claude CLI");
		connected = false;
	}

	@Override
	public void close() {
		disconnect();
		try {
			transport.close();
		}
		catch (Exception e) {
			logger.warn("Error closing transport", e);
		}
		try {
			reactiveTransport.close();
		}
		catch (Exception e) {
			logger.warn("Error closing reactive transport", e);
		}
	}

	private void validateConnected() throws ClaudeSDKException {
		if (!connected) {
			throw new ClaudeSDKException("Client is not connected. Call connect() first.");
		}
	}

	/**
	 * Builds a QueryResult from messages and options.
	 * @param messages the list of messages
	 * @param options the CLI options
	 * @return the built QueryResult
	 */
	private QueryResult buildQueryResult(List<Message> messages, CLIOptions options) {
		// Find the result message to extract rich metadata
		Optional<ResultMessage> resultMessage = messages.stream()
			.filter(m -> m instanceof ResultMessage)
			.map(m -> (ResultMessage) m)
			.findFirst();

		// Build metadata from result message (same logic as Query class)
		Metadata metadata = resultMessage
			.map(rm -> rm.toMetadata(options.getModel() != null ? options.getModel() : "unknown"))
			.orElse(createSimpleMetadata(options));

		// Determine result status (same logic as Query class)
		ResultStatus status = determineStatus(messages, resultMessage);

		return QueryResult.builder().messages(messages).metadata(metadata).status(status).build();
	}

	/**
	 * Determines the result status based on messages and result data.
	 */
	private ResultStatus determineStatus(List<Message> messages, Optional<ResultMessage> resultMessage) {
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

	/**
	 * Creates simple metadata when result message is not available.
	 * @param options the CLI options
	 * @return the created metadata
	 */
	private Metadata createSimpleMetadata(CLIOptions options) {
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
			.sessionId("client-session")
			.numTurns(1)
			.build();
	}

}
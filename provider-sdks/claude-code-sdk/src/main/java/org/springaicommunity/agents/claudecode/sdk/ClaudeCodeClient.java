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

package org.springaicommunity.agents.claudecode.sdk;

import org.springaicommunity.agents.claudecode.sdk.config.OutputFormat;
import org.springaicommunity.agents.claudecode.sdk.exceptions.ClaudeSDKException;
import org.springaicommunity.agents.claudecode.sdk.transport.CLIOptions;
import org.springaicommunity.agents.claudecode.sdk.transport.CLITransport;
import org.springaicommunity.agents.claudecode.sdk.transport.ReactiveTransport;
import org.springaicommunity.agents.claudecode.sdk.types.*;
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
public class ClaudeCodeClient implements AutoCloseable {

	private static final Logger logger = LoggerFactory.getLogger(ClaudeCodeClient.class);

	private final CLITransport transport;

	private final ReactiveTransport reactiveTransport;

	private final CLIOptions defaultOptions;

	private volatile boolean connected = false;

	private ClaudeCodeClient(CLITransport transport, ReactiveTransport reactiveTransport, CLIOptions defaultOptions) {
		this.transport = transport;
		this.reactiveTransport = reactiveTransport;
		this.defaultOptions = defaultOptions;
	}

	/**
	 * Creates a new API client with default working directory and options.
	 */
	public static ClaudeCodeClient create() throws ClaudeSDKException {
		return create(CLIOptions.defaultOptions());
	}

	/**
	 * Creates a new API client with specified options.
	 */
	public static ClaudeCodeClient create(CLIOptions options) throws ClaudeSDKException {
		return create(options, Paths.get(System.getProperty("user.dir")));
	}

	/**
	 * Creates a new API client with specified options and working directory.
	 */
	public static ClaudeCodeClient create(CLIOptions options, Path workingDirectory) throws ClaudeSDKException {
		return create(options, workingDirectory, null);
	}

	/**
	 * Creates a new API client with specified options, working directory, and CLI path.
	 */
	public static ClaudeCodeClient create(CLIOptions options, Path workingDirectory, String claudePath)
			throws ClaudeSDKException {
		CLITransport transport = new CLITransport(workingDirectory, options.getTimeout(), claudePath);
		ReactiveTransport reactiveTransport = new ReactiveTransport(workingDirectory, options.getTimeout(), claudePath);
		return new ClaudeCodeClient(transport, reactiveTransport, options);
	}

	/**
	 * Connects to Claude CLI and validates availability.
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
	 * Executes a synchronous query and returns the complete result.
	 */
	public QueryResult query(String prompt) throws ClaudeSDKException {
		return query(prompt, defaultOptions);
	}

	/**
	 * Executes a synchronous query with specified options.
	 */
	public QueryResult query(String prompt, CLIOptions options) throws ClaudeSDKException {
		validateConnected();

		logger.info("Executing client query with prompt length: {}", prompt.length());

		List<Message> messages = transport.executeQuery(prompt, options);
		return buildQueryResult(messages, options);
	}

	/**
	 * Executes a streaming query with real-time message processing.
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
	 */
	public void queryStreaming(String prompt, CLIOptions options, Consumer<Message> messageHandler)
			throws ClaudeSDKException {
		validateConnected();

		logger.info("Executing streaming client query with prompt length: {}", prompt.length());

		transport.executeStreamingQuery(prompt, options, messageHandler);
	}

	/**
	 * Executes an async query returning a Mono<QueryResult>. True reactive implementation
	 * with non-blocking I/O.
	 */
	public Mono<QueryResult> queryAsync(String prompt) {
		return queryAsync(prompt, defaultOptions);
	}

	/**
	 * Executes an async query with specified options returning a Mono<QueryResult>.
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
	 * Executes an async streaming query returning a Flux<Message>. True reactive
	 * streaming with backpressure support.
	 */
	public Flux<Message> queryStreamAsync(String prompt) {
		return queryStreamAsync(prompt, defaultOptions);
	}

	/**
	 * Executes an async streaming query with specified options returning a Flux<Message>.
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
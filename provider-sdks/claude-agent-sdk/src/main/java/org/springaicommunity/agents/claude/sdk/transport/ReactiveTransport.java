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

package org.springaicommunity.agents.claude.sdk.transport;

import org.springaicommunity.agents.claude.sdk.config.OutputFormat;
import org.springaicommunity.agents.claude.sdk.exceptions.*;
import org.springaicommunity.agents.claude.sdk.parsing.MessageParser;
import org.springaicommunity.agents.claude.sdk.resilience.ResilienceManager;
import org.springaicommunity.agents.claude.sdk.resilience.RetryConfiguration;
import org.springaicommunity.agents.claude.sdk.types.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.StartedProcess;
import org.zeroturnaround.exec.stream.LogOutputStream;
import org.zeroturnaround.exec.stream.slf4j.Slf4jStream;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Reactive wrapper for CLITransport providing true non-blocking streaming. Implements
 * Project Reactor patterns for real-time message processing.
 */
public class ReactiveTransport implements AutoCloseable {

	private static final Logger logger = LoggerFactory.getLogger(ReactiveTransport.class);

	private final CLITransport cliTransport;

	private final String claudeCommand;

	private final Path workingDirectory;

	private final MessageParser messageParser;

	private final Duration defaultTimeout;

	private final ResilienceManager resilienceManager;

	private final AtomicBoolean closed = new AtomicBoolean(false);

	public ReactiveTransport(Path workingDirectory) {
		this(workingDirectory, Duration.ofMinutes(2));
	}

	public ReactiveTransport(Path workingDirectory, Duration defaultTimeout) {
		this(workingDirectory, defaultTimeout, null);
	}

	public ReactiveTransport(Path workingDirectory, Duration defaultTimeout, String claudePath) {
		this.workingDirectory = workingDirectory;
		this.defaultTimeout = defaultTimeout;
		this.messageParser = new MessageParser();
		this.cliTransport = new CLITransport(workingDirectory, defaultTimeout);
		this.claudeCommand = claudePath != null ? claudePath : discoverClaudeCommand();
		this.resilienceManager = new ResilienceManager(RetryConfiguration.defaultNetwork(), defaultTimeout);
	}

	/**
	 * Executes a reactive query returning a Flux of messages as they arrive. True
	 * non-blocking implementation with backpressure support and resilience.
	 */
	public Flux<Message> executeReactiveQuery(String prompt, CLIOptions options) {
		return resilienceManager.executeResilientFlux("reactive-query",
				() -> executeReactiveQueryInternal(prompt, options));
	}

	/**
	 * Internal implementation of reactive query without resilience wrapper.
	 */
	private Flux<Message> executeReactiveQueryInternal(String prompt, CLIOptions options) {
		return Flux.<Message>create(sink -> {
			if (closed.get()) {
				sink.error(new ClaudeSDKException("Transport is closed"));
				return;
			}

			try {
				List<String> command = buildCommand(prompt, options);
				logger.info("🔍 CLAUDE CLI REACTIVE COMMAND: {}", command);

				// Create reactive message processor
				ReactiveMessageProcessor processor = new ReactiveMessageProcessor(sink);

				// Start process asynchronously
				StartedProcess process = new ProcessExecutor().command(command)
					.directory(workingDirectory.toFile())
					.environment("CLAUDE_CODE_ENTRYPOINT", "sdk-java")
					.redirectOutput(processor)
					.redirectError(Slf4jStream.of(getClass()).asError())
					.start();

				// Handle process completion
				CompletableFuture<Void> completion = CompletableFuture.runAsync(() -> {
					try {
						process.getFuture().get(options.getTimeout().toMillis(), TimeUnit.MILLISECONDS);
						int exitCode = process.getProcess().exitValue();

						if (exitCode == 0) {
							sink.complete();
						}
						else if (exitCode == 1 && processor.hasReceivedMessages()) {
							// Claude CLI has a bug where streaming operations return exit
							// code 1
							// even when successful. If we received messages, treat it as
							// success.
							logger.debug("Claude CLI returned exit code 1 but received messages - treating as success");
							sink.complete();
						}
						else {
							sink.error(ProcessExecutionException.withExitCode("Claude CLI process failed", exitCode));
						}
					}
					catch (Exception e) {
						sink.error(new ProcessExecutionException("Process execution failed", e));
					}
				});

				// Handle cancellation
				sink.onCancel(() -> {
					logger.debug("Reactive query cancelled, terminating process");
					try {
						process.getProcess().destroyForcibly();
						completion.cancel(true);
					}
					catch (Exception e) {
						logger.warn("Error cancelling process", e);
					}
				});

			}
			catch (Exception e) {
				sink.error(new ProcessExecutionException("Failed to start reactive query", e));
			}
		})
			.doOnSubscribe(subscription -> logger.info("Starting reactive query"))
			.doOnComplete(() -> logger.info("Reactive query completed"))
			.doOnError(error -> logger.error("Reactive query failed", error))
			.subscribeOn(Schedulers.boundedElastic());
	}

	/**
	 * Executes a reactive query returning a Mono with the complete result. Collects all
	 * messages and builds a QueryResult with resilience.
	 */
	public Mono<List<Message>> executeReactiveQueryComplete(String prompt, CLIOptions options) {
		return resilienceManager.executeResilientMono("complete-query",
				() -> executeReactiveQuery(prompt, options).collectList()
					.timeout(options.getTimeout())
					.doOnSuccess(messages -> logger.info("Collected {} messages", messages.size())));
	}

	/**
	 * Tests if Claude CLI is available reactively with resilience.
	 */
	public Mono<Boolean> isAvailableAsync() {
		return resilienceManager
			.executeResilientMono("availability-check",
					() -> Mono.fromCallable(() -> cliTransport.isAvailable()).subscribeOn(Schedulers.boundedElastic()))
			.onErrorReturn(false);
	}

	/**
	 * Gets the Claude CLI version reactively with resilience.
	 */
	public Mono<String> getVersionAsync() {
		return resilienceManager.executeResilientMono("version-check",
				() -> Mono.fromCallable(() -> cliTransport.getVersion()).subscribeOn(Schedulers.boundedElastic()));
	}

	/**
	 * Gets resilience manager metrics for monitoring.
	 */
	public ResilienceManager getResilienceManager() {
		return resilienceManager;
	}

	private List<String> buildCommand(String prompt, CLIOptions options) {
		List<String> command = new ArrayList<>();
		command.add(claudeCommand);
		command.add("--print"); // Essential for autonomous operations - enables
								// programmatic execution

		// For reactive transport, prefer streaming formats but respect user choice
		OutputFormat format = options.getOutputFormat();
		if (format == OutputFormat.TEXT) {
			// TEXT format doesn't work well with reactive streams, warn and use
			// STREAM_JSON
			logger.warn("TEXT format not recommended for reactive streams, using STREAM_JSON instead");
			format = OutputFormat.STREAM_JSON;
		}

		command.add("--output-format");
		command.add(format.getValue());

		// Add --verbose flag for streaming formats
		if (format == OutputFormat.STREAM_JSON) {
			command.add("--verbose"); // Critical: Required with stream-json
		}

		// Add options
		if (options.getModel() != null) {
			command.add("--model");
			command.add(options.getModel());
		}

		if (options.getSystemPrompt() != null) {
			command.add("--append-system-prompt");
			command.add(options.getSystemPrompt());
		}

		// Note: Claude CLI doesn't support --max-tokens option
		// Token limits are controlled by the model configuration

		// Add tool configuration
		if (!options.getAllowedTools().isEmpty()) {
			command.add("--allowed-tools");
			command.add(String.join(",", options.getAllowedTools()));
		}

		if (!options.getDisallowedTools().isEmpty()) {
			command.add("--disallowed-tools");
			command.add(String.join(",", options.getDisallowedTools()));
		}

		// Add permission mode
		if (options.getPermissionMode() != null && options
			.getPermissionMode() != org.springaicommunity.agents.claude.sdk.config.PermissionMode.DEFAULT) {
			command.add("--permission-mode");
			command.add(options.getPermissionMode().getValue());
		}

		// Add interactive mode
		if (options.isInteractive()) {
			command.add("--interactive");
		}

		// Add the prompt
		command.add("--print");
		command.add(prompt);

		return command;
	}

	private String discoverClaudeCommand() {
		String[] candidates = { "claude", "claude-code", System.getProperty("user.home") + "/.local/bin/claude" };

		for (String candidate : candidates) {
			try {
				ProcessResult result = new ProcessExecutor().command(candidate, "--version")
					.timeout(5, TimeUnit.SECONDS)
					.readOutput(true)
					.execute();

				if (result.getExitValue() == 0) {
					logger.info("Found Claude CLI at: {}", candidate);
					return candidate;
				}
			}
			catch (Exception e) {
				logger.debug("Claude CLI not found at: {}", candidate);
			}
		}

		logger.warn("Claude CLI not found, using default 'claude'");
		return "claude";
	}

	@Override
	public void close() {
		if (!closed.getAndSet(true)) {
			try {
				cliTransport.close();
			}
			catch (Exception e) {
				logger.warn("Error closing underlying transport", e);
			}
			logger.debug("ReactiveTransport closed");
		}
	}

	/**
	 * Reactive message processor for streaming responses. Provides backpressure and error
	 * recovery.
	 */
	private class ReactiveMessageProcessor extends LogOutputStream {

		private final FluxSink<Message> sink;

		private final StringBuilder buffer = new StringBuilder();

		private final AtomicBoolean receivedMessages = new AtomicBoolean(false);

		public ReactiveMessageProcessor(FluxSink<Message> sink) {
			this.sink = sink;
		}

		public boolean hasReceivedMessages() {
			return receivedMessages.get();
		}

		@Override
		protected void processLine(String line) {
			try {
				if (sink.isCancelled()) {
					return; // Stop processing if cancelled
				}

				if (line.trim().startsWith("{") && line.trim().endsWith("}")) {
					// Complete JSON line
					Message message = parseMessage(line);
					if (message != null) {
						receivedMessages.set(true);
						sink.next(message);
					}
				}
				else {
					// Buffer partial JSON
					buffer.append(line).append("\n");

					// Try to parse buffered content
					String buffered = buffer.toString().trim();
					if (buffered.startsWith("{") && buffered.endsWith("}")) {
						Message message = parseMessage(buffered);
						if (message != null) {
							receivedMessages.set(true);
							sink.next(message);
							buffer.setLength(0); // Clear buffer
						}
					}

					// Prevent buffer overflow
					if (buffer.length() > 64 * 1024) { // 64KB limit
						logger.warn("JSON buffer overflow, clearing buffer");
						buffer.setLength(0);
					}
				}
			}
			catch (Exception e) {
				logger.warn("Failed to process reactive line: {}", line, e);
				// Continue processing - don't fail the entire stream for one bad message
			}
		}

		private Message parseMessage(String json) {
			try {
				return messageParser.parseMessage(json);
			}
			catch (Exception e) {
				logger.warn("Failed to parse reactive JSON: {}", json.substring(0, Math.min(100, json.length())), e);
				return null;
			}
		}

	}

}
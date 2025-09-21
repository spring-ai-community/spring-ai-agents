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

package org.springaicommunity.agents.claudecode.sdk.transport;

import org.springaicommunity.agents.claudecode.sdk.config.ClaudeCliDiscovery;
import org.springaicommunity.agents.claudecode.sdk.config.OutputFormat;
import org.springaicommunity.agents.claudecode.sdk.exceptions.*;
import org.springaicommunity.agents.claudecode.sdk.parsing.JsonResultParser;
import org.springaicommunity.agents.claudecode.sdk.parsing.MessageParser;
import org.springaicommunity.agents.claudecode.sdk.types.AssistantMessage;
import org.springaicommunity.agents.claudecode.sdk.types.Message;
import org.springaicommunity.agents.claudecode.sdk.types.ResultMessage;
import org.springaicommunity.agents.claudecode.sdk.types.TextBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.stream.LogOutputStream;
import org.zeroturnaround.exec.stream.slf4j.Slf4jStream;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Production-ready Claude CLI transport layer using zt-exec ProcessExecutor. Handles
 * process lifecycle, streaming output, and error recovery.
 */
public class CLITransport implements AutoCloseable {

	private static final Logger logger = LoggerFactory.getLogger(CLITransport.class);

	private final String claudeCommand;

	private final Path workingDirectory;

	private final MessageParser messageParser;

	private final JsonResultParser jsonResultParser;

	private final Duration defaultTimeout;

	public CLITransport(Path workingDirectory) {
		this(workingDirectory, Duration.ofMinutes(2));
	}

	public CLITransport(Path workingDirectory, Duration defaultTimeout) {
		this(workingDirectory, defaultTimeout, null);
	}

	public CLITransport(Path workingDirectory, Duration defaultTimeout, String claudePath) {
		this.workingDirectory = workingDirectory;
		this.defaultTimeout = defaultTimeout;
		this.messageParser = new MessageParser();
		this.jsonResultParser = new JsonResultParser();
		this.claudeCommand = claudePath != null ? claudePath : discoverClaudePathFallback();
	}

	private String discoverClaudePathFallback() {
		try {
			return ClaudeCliDiscovery.discoverClaudePath();
		}
		catch (Exception e) {
			logger.warn("Could not discover Claude CLI path, using hardcoded fallback", e);
			return "/home/mark/.nvm/versions/node/v22.15.0/bin/claude";
		}
	}

	/**
	 * Executes a synchronous query and returns all messages.
	 */
	public List<Message> executeQuery(String prompt, CLIOptions options) throws ClaudeSDKException {
		List<Message> messages = new ArrayList<>();

		try {
			// Build command arguments
			List<String> command = buildCommand(prompt, options);

			// Remove debug flag now that authentication is working

			// Handle different output formats
			if (options.getOutputFormat() == OutputFormat.JSON) {
				// For JSON format, get the complete output as a string
				ProcessExecutor executor = new ProcessExecutor().command(command)
					.directory(workingDirectory.toFile())
					.environment("CLAUDE_CODE_ENTRYPOINT", "sdk-java")
					.timeout(options.getTimeout().toMillis(), TimeUnit.MILLISECONDS)
					.redirectError(Slf4jStream.of(getClass()).asError())
					.readOutput(true);

				// Pass through ANTHROPIC_API_KEY if set
				String apiKey = System.getenv("ANTHROPIC_API_KEY");
				if (apiKey != null) {
					logger.info("Setting ANTHROPIC_API_KEY environment variable for Claude CLI (key present: {})",
							apiKey.length() > 0);
					executor.environment("ANTHROPIC_API_KEY", apiKey);
				}
				else {
					logger.warn("ANTHROPIC_API_KEY environment variable not found - Claude CLI may fail");
				}

				ProcessResult result = executor.execute();

				// Check exit code and provide detailed error information
				if (result.getExitValue() != 0) {
					logger.error("Claude CLI command failed with exit code {}", result.getExitValue());
					logger.error("Command executed: {}", command);
					logger.error("Working directory: {}", workingDirectory);
					logger.error("Stdout output: {}", result.outputUTF8());
					throw ProcessExecutionException.withExitCode("Claude CLI process failed", result.getExitValue());
				}

				// Parse the complete JSON result with retry logic for empty responses
				String jsonOutput = result.outputUTF8().trim();
				if (!jsonOutput.isEmpty()) {
					ResultMessage resultMessage = jsonResultParser.parseJsonResult(jsonOutput);
					messages.add(resultMessage);

					// Create AssistantMessage from the result content
					if (resultMessage.result() != null && !resultMessage.result().isEmpty()) {
						AssistantMessage assistantMessage = new AssistantMessage(
								List.of(new TextBlock(resultMessage.result())));
						messages.add(assistantMessage);
					}
				}
				else {
					// EMPTY RESPONSE BUG: Claude CLI returned empty output despite
					// successful execution
					// This is a known intermittent issue where the CLI process completes
					// successfully
					// but the output buffer is empty, even though tokens are charged
					logger.warn("Empty response detected from Claude CLI - this may be a CLI buffering issue");

					// For now, we'll let the caller handle retries at the application
					// level
					// Future enhancement: Implement automatic retry logic here in the SDK
					// with exponential backoff and configurable max attempts
				}
			}
			else {
				// For streaming formats (STREAM_JSON, TEXT), use the collector
				MessageCollector collector = new MessageCollector(messages::add, options.getOutputFormat());

				ProcessExecutor executor = new ProcessExecutor().command(command)
					.directory(workingDirectory.toFile())
					.environment("CLAUDE_CODE_ENTRYPOINT", "sdk-java")
					.timeout(options.getTimeout().toMillis(), TimeUnit.MILLISECONDS)
					.redirectOutput(collector)
					.redirectError(Slf4jStream.of(getClass()).asError())
					.readOutput(true);

				// Pass through ANTHROPIC_API_KEY if set
				String apiKey = System.getenv("ANTHROPIC_API_KEY");
				if (apiKey != null) {
					logger.info("Setting ANTHROPIC_API_KEY environment variable for Claude CLI (key present: {})",
							apiKey.length() > 0);
					executor.environment("ANTHROPIC_API_KEY", apiKey);
				}
				else {
					logger.warn("ANTHROPIC_API_KEY environment variable not found - Claude CLI may fail");
				}

				ProcessResult result = executor.execute();

				// Check exit code and provide detailed error information
				if (result.getExitValue() != 0) {
					logger.error("Claude CLI streaming command failed with exit code {}", result.getExitValue());
					logger.error("Command executed: {}", command);
					logger.error("Working directory: {}", workingDirectory);
					logger.error("Stdout output: {}", result.outputUTF8());
					throw ProcessExecutionException.withExitCode("Claude CLI streaming process failed",
							result.getExitValue());
				}

				// Manually close collector to flush any buffered content (important for
				// TEXT format)
				try {
					collector.close();
				}
				catch (IOException e) {
					logger.warn("Failed to close message collector", e);
				}
			}

			logger.info("Query completed successfully with {} messages", messages.size());
			return messages;

		}
		catch (ClaudeSDKException e) {
			throw e;
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new ProcessExecutionException("Query was interrupted", e);
		}
		catch (IOException | TimeoutException e) {
			throw new ProcessExecutionException("Failed to execute query", e);
		}
		catch (Exception e) {
			throw new ProcessExecutionException("Unexpected error during query", e);
		}
	}

	/**
	 * Executes a streaming query with real-time message processing.
	 */
	public void executeStreamingQuery(String prompt, CLIOptions options, Consumer<Message> messageHandler)
			throws ClaudeSDKException {
		try {
			// Build command arguments
			List<String> command = buildCommand(prompt, options);

			// Remove debug flag now that authentication is working

			// Create streaming message processor
			StreamingMessageProcessor processor = new StreamingMessageProcessor(messageHandler,
					options.getOutputFormat());

			// Execute process
			ProcessExecutor executor = new ProcessExecutor().command(command)
				.directory(workingDirectory.toFile())
				.environment("CLAUDE_CODE_ENTRYPOINT", "sdk-java")
				.timeout(options.getTimeout().toMillis(), TimeUnit.MILLISECONDS)
				.redirectOutput(processor)
				.redirectError(Slf4jStream.of(getClass()).asError())
				.readOutput(true);

			// Pass through ANTHROPIC_API_KEY if set
			String apiKey = System.getenv("ANTHROPIC_API_KEY");
			if (apiKey != null) {
				logger.warn("Setting ANTHROPIC_API_KEY environment variable for streaming Claude CLI (key present: {})",
						apiKey.length() > 0);
				executor.environment("ANTHROPIC_API_KEY", apiKey);
			}
			else {
				logger.warn("ANTHROPIC_API_KEY environment variable not found for streaming - Claude CLI may fail");
			}

			ProcessResult result = executor.execute();

			// Check exit code and provide detailed error information
			if (result.getExitValue() != 0) {
				logger.error("Claude CLI streaming query failed with exit code {}", result.getExitValue());
				logger.error("Command executed: {}", command);
				logger.error("Working directory: {}", workingDirectory);
				logger.error("Stdout output: {}", result.outputUTF8());
				throw ProcessExecutionException.withExitCode("Claude CLI streaming process failed",
						result.getExitValue());
			}

			logger.info("Streaming query completed successfully");

		}
		catch (ClaudeSDKException e) {
			throw e;
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new ProcessExecutionException("Streaming query was interrupted", e);
		}
		catch (IOException | TimeoutException e) {
			throw new ProcessExecutionException("Failed to execute streaming query", e);
		}
		catch (Exception e) {
			throw new ProcessExecutionException("Unexpected error during streaming query", e);
		}
	}

	/**
	 * Tests if Claude CLI is available and working.
	 */
	public boolean isAvailable() {
		try {
			ProcessResult result = new ProcessExecutor().command(claudeCommand, "--version")
				.timeout(5, TimeUnit.SECONDS)
				.readOutput(true)
				.execute();

			return result.getExitValue() == 0;
		}
		catch (Exception e) {
			logger.debug("Claude CLI availability check failed", e);
			return false;
		}
	}

	/**
	 * Gets the Claude CLI version.
	 */
	public String getVersion() throws ClaudeSDKException {
		try {
			ProcessResult result = new ProcessExecutor().command(claudeCommand, "--version")
				.timeout(5, TimeUnit.SECONDS)
				.readOutput(true)
				.execute();

			if (result.getExitValue() != 0) {
				throw new CLIConnectionException("Failed to get Claude CLI version");
			}

			return result.outputUTF8().trim();
		}
		catch (Exception e) {
			throw new CLIConnectionException("Failed to get Claude CLI version", e);
		}
	}

	/**
	 * Parses command output into Messages. This is useful for integrating with external
	 * execution environments.
	 * @param output the command output to parse
	 * @param options the CLI options used for the command
	 * @return the parsed messages
	 * @throws ClaudeSDKException if parsing fails
	 */
	public List<Message> parseOutput(String output, CLIOptions options) throws ClaudeSDKException {
		List<Message> messages = new ArrayList<>();

		try {
			// Use the same parsing logic as executeQuery
			if (options.getOutputFormat() == OutputFormat.JSON) {
				String jsonOutput = output.trim();
				if (!jsonOutput.isEmpty()) {
					ResultMessage resultMessage = jsonResultParser.parseJsonResult(jsonOutput);
					messages.add(resultMessage);

					// Create AssistantMessage from the result content
					if (resultMessage.result() != null && !resultMessage.result().isEmpty()) {
						AssistantMessage assistantMessage = new AssistantMessage(
								List.of(new TextBlock(resultMessage.result())));
						messages.add(assistantMessage);
					}
				}
			}
			else {
				// For STREAM_JSON format, parse line by line
				String[] lines = output.split("\n");
				for (String line : lines) {
					line = line.trim();
					if (!line.isEmpty()) {
						try {
							Message message = messageParser.parseMessage(line);
							if (message != null) {
								messages.add(message);
							}
						}
						catch (Exception e) {
							// Skip unparseable lines - they might be debug output
							logger.debug("Skipping unparseable line: {}", line);
						}
					}
				}
			}
		}
		catch (Exception e) {
			throw new ProcessExecutionException("Failed to parse command output: " + e.getMessage(), e);
		}

		return messages;
	}

	public List<String> buildCommand(String prompt, CLIOptions options) {
		List<String> command = new ArrayList<>();
		command.add(claudeCommand);
		command.add("--print"); // Essential for autonomous operations - enables
								// programmatic execution
		command.add("--output-format");
		command.add(options.getOutputFormat().getValue());

		// Add --verbose flag only for streaming JSON format
		if (options.getOutputFormat() == OutputFormat.STREAM_JSON) {
			command.add("--verbose"); // Required with stream-json
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
			.getPermissionMode() != org.springaicommunity.agents.claudecode.sdk.config.PermissionMode.DEFAULT) {
			if (options
				.getPermissionMode() == org.springaicommunity.agents.claudecode.sdk.config.PermissionMode.DANGEROUSLY_SKIP_PERMISSIONS) {
				command.add("--dangerously-skip-permissions");
			}
			else {
				command.add("--permission-mode");
				command.add(options.getPermissionMode().getValue());
			}
		}

		// Add interactive mode
		if (options.isInteractive()) {
			command.add("--interactive");
		}

		// Add the prompt using -- separator to prevent argument parsing issues
		command.add("--"); // Everything after this is positional arguments
		command.add(prompt);

		logger.info("🔍 CLAUDE CLI COMMAND: {}", command);
		return command;
	}

	private String discoverClaudeCommand() {
		// First check if a specific path was provided via system property
		String systemPath = System.getProperty("claude.cli.path");
		if (systemPath != null) {
			try {
				ProcessResult result = new ProcessExecutor().command(systemPath, "--version")
					.timeout(5, TimeUnit.SECONDS)
					.readOutput(true)
					.execute();

				if (result.getExitValue() == 0) {
					logger.info("Found Claude CLI at system property path: {}", systemPath);
					return systemPath;
				}
			}
			catch (Exception e) {
				logger.warn("Claude CLI not found at system property path: {}", systemPath, e);
			}
		}

		// Fall back to discovering in common locations
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
		// Cleanup resources if needed
		logger.debug("CLITransport closed");
	}

	/**
	 * Message collector for synchronous queries.
	 */
	private class MessageCollector extends LogOutputStream {

		private final Consumer<Message> messageHandler;

		private final OutputFormat outputFormat;

		private final StringBuilder buffer = new StringBuilder();

		public MessageCollector(Consumer<Message> messageHandler, OutputFormat outputFormat) {
			this.messageHandler = messageHandler;
			this.outputFormat = outputFormat;
		}

		@Override
		protected void processLine(String line) {
			try {
				if (outputFormat == OutputFormat.TEXT) {
					// For TEXT format, accumulate all lines as the result
					buffer.append(line).append("\n");
				}
				else if (outputFormat == OutputFormat.STREAM_JSON) {
					// For streaming JSON format, parse individual JSON objects
					if (line.trim().startsWith("{") && line.trim().endsWith("}")) {
						// Complete JSON line
						Message message = parseMessage(line);
						if (message != null) {
							messageHandler.accept(message);
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
								messageHandler.accept(message);
								buffer.setLength(0); // Clear buffer
							}
						}
					}
				}
			}
			catch (Exception e) {
				logger.warn("Failed to process line: {}", line, e);
			}
		}

		/**
		 * Called when the stream is closed. For TEXT format, create a result message from
		 * the accumulated buffer.
		 */
		@Override
		public void close() throws IOException {
			super.close();

			if (outputFormat == OutputFormat.TEXT && buffer.length() > 0) {
				// Create a simple result message for text output
				String textResult = buffer.toString().trim();
				if (!textResult.isEmpty()) {
					ResultMessage textMessage = ResultMessage.builder()
						.subtype("success")
						.result(textResult)
						.isError(false)
						.numTurns(1)
						.durationMs(0)
						.durationApiMs(0)
						.build();
					messageHandler.accept(textMessage);
				}
			}
		}

		private Message parseMessage(String json) {
			try {
				return messageParser.parseMessage(json);
			}
			catch (Exception e) {
				logger.warn("Failed to parse JSON: {}", json.substring(0, Math.min(100, json.length())), e);
				return null;
			}
		}

	}

	/**
	 * Streaming message processor for real-time queries.
	 */
	private class StreamingMessageProcessor extends LogOutputStream {

		private final Consumer<Message> messageHandler;

		private final OutputFormat outputFormat;

		private final StringBuilder buffer = new StringBuilder();

		public StreamingMessageProcessor(Consumer<Message> messageHandler, OutputFormat outputFormat) {
			this.messageHandler = messageHandler;
			this.outputFormat = outputFormat;
		}

		@Override
		protected void processLine(String line) {
			try {
				if (outputFormat == OutputFormat.TEXT) {
					// For TEXT format, accumulate all lines as the result
					buffer.append(line).append("\n");
				}
				else if (outputFormat == OutputFormat.STREAM_JSON) {
					// For streaming JSON format, parse individual JSON objects
					if (line.trim().startsWith("{") && line.trim().endsWith("}")) {
						Message message = parseMessage(line);
						if (message != null) {
							messageHandler.accept(message);
						}
					}
				}
			}
			catch (Exception e) {
				logger.warn("Failed to process streaming line: {}", line, e);
			}
		}

		/**
		 * Called when the stream is closed. For TEXT format, create a result message from
		 * the accumulated buffer.
		 */
		@Override
		public void close() throws IOException {
			super.close();

			if (outputFormat == OutputFormat.TEXT && buffer.length() > 0) {
				// Create a simple result message for text output
				String textResult = buffer.toString().trim();
				if (!textResult.isEmpty()) {
					ResultMessage textMessage = ResultMessage.builder()
						.subtype("success")
						.result(textResult)
						.isError(false)
						.numTurns(1)
						.durationMs(0)
						.durationApiMs(0)
						.build();
					messageHandler.accept(textMessage);
				}
			}
		}

		private Message parseMessage(String json) {
			try {
				return messageParser.parseMessage(json);
			}
			catch (Exception e) {
				logger.warn("Failed to parse streaming JSON: {}", json.substring(0, Math.min(100, json.length())), e);
				return null;
			}
		}

	}

}
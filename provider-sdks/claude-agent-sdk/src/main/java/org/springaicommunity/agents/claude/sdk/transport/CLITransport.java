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

import org.springaicommunity.agents.claude.sdk.config.ClaudeCliDiscovery;
import org.springaicommunity.agents.claude.sdk.config.OutputFormat;
import org.springaicommunity.agents.claude.sdk.exceptions.*;
import org.springaicommunity.agents.claude.sdk.mcp.McpServerConfig;
import org.springaicommunity.agents.claude.sdk.parsing.JsonResultParser;
import org.springaicommunity.agents.claude.sdk.parsing.MessageParser;
import org.springaicommunity.agents.claude.sdk.types.AssistantMessage;
import org.springaicommunity.agents.claude.sdk.types.Message;
import org.springaicommunity.agents.claude.sdk.types.ResultMessage;
import org.springaicommunity.agents.claude.sdk.types.TextBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.stream.LogOutputStream;
import org.zeroturnaround.exec.stream.slf4j.Slf4jStream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

	private final ObjectMapper objectMapper;

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
		this.objectMapper = new ObjectMapper();
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
					throw TransportException.withExitCode("Claude CLI process failed", result.getExitValue());
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

				// Pass through ANTHROPIC_BASE_URL if set
				if (options.baseUrl() != null && !options.baseUrl().isBlank()) {
					logger.info("Setting ANTHROPIC_BASE_URL environment variable for Claude CLI (base URL: {})",
							options.baseUrl());
					executor.environment("ANTHROPIC_BASE_URL", options.baseUrl());
				}

				// Pass through ANTHROPIC_API_KEY if set
				String apiKey;
				if (options.apiKey() != null && !options.apiKey().isBlank()) {
					apiKey = options.apiKey();
				}
				else {
					apiKey = System.getenv("ANTHROPIC_API_KEY");
				}
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
					throw TransportException.withExitCode("Claude CLI streaming process failed", result.getExitValue());
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
			throw new TransportException("Query was interrupted", e);
		}
		catch (IOException | TimeoutException e) {
			throw new TransportException("Failed to execute query", e);
		}
		catch (Exception e) {
			throw new TransportException("Unexpected error during query", e);
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
				throw TransportException.withExitCode("Claude CLI streaming process failed", result.getExitValue());
			}

			logger.info("Streaming query completed successfully");

		}
		catch (ClaudeSDKException e) {
			throw e;
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new TransportException("Streaming query was interrupted", e);
		}
		catch (IOException | TimeoutException e) {
			throw new TransportException("Failed to execute streaming query", e);
		}
		catch (Exception e) {
			throw new TransportException("Unexpected error during streaming query", e);
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
				throw new TransportException("Failed to get Claude CLI version");
			}

			return result.outputUTF8().trim();
		}
		catch (Exception e) {
			throw new TransportException("Failed to get Claude CLI version", e);
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
			throw new TransportException("Failed to parse command output: " + e.getMessage(), e);
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
			.getPermissionMode() != org.springaicommunity.agents.claude.sdk.config.PermissionMode.DEFAULT) {
			if (options
				.getPermissionMode() == org.springaicommunity.agents.claude.sdk.config.PermissionMode.DANGEROUSLY_SKIP_PERMISSIONS) {
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

		// Add setting sources (Claude Agent SDK v0.1.0)
		if (options.getSettingSources() != null && !options.getSettingSources().isEmpty()) {
			command.add("--setting-sources");
			command.add(String.join(",", options.getSettingSources()));
		}

		// Add agents JSON (Claude Agent SDK v0.1.0)
		if (options.getAgents() != null && !options.getAgents().trim().isEmpty()) {
			command.add("--agents");
			command.add(options.getAgents());
		}

		// Add fork session flag (Claude Agent SDK v0.1.0)
		if (options.isForkSession()) {
			command.add("--fork-session");
		}

		// Add include partial messages flag (Claude Agent SDK v0.1.0)
		if (options.isIncludePartialMessages()) {
			command.add("--include-partial-messages");
		}

		// Add max thinking tokens for extended thinking support
		if (options.getMaxThinkingTokens() != null) {
			command.add("--max-thinking-tokens");
			command.add(String.valueOf(options.getMaxThinkingTokens()));
		}

		// Add JSON schema for structured output support
		if (options.getJsonSchema() != null && !options.getJsonSchema().isEmpty()) {
			try {
				String schemaJson = objectMapper.writeValueAsString(options.getJsonSchema());
				command.add("--json-schema");
				command.add(schemaJson);
			}
			catch (JsonProcessingException e) {
				logger.warn("Failed to serialize JSON schema, skipping --json-schema flag", e);
			}
		}

		// Add MCP server configuration
		if (options.getMcpServers() != null && !options.getMcpServers().isEmpty()) {
			try {
				Map<String, Object> serversForCli = buildMcpConfigForCli(options.getMcpServers());
				if (!serversForCli.isEmpty()) {
					String mcpConfigJson = objectMapper.writeValueAsString(Map.of("mcpServers", serversForCli));
					command.add("--mcp-config");
					command.add(mcpConfigJson);
				}
			}
			catch (JsonProcessingException e) {
				logger.warn("Failed to serialize MCP config, skipping --mcp-config flag", e);
			}
		}

		// Add max turns for budget control
		if (options.getMaxTurns() != null) {
			command.add("--max-turns");
			command.add(String.valueOf(options.getMaxTurns()));
		}

		// Add max budget USD for cost control
		if (options.getMaxBudgetUsd() != null) {
			command.add("--max-budget-usd");
			command.add(String.valueOf(options.getMaxBudgetUsd()));
		}

		// Add fallback model
		if (options.getFallbackModel() != null && !options.getFallbackModel().isEmpty()) {
			command.add("--fallback-model");
			command.add(options.getFallbackModel());
		}

		// Add append system prompt (uses preset mode with append)
		if (options.getAppendSystemPrompt() != null && !options.getAppendSystemPrompt().isEmpty()) {
			command.add("--append-system-prompt");
			command.add(options.getAppendSystemPrompt());
		}

		// ============================================================
		// Advanced options for full Python SDK parity
		// ============================================================

		// Add directories (repeated flag)
		if (options.getAddDirs() != null && !options.getAddDirs().isEmpty()) {
			for (var dir : options.getAddDirs()) {
				command.add("--add-dir");
				command.add(dir.toString());
			}
		}

		// Custom settings file
		if (options.getSettings() != null && !options.getSettings().isBlank()) {
			command.add("--settings");
			command.add(options.getSettings());
		}

		// Permission prompt tool name
		if (options.getPermissionPromptToolName() != null && !options.getPermissionPromptToolName().isBlank()) {
			command.add("--permission-prompt-tool");
			command.add(options.getPermissionPromptToolName());
		}

		// Plugins (repeated flag)
		if (options.getPlugins() != null && !options.getPlugins().isEmpty()) {
			for (var plugin : options.getPlugins()) {
				command.add("--plugin-dir");
				command.add(plugin.path().toString());
			}
		}

		// Extra args (arbitrary flags - MUST BE LAST before prompt)
		if (options.getExtraArgs() != null && !options.getExtraArgs().isEmpty()) {
			for (var entry : options.getExtraArgs().entrySet()) {
				String flag = entry.getKey();
				String value = entry.getValue();
				if (value == null) {
					// Boolean flag (no value)
					command.add("--" + flag);
				}
				else {
					// Flag with value
					command.add("--" + flag);
					command.add(value);
				}
			}
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

	/**
	 * Builds the MCP config map for CLI serialization. SDK servers have their instances
	 * stripped (not serializable); only type and name are passed.
	 * @param mcpServers the MCP server configuration map
	 * @return map suitable for JSON serialization to CLI
	 */
	private Map<String, Object> buildMcpConfigForCli(Map<String, McpServerConfig> mcpServers) {
		Map<String, Object> serversForCli = new LinkedHashMap<>();
		for (Map.Entry<String, McpServerConfig> entry : mcpServers.entrySet()) {
			McpServerConfig config = entry.getValue();
			if (config instanceof McpServerConfig.McpSdkServerConfig sdk) {
				// SDK servers: pass type and name only, NOT the instance
				serversForCli.put(entry.getKey(), Map.of("type", "sdk", "name", sdk.name()));
			}
			else {
				// External servers (stdio, sse, http): pass as-is
				serversForCli.put(entry.getKey(), config);
			}
		}
		return serversForCli;
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